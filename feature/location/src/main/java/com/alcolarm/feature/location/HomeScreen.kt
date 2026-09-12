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
import androidx.compose.ui.res.stringResource
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
import java.text.DateFormat
import java.util.Date

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
        onPauseReflect = {
            viewModel.pauseAlertsFor30Minutes()
            onPauseReflect()
        },
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
                title = stringResource(R.string.home_pause_title),
                subtitle = stringResource(R.string.home_pause_subtitle),
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
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = ClearSignalColors.OnDark,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.home_cd_settings),
                        tint = ClearSignalColors.OnDark,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDarkMuted,
            )
            Spacer(Modifier.height(20.dp))

            MonitoringCard(monitoring = monitoring)

            if (!monitoring.permissionGranted) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.home_location_privacy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearSignalColors.OnDarkMuted,
                )
                Spacer(Modifier.height(12.dp))
                SignalPrimaryButton(
                    text = stringResource(R.string.home_allow_location),
                    onClick = onRequestLocationPermission,
                )
            }

            if (showBackgroundRationale || monitoring.needsBackgroundLocation) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.home_background_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearSignalColors.OnDarkMuted,
                )
                Spacer(Modifier.height(12.dp))
                SignalPrimaryButton(
                    text = stringResource(R.string.home_allow_background_location),
                    onClick = onRequestBackgroundLocation,
                )
            }

            if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.home_notifications_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClearSignalColors.OnDarkMuted,
                )
                Spacer(Modifier.height(12.dp))
                SignalPrimaryButton(
                    text = stringResource(R.string.home_allow_notifications),
                    onClick = onRequestNotificationPermission,
                )
            }

            Spacer(Modifier.height(28.dp))

            val context = LocalContext.current
            val noneSelected = stringResource(R.string.home_none_selected)
            val notSet = stringResource(R.string.home_not_set)
            val reasonLabels = profile.quitReasons.map { context.getString(it.labelRes) }
            val riskLabels = profile.riskPlaces.map { context.getString(it.labelRes) }
            SummaryBlock(
                title = stringResource(R.string.home_your_reasons),
                body = if (reasonLabels.isEmpty()) noneSelected else reasonLabels.joinToString(", "),
            )
            Spacer(Modifier.height(16.dp))
            SummaryBlock(
                title = stringResource(R.string.home_risk_places),
                body = if (riskLabels.isEmpty()) noneSelected else riskLabels.joinToString(", "),
            )
            Spacer(Modifier.height(16.dp))
            SummaryBlock(
                title = stringResource(R.string.home_emergency_contact),
                body = listOf(profile.emergencyContact.name, profile.emergencyContact.phoneNumber)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { notSet },
            )

            Spacer(Modifier.height(28.dp))
            SignalSecondaryButton(
                text = stringResource(R.string.home_settings),
                onClick = onOpenSettings,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_simulate_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
            )
            Spacer(Modifier.height(12.dp))
            SignalPrimaryButton(
                text = stringResource(R.string.home_simulate_button),
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
        MonitoringUiState.SNOOZED -> ClearSignalColors.SoftBlue
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
            text = stringResource(R.string.home_live_risk_watch),
            style = MaterialTheme.typography.titleMedium,
            color = ClearSignalColors.SoftBlue,
        )
        Spacer(Modifier.height(6.dp))
        val now = System.currentTimeMillis()
        val snoozeActive = monitoring.snoozeUntilEpochMs > now
        val statusText = if (snoozeActive) {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(
                Date(monitoring.snoozeUntilEpochMs),
            )
            stringResource(R.string.status_paused_until, time)
        } else {
            stringResource(monitoring.statusKey.labelRes)
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = accent,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                !monitoring.permissionGranted -> stringResource(R.string.home_location_off)
                snoozeActive -> stringResource(R.string.home_monitoring_snoozed)
                monitoring.watchMode == WatchModeUi.BACKGROUND ->
                    stringResource(R.string.home_monitoring_background)
                monitoring.watchMode == WatchModeUi.FOREGROUND_ONLY ||
                    monitoring.monitoringActive ->
                    stringResource(R.string.home_monitoring_foreground)
                else -> stringResource(R.string.home_monitoring_paused)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = ClearSignalColors.OnDarkMuted,
        )
        val match = monitoring.lastMatchedPlaceName
        val risk = monitoring.lastMatchedRisk
        if (match != null && risk != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${LocalContext.current.getString(risk.labelRes)} · $match",
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
