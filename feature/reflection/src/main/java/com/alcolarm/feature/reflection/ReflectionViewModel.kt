package com.alcolarm.feature.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.FamilyPhotoStore
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReflectionMode {
    /** From Pause — user may skip ("Not now"). */
    OPTIONAL,
    /** From emergency call no-answer — must complete both answers. */
    MANDATORY,
}

data class ReflectionAnswers(
    val turnAround: String = "",
    val drinkAgain: String = "",
)

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    familyPhotoStore: FamilyPhotoStore,
) : ViewModel() {
    val profile: StateFlow<UserProfile> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserProfile(),
    )

    val photoFiles: StateFlow<List<File>> = repository.profile
        .map { p -> p.familyPhotoUris.mapNotNull { familyPhotoStore.resolveFile(it) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _answers = MutableStateFlow(ReflectionAnswers())
    val answers: StateFlow<ReflectionAnswers> = _answers.asStateFlow()

    private val _prefillReady = MutableStateFlow(false)
    val prefillReady: StateFlow<Boolean> = _prefillReady.asStateFlow()

    init {
        viewModelScope.launch {
            repository.reflectionAnswers.collect { saved ->
                if (!_prefillReady.value) {
                    _answers.value = ReflectionAnswers(
                        turnAround = saved.turnAround,
                        drinkAgain = saved.drinkAgain,
                    )
                    _prefillReady.value = true
                }
            }
        }
    }

    fun onTurnAroundChanged(text: String) {
        _answers.update { it.copy(turnAround = text) }
    }

    fun onDrinkAgainChanged(text: String) {
        _answers.update { it.copy(drinkAgain = text) }
    }

    fun persistTurnAround() {
        val text = _answers.value.turnAround.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repository.setReflectionTurnAroundAnswer(text)
        }
    }

    fun persistDrinkAgain() {
        val text = _answers.value.drinkAgain.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repository.setReflectionDrinkAgainAnswer(text)
        }
    }

    fun persistBoth() {
        val current = _answers.value
        viewModelScope.launch {
            repository.setReflectionAnswers(
                turnAround = current.turnAround.trim(),
                drinkAgain = current.drinkAgain.trim(),
            )
        }
    }
}
