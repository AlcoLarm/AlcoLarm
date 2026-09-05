package com.alcolarm.feature.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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

enum class LocationUpdateMode {
    /** High accuracy while Home is resumed / near a candidate. */
    FOREGROUND,
    /** Balanced power while background FGS runs away from candidates. */
    BACKGROUND,
    /** Slightly more frequent balanced updates while still near a candidate in background. */
    BACKGROUND_NEAR,
}

/**
 * Live location via Play Services FusedLocationProviderClient.
 * Keeps only the current sample in memory — no persisted history trail.
 * (A short ring buffer for still/dwell math lives in [RiskWatchEngine] and is cleared on stop.)
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

    private var activeMode: LocationUpdateMode? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _current.value = loc.toSample()
            Log.d(
                TAG,
                "Live fix accuracy=${loc.accuracy}m speed=${if (loc.hasSpeed()) loc.speed else "n/a"} mode=$activeMode (coords omitted from prefs)",
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

    fun hasBackgroundLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return hasLocationPermission()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun start(mode: LocationUpdateMode = LocationUpdateMode.FOREGROUND) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "start() skipped — no location permission")
            return
        }
        if (_isTracking.value && activeMode == mode) return
        if (_isTracking.value) {
            fusedClient.removeLocationUpdates(callback)
        }
        val request = buildRequest(mode)
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        activeMode = mode
        _isTracking.value = true
        Log.d(TAG, "Fused location updates started mode=$mode")

        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && _current.value == null) {
                _current.value = loc.toSample()
            }
        }
    }

    /** Switch interval/priority without tearing down tracking if already on. */
    @SuppressLint("MissingPermission")
    fun setMode(mode: LocationUpdateMode) {
        if (!_isTracking.value) {
            start(mode)
            return
        }
        if (activeMode == mode) return
        start(mode)
    }

    fun stop() {
        if (!_isTracking.value) return
        fusedClient.removeLocationUpdates(callback)
        _isTracking.value = false
        activeMode = null
        Log.d(TAG, "Fused location updates stopped")
    }

    /** Clear in-memory sample (e.g. on process teardown). Never writes to disk. */
    fun clearSample() {
        _current.value = null
    }

    private fun buildRequest(mode: LocationUpdateMode): LocationRequest {
        val (priority, interval, minInterval) = when (mode) {
            LocationUpdateMode.FOREGROUND -> Triple(
                Priority.PRIORITY_HIGH_ACCURACY,
                FOREGROUND_INTERVAL_MS,
                FOREGROUND_MIN_INTERVAL_MS,
            )
            LocationUpdateMode.BACKGROUND -> Triple(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                BACKGROUND_INTERVAL_MS,
                BACKGROUND_MIN_INTERVAL_MS,
            )
            LocationUpdateMode.BACKGROUND_NEAR -> Triple(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                BACKGROUND_NEAR_INTERVAL_MS,
                BACKGROUND_NEAR_MIN_INTERVAL_MS,
            )
        }
        return LocationRequest.Builder(priority, interval)
            .setMinUpdateIntervalMillis(minInterval)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .setWaitForAccurateLocation(false)
            .build()
    }

    companion object {
        private const val TAG = "AlcoLarm.Location"

        private const val FOREGROUND_INTERVAL_MS = 10_000L
        private const val FOREGROUND_MIN_INTERVAL_MS = 5_000L

        /** Balanced while backgrounded and not near a candidate (~30–45 s). */
        private const val BACKGROUND_INTERVAL_MS = 40_000L
        private const val BACKGROUND_MIN_INTERVAL_MS = 20_000L

        /** Near a candidate: frequent enough to accumulate a 15 s dwell. */
        private const val BACKGROUND_NEAR_INTERVAL_MS = 8_000L
        private const val BACKGROUND_NEAR_MIN_INTERVAL_MS = 4_000L

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
