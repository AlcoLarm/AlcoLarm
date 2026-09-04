package com.alcolarm.feature.location

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.RiskPlaceId
import com.alcolarm.core.model.UserProfile
import com.alcolarm.core.model.friendly
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
    private var consecutiveHits = 0
    private var lastHitRisk: RiskPlaceId? = null
    private var lastAlertDismissedAt = 0L
    private var lastCheckLat = Double.NaN
    private var lastCheckLng = Double.NaN
    private var lastCheckAt = 0L

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
                    "Watching nearby…"
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
        if (!detector.hasApiKey()) {
            _monitoring.update {
                it.copy(
                    permissionGranted = true,
                    monitoringActive = false,
                    uiState = MonitoringUiState.MISSING_API_KEY,
                    statusMessage = "Add MAPS_API_KEY in local.properties to enable live detection",
                )
            }
            // Still start location so permission/tracking UI is exercisable; checks no-op without key.
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
                delay(CHECK_INTERVAL_MS)
            }
        }
        Log.d(TAG, "Risk monitoring loop started")
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        locationTracker.stop()
        consecutiveHits = 0
        lastHitRisk = null
        _monitoring.update {
            it.copy(monitoringActive = false)
        }
        Log.d(TAG, "Risk monitoring stopped")
    }

    /** Call when user dismisses the alert screen — debounce re-triggers. */
    fun onAlertDismissed() {
        lastAlertDismissedAt = System.currentTimeMillis()
        consecutiveHits = 0
        lastHitRisk = null
        _monitoring.update {
            it.copy(
                uiState = MonitoringUiState.WATCHING,
                statusMessage = "Watching nearby…",
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
        if (PlacesTypeMapping.detectable(risks).isEmpty()) {
            _monitoring.update {
                it.copy(
                    uiState = MonitoringUiState.NO_DETECTABLE_RISKS,
                    statusMessage = "Select bar / liquor store / supermarket / party to watch",
                )
            }
            return
        }

        if (!detector.hasApiKey()) {
            _monitoring.update {
                it.copy(
                    uiState = MonitoringUiState.MISSING_API_KEY,
                    statusMessage = "Add MAPS_API_KEY in local.properties to enable live detection",
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
        val movedEnough = lastCheckLat.isNaN() ||
            haversineMeters(lastCheckLat, lastCheckLng, sample.latitude, sample.longitude) >= MIN_MOVE_METERS
        val intervalElapsed = now - lastCheckAt >= CHECK_INTERVAL_MS
        if (!movedEnough && !intervalElapsed && lastCheckAt > 0L) {
            return
        }

        lastCheckLat = sample.latitude
        lastCheckLng = sample.longitude
        lastCheckAt = now

        when (val result = detector.detectNearby(sample.latitude, sample.longitude, risks)) {
            is RiskDetectResult.Match -> {
                val risk = result.match.riskPlaceId
                if (risk == lastHitRisk) {
                    consecutiveHits++
                } else {
                    lastHitRisk = risk
                    consecutiveHits = 1
                }
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.NEAR_RISK,
                        statusMessage = "Near a risk place",
                        lastMatchedPlaceName = result.match.placeName,
                        lastMatchedRisk = risk,
                    )
                }
                val cooledDown = now - lastAlertDismissedAt >= ALERT_COOLDOWN_MS
                if (consecutiveHits >= DWELL_HITS && cooledDown) {
                    consecutiveHits = 0
                    lastAlertDismissedAt = now
                    alertBus.emit(
                        RiskAlertEvent(
                            riskPlaceId = risk,
                            placeName = result.match.placeName,
                            simulated = false,
                        ),
                    )
                }
            }
            RiskDetectResult.None -> {
                consecutiveHits = 0
                lastHitRisk = null
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.WATCHING,
                        statusMessage = "Watching nearby…",
                        lastMatchedPlaceName = null,
                        lastMatchedRisk = null,
                    )
                }
            }
            RiskDetectResult.MissingApiKey -> {
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.MISSING_API_KEY,
                        statusMessage = "Add MAPS_API_KEY in local.properties to enable live detection",
                    )
                }
            }
            is RiskDetectResult.Error -> {
                _monitoring.update {
                    it.copy(
                        uiState = MonitoringUiState.CHECK_ERROR,
                        statusMessage = "Check failed — will retry",
                    )
                }
                Log.w(TAG, "Detect error: ${result.message}")
            }
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
                !detector.hasApiKey() -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                    uiState = MonitoringUiState.MISSING_API_KEY,
                    statusMessage = "Add MAPS_API_KEY in local.properties to enable live detection",
                )
                PlacesTypeMapping.detectable(risks).isEmpty() -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                    uiState = MonitoringUiState.NO_DETECTABLE_RISKS,
                    statusMessage = "Select bar / liquor store / supermarket / party to watch",
                )
                current.uiState == MonitoringUiState.NEAR_RISK -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                )
                else -> current.copy(
                    permissionGranted = true,
                    monitoringActive = tracking,
                    uiState = MonitoringUiState.WATCHING,
                    statusMessage = "Watching nearby…",
                )
            }
        }
    }

    companion object {
        private const val TAG = "AlcoLarm.HomeVM"
        private const val CHECK_INTERVAL_MS = 50_000L
        private const val MIN_MOVE_METERS = 40.0
        private const val DWELL_HITS = 2
        private const val ALERT_COOLDOWN_MS = 5 * 60_000L

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
