package com.alcolarm.feature.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.core.designsystem.component.SignalChip
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.component.SignalSecondaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.QuitReasonId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingRoute(
    onContinue: () -> Unit,
    editMode: Boolean = false,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // OpenDocument so we can takePersistableUriPermission; bytes are then copied app-scoped.
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.importFamilyPhoto(it) }
    }

    OnboardingScreen(
        state = state,
        editMode = editMode,
        onToggleReason = viewModel::toggleReason,
        onHealthNotes = viewModel::updateHealthNotes,
        onFamilyNotes = viewModel::updateFamilyNotes,
        onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
        onRemovePhoto = viewModel::removeFamilyPhoto,
        onContinue = { viewModel.saveAndContinue(onContinue) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onToggleReason: (QuitReasonId) -> Unit,
    onHealthNotes: (String) -> Unit,
    onFamilyNotes: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onContinue: () -> Unit,
    editMode: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.height(24.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuitReasonId.entries.forEach { id ->
                SignalChip(
                    label = stringResource(id.labelRes),
                    selected = id in state.selectedReasons,
                    onClick = { onToggleReason(id) },
                )
            }
        }

        if (QuitReasonId.HEALTH in state.selectedReasons) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_health_notes_title),
                style = MaterialTheme.typography.titleLarge,
                color = ClearSignalColors.OnDark,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.healthNotes,
                onValueChange = onHealthNotes,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.onboarding_health_notes_hint)) },
                minLines = 2,
                colors = fieldColors(),
            )
        }

        if (QuitReasonId.FAMILY in state.selectedReasons) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_family_notes_title),
                style = MaterialTheme.typography.titleLarge,
                color = ClearSignalColors.OnDark,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.familyNotes,
                onValueChange = onFamilyNotes,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.onboarding_family_notes_hint)) },
                minLines = 2,
                colors = fieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            SignalSecondaryButton(
                text = if (state.familyPhotoUris.isEmpty()) {
                    stringResource(R.string.onboarding_add_photo)
                } else {
                    stringResource(R.string.onboarding_add_another_photo, state.familyPhotoUris.size)
                },
                onClick = onPickPhoto,
            )
            state.familyPhotoUris.forEach { _ ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_photo_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearSignalColors.TealSupport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
            if (state.familyPhotoUris.isNotEmpty()) {
                SignalSecondaryButton(
                    text = stringResource(R.string.onboarding_remove_last_photo),
                    onClick = { onRemovePhoto(state.familyPhotoUris.last()) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        SignalPrimaryButton(
            text = if (editMode) {
                stringResource(R.string.action_save)
            } else {
                stringResource(R.string.action_continue)
            },
            onClick = onContinue,
            enabled = state.selectedReasons.isNotEmpty(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ClearSignalColors.SoftBlue,
    unfocusedBorderColor = ClearSignalColors.Outline,
    focusedTextColor = ClearSignalColors.OnDark,
    unfocusedTextColor = ClearSignalColors.OnDark,
    cursorColor = ClearSignalColors.SoftBlue,
    focusedPlaceholderColor = ClearSignalColors.OnDarkMuted,
    unfocusedPlaceholderColor = ClearSignalColors.OnDarkMuted,
)
