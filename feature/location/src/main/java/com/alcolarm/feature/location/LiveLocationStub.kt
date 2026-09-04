package com.alcolarm.feature.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live location only — no history buffer, no trail persistence.
 * Places / geofencing will replace the simulate path later.
 *
 * ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION are not declared in the app
 * manifest yet; they return when Places / geofencing is wired. The simulate
 * path does not need runtime location permission.
 */
data class LiveLocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)

@Singleton
class LiveLocationStub @Inject constructor() {
    private val _current = MutableStateFlow<LiveLocationSample?>(null)
    val current: Flow<LiveLocationSample?> = _current.asStateFlow()

    /** Stub until FusedLocationProvider is wired. */
    fun publishStubSample(lat: Double = 0.0, lng: Double = 0.0) {
        _current.value = LiveLocationSample(lat, lng, accuracyMeters = 25f)
    }

    fun clear() {
        _current.value = null
    }
}
