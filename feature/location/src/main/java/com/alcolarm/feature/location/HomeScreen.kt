package com.alcolarm.feature.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.core.designsystem.component.PauseBanner
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.component.SignalSecondaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile
import com.alcolarm.core.model.friendly

@Composable
fun HomeRoute(
    onPauseReflect: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val monitoring by viewModel.monitoring.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationsGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var showBackgroundRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showBackgroundRationale = !hasBackgroundLocationPermission(context)
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        showBackgroundRationale = false
        if (granted) {
            viewModel.onBackgroundLocationGranted()
        } else {
            viewModel.refreshPermissionFromSystem()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted || hasNotificationPermission(context)
    }

    fun hasLocationPermission(): Boolean {
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

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    notificationsGranted = hasNotificationPermission(context)
                    if (hasBackgroundLocationPermission(context)) {
                        showBackgroundRationale = false
                    } else if (hasLocationPermission() &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ) {
                        showBackgroundRationale = true
                    }
                    viewModel.onHomeResumed()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onHomePaused()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onHomePaused()
        }
    }

    HomeScreen(
        profile = profile,
        monitoring = monitoring,
        notificationsGranted = notificationsGranted,
        showBackgroundRationale = showBackgroundRationale &&
            hasLocationPermission() &&
            !hasBackgroundLocationPermission(context),
        onRequestLocationPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onRequestBackgroundLocation = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                } else {
                    backgroundPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    )
                }
            }
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onPauseReflect = onPauseReflect,
        onOpenSettings = onOpenSettings,
        onSimulateAlert = { viewModel.simulateAlert() },
    )
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasBackgroundLocationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun HomeScreen(
    profile: UserProfile,
    monitoring: HomeMonitoringUi,
    notificationsGranted: Boolean,
    showBackgroundRationale: Boolean,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onPauseReflect: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onSimulateAlert: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack),
    ) {
        if (monitoring.uiState == MonitoringUiState.NEAR_RISK) {
            PauseBanner(
                title = "PAUSE",
                subtitle = "Nearby risk — tap to breathe & reflect",
                onClick = onPauseReflect,
                modifier = Modifier.statusBarsPadding(),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "You’re set up",
                    style = MaterialTheme.typography.headlineLarge,
                    color = ClearSignalColors.OnDark,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = ClearSignalColors.OnDark,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Watching nearby risk places via open map data. We don’t keep a location history.",
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

            if (showBackgroundRationale || monitoring.needsBackgroundLocation) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "To keep alerts working when the app is closed, allow location “all the time” in system settings. " +
                        "AlcoLarm shows a simple “Location on” notice while watching — nothing about alcohol or recovery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearSignalColors.OnDarkMuted,
                )
                Spacer(Modifier.height(12.dp))
                SignalPrimaryButton(
                    text = "Allow background location",
                    onClick = onRequestBackgroundLocation,
                )
            }

            if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Allow notifications so a discreet call-style alert can reach you when needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearSignalColors.OnDarkMuted,
                )
                Spacer(Modifier.height(12.dp))
                SignalPrimaryButton(
                    text = "Allow notifications",
                    onClick = onRequestNotificationPermission,
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

            Spacer(Modifier.height(28.dp))
            SignalSecondaryButton(
                text = "Settings",
                onClick = onOpenSettings,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Try the call-style alert anytime — useful for practice and testing.",
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
            )
            Spacer(Modifier.height(12.dp))
            SignalPrimaryButton(
                text = "Simulate risk alert",
                onClick = onSimulateAlert,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonitoringCard(monitoring: HomeMonitoringUi) {
    val accent = when (monitoring.uiState) {
        MonitoringUiState.NEAR_RISK -> ClearSignalColors.Amber
        MonitoringUiState.PERMISSION_NEEDED,
        MonitoringUiState.CHECK_ERROR,
        -> ClearSignalColors.OnDarkMuted
        else -> ClearSignalColors.TealSupport
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClearSignalColors.Surface, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Live risk watch",
            style = MaterialTheme.typography.titleMedium,
            color = ClearSignalColors.SoftBlue,
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
                monitoring.watchMode == WatchModeUi.BACKGROUND ->
                    "Monitoring: on (background)"
                monitoring.watchMode == WatchModeUi.FOREGROUND_ONLY ||
                    monitoring.monitoringActive ->
                    "Monitoring: on (foreground)"
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
        color = ClearSignalColors.SoftBlue,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = ClearSignalColors.OnDark,
    )
}
