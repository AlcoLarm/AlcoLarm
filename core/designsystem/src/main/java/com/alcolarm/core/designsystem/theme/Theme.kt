package com.alcolarm.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QuietCompanionScheme = lightColorScheme(
    primary = ClearSignalColors.SoftBlue,
    onPrimary = Color.White,
    primaryContainer = ClearSignalColors.SoftBlueContainer,
    onPrimaryContainer = ClearSignalColors.OnDark,
    secondary = ClearSignalColors.Amber,
    onSecondary = ClearSignalColors.OnDark,
    secondaryContainer = ClearSignalColors.AmberContainer,
    onSecondaryContainer = ClearSignalColors.OnDark,
    tertiary = ClearSignalColors.TealSupport,
    onTertiary = Color.White,
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
        colorScheme = QuietCompanionScheme,
        typography = ClearSignalTypography,
        content = content,
    )
}
