package com.alcolarm.feature.location

/**
 * In-memory only — never written to DataStore / disk.
 *
 * @param speedMps Ground speed from Fused Location when [android.location.Location.hasSpeed]
 *   is true; null when the provider did not report speed.
 */
data class LiveLocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long = System.currentTimeMillis(),
    val speedMps: Float? = null,
)
