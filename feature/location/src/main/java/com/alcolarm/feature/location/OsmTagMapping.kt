package com.alcolarm.feature.location

import com.alcolarm.core.model.RiskPlaceId

/**
 * Maps user risk chips → OpenStreetMap Overpass tags.
 * HOME_ALONE / OTHER are not auto-detected.
 */
object OsmTagMapping {

    data class OsmFilter(
        val key: String,
        val values: List<String>,
    )

    /** Per-risk Overpass search radius (supermarket/convenience larger). */
    fun searchRadiusMeters(risk: RiskPlaceId): Int = when (risk) {
        RiskPlaceId.SUPERMARKET -> 180
        RiskPlaceId.BAR,
        RiskPlaceId.LIQUOR_STORE,
        RiskPlaceId.PARTY -> 120
        RiskPlaceId.HOME_ALONE,
        RiskPlaceId.OTHER -> 120
    }

    fun filtersFor(risk: RiskPlaceId): List<OsmFilter> = when (risk) {
        RiskPlaceId.BAR -> listOf(
            OsmFilter("amenity", listOf("bar", "pub", "biergarten")),
        )
        RiskPlaceId.LIQUOR_STORE -> listOf(
            OsmFilter("shop", listOf("alcohol", "wine")),
        )
        RiskPlaceId.SUPERMARKET -> listOf(
            OsmFilter("shop", listOf("supermarket", "convenience")),
        )
        RiskPlaceId.PARTY -> listOf(
            OsmFilter("amenity", listOf("nightclub", "bar")),
        )
        RiskPlaceId.HOME_ALONE -> emptyList()
        RiskPlaceId.OTHER -> emptyList()
    }

    /** Risk chips that can be auto-detected via OSM. */
    fun detectable(selected: Set<RiskPlaceId>): Set<RiskPlaceId> =
        selected.filter { filtersFor(it).isNotEmpty() }.toSet()

    /**
     * Resolve an OSM element's tags to a selected risk chip.
     * More specific nightlife tags win for PARTY; shared `bar` prefers BAR when both selected.
     */
    fun riskForTags(tags: Map<String, String>, selected: Set<RiskPlaceId>): RiskPlaceId? {
        val amenity = tags["amenity"]
        val shop = tags["shop"]

        if (amenity == "nightclub" && RiskPlaceId.PARTY in selected) {
            return RiskPlaceId.PARTY
        }
        if (amenity in setOf("bar", "pub", "biergarten")) {
            when {
                RiskPlaceId.BAR in selected -> return RiskPlaceId.BAR
                RiskPlaceId.PARTY in selected -> return RiskPlaceId.PARTY
            }
        }
        if (shop in setOf("alcohol", "wine") && RiskPlaceId.LIQUOR_STORE in selected) {
            return RiskPlaceId.LIQUOR_STORE
        }
        if (shop in setOf("supermarket", "convenience") && RiskPlaceId.SUPERMARKET in selected) {
            return RiskPlaceId.SUPERMARKET
        }
        return null
    }

    fun tagLabels(tags: Map<String, String>): List<String> = buildList {
        tags["amenity"]?.let { add("amenity=$it") }
        tags["shop"]?.let { add("shop=$it") }
    }
}
