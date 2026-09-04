package com.alcolarm.feature.location

/**
 * In-memory only — never written to DataStore / disk.
 */
data class LiveLocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long = System.currentTimeMillis(),
)
