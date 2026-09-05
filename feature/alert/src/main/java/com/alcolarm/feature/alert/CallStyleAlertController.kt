package com.alcolarm.feature.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Incoming-call style ring + vibrate + high-priority full-screen notification.
 * Public surfaces stay anonymous (no alcohol / relapse / risk wording).
 */
@Singleton
class CallStyleAlertController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lock = Any()
    private var ringtone: Ringtone? = null
    private var active = false

    fun start(contactDisplayName: String?, contactPhone: String?) {
        synchronized(lock) {
            if (active) {
                stopLocked()
            }
            active = true
            ensureChannel()
            playRingtone()
            startVibration()
            postCallNotification(contactDisplayName, contactPhone)
            Log.d(TAG, "Call-style alert started")
        }
    }

    fun stop() {
        synchronized(lock) {
            if (!active) return
            stopLocked()
            Log.d(TAG, "Call-style alert stopped")
        }
    }

    private fun stopLocked() {
        active = false
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
        stopVibration()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun playRingtone() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: return
        val tone = RingtoneManager.getRingtone(context, uri) ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        tone.audioAttributes = attrs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            tone.isLooping = true
        }
        try {
            tone.play()
            ringtone = tone
        } catch (e: Exception) {
            Log.w(TAG, "Ringtone play failed", e)
        }
    }

    private fun startVibration() {
        val vibrator = vibrator() ?: return
        // Call-like: short buzz, pause, longer buzz, longer pause — repeat.
        val pattern = longArrayOf(0, 400, 200, 900, 700)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator()?.cancel()
    }

    private fun vibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming calls",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Call-style alerts"
            // Ringtone is played separately so we can loop / stop cleanly.
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun postCallNotification(contactDisplayName: String?, contactPhone: String?) {
        val title = contactDisplayName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Incoming call"
        val text = when {
            !contactPhone.isNullOrBlank() -> maskPhone(contactPhone)
            else -> "Mobile"
        }

        val openIntent = Intent().apply {
            setClassName(context.packageName, MAIN_ACTIVITY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ALERT, true)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context,
            REQUEST_FULL_SCREEN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentPi = PendingIntent.getActivity(
            context,
            REQUEST_CONTENT,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPi)
            .setFullScreenIntent(fullScreenPi, true)
            .setSilent(true) // we own ringtone + vibration
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing", e)
        }
    }

    companion object {
        private const val TAG = "AlcoLarm.CallAlert"
        const val CHANNEL_ID = "call_style_alert"
        const val NOTIFICATION_ID = 42001
        const val EXTRA_OPEN_ALERT = "com.alcolarm.OPEN_ALERT"
        private const val MAIN_ACTIVITY = "com.alcolarm.app.MainActivity"
        private const val REQUEST_FULL_SCREEN = 42011
        private const val REQUEST_CONTENT = 42012

        fun maskPhone(raw: String): String {
            val digits = raw.filter { it.isDigit() }
            if (digits.length < 4) return "Mobile"
            val last = digits.takeLast(4)
            return "••• •• ${last.substring(0, 2)} ${last.substring(2)}"
        }
    }
}
