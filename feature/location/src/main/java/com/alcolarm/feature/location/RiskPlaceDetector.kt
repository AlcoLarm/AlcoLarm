package com.alcolarm.feature.location

import android.util.Log
import com.alcolarm.core.model.RiskPlaceId
import com.alcolarm.feature.location.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
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
    data object MissingApiKey : RiskDetectResult()
    data class Error(val message: String) : RiskDetectResult()
}

/**
 * Google Places Nearby Search (REST) around the current live fix.
 * Does not store coordinates — only returns whether a matching amenity is nearby.
 */
@Singleton
class RiskPlaceDetector @Inject constructor(
    private val http: OkHttpClient,
) {
    fun hasApiKey(): Boolean = BuildConfig.MAPS_API_KEY.isNotBlank()

    suspend fun detectNearby(
        latitude: Double,
        longitude: Double,
        selectedRisks: Set<RiskPlaceId>,
        radiusMeters: Int = SEARCH_RADIUS_METERS,
    ): RiskDetectResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank()) {
            return@withContext RiskDetectResult.MissingApiKey
        }
        val detectable = PlacesTypeMapping.detectable(selectedRisks)
        if (detectable.isEmpty()) {
            return@withContext RiskDetectResult.None
        }

        // Nearby Search accepts one `type` per request — query each unique type.
        val typesToQuery = detectable
            .flatMap { PlacesTypeMapping.placeTypesFor(it) }
            .distinct()

        for (type in typesToQuery) {
            val match = queryNearby(apiKey, latitude, longitude, radiusMeters, type, detectable)
            when {
                match != null -> return@withContext RiskDetectResult.Match(match)
            }
        }
        RiskDetectResult.None
    }

    private fun queryNearby(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        type: String,
        selected: Set<RiskPlaceId>,
    ): NearbyRiskMatch? {
        val url = NEARBY_URL.toHttpUrl().newBuilder()
            .addQueryParameter("location", "$latitude,$longitude")
            .addQueryParameter("radius", radiusMeters.toString())
            .addQueryParameter("type", type)
            .addQueryParameter("key", apiKey)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AlcoLarm/0.2.0-mvp (Android; recovery support)")
            .get()
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Places Nearby HTTP ${response.code} for type=$type")
                    return null
                }
                val body = response.body?.string().orEmpty()
                parseFirstMatch(body, type, selected)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Places Nearby failed for type=$type: ${e.message}")
            null
        }
    }

    private fun parseFirstMatch(
        json: String,
        requestedType: String,
        selected: Set<RiskPlaceId>,
    ): NearbyRiskMatch? {
        val root = JSONObject(json)
        val status = root.optString("status")
        if (status != "OK" && status != "ZERO_RESULTS") {
            Log.w(TAG, "Places status=$status error=${root.optString("error_message")}")
            return null
        }
        if (status == "ZERO_RESULTS") return null

        val results = root.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val first = results.getJSONObject(0)
        val name = first.optString("name", "Nearby place")
        val typesJson = first.optJSONArray("types")
        val types = buildList {
            if (typesJson != null) {
                for (i in 0 until typesJson.length()) {
                    add(typesJson.getString(i))
                }
            }
            if (isEmpty()) add(requestedType)
        }

        val matchedRisk = types
            .mapNotNull { PlacesTypeMapping.riskForPlaceType(it) }
            .firstOrNull { it in selected }
            ?: PlacesTypeMapping.riskForPlaceType(requestedType)?.takeIf { it in selected }

        return matchedRisk?.let {
            Log.d(TAG, "Nearby match risk=$it name=$name types=$types")
            NearbyRiskMatch(riskPlaceId = it, placeName = name, placeTypes = types)
        }
    }

    companion object {
        private const val TAG = "AlcoLarm.Places"
        private const val NEARBY_URL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
        const val SEARCH_RADIUS_METERS = 120
    }
}
