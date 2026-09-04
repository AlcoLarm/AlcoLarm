package com.alcolarm.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alcolarm.core.designsystem.theme.ClearSignalColors

/**
 * High-visibility strip for the alert screen — steady pulse, not frantic.
 */
@Composable
fun AlarmStrip(
    message: String,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "alarm")
    val alpha by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alarmAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(ClearSignalColors.Amber)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = ClearSignalColors.NearBlack,
            textAlign = TextAlign.Center,
        )
    }
}
