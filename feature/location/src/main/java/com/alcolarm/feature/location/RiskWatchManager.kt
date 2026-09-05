package com.alcolarm.feature.location

import android.content.Context
import android.util.Log
import com.alcolarm.core.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates foreground-session vs background FGS ownership of [RiskWatchEngine].
 *
 * - Background location granted + user wants watch → [RiskWatchService]
 * - Fine location only → in-process session while Home is resumed
 */
@Singleton
class RiskWatchManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: RiskWatchEngine,
    private val locationTracker: LiveLocationTracker,
    private val repository: UserPreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    enum class Owner { NONE, FOREGROUND_SESSION, BACKGROUND_SERVICE }

    private val _owner = MutableStateFlow(Owner.NONE)
    val owner: StateFlow<Owner> = _owner.asStateFlow()

    val monitoring: StateFlow<HomeMonitoringUi> = engine.monitoring

    val backgroundWatchEnabled: StateFlow<Boolean> = repository.backgroundWatchEnabled.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    private val _needsBackgroundLocation = MutableStateFlow(false)
    /** True when user has fine location but not background — show status CTA. */
    val needsBackgroundLocation: StateFlow<Boolean> = _needsBackgroundLocation.asStateFlow()

    fun hasLocationPermission(): Boolean = locationTracker.hasLocationPermission()
    fun hasBackgroundLocationPermission(): Boolean = locationTracker.hasBackgroundLocationPermission()
    fun isBackgroundServiceRunning(): Boolean = _owner.value == Owner.BACKGROUND_SERVICE

    fun onBackgroundServiceStarted() {
        _owner.value = Owner.BACKGROUND_SERVICE
        _needsBackgroundLocation.value = false
        Log.d(TAG, "Background service owns watch")
    }

    fun onBackgroundServiceStopped() {
        if (_owner.value == Owner.BACKGROUND_SERVICE) {
            _owner.value = Owner.NONE
        }
        Log.d(TAG, "Background service stopped")
    }

    /**
     * Called from Home ON_RESUME when location is granted and risks are set up.
     * Prefers auto-starting the FGS when background location is available.
     */
    fun onHomeResumed() {
        scope.launch {
            mutex.withLock {
                engine.refreshPermissionBanner()
                if (!locationTracker.hasLocationPermission()) {
                    _needsBackgroundLocation.value = false
                    return@withLock
                }
                val wantBackground = repository.backgroundWatchEnabled.first()
                val hasBg = locationTracker.hasBackgroundLocationPermission()
                _needsBackgroundLocation.value = wantBackground && !hasBg

                if (wantBackground && hasBg) {
                    if (_owner.value != Owner.BACKGROUND_SERVICE) {
                        // Stop any foreground-only session before promoting to FGS.
                        if (_owner.value == Owner.FOREGROUND_SESSION) {
                            // Engine keeps running; service will re-bind mode.
                        }
                        RiskWatchService.start(context)
                        _owner.value = Owner.BACKGROUND_SERVICE
                    }
                } else {
                    // Foreground-only session while Home is visible.
                    if (_owner.value == Owner.BACKGROUND_SERVICE) {
                        // Lost background permission — stop FGS, fall back.
                        RiskWatchService.stop(context)
                        _owner.value = Owner.NONE
                    }
                    if (_owner.value != Owner.BACKGROUND_SERVICE) {
                        _owner.value = Owner.FOREGROUND_SESSION
                        engine.start(scope, LocationUpdateMode.FOREGROUND)
                    }
                }
            }
        }
    }

    /**
     * Home ON_PAUSE: do **not** stop if background FGS owns the watch.
     */
    fun onHomePaused() {
        scope.launch {
            mutex.withLock {
                if (_owner.value == Owner.BACKGROUND_SERVICE) {
                    Log.d(TAG, "Home paused — background watch continues")
                    return@withLock
                }
                if (_owner.value == Owner.FOREGROUND_SESSION) {
                    engine.stop()
                    _owner.value = Owner.NONE
                    Log.d(TAG, "Home paused — foreground session stopped")
                }
            }
        }
    }

    /** User toggled background watch off, or cleared / logged out. */
    fun stopCompletely() {
        scope.launch {
            mutex.withLock {
                if (_owner.value == Owner.BACKGROUND_SERVICE) {
                    RiskWatchService.stop(context)
                }
                engine.stop()
                locationTracker.clearSample()
                _owner.value = Owner.NONE
                Log.d(TAG, "Watch stopped completely")
            }
        }
    }

    fun setBackgroundWatchEnabled(enabled: Boolean) {
        scope.launch {
            repository.setBackgroundWatchEnabled(enabled)
            if (!enabled) {
                stopCompletely()
            } else {
                onHomeResumed()
            }
        }
    }

    /** After background location is granted from settings / rationale. */
    fun onBackgroundPermissionGranted() {
        scope.launch {
            repository.setBackgroundWatchEnabled(true)
            onHomeResumed()
        }
    }

    companion object {
        private const val TAG = "AlcoLarm.WatchManager"
    }
}
