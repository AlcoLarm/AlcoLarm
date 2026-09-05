package com.alcolarm.feature.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground live location via Play Services FusedLocationProviderClient.
 * Keeps only the current sample in memory — no persisted history trail.
 * (A short ring buffer for still/dwell math lives in [HomeViewModel] and is cleared on stop.)
 */
@Singleton
class LiveLocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedClient: FusedLocationProviderClient,
) {
    private val _current = MutableStateFlow<LiveLocationSample?>(null)
    val current: StateFlow<LiveLocationSample?> = _current.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _current.value = loc.toSample()
            Log.d(
                TAG,
                "Live fix accuracy=${loc.accuracy}m speed=${if (loc.hasSpeed()) loc.speed else "n/a"} (coords omitted from prefs)",
            )
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (_isTracking.value) return
        if (!hasLocationPermission()) {
            Log.w(TAG, "start() skipped — no location permission")
            return
        }
        // Frequent enough for speed/stillness; min distance 0 so standing still still yields samples.
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS,
        )
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .setWaitForAccurateLocation(false)
            .build()

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        _isTracking.value = true
        Log.d(TAG, "Fused location updates started (foreground MVP, speed-aware)")

        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && _current.value == null) {
                _current.value = loc.toSample()
            }
        }
    }

    fun stop() {
        if (!_isTracking.value) return
        fusedClient.removeLocationUpdates(callback)
        _isTracking.value = false
        Log.d(TAG, "Fused location updates stopped")
    }

    /** Clear in-memory sample (e.g. on process teardown). Never writes to disk. */
    fun clearSample() {
        _current.value = null
    }

    companion object {
        private const val TAG = "AlcoLarm.Location"
        /** ~15 s cadence so still/dwell can accumulate without waiting on large gaps. */
        private const val UPDATE_INTERVAL_MS = 10_000L
        private const val MIN_UPDATE_INTERVAL_MS = 5_000L
        /** 0 m — must receive fixes while the user is stopped (pass-by vs stay). */
        private const val MIN_DISTANCE_METERS = 0f

        private fun Location.toSample(): LiveLocationSample =
            LiveLocationSample(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracy,
                timestampMillis = time.takeIf { it > 0 } ?: System.currentTimeMillis(),
                speedMps = if (hasSpeed()) speed else null,
            )
    }
}
