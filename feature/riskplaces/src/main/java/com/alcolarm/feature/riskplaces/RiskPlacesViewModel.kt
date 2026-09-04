package com.alcolarm.feature.riskplaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.RiskPlaceId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RiskPlacesUiState(
    val selected: Set<RiskPlaceId> = emptySet(),
)

@HiltViewModel
class RiskPlacesViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiskPlacesUiState())
    val uiState: StateFlow<RiskPlacesUiState> = _uiState.asStateFlow()

    fun toggle(id: RiskPlaceId) {
        _uiState.update { state ->
            val next = state.selected.toMutableSet()
            if (!next.add(id)) next.remove(id)
            state.copy(selected = next)
        }
    }

    fun saveAndContinue(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.setRiskPlaces(_uiState.value.selected)
            onDone()
        }
    }
}
