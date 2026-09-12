package com.alcolarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.UserProfile
import com.alcolarm.feature.location.RiskWatchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val watchManager: RiskWatchManager,
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserProfile(),
    )

    val backgroundWatchEnabled: StateFlow<Boolean> = repository.backgroundWatchEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val alertSnoozeUntilEpochMs: StateFlow<Long> = repository.alertSnoozeUntilEpochMs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0L,
    )

    fun setBackgroundWatchEnabled(enabled: Boolean) {
        watchManager.setBackgroundWatchEnabled(enabled)
    }

    fun resumeAlertsNow() {
        viewModelScope.launch {
            repository.clearAlertSnooze()
        }
    }
}
