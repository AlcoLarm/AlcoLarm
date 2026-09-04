package com.alcolarm.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClearSignalDarkScheme = darkColorScheme(
    primary = ClearSignalColors.Amber,
    onPrimary = ClearSignalColors.NearBlack,
    primaryContainer = ClearSignalColors.AmberContainer,
    onPrimaryContainer = ClearSignalColors.Amber,
    secondary = ClearSignalColors.TealSupport,
    onSecondary = ClearSignalColors.NearBlack,
    background = ClearSignalColors.NearBlack,
    onBackground = ClearSignalColors.OnDark,
    surface = ClearSignalColors.Surface,
    onSurface = ClearSignalColors.OnDark,
    surfaceVariant = ClearSignalColors.SurfaceElevated,
    onSurfaceVariant = ClearSignalColors.OnDarkMuted,
    outline = ClearSignalColors.Outline,
    error = ClearSignalColors.Danger,
    onError = Color.White,
)

@Composable
fun AlcoLarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClearSignalDarkScheme,
        typography = ClearSignalTypography,
        content = content,
    )
}
