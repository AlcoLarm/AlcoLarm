package com.alcolarm.core.model

enum class RiskPlaceId {
    BAR,
    LIQUOR_STORE,
    SUPERMARKET,
    PARTY,
    HOME_ALONE,
    OTHER,
    ;

    /** User-facing label for chips, home summary, and alert. */
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

/** Alias for call sites that prefer extension-style naming. */
fun RiskPlaceId.friendly(): String = displayLabel

data class RiskPlaceSelection(
    val id: RiskPlaceId,
    val label: String,
    val selected: Boolean = false,
)
