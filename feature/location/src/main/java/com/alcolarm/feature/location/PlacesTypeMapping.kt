package com.alcolarm.feature.location

import com.alcolarm.core.model.RiskPlaceId

/**
 * Maps user risk chips → Google Places Nearby Search `type` values.
 * HOME_ALONE / OTHER are not auto-detected via Places.
 */
object PlacesTypeMapping {

    fun placeTypesFor(risk: RiskPlaceId): List<String> = when (risk) {
        RiskPlaceId.BAR -> listOf("bar")
        RiskPlaceId.LIQUOR_STORE -> listOf("liquor_store")
        RiskPlaceId.SUPERMARKET -> listOf("supermarket", "convenience_store")
        RiskPlaceId.PARTY -> listOf("night_club")
        RiskPlaceId.HOME_ALONE -> emptyList()
        RiskPlaceId.OTHER -> emptyList()
    }

    /** All detectable risk chips that can match Places types. */
    fun detectable(selected: Set<RiskPlaceId>): Set<RiskPlaceId> =
        selected.filter { placeTypesFor(it).isNotEmpty() }.toSet()

    fun riskForPlaceType(type: String): RiskPlaceId? = when (type) {
        "bar" -> RiskPlaceId.BAR
        "liquor_store" -> RiskPlaceId.LIQUOR_STORE
        "supermarket", "convenience_store" -> RiskPlaceId.SUPERMARKET
        "night_club" -> RiskPlaceId.PARTY
        else -> null
    }
}
