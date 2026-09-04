package com.alcolarm.app.navigation

import androidx.lifecycle.ViewModel
import com.alcolarm.core.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class StartDestinationViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {
    /**
     * Suspends until DataStore emits the first profile snapshot, then returns
     * whether onboarding is complete. Prefer this over reading a StateFlow
     * `.value` after a blind delay (which races the initialValue).
     */
    suspend fun awaitOnboardingComplete(): Boolean =
        repository.profile.map { it.onboardingComplete }.first()
}
