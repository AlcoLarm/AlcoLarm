package com.alcolarm.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alcolarm.core.designsystem.theme.ClearSignalColors

/**
 * Large, obvious pause / alert banner — soft sage Quiet Companion styling,
 * still big and easy to hit on Alert (+ Home when near-risk).
 */
@Composable
fun PauseBanner(
    title: String = "PAUSE",
    subtitle: String = "Tap to silence & reflect",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "pauseBanner")
    val alpha by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pauseBannerAlpha",
    )
    val shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(shape)
            .background(ClearSignalColors.NeoBrutalAmber)
            .border(2.dp, ClearSignalColors.SoftBlue, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 24.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = ClearSignalColors.NeoBrutalInk,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = ClearSignalColors.NeoBrutalInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Backward-compatible alias — prefer [PauseBanner] for tappable pause.
 */
@Composable
fun AlarmStrip(
    message: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    PauseBanner(
        title = "PAUSE",
        subtitle = message,
        onClick = onClick,
        modifier = modifier,
    )
}
