package com.alcolarm.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.RiskPlaceId
import com.alcolarm.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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
    repository: UserPreferencesRepository,
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
    ) { base, owner, needsBg ->
        val mode = when (owner) {
            RiskWatchManager.Owner.BACKGROUND_SERVICE -> WatchModeUi.BACKGROUND
            RiskWatchManager.Owner.FOREGROUND_SESSION -> WatchModeUi.FOREGROUND_ONLY
            RiskWatchManager.Owner.NONE -> WatchModeUi.OFF
        }
        val statusMessage = when {
            !base.permissionGranted -> base.statusMessage
            needsBg && base.uiState == MonitoringUiState.WATCHING ->
                "Background location needed for alerts when app is closed"
            else -> base.statusMessage
        }
        base.copy(
            watchMode = mode,
            needsBackgroundLocation = needsBg,
            statusMessage = statusMessage,
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
