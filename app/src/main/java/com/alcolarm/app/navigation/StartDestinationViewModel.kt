package com.alcolarm.app.navigation

import androidx.lifecycle.ViewModel
import com.alcolarm.core.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class StartRouting(
    val disclaimerAccepted: Boolean,
    val onboardingComplete: Boolean,
)

@HiltViewModel
class StartDestinationViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {
    /**
     * Suspends until DataStore emits the first prefs snapshot, then returns
     * disclaimer + onboarding routing flags.
     */
    suspend fun awaitStartRouting(): StartRouting {
        val accepted = repository.disclaimerAccepted.first()
        val complete = repository.profile.first().onboardingComplete
        return StartRouting(
            disclaimerAccepted = accepted,
            onboardingComplete = complete,
        )
    }
}
