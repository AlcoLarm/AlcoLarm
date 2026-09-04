package com.alcolarm.feature.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile
import com.alcolarm.core.model.friendly

@Composable
fun HomeRoute(
    showSimulateAlert: Boolean,
    onSimulateAlert: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    HomeScreen(
        profile = profile,
        showSimulateAlert = showSimulateAlert,
        onSimulateAlert = onSimulateAlert,
    )
}

@Composable
fun HomeScreen(
    profile: UserProfile,
    showSimulateAlert: Boolean,
    onSimulateAlert: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "You’re set up",
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Live location will watch for your risk places. Nothing is stored as a history trail.",
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.height(28.dp))

        SummaryBlock(
            title = "Your reasons",
            body = if (profile.quitReasons.isEmpty()) {
                "None selected yet"
            } else {
                profile.quitReasons.joinToString(", ") { it.friendly() }
            },
        )
        Spacer(Modifier.height(16.dp))
        SummaryBlock(
            title = "Risk places",
            body = if (profile.riskPlaces.isEmpty()) {
                "None selected yet"
            } else {
                profile.riskPlaces.joinToString(", ") { it.friendly() }
            },
        )
        Spacer(Modifier.height(16.dp))
        SummaryBlock(
            title = "Emergency contact",
            body = listOf(profile.emergencyContact.name, profile.emergencyContact.phoneNumber)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { "Not set" },
        )

        if (showSimulateAlert) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "Places integration isn’t wired yet. Use Simulate to preview the alert experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
            )
            Spacer(Modifier.height(16.dp))
            SignalPrimaryButton(
                text = "Simulate risk alert",
                onClick = onSimulateAlert,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryBlock(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = ClearSignalColors.Amber,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = ClearSignalColors.OnDark,
    )
}
