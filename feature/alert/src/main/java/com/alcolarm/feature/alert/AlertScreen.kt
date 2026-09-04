package com.alcolarm.feature.alert

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alcolarm.core.designsystem.component.AlarmStrip
import com.alcolarm.core.designsystem.component.DialButton
import com.alcolarm.core.designsystem.component.SignalSecondaryButton
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
    AlertScreen(
        profile = profile,
        familyPhotoFiles = familyPhotos,
        onDial = {
            val phone = profile.emergencyContact.phoneNumber
            if (phone.isNotBlank()) {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phone")
                    },
                )
            }
        },
        onDismiss = onDismiss,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack),
    ) {
        AlarmStrip(message = "Pause. You’ve got this.")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = "Remember why you started",
                style = MaterialTheme.typography.headlineMedium,
                color = ClearSignalColors.OnDark,
            )
            Spacer(Modifier.height(16.dp))

            if (profile.quitReasons.isEmpty()) {
                Text(
                    text = "Your reasons will show here after onboarding.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ClearSignalColors.OnDarkMuted,
                )
            } else {
                profile.quitReasons.forEach { reason ->
                    Text(
                        text = "· ${reason.friendly()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ClearSignalColors.OnDark,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            if (profile.healthNotes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Health",
                    style = MaterialTheme.typography.titleLarge,
                    color = ClearSignalColors.Amber,
                )
                Text(
                    text = profile.healthNotes,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ClearSignalColors.OnDark,
                )
            }

            if (profile.familyNotes.isNotBlank() || familyPhotoFiles.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Family",
                    style = MaterialTheme.typography.titleLarge,
                    color = ClearSignalColors.Amber,
                )
                if (profile.familyNotes.isNotBlank()) {
                    Text(
                        text = profile.familyNotes,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ClearSignalColors.OnDark,
                    )
                }
                if (familyPhotoFiles.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 8.dp),
                    ) {
                        items(familyPhotoFiles, key = { it.absolutePath }) { file ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(file)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Family photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            val contactLabel = profile.emergencyContact.name.ifBlank { "your contact" }
            DialButton(
                label = "Dial $contactLabel",
                onClick = onDial,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            SignalSecondaryButton(text = "I’m OK — close alert", onClick = onDismiss)
            Spacer(Modifier.height(24.dp))
        }
    }
}
