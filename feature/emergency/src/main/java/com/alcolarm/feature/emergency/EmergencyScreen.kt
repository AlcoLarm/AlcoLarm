package com.alcolarm.feature.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.component.SignalSecondaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors

@Composable
fun EmergencyRoute(
    onContinue: () -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    EmergencyScreen(
        state = state,
        canContinue = state.name.isNotBlank() &&
            state.phoneNumber.filter { it.isDigit() }.length >= 7,
        onName = viewModel::updateName,
        onPhone = viewModel::updatePhone,
        onTestDial = {
            val phone = state.phoneNumber.trim()
            if (phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                context.startActivity(intent)
            }
        },
        onContinue = { viewModel.saveAndContinue(onContinue) },
    )
}

@Composable
fun EmergencyScreen(
    state: EmergencyUiState,
    canContinue: Boolean,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onTestDial: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = "Who can you call?",
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "One trusted person. On an alert, Dial opens your phone app — nothing is sent automatically.",
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.name,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            singleLine = true,
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = onPhone,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(16.dp))
        SignalSecondaryButton(
            text = "Test dial (opens phone app)",
            onClick = onTestDial,
            enabled = state.phoneNumber.isNotBlank(),
        )
        Spacer(Modifier.weight(1f))
        SignalPrimaryButton(
            text = "Finish setup",
            onClick = onContinue,
            enabled = canContinue,
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
    focusedLabelColor = ClearSignalColors.SoftBlue,
    unfocusedLabelColor = ClearSignalColors.OnDarkMuted,
    cursorColor = ClearSignalColors.SoftBlue,
)
