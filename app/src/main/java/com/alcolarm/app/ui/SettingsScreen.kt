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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.app.R
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onEditReasons: () -> Unit,
    onEditRiskPlaces: () -> Unit,
    onEditEmergency: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val backgroundWatch by viewModel.backgroundWatchEnabled.collectAsStateWithLifecycle()
    val snoozeUntil by viewModel.alertSnoozeUntilEpochMs.collectAsStateWithLifecycle()

    SettingsScreen(
        profile = profile,
        backgroundWatchEnabled = backgroundWatch,
        snoozeUntilEpochMs = snoozeUntil,
        onBack = onBack,
        onEditReasons = onEditReasons,
        onEditRiskPlaces = onEditRiskPlaces,
        onEditEmergency = onEditEmergency,
        onOpenDisclaimer = onOpenDisclaimer,
        onBackgroundWatchChange = viewModel::setBackgroundWatchEnabled,
        onResumeAlertsNow = viewModel::resumeAlertsNow,
    )
}

@Composable
fun SettingsScreen(
    profile: UserProfile,
    backgroundWatchEnabled: Boolean,
    snoozeUntilEpochMs: Long = 0L,
    onBack: () -> Unit,
    onEditReasons: () -> Unit,
    onEditRiskPlaces: () -> Unit,
    onEditEmergency: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onBackgroundWatchChange: (Boolean) -> Unit,
    onResumeAlertsNow: () -> Unit = {},
) {
    val context = LocalContext.current
    val noneSelected = stringResource(R.string.none_selected_yet)
    val notSet = stringResource(R.string.not_set)
    val reasonLabels = profile.quitReasons.map { context.getString(it.labelRes) }
    val riskLabels = profile.riskPlaces.map { context.getString(it.labelRes) }

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
                    contentDescription = stringResource(R.string.action_back),
                    tint = ClearSignalColors.OnDark,
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = ClearSignalColors.OnDark,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(20.dp))

        SettingsRow(
            title = stringResource(R.string.settings_your_reasons),
            subtitle = if (reasonLabels.isEmpty()) noneSelected else reasonLabels.joinToString(", "),
            onClick = onEditReasons,
        )
        SettingsRow(
            title = stringResource(R.string.settings_risk_places),
            subtitle = if (riskLabels.isEmpty()) noneSelected else riskLabels.joinToString(", "),
            onClick = onEditRiskPlaces,
        )
        SettingsRow(
            title = stringResource(R.string.settings_emergency_contact),
            subtitle = listOf(profile.emergencyContact.name, profile.emergencyContact.phoneNumber)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { notSet },
            onClick = onEditEmergency,
        )
        SettingsRow(
            title = stringResource(R.string.settings_about_disclaimer),
            subtitle = stringResource(R.string.settings_about_subtitle),
            onClick = onOpenDisclaimer,
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
                    text = stringResource(R.string.settings_background_monitoring),
                    style = MaterialTheme.typography.titleLarge,
                    color = ClearSignalColors.OnDark,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_background_monitoring_desc),
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

        val now = System.currentTimeMillis()
        if (snoozeUntilEpochMs > now) {
            Spacer(Modifier.height(8.dp))
            val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                .format(java.util.Date(snoozeUntilEpochMs))
            SettingsRow(
                title = stringResource(R.string.settings_resume_alerts),
                subtitle = stringResource(R.string.settings_resume_alerts_subtitle, time),
                onClick = onResumeAlertsNow,
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
