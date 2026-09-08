package com.alcolarm.feature.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quietly restarts [RiskWatchService] after device boot or app update when the
 * user previously had monitoring enabled. Does **not** open [MainActivity] or
 * show any UI beyond the existing FGS notification.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var watchManager: RiskWatchManager

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in HANDLED_ACTIONS) return

        Log.d(TAG, "Received $action — attempting quiet watch resume")
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                watchManager.resumeWatchAfterBoot()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlcoLarm.BootReceiver"

        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
