package com.alcolarm.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NordicCalmScheme = darkColorScheme(
    primary = ClearSignalColors.SoftBlue,
    onPrimary = ClearSignalColors.NearBlack,
    primaryContainer = ClearSignalColors.SoftBlueContainer,
    onPrimaryContainer = ClearSignalColors.SoftBlue,
    secondary = ClearSignalColors.Amber,
    onSecondary = ClearSignalColors.NearBlack,
    secondaryContainer = ClearSignalColors.AmberContainer,
    onSecondaryContainer = ClearSignalColors.Amber,
    tertiary = ClearSignalColors.TealSupport,
    onTertiary = ClearSignalColors.NearBlack,
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
        colorScheme = NordicCalmScheme,
        typography = ClearSignalTypography,
        content = content,
    )
}
