package com.alcolarm.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * AlcoLarm palette:
 * - Everyday UI — Nordic calm (cool blue-gray surfaces, soft blue primary)
 * - Dial / urgent — Clear Signal amber accent
 * - Pause / alert banner — Neo brutal high contrast
 */
object ClearSignalColors {
    val NearBlack = Color(0xFF0F1419)
    val Surface = Color(0xFF1A222C)
    val SurfaceElevated = Color(0xFF24303C)
    val Outline = Color(0xFF3D4A5C)

    val OnDark = Color(0xFFE8EEF4)
    val OnDarkMuted = Color(0xFF9AA8B8)

    /** Soft blue — everyday primary actions (Continue, Allow, etc.). */
    val SoftBlue = Color(0xFF6B9BD1)
    val SoftBlueDim = Color(0xFF557FAD)
    val SoftBlueContainer = Color(0xFF1E3348)

    /** Amber — dial, call, and urgent accents only. */
    val Amber = Color(0xFFFFB020)
    val AmberDim = Color(0xFFE09810)
    val AmberContainer = Color(0xFF3D2E0A)

    /** Neo brutal pause banner. */
    val NeoBrutalAmber = Color(0xFFFFC107)
    val NeoBrutalInk = Color(0xFF0A0A0A)

    val TealSupport = Color(0xFF2EC4B6)
    val Danger = Color(0xFFFF5A5F)
    val Success = Color(0xFF3DDC97)
}
