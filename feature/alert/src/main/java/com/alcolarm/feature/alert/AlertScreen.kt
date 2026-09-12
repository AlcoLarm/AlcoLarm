package com.alcolarm.feature.alert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alcolarm.core.designsystem.component.DialButton
import com.alcolarm.core.designsystem.component.PauseBanner
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile
import java.io.File

@Composable
fun AlertRoute(
    onPauseReflect: () -> Unit,
    onDialReturn: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: AlertViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val familyPhotos by viewModel.familyPhotoFiles.collectAsStateWithLifecycle()
    val alertWarningCount by viewModel.alertWarningCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.recordAlertShown()
    }

    // Phone queued while waiting for CALL_PHONE runtime permission result.
    var pendingCallPhone by remember { mutableStateOf<String?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val phone = pendingCallPhone
        pendingCallPhone = null
        if (phone.isNullOrBlank()) return@rememberLauncherForActivityResult
        if (granted) {
            launchEmergencyCall(
                context = context,
                phone = phone,
                stopRingtone = { viewModel.stopCallStyleAlert() },
                markDialStarted = { viewModel.markDialStarted() },
            )
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.alert_toast_call_denied),
                Toast.LENGTH_SHORT,
            ).show()
            launchEmergencyDialFallback(
                context = context,
                phone = phone,
                stopRingtone = { viewModel.stopCallStyleAlert() },
                markDialStarted = { viewModel.markDialStarted() },
            )
        }
    }

    DisposableEffect(Unit) {
        viewModel.startCallStyleAlert()
        onDispose {
            viewModel.stopCallStyleAlert()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (viewModel.consumeDialReturn()) {
                    viewModel.stopCallStyleAlert()
                    onDialReturn()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AlertScreen(
        profile = profile,
        familyPhotoFiles = familyPhotos,
        showBuyMeCoffee = alertWarningCount >= AlertViewModel.BMC_SHOW_FROM_COUNT,
        onPause = {
            viewModel.stopCallStyleAlert()
            viewModel.pauseAlertsFor30Minutes()
            onPauseReflect()
        },
        onDial = {
            val phone = profile.emergencyContact.phoneNumber.trim()
            if (phone.isBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.alert_toast_no_contact),
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                val hasCallPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCallPermission) {
                    launchEmergencyCall(
                        context = context,
                        phone = phone,
                        stopRingtone = { viewModel.stopCallStyleAlert() },
                        markDialStarted = { viewModel.markDialStarted() },
                    )
                } else {
                    pendingCallPhone = phone
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            }
        },
        onDismiss = {
            viewModel.stopCallStyleAlert()
            onDismiss()
        },
    )
}

/**
 * Places a direct call with [Intent.ACTION_CALL] (`tel:` URI). Requires CALL_PHONE.
 * Stops call-style ringtone/vibrate so they do not fight the Phone app.
 */
private fun launchEmergencyCall(
    context: android.content.Context,
    phone: String,
    stopRingtone: () -> Unit,
    markDialStarted: () -> Unit,
) {
    stopRingtone()
    markDialStarted()
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phone")
            },
        )
    }.onFailure {
        // Rare: no dialer / security exception — open dialer as last resort.
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                },
            )
        }
    }
}

/**
 * Fallback when CALL_PHONE was denied: open the dialer once (ACTION_DIAL).
 */
private fun launchEmergencyDialFallback(
    context: android.content.Context,
    phone: String,
    stopRingtone: () -> Unit,
    markDialStarted: () -> Unit,
) {
    stopRingtone()
    markDialStarted()
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            },
        )
    }
}

@Composable
fun AlertScreen(
    profile: UserProfile,
    familyPhotoFiles: List<File>,
    showBuyMeCoffee: Boolean,
    onPause: () -> Unit,
    onDial: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dialLabel = profile.emergencyContact.name
        .trim()
        .substringBefore(" ")
        .ifBlank { stringResource(R.string.alert_dial_fallback) }
    val callLabel = profile.emergencyContact.name
        .trim()
        .ifBlank { stringResource(R.string.alert_incoming_call) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack),
    ) {
        Text(
            text = callLabel,
            style = MaterialTheme.typography.labelLarge,
            color = ClearSignalColors.OnDarkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 4.dp),
        )

        PauseBanner(
            title = stringResource(R.string.alert_pause_title),
            subtitle = stringResource(R.string.alert_pause_subtitle),
            onClick = onPause,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (familyPhotoFiles.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { familyPhotoFiles.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(familyPhotoFiles[page])
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.alert_cd_loved_ones_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                NoPhotoHero(profile = profile)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.2f to ClearSignalColors.NearBlack.copy(alpha = 0.35f),
                                0.5f to ClearSignalColors.NearBlack.copy(alpha = 0.88f),
                                1f to ClearSignalColors.NearBlack,
                            ),
                        ),
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (familyPhotoFiles.isNotEmpty()) {
                    OverlayReasons(profile = profile)
                    Spacer(Modifier.height(12.dp))
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.alert_im_ok_close),
                        style = MaterialTheme.typography.labelLarge,
                        color = ClearSignalColors.OnDarkMuted,
                    )
                }
                Spacer(Modifier.height(4.dp))
                DialButton(
                    label = dialLabel,
                    onClick = onDial,
                )
                if (showBuyMeCoffee) {
                    Spacer(Modifier.height(10.dp))
                    val bmcUrl = stringResource(R.string.bmc_url)
                    Text(
                        text = stringResource(R.string.bmc_support_link),
                        style = MaterialTheme.typography.bodySmall,
                        color = ClearSignalColors.OnDarkMuted.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(bmcUrl)),
                                    )
                                }
                            }
                            .padding(vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoPhotoHero(profile: UserProfile) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.alert_remember_why),
            style = MaterialTheme.typography.headlineMedium,
            color = ClearSignalColors.OnDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (profile.quitReasons.isEmpty()) {
            Text(
                text = stringResource(R.string.alert_reasons_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDarkMuted,
                textAlign = TextAlign.Center,
            )
        } else {
            profile.quitReasons.forEach { reason ->
                Text(
                    text = context.getString(reason.labelRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ClearSignalColors.OnDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
        if (profile.healthNotes.isNotBlank()) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = profile.healthNotes,
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDark,
                textAlign = TextAlign.Center,
            )
        }
        if (profile.familyNotes.isNotBlank()) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = profile.familyNotes,
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDark,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(220.dp))
    }
}

@Composable
private fun OverlayReasons(profile: UserProfile) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (profile.quitReasons.isNotEmpty()) {
            Text(
                text = profile.quitReasons.joinToString(" · ") {
                    context.getString(it.labelRes)
                },
                style = MaterialTheme.typography.titleMedium,
                color = ClearSignalColors.OnDark,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
        if (profile.familyNotes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = profile.familyNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        } else if (profile.healthNotes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = profile.healthNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
