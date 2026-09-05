package com.alcolarm.feature.alert

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pragmatic MVP: after Dial from alert, when the user returns to the app
 * within [DEFAULT_WINDOW_MS], treat as a dial-return that needs an outcome choice.
 */
@Singleton
class DialReturnTracker @Inject constructor() {
    @Volatile
    private var dialStartedAtElapsedMs: Long? = null

    fun markDialStarted() {
        dialStartedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    fun clear() {
        dialStartedAtElapsedMs = null
    }

    /**
     * @return true if a dial was started within the window and was consumed.
     */
    fun consumePendingWithin(windowMs: Long = DEFAULT_WINDOW_MS): Boolean {
        val started = dialStartedAtElapsedMs ?: return false
        dialStartedAtElapsedMs = null
        val age = SystemClock.elapsedRealtime() - started
        return age in 0..windowMs
    }

    fun hasPendingWithin(windowMs: Long = DEFAULT_WINDOW_MS): Boolean {
        val started = dialStartedAtElapsedMs ?: return false
        val age = SystemClock.elapsedRealtime() - started
        return age in 0..windowMs
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 5 * 60_000L
    }
}
