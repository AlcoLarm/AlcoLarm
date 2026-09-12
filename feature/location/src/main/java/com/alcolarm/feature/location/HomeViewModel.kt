package com.alcolarm.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.RiskPlaceId
import com.alcolarm.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Home UI state. Detection runs in [RiskWatchEngine], optionally hosted by
 * [RiskWatchService] so monitoring continues when Home is paused.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val watchManager: RiskWatchManager,
    private val alertBus: RiskAlertBus,
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserProfile(),
    )

    val monitoring: StateFlow<HomeMonitoringUi> = combine(
        watchManager.monitoring,
        watchManager.owner,
        watchManager.needsBackgroundLocation,
        repository.alertSnoozeUntilEpochMs,
    ) { base, owner, needsBg, snoozeUntil ->
        val mode = when (owner) {
            RiskWatchManager.Owner.BACKGROUND_SERVICE -> WatchModeUi.BACKGROUND
            RiskWatchManager.Owner.FOREGROUND_SESSION -> WatchModeUi.FOREGROUND_ONLY
            RiskWatchManager.Owner.NONE -> WatchModeUi.OFF
        }
        val now = System.currentTimeMillis()
        val snoozed = snoozeUntil > now
        val statusKey = when {
            !base.permissionGranted -> base.statusKey
            snoozed -> MonitoringStatusKey.SNOOZED
            needsBg && base.uiState == MonitoringUiState.WATCHING ->
                MonitoringStatusKey.BACKGROUND_LOCATION_NEEDED
            else -> base.statusKey
        }
        val uiState = when {
            !base.permissionGranted -> base.uiState
            snoozed -> MonitoringUiState.SNOOZED
            else -> base.uiState
        }
        base.copy(
            watchMode = mode,
            needsBackgroundLocation = needsBg,
            statusKey = statusKey,
            uiState = uiState,
            snoozeUntilEpochMs = if (snoozed) snoozeUntil else 0L,
            monitoringActive = base.monitoringActive || owner != RiskWatchManager.Owner.NONE,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeMonitoringUi(),
    )

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            watchManager.onHomeResumed()
        } else {
            watchManager.stopCompletely()
        }
    }

    fun refreshPermissionFromSystem() {
        watchManager.onHomeResumed()
    }

    fun onHomeResumed() {
        watchManager.onHomeResumed()
    }

    fun onHomePaused() {
        watchManager.onHomePaused()
    }

    fun onBackgroundLocationGranted() {
        watchManager.onBackgroundPermissionGranted()
    }

    fun setBackgroundWatchEnabled(enabled: Boolean) {
        watchManager.setBackgroundWatchEnabled(enabled)
    }

    fun pauseAlertsFor30Minutes() {
        viewModelScope.launch {
            repository.pauseAlertsFor()
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
        // Do not stop background FGS when the ViewModel is cleared (config change / leave Home).
        // Background ownership lives in RiskWatchManager / RiskWatchService.
        // Foreground-only sessions are stopped via onHomePaused.
        super.onCleared()
    }

    companion object {
        /** Re-exported for tests / docs that referenced HomeViewModel dwell constants. */
        const val DWELL_REQUIRED_MS = RiskWatchEngine.DWELL_REQUIRED_MS
        const val STOP_SPEED_MPS = RiskWatchEngine.STOP_SPEED_MPS
    }
}
