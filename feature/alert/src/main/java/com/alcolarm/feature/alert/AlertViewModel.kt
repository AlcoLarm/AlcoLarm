package com.alcolarm.feature.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alcolarm.core.data.FamilyPhotoStore
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AlertViewModel @Inject constructor(
    repository: UserPreferencesRepository,
    familyPhotoStore: FamilyPhotoStore,
) : ViewModel() {
    val profile: StateFlow<UserProfile> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserProfile(),
    )

    /** Resolved app-scoped family photo files for Coil / Image loading. */
    val familyPhotoFiles: StateFlow<List<File>> = repository.profile
        .map { profile ->
            profile.familyPhotoUris.mapNotNull { familyPhotoStore.resolveFile(it) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
