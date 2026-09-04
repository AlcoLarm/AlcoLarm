package com.alcolarm.feature.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile
import com.alcolarm.core.model.friendly

@Composable
fun HomeRoute(
    showSimulateAlert: Boolean,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val monitoring by viewModel.monitoring.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshPermissionFromSystem()
                    if (hasPermission()) {
                        viewModel.startMonitoring()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopMonitoring()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopMonitoring()
        }
    }

    HomeScreen(
        profile = profile,
        monitoring = monitoring,
        showSimulateAlert = showSimulateAlert,
        onRequestLocationPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onSimulateAlert = { viewModel.simulateAlert() },
    )
}

@Composable
fun HomeScreen(
    profile: UserProfile,
    monitoring: HomeMonitoringUi,
    showSimulateAlert: Boolean,
    onRequestLocationPermission: () -> Unit,
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
            text = "Live location watches for places you marked as risky. We don’t keep a location history.",
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.height(20.dp))

        MonitoringCard(monitoring = monitoring)

        if (!monitoring.permissionGranted) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "AlcoLarm uses your location only to warn you near places you marked as risky — we don’t keep a location history.",
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
            )
            Spacer(Modifier.height(12.dp))
            SignalPrimaryButton(
                text = "Allow location",
                onClick = onRequestLocationPermission,
            )
        }

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
                text = "Debug: Simulate still works even without Places / location.",
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
private fun MonitoringCard(monitoring: HomeMonitoringUi) {
    val accent = when (monitoring.uiState) {
        MonitoringUiState.NEAR_RISK -> ClearSignalColors.Amber
        MonitoringUiState.PERMISSION_NEEDED,
        MonitoringUiState.MISSING_API_KEY,
        MonitoringUiState.CHECK_ERROR,
        -> ClearSignalColors.OnDarkMuted
        else -> ClearSignalColors.TealSupport
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClearSignalColors.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Live risk watch",
            style = MaterialTheme.typography.titleMedium,
            color = ClearSignalColors.Amber,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = monitoring.statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = accent,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                !monitoring.permissionGranted -> "Location: off"
                monitoring.monitoringActive -> "Monitoring: on (foreground)"
                else -> "Monitoring: paused"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = ClearSignalColors.OnDarkMuted,
        )
        val match = monitoring.lastMatchedPlaceName
        val risk = monitoring.lastMatchedRisk
        if (match != null && risk != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${risk.friendly()} · $match",
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDark,
            )
        }
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
