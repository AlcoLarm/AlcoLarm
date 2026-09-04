package com.alcolarm.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.QuitReasonId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val selectedReasons: Set<QuitReasonId> = emptySet(),
    val healthNotes: String = "",
    val familyNotes: String = "",
    val familyPhotoUris: List<String> = emptyList(),
    val saved: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun toggleReason(id: QuitReasonId) {
        _uiState.update { state ->
            val next = state.selectedReasons.toMutableSet()
            if (!next.add(id)) next.remove(id)
            state.copy(selectedReasons = next)
        }
    }

    fun updateHealthNotes(text: String) {
        _uiState.update { it.copy(healthNotes = text) }
    }

    fun updateFamilyNotes(text: String) {
        _uiState.update { it.copy(familyNotes = text) }
    }

    /** MVP stub: append a placeholder URI when photo picker returns. */
    fun addFamilyPhotoUri(uri: String) {
        _uiState.update { it.copy(familyPhotoUris = it.familyPhotoUris + uri) }
    }

    fun removeFamilyPhotoUri(uri: String) {
        _uiState.update { it.copy(familyPhotoUris = it.familyPhotoUris - uri) }
    }

    fun saveAndContinue(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            repository.setQuitReasons(
                reasons = state.selectedReasons,
                healthNotes = state.healthNotes,
                familyNotes = state.familyNotes,
                familyPhotoUris = state.familyPhotoUris,
            )
            _uiState.update { it.copy(saved = true) }
            onDone()
        }
    }
}
