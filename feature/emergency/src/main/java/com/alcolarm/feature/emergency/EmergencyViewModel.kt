package com.alcolarm.feature.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.EmergencyContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmergencyUiState(
    val name: String = "",
    val phoneNumber: String = "",
    val loaded: Boolean = false,
)

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.profile.first()
            _uiState.update {
                it.copy(
                    name = profile.emergencyContact.name,
                    phoneNumber = profile.emergencyContact.phoneNumber,
                    loaded = true,
                )
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun updatePhone(value: String) {
        _uiState.update { it.copy(phoneNumber = value) }
    }

    fun saveAndContinue(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            repository.setEmergencyContact(
                EmergencyContact(name = state.name.trim(), phoneNumber = state.phoneNumber.trim()),
            )
            onDone()
        }
    }

    fun canContinue(): Boolean {
        val s = _uiState.value
        return s.name.isNotBlank() && s.phoneNumber.filter { it.isDigit() }.length >= 7
    }
}
