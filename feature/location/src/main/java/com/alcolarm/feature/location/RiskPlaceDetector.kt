package com.alcolarm.feature.location

import android.util.Log
import com.alcolarm.core.model.RiskPlaceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class NearbyRiskMatch(
    val riskPlaceId: RiskPlaceId,
    val placeName: String,
    val placeTypes: List<String>,
)

sealed class RiskDetectResult {
    data class Match(val match: NearbyRiskMatch) : RiskDetectResult()
    data object None : RiskDetectResult()
    data class Error(val message: String) : RiskDetectResult()
}

/**
 * OpenStreetMap Overpass nearby search around the current live fix.
 * Free — no API key. Does not store coordinates; only returns whether a matching amenity is nearby.
 */
@Singleton
class RiskPlaceDetector @Inject constructor(
    private val http: OkHttpClient,
) {
    private val throttleMutex = Mutex()
    private var lastRequestAtMs = 0L

    suspend fun detectNearby(
        latitude: Double,
        longitude: Double,
        selectedRisks: Set<RiskPlaceId>,
        radiusMeters: Int = SEARCH_RADIUS_METERS,
    ): RiskDetectResult = withContext(Dispatchers.IO) {
        val detectable = OsmTagMapping.detectable(selectedRisks)
        if (detectable.isEmpty()) {
            return@withContext RiskDetectResult.None
        }

        throttlePolitely()

        val query = buildOverpassQuery(latitude, longitude, radiusMeters, detectable)
        val request = Request.Builder()
            .url(OVERPASS_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .post(FormBody.Builder().add("data", query).build())
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = "Overpass HTTP ${response.code}"
                    Log.w(TAG, "$msg body=${body.take(200)}")
                    return@withContext RiskDetectResult.Error(msg)
                }
                parseFirstMatch(body, detectable)
                    ?: RiskDetectResult.None
            }
        } catch (e: Exception) {
            Log.e(TAG, "Overpass failed: ${e.message}")
            RiskDetectResult.Error(e.message ?: "network error")
        }
    }

    private suspend fun throttlePolitely() {
        throttleMutex.withLock {
            val now = System.currentTimeMillis()
            val wait = MIN_REQUEST_GAP_MS - (now - lastRequestAtMs)
            if (wait > 0) {
                delay(wait)
            }
            lastRequestAtMs = System.currentTimeMillis()
        }
    }

    private fun buildOverpassQuery(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        selected: Set<RiskPlaceId>,
    ): String {
        val filters = selected.flatMap { OsmTagMapping.filtersFor(it) }
            .groupBy { it.key }
            .mapValues { (_, list) -> list.flatMap { it.values }.distinct() }

        val clauses = buildString {
            for ((key, values) in filters) {
                val regex = values.joinToString("|")
                append("  node(around:$radiusMeters,$latitude,$longitude)[\"$key\"~\"^($regex)$\"];\n")
                append("  way(around:$radiusMeters,$latitude,$longitude)[\"$key\"~\"^($regex)$\"];\n")
            }
        }

        return """
            [out:json][timeout:15];
            (
            $clauses
            );
            out center tags 12;
        """.trimIndent()
    }

    private fun parseFirstMatch(
        json: String,
        selected: Set<RiskPlaceId>,
    ): RiskDetectResult.Match? {
        val root = JSONObject(json)
        val elements = root.optJSONArray("elements") ?: return null
        if (elements.length() == 0) return null

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tagsObj = el.optJSONObject("tags") ?: continue
            val tags = buildMap {
                val keys = tagsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, tagsObj.optString(k))
                }
            }
            val risk = OsmTagMapping.riskForTags(tags, selected) ?: continue
            val name = tags["name"]
                ?.takeIf { it.isNotBlank() }
                ?: tags["brand"]
                ?.takeIf { it.isNotBlank() }
                ?: "Nearby place"
            val types = OsmTagMapping.tagLabels(tags).ifEmpty { listOf(risk.name.lowercase()) }
            Log.d(TAG, "Nearby OSM match risk=$risk name=$name types=$types")
            return RiskDetectResult.Match(
                NearbyRiskMatch(riskPlaceId = risk, placeName = name, placeTypes = types),
            )
        }
        return null
    }

    companion object {
        private const val TAG = "AlcoLarm.Overpass"
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
        private const val USER_AGENT = "AlcoLarm/0.5 (recovery support app)"
        /** Nearby search radius (~120 m). Unchanged for stop+dwell alerts. */
        const val SEARCH_RADIUS_METERS = 120
        /** Be polite to the public Overpass instance (extra to HomeVM's ~50s cycle). */
        private const val MIN_REQUEST_GAP_MS = 10_000L
    }
}
