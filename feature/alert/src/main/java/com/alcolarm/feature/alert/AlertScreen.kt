package com.alcolarm.feature.alert

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alcolarm.core.designsystem.component.AlarmStrip
import com.alcolarm.core.designsystem.component.DialButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile
import com.alcolarm.core.model.friendly
import java.io.File

@Composable
fun AlertRoute(
    onDismiss: () -> Unit,
    viewModel: AlertViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val familyPhotos by viewModel.familyPhotoFiles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        viewModel.startCallStyleAlert()
        onDispose {
            viewModel.stopCallStyleAlert()
        }
    }

    AlertScreen(
        profile = profile,
        familyPhotoFiles = familyPhotos,
        onDial = {
            viewModel.stopCallStyleAlert()
            val phone = profile.emergencyContact.phoneNumber
            if (phone.isNotBlank()) {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phone")
                    },
                )
            }
        },
        onDismiss = {
            viewModel.stopCallStyleAlert()
            onDismiss()
        },
    )
}

@Composable
fun AlertScreen(
    profile: UserProfile,
    familyPhotoFiles: List<File>,
    onDial: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dialLabel = profile.emergencyContact.name
        .trim()
        .substringBefore(" ")
        .ifBlank { "Dial" }
    val callLabel = profile.emergencyContact.name
        .trim()
        .ifBlank { "Incoming call" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack),
    ) {
        // Subtle call metaphor (private in-app; public notification stays anonymous).
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

        AlarmStrip(
            message = "Pause. You’ve got this.",
            modifier = Modifier.padding(vertical = 0.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Dominant visual: full-bleed photos or strong dark reasons block
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
                        contentDescription = "Family photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                NoPhotoHero(profile = profile)
            }

            // Bottom scrim: reasons overlay + dismiss + round dial
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.25f to ClearSignalColors.NearBlack.copy(alpha = 0.55f),
                                0.55f to ClearSignalColors.NearBlack.copy(alpha = 0.88f),
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
                        text = "I’m OK — close",
                        style = MaterialTheme.typography.labelLarge,
                        color = ClearSignalColors.OnDarkMuted,
                    )
                }
                Spacer(Modifier.height(4.dp))
                DialButton(
                    label = dialLabel,
                    onClick = onDial,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoPhotoHero(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Remember why you started",
            style = MaterialTheme.typography.headlineMedium,
            color = ClearSignalColors.OnDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (profile.quitReasons.isEmpty()) {
            Text(
                text = "Your reasons will show here after onboarding.",
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDarkMuted,
                textAlign = TextAlign.Center,
            )
        } else {
            profile.quitReasons.forEach { reason ->
                Text(
                    text = reason.friendly(),
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
                text = "Health",
                style = MaterialTheme.typography.titleMedium,
                color = ClearSignalColors.Amber,
            )
            Spacer(Modifier.height(8.dp))
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
                text = "Family",
                style = MaterialTheme.typography.titleMedium,
                color = ClearSignalColors.Amber,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = profile.familyNotes,
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDark,
                textAlign = TextAlign.Center,
            )
        }
        // Leave room so content isn't hidden behind the bottom dial overlay
        Spacer(Modifier.height(220.dp))
    }
}

@Composable
private fun OverlayReasons(profile: UserProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (profile.quitReasons.isNotEmpty()) {
            Text(
                text = profile.quitReasons.joinToString(" · ") { it.friendly() },
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
