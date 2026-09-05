package com.alcolarm.feature.location

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.RiskPlaceId
import com.alcolarm.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Home monitoring: alert only when the user **stops** near a selected risk place
 * (still + nearby continuously for [DWELL_REQUIRED_MS]), not when merely passing by.
 *
 * Stillness: [STOP_SPEED_MPS] when Fused reports speed, else displacement under
 * [DISPLACEMENT_STILL_METERS] across the last [STILL_WINDOW_MS] of in-memory samples.
 * Search radius remains [RiskPlaceDetector.SEARCH_RADIUS_METERS] (~120 m).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: UserPreferencesRepository,
    private val locationTracker: LiveLocationTracker,
    private val detector: RiskPlaceDetector,
    private val alertBus: RiskAlertBus,
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserProfile(),
    )

    private val _monitoring = MutableStateFlow(
        HomeMonitoringUi(
            permissionGranted = locationTracker.hasLocationPermission(),
            statusMessage = if (locationTracker.hasLocationPermission()) {
                "Starting…"
            } else {
                "Permission needed"
            },
            uiState = if (locationTracker.hasLocationPermission()) {
                MonitoringUiState.WATCHING
            } else {
                MonitoringUiState.PERMISSION_NEEDED
            },
        ),
    )
    val monitoring: StateFlow<HomeMonitoringUi> = _monitoring.asStateFlow()

    private var monitorJob: Job? = null
    private var lastAlertDismissedAt = 0L

    /** Wall-clock ms spent continuously still + nearby; reset on move or leave radius. */
    private var stillNearMs = 0L
    private var lastDwellTickAt = 0L
    private var lastOverpassAt = 0L
    private var cachedMatch: NearbyRiskMatch? = null

    /**
     * Short in-memory ring of recent fused samples for still/dwell math only.
     * Never persisted; cleared in [stopMonitoring] / [onCleared].
     */
    private val sampleRing = ArrayDeque<LiveLocationSample>(SAMPLE_RING_CAPACITY)

    init {
        viewModelScope.launch {
            combine(
                locationTracker.isTracking,
                locationTracker.current.map { it != null }.distinctUntilChanged(),
                profile.map { it.riskPlaces }.distinctUntilChanged(),
            ) { tracking, _, risks ->
                Triple(tracking, locationTracker.hasLocationPermission(), risks)
            }.collectLatest { (tracking, permitted, risks) ->
                refreshStatusBanner(tracking, permitted, risks)
            }
        }
        viewModelScope.launch {
            alertBus.dismissed.collect {
                onAlertDismissed()
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _monitoring.update {
            it.copy(
                permissionGranted = granted,
                uiState = if (granted) MonitoringUiState.WATCHING else MonitoringUiState.PERMISSION_NEEDED,
                statusMessage = if (granted) {
                    "Watching nearby risk places via open map data…"
                } else {
                    "Permission needed"
                },
            )
        }
        if (granted) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }

    fun refreshPermissionFromSystem() {
        val granted = locationTracker.hasLocationPermission()
        _monitoring.update { it.copy(permissionGranted = granted) }
        if (granted && monitorJob == null) {
            startMonitoring()
        }
    }

    fun startMonitoring() {
        if (!locationTracker.hasLocationPermission()) {
            _monitoring.update {
                it.copy(
                    permissionGranted = false,
                    monitoringActive = false,
                    uiState = MonitoringUiState.PERMISSION_NEEDED,
                    statusMessage = "Permission needed",
                )
            }
            return
        }

        locationTracker.start()
        _monitoring.update {
            it.copy(
                permissionGranted = true,
                monitoringActive = true,
            )
        }

        if (monitorJob?.isActive == true) return
        monitorJob = viewModelScope.launch {
            while (isActive) {
                runDetectionCycle()
                delay(EVAL_INTERVAL_MS)
            }
        }
        Log.d(TAG, "Risk monitoring loop started (stop+dwell, OSM Overpass)")
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        locationTracker.stop()
        clearDwellState()
        sampleRing.clear()
        _monitoring.update {
            it.copy(monitoringActive = false)
        }
        Log.d(TAG, "Risk monitoring stopped; sample ring cleared")
    }

    /** Call when user dismisses the alert screen — debounce re-triggers. */
    fun onAlertDismissed() {
        lastAlertDismissedAt = System.currentTimeMillis()
        clearDwellState()
        _monitoring.update {
            it.copy(
                uiState = MonitoringUiState.WATCHING,
                statusMessage = "Watching nearby risk places via open map data…",
                lastMatchedPlaceName = null,
                lastMatchedRisk = null,
            )
        }
    }

    fun simulateAlert() {
        alertBus.tryEmit(
            RiskAlertEvent(
                riskPlaceId = RiskPlaceId.BAR,
                placeName = "Simulated place",
                simulated = true,
            ),
        )
    }

    override fun onCleared() {
        stopMonitoring()
        // Drop in-memory sample; never persisted.
        locationTracker.clearSample()
        super.onCleared()
    }

    private fun clearDwellState() {
        stillNearMs = 0L
        lastDwellTickAt = 0L
        cachedMatch = null
        lastOverpassAt = 0L
    }

    private fun pushSample(sample: LiveLocationSample) {
        sampleRing.addLast(sample)
        while (sampleRing.size > SAMPLE_RING_CAPACITY) {
            sampleRing.removeFirst()
        }
        val oldestAllowed = sample.timestampMillis - STILL_WINDOW_MS * 2
        while (sampleRing.isNotEmpty() && sampleRing.first().timestampMillis < oldestAllowed) {
            sampleRing.removeFirst()
        }
    }

    /**
     * Still when:
     * - latest sample has speed and speed < [STOP_SPEED_MPS], or
     * - no speed: displacement over the last ~[STILL_WINDOW_MS] is under [DISPLACEMENT_STILL_METERS].
     */
    private fun isStill(now: Long): Boolean {
        val latest = sampleRing.lastOrNull() ?: return false
        val speed = latest.speedMps
        if (speed != null) {
            return speed < STOP_SPEED_MPS
        }

        val windowStart = now - STILL_WINDOW_MS
        val inWindow = sampleRing.filter { it.timestampMillis >= windowStart }
        if (inWindow.size < 2) return false
        val oldest = inWindow.first()
        val newest = inWindow.last()
        val spanMs = newest.timestampMillis - oldest.timestampMillis
        // Need a meaningful slice of the stillness window before trusting displacement.
        if (spanMs < STILL_WINDOW_MS / 2) return false
        val displacement = haversineMeters(
            oldest.latitude,
            oldest.longitude,
            newest.latitude,
            newest.longitude,
        )
        return displacement < DISPLACEMENT_STILL_METERS
    }

    private suspend fun runDetectionCycle() {
        val permitted = locationTracker.hasLocationPermission()
        if (!permitted) {
            _monitoring.update {
                it.copy(
                    permissionGranted = false,
                    uiState = MonitoringUiState.PERMISSION_NEEDED,
                    statusMessage = "Permission needed",
                )
            }
            return
        }

        val risks = profile.value.riskPlaces
        if (OsmTagMapping.detectable(risks).isEmpty()) {
            clearDwellState()
            _monitoring.update {
                it.copy(
                    uiState = MonitoringUiState.NO_DETECTABLE_RISKS,
                    statusMessage = "Select bar / liquor store / supermarket / party to watch",
                )
            }
            return
        }

        val sample = locationTracker.current.value
        if (sample == null) {
            _monitoring.update {
                it.copy(
                    uiState = MonitoringUiState.WATCHING,
                    statusMessage = "Getting location…",
                )
            }
            return
        }

        val now = System.currentTimeMillis()
        pushSample(sample)
        val still = isStill(now)

        // Overpass only while still (pass-bys skip place queries).
        // Query immediately when still with no cache; otherwise every OVERPASS_INTERVAL_MS.
        var overpassError: String? = null
        val needsOverpass = still && (
            cachedMatch == null || now - lastOverpassAt >= OVERPASS_INTERVAL_MS
        )
        if (needsOverpass) {
            lastOverpassAt = now
            when (val result = detector.detectNearby(sample.latitude, sample.longitude, risks)) {
                is RiskDetectResult.Match -> {
                    cachedMatch = result.match
                }
                RiskDetectResult.None -> {
                    cachedMatch = null
                }
                is RiskDetectResult.Error -> {
                    overpassError = result.message
                    Log.w(TAG, "Detect error: ${result.message}")
                }
            }
        }

        if (!still) {
            // Moving / pass-by — reset dwell and drop nearby cache (no stale hit after leaving).
            stillNearMs = 0L
            lastDwellTickAt = 0L
            cachedMatch = null
            if (overpassError != null) {
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.CHECK_ERROR,
                        statusMessage = "Check failed — will retry",
                    )
                }
            } else {
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.WATCHING,
                        statusMessage = "Watching nearby risk places via open map data…",
                        lastMatchedPlaceName = null,
                        lastMatchedRisk = null,
                    )
                }
            }
            return
        }

        val match = cachedMatch
        if (match == null) {
            stillNearMs = 0L
            lastDwellTickAt = 0L
            if (overpassError != null) {
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.CHECK_ERROR,
                        statusMessage = "Check failed — will retry",
                    )
                }
            } else {
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.WATCHING,
                        statusMessage = "Watching nearby risk places via open map data…",
                        lastMatchedPlaceName = null,
                        lastMatchedRisk = null,
                    )
                }
            }
            return
        }

        // Still + nearby: accumulate continuous dwell time.
        if (lastDwellTickAt > 0L) {
            stillNearMs += now - lastDwellTickAt
        }
        lastDwellTickAt = now

        _monitoring.update {
            it.copy(
                uiState = MonitoringUiState.NEAR_RISK,
                statusMessage = "Near — confirming you’ve stopped…",
                lastMatchedPlaceName = match.placeName,
                lastMatchedRisk = match.riskPlaceId,
            )
        }

        val cooledDown = now - lastAlertDismissedAt >= ALERT_COOLDOWN_MS
        if (stillNearMs >= DWELL_REQUIRED_MS && cooledDown) {
            Log.d(TAG, "Dwell met stillNearMs=$stillNearMs — alerting")
            stillNearMs = 0L
            lastDwellTickAt = 0L
            lastAlertDismissedAt = now
            cachedMatch = null
            alertBus.emit(
                RiskAlertEvent(
                    riskPlaceId = match.riskPlaceId,
                    placeName = match.placeName,
                    simulated = false,
                ),
            )
        }
    }

    private fun refreshStatusBanner(
        tracking: Boolean,
        permitted: Boolean,
        risks: Set<RiskPlaceId>,
    ) {
        _monitoring.update { current ->
            when {
                !permitted -> current.copy(
                    permissionGranted = false,
                    monitoringActive = false,
                    uiState = MonitoringUiState.PERMISSION_NEEDED,
                    statusMessage = "Permission needed",
                )
                OsmTagMapping.detectable(risks).isEmpty() -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                    uiState = MonitoringUiState.NO_DETECTABLE_RISKS,
                    statusMessage = "Select bar / liquor store / supermarket / party to watch",
                )
                current.uiState == MonitoringUiState.NEAR_RISK -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                )
                current.uiState == MonitoringUiState.CHECK_ERROR -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                )
                else -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                    uiState = MonitoringUiState.WATCHING,
                    statusMessage = "Watching nearby risk places via open map data…",
                )
            }
        }
    }

    companion object {
        private const val TAG = "AlcoLarm.HomeVM"

        /** Local still/dwell evaluation cadence (Overpass is throttled separately). */
        private const val EVAL_INTERVAL_MS = 5_000L

        /** Min gap between Overpass queries while the user is still. */
        private const val OVERPASS_INTERVAL_MS = 25_000L

        /**
         * Speed below this (m/s) counts as stopped when Fused reports [Location.hasSpeed].
         * ~0.7 m/s ≈ 2.5 km/h — walking pace is higher; standing / creeping GPS noise is usually under.
         */
        const val STOP_SPEED_MPS = 0.7f

        /**
         * When speed is unavailable: max displacement across [STILL_WINDOW_MS] to count as still.
         * Mid of the ~15–20 m product band.
         */
        const val DISPLACEMENT_STILL_METERS = 18.0

        /** Lookback window for displacement-based stillness (short ~25–30 s band). */
        const val STILL_WINDOW_MS = 30_000L

        /**
         * Continuous still + nearby time required before alerting (~15 s).
         * Intentionally short so the user is warned before they can grab a drink — not 2 minutes.
         */
        const val DWELL_REQUIRED_MS = 15_000L

        /** After dismiss, do not re-alert for this long. */
        private const val ALERT_COOLDOWN_MS = 5 * 60_000L

        /** In-memory samples only (~2× still window at ~10–15 s fixes). */
        private const val SAMPLE_RING_CAPACITY = 16

        private fun haversineMeters(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
        ): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(Math.toRadians(lat1)) *
                kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2).let { it * it }
            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            return abs(r * c)
        }
    }
}
