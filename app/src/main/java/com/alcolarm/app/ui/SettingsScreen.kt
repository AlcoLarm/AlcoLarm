package com.alcolarm.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile
import com.alcolarm.core.model.friendly

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onEditReasons: () -> Unit,
    onEditRiskPlaces: () -> Unit,
    onEditEmergency: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val backgroundWatch by viewModel.backgroundWatchEnabled.collectAsStateWithLifecycle()

    SettingsScreen(
        profile = profile,
        backgroundWatchEnabled = backgroundWatch,
        onBack = onBack,
        onEditReasons = onEditReasons,
        onEditRiskPlaces = onEditRiskPlaces,
        onEditEmergency = onEditEmergency,
        onBackgroundWatchChange = viewModel::setBackgroundWatchEnabled,
    )
}

@Composable
fun SettingsScreen(
    profile: UserProfile,
    backgroundWatchEnabled: Boolean,
    onBack: () -> Unit,
    onEditReasons: () -> Unit,
    onEditRiskPlaces: () -> Unit,
    onEditEmergency: () -> Unit,
    onBackgroundWatchChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ClearSignalColors.OnDark,
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = ClearSignalColors.OnDark,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Update anytime — changes save to this device.",
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(20.dp))

        SettingsRow(
            title = "Your reasons",
            subtitle = if (profile.quitReasons.isEmpty()) {
                "None selected yet"
            } else {
                profile.quitReasons.joinToString(", ") { it.friendly() }
            },
            onClick = onEditReasons,
        )
        SettingsRow(
            title = "Risk places",
            subtitle = if (profile.riskPlaces.isEmpty()) {
                "None selected yet"
            } else {
                profile.riskPlaces.joinToString(", ") { it.friendly() }
            },
            onClick = onEditRiskPlaces,
        )
        SettingsRow(
            title = "Emergency contact",
            subtitle = listOf(profile.emergencyContact.name, profile.emergencyContact.phoneNumber)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { "Not set" },
            onClick = onEditEmergency,
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Background monitoring",
                    style = MaterialTheme.typography.titleLarge,
                    color = ClearSignalColors.OnDark,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Keep watching nearby risk places when the app is closed (needs “all the time” location).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearSignalColors.OnDarkMuted,
                )
            }
            Switch(
                checked = backgroundWatchEnabled,
                onCheckedChange = onBackgroundWatchChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ClearSignalColors.Surface,
                    checkedTrackColor = ClearSignalColors.SoftBlue,
                    uncheckedThumbColor = ClearSignalColors.OnDarkMuted,
                    uncheckedTrackColor = ClearSignalColors.Outline,
                ),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ClearSignalColors.SoftBlue,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDark,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ClearSignalColors.OnDarkMuted,
        )
    }
}
