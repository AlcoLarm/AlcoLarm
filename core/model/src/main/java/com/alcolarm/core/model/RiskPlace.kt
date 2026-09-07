package com.alcolarm.core.model

import androidx.annotation.StringRes

enum class RiskPlaceId {
    BAR,
    LIQUOR_STORE,
    SUPERMARKET,
    PARTY,
    HOME_ALONE,
    OTHER,
    ;

    /** User-facing label string resource for chips, home summary, and alert. */
    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            BAR -> R.string.risk_place_bar
            LIQUOR_STORE -> R.string.risk_place_liquor_store
            SUPERMARKET -> R.string.risk_place_supermarket
            PARTY -> R.string.risk_place_party
            HOME_ALONE -> R.string.risk_place_home_alone
            OTHER -> R.string.risk_place_other
        }

    /** English fallback for non-UI / logging. Prefer [labelRes] in Compose. */
    @Deprecated("Use labelRes with stringResource / getString", ReplaceWith("labelRes"))
    val displayLabel: String
        get() = when (this) {
            BAR -> "Bar"
            LIQUOR_STORE -> "Liquor store"
            SUPERMARKET -> "Supermarket"
            PARTY -> "Party / gathering"
            HOME_ALONE -> "Home alone"
            OTHER -> "Other"
        }
}

/** Alias kept for call sites; prefer Compose stringResource(id.labelRes). */
@Deprecated("Use labelRes with stringResource", ReplaceWith("labelRes"))
fun RiskPlaceId.friendly(): String = displayLabel

data class RiskPlaceSelection(
    val id: RiskPlaceId,
    val label: String,
    val selected: Boolean = false,
)
