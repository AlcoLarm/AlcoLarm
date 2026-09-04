package com.alcolarm.core.model

enum class RiskPlaceId {
    BAR,
    LIQUOR_STORE,
    SUPERMARKET,
    PARTY,
    HOME_ALONE,
    OTHER,
}

data class RiskPlaceSelection(
    val id: RiskPlaceId,
    val label: String,
    val selected: Boolean = false,
)
