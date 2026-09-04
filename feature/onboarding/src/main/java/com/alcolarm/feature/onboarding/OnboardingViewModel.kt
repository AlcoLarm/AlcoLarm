package com.alcolarm.feature.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.FamilyPhotoStore
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.QuitReasonId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class OnboardingUiState(
    val selectedReasons: Set<QuitReasonId> = emptySet(),
    val healthNotes: String = "",
    val familyNotes: String = "",
    /** Relative paths under filesDir (family_photos/…) from [FamilyPhotoStore]. */
    val familyPhotoUris: List<String> = emptyList(),
    val saved: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val familyPhotoStore: FamilyPhotoStore,
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

    /** Copy picked document into app-internal storage; store relative path in state. */
    fun importFamilyPhoto(uri: Uri) {
        viewModelScope.launch {
            val relativePath = withContext(Dispatchers.IO) {
                familyPhotoStore.importPhoto(uri)
            } ?: return@launch
            _uiState.update { it.copy(familyPhotoUris = it.familyPhotoUris + relativePath) }
        }
    }

    fun removeFamilyPhoto(storedPath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                familyPhotoStore.deleteIfAppScoped(storedPath)
            }
            _uiState.update { it.copy(familyPhotoUris = it.familyPhotoUris - storedPath) }
        }
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
