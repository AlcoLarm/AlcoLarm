package com.alcolarm.feature.location

import android.util.Log
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.RiskPlaceId
import com.alcolarm.feature.alert.CallStyleAlertController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Stop+dwell risk detection loop that can run under a foreground service or an
 * in-process foreground session. No location history is written to disk.
 */
@Singleton
class RiskWatchEngine @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val locationTracker: LiveLocationTracker,
    private val detector: RiskPlaceDetector,
    private val alertBus: RiskAlertBus,
    private val callStyleAlert: CallStyleAlertController,
) {
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

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val mutex = Mutex()
    private var scope: CoroutineScope? = null
    private var monitorJob: Job? = null
    private var dismissJob: Job? = null
    private var activeMode: LocationUpdateMode = LocationUpdateMode.FOREGROUND

    private var lastAlertDismissedAt = 0L
    private var stillNearMs = 0L
    private var lastDwellTickAt = 0L
    private var lastOverpassAt = 0L
    private var cachedMatch: NearbyRiskMatch? = null
    private val sampleRing = ArrayDeque<LiveLocationSample>(SAMPLE_RING_CAPACITY)

    suspend fun start(scope: CoroutineScope, mode: LocationUpdateMode) = mutex.withLock {
        this.scope = scope
        activeMode = mode
        if (!locationTracker.hasLocationPermission()) {
            _monitoring.update {
                it.copy(
                    permissionGranted = false,
                    monitoringActive = false,
                    uiState = MonitoringUiState.PERMISSION_NEEDED,
                    statusMessage = "Permission needed",
                )
            }
            return@withLock
        }
        locationTracker.start(mode)
        _running.value = true
        _monitoring.update {
            it.copy(permissionGranted = true, monitoringActive = true)
        }
        if (monitorJob?.isActive == true) {
            locationTracker.setMode(mode)
            return@withLock
        }
        monitorJob = scope.launch {
            while (isActive) {
                runDetectionCycle()
                delay(EVAL_INTERVAL_MS)
            }
        }
        if (dismissJob?.isActive != true) {
            dismissJob = scope.launch {
                alertBus.dismissed.collect { onAlertDismissed() }
            }
        }
        Log.d(TAG, "Risk watch loop started mode=$mode")
    }

    suspend fun setMode(mode: LocationUpdateMode) = mutex.withLock {
        activeMode = mode
        if (_running.value) {
            locationTracker.setMode(mode)
        }
    }

    suspend fun stop() = mutex.withLock {
        monitorJob?.cancel()
        monitorJob = null
        dismissJob?.cancel()
        dismissJob = null
        locationTracker.stop()
        clearDwellState()
        sampleRing.clear()
        _running.value = false
        _monitoring.update { it.copy(monitoringActive = false) }
        Log.d(TAG, "Risk watch stopped; sample ring cleared")
    }

    fun onAlertDismissed() {
        lastAlertDismissedAt = System.currentTimeMillis()
        clearDwellState()
        _monitoring.update {
            it.copy(
                uiState = MonitoringUiState.WATCHING,
                statusMessage = defaultWatchMessage(),
                lastMatchedPlaceName = null,
                lastMatchedRisk = null,
            )
        }
    }

    fun refreshPermissionBanner() {
        val permitted = locationTracker.hasLocationPermission()
        if (!permitted) {
            _monitoring.update {
                it.copy(
                    permissionGranted = false,
                    monitoringActive = false,
                    uiState = MonitoringUiState.PERMISSION_NEEDED,
                    statusMessage = "Permission needed",
                )
            }
        }
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

        val profile = repository.profile.first()
        val risks = profile.riskPlaces
        if (OsmTagMapping.detectable(risks).isEmpty()) {
            clearDwellState()
            maybeAdjustLocationMode(nearCandidate = false)
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

        var overpassError: String? = null
        val needsOverpass = still && (
            cachedMatch == null || now - lastOverpassAt >= OVERPASS_INTERVAL_MS
        )
        if (needsOverpass) {
            lastOverpassAt = now
            when (val result = detector.detectNearby(sample.latitude, sample.longitude, risks)) {
                is RiskDetectResult.Match -> cachedMatch = result.match
                RiskDetectResult.None -> cachedMatch = null
                is RiskDetectResult.Error -> {
                    overpassError = result.message
                    Log.w(TAG, "Detect error: ${result.message}")
                }
            }
        }

        if (!still) {
            stillNearMs = 0L
            lastDwellTickAt = 0L
            cachedMatch = null
            maybeAdjustLocationMode(nearCandidate = false)
            publishMovingOrIdle(overpassError)
            return
        }

        val match = cachedMatch
        if (match == null) {
            stillNearMs = 0L
            lastDwellTickAt = 0L
            maybeAdjustLocationMode(nearCandidate = false)
            publishMovingOrIdle(overpassError)
            return
        }

        maybeAdjustLocationMode(nearCandidate = true)

        if (lastDwellTickAt > 0L) {
            stillNearMs += now - lastDwellTickAt
        }
        lastDwellTickAt = now

        _monitoring.update {
            it.copy(
                permissionGranted = true,
                monitoringActive = true,
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
            val event = RiskAlertEvent(
                riskPlaceId = match.riskPlaceId,
                placeName = match.placeName,
                simulated = false,
            )
            alertBus.emit(event)
            val contact = profile.emergencyContact
            val name = contact.name.trim().takeIf { it.isNotEmpty() }
            val phone = contact.phoneNumber.trim().takeIf { it.isNotEmpty() }
            // Fire call-style even when UI is not resumed (background FGS).
            callStyleAlert.start(name, phone)
        }
    }

    private suspend fun maybeAdjustLocationMode(nearCandidate: Boolean) {
        if (activeMode == LocationUpdateMode.FOREGROUND) return
        val desired = if (nearCandidate) {
            LocationUpdateMode.BACKGROUND_NEAR
        } else {
            LocationUpdateMode.BACKGROUND
        }
        if (desired != activeMode) {
            activeMode = desired
            locationTracker.setMode(desired)
        }
    }

    private fun publishMovingOrIdle(overpassError: String?) {
        if (overpassError != null) {
            _monitoring.update {
                it.copy(
                    permissionGranted = true,
                    monitoringActive = true,
                    uiState = MonitoringUiState.CHECK_ERROR,
                    statusMessage = "Check failed — will retry",
                )
            }
        } else {
            _monitoring.update {
                it.copy(
                    permissionGranted = true,
                    monitoringActive = true,
                    uiState = MonitoringUiState.WATCHING,
                    statusMessage = defaultWatchMessage(),
                    lastMatchedPlaceName = null,
                    lastMatchedRisk = null,
                )
            }
        }
    }

    private fun defaultWatchMessage(): String =
        "Watching nearby risk places via open map data…"

    companion object {
        private const val TAG = "AlcoLarm.WatchEngine"

        private const val EVAL_INTERVAL_MS = 3_000L
        private const val OVERPASS_INTERVAL_MS = 10_000L
        const val STOP_SPEED_MPS = 0.7f
        const val DISPLACEMENT_STILL_METERS = 14.0
        const val STILL_WINDOW_MS = 8_000L
        const val DWELL_REQUIRED_MS = 5_000L
        private const val ALERT_COOLDOWN_MS = 5 * 60_000L
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
