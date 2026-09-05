package com.alcolarm.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.alcolarm.app.navigation.AlcoLarmNavHost
import com.alcolarm.core.designsystem.theme.AlcoLarmTheme
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.feature.alert.CallStyleAlertController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openAlertRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyLockScreenFlagsIfNeeded(intent)
        consumeOpenAlertIntent(intent)
        setContent {
            AlcoLarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClearSignalColors.NearBlack,
                ) {
                    AlcoLarmNavHost(
                        openAlertRequested = openAlertRequest,
                        onOpenAlertConsumed = { openAlertRequest = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLockScreenFlagsIfNeeded(intent)
        consumeOpenAlertIntent(intent)
    }

    private fun consumeOpenAlertIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(CallStyleAlertController.EXTRA_OPEN_ALERT, false) == true) {
            openAlertRequest = true
            intent.removeExtra(CallStyleAlertController.EXTRA_OPEN_ALERT)
        }
    }

    private fun applyLockScreenFlagsIfNeeded(intent: Intent?) {
        if (intent?.getBooleanExtra(CallStyleAlertController.EXTRA_OPEN_ALERT, false) != true) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }
}
