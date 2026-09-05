package com.alcolarm.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * AlcoLarm palette — Quiet Companion (B2):
 * cream/off-white surfaces, soft sage primary, gentle amber dial accent.
 * Pause banner stays large & obvious with soft sage (not harsh neo-brutal).
 *
 * Token names are historical; values are Quiet Companion.
 */
object ClearSignalColors {
    /** Cream app background. */
    val NearBlack = Color(0xFFF7F5F0)
    val Surface = Color(0xFFFFFEFA)
    val SurfaceElevated = Color(0xFFEEECE4)
    val Outline = Color(0xFFD2CFC4)

    /** Ink on cream. */
    val OnDark = Color(0xFF2A2F2A)
    val OnDarkMuted = Color(0xFF6B7368)

    /** Sage — everyday primary CTAs. */
    val SoftBlue = Color(0xFF828F73)
    val SoftBlueDim = Color(0xFF6F7C61)
    val SoftBlueContainer = Color(0xFFE4E8DC)

    /** Soft warm amber — dial / call accent. */
    val Amber = Color(0xFFD4A05A)
    val AmberDim = Color(0xFFC08E48)
    val AmberContainer = Color(0xFFF3E6D2)

    /** Soft sage pause banner (big, tappable). */
    val NeoBrutalAmber = Color(0xFFC9D4BC)
    val NeoBrutalInk = Color(0xFF2C3A28)

    val TealSupport = Color(0xFF6E9B8A)
    val Danger = Color(0xFFC75B5E)
    val Success = Color(0xFF5A9E78)

    /** Explicit Quiet Companion aliases. */
    val Cream = NearBlack
    val Sage = SoftBlue
    val Ink = OnDark
    val InkMuted = OnDarkMuted
}
