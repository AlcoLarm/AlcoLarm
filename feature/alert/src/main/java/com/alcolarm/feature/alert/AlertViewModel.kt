package com.alcolarm.feature.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.FamilyPhotoStore
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/** Result of the once-per-alert-session auto-dial attempt. */
sealed class AutoDialDecision {
    data class Dial(val phone: String) : AutoDialDecision()
    data object MissingContact : AutoDialDecision()
    data object AlreadyHandled : AutoDialDecision()
}

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    familyPhotoStore: FamilyPhotoStore,
    private val callStyleAlert: CallStyleAlertController,
    private val dialReturnTracker: DialReturnTracker,
) : ViewModel() {
    val profile: StateFlow<UserProfile> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserProfile(),
    )

    /** Resolved app-scoped loved-ones photo files for Coil / Image loading. */
    val familyPhotoFiles: StateFlow<List<File>> = repository.profile
        .map { profile ->
            profile.familyPhotoUris.mapNotNull { familyPhotoStore.resolveFile(it) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Once per alert session: auto-dial (or missing-contact notice) runs at most once,
     * so returning from the dialer does not launch Intent.ACTION_DIAL again.
     */
    private val autoDialHandled = AtomicBoolean(false)

    /**
     * Reads the real DataStore profile (not StateFlow initialValue), then claims the
     * once-per-session slot and decides whether to dial or show a missing-contact toast.
     */
    suspend fun decideAutoDial(): AutoDialDecision {
        if (autoDialHandled.get()) return AutoDialDecision.AlreadyHandled
        val loaded = repository.profile.first()
        if (!autoDialHandled.compareAndSet(false, true)) {
            return AutoDialDecision.AlreadyHandled
        }
        val phone = loaded.emergencyContact.phoneNumber.trim()
        return if (phone.isNotBlank()) {
            AutoDialDecision.Dial(phone)
        } else {
            AutoDialDecision.MissingContact
        }
    }

    fun startCallStyleAlert() {
        val contact = profile.value.emergencyContact
        val name = contact.name.trim().takeIf { it.isNotEmpty() }
        val phone = contact.phoneNumber.trim().takeIf { it.isNotEmpty() }
        callStyleAlert.start(name, phone)
    }

    fun stopCallStyleAlert() {
        callStyleAlert.stop()
    }

    fun markDialStarted() {
        dialReturnTracker.markDialStarted()
    }

    fun consumeDialReturn(): Boolean = dialReturnTracker.consumePendingWithin()

    override fun onCleared() {
        callStyleAlert.stop()
        super.onCleared()
    }
}
