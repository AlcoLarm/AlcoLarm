package com.alcolarm.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alcolarm.core.designsystem.theme.ClearSignalColors

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AlcoLarm",
                style = MaterialTheme.typography.displayLarge,
                color = ClearSignalColors.SoftBlue,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A quiet companion when it matters.",
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDarkMuted,
            )
        }
    }
}
