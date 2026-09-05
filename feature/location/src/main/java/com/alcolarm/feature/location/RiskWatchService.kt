package com.alcolarm.feature.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Foreground service (type=location) that keeps [RiskWatchEngine] running when the
 * app UI is backgrounded. Ongoing notification is anonymous / shame-free.
 */
@AndroidEntryPoint
class RiskWatchService : Service() {

    @Inject lateinit var engine: RiskWatchEngine
    @Inject lateinit var manager: RiskWatchManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stop action received")
                serviceScope.launch {
                    engine.stop()
                    manager.onBackgroundServiceStopped()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            else -> {
                startAsForeground()
                serviceScope.launch {
                    engine.start(serviceScope, LocationUpdateMode.BACKGROUND)
                    manager.onBackgroundServiceStarted()
                }
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        runBlocking {
            // Ensure fused updates stop if the system tears down the FGS.
            if (manager.isBackgroundServiceRunning() || engine.running.value) {
                engine.stop()
            }
        }
        manager.onBackgroundServiceStopped()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        ensureChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "Foreground location service started")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.risk_watch_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.risk_watch_notification_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPi = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.risk_watch_notification_title))
            .setContentText(getString(R.string.risk_watch_notification_text))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPi)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "AlcoLarm.WatchService"
        const val CHANNEL_ID = "risk_watch_location"
        const val NOTIFICATION_ID = 41001
        const val ACTION_STOP = "com.alcolarm.feature.location.STOP_WATCH"

        fun start(context: Context) {
            val intent = Intent(context, RiskWatchService::class.java)
            ContextCompatStart.start(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RiskWatchService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                context.stopService(Intent(context, RiskWatchService::class.java))
            }
        }
    }
}

/** Local helper to avoid importing androidx.core at call sites twice. */
private object ContextCompatStart {
    fun start(context: Context, intent: Intent) {
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }
}
