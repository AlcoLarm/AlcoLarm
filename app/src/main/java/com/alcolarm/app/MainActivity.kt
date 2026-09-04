package com.alcolarm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.alcolarm.app.navigation.AlcoLarmNavHost
import com.alcolarm.core.designsystem.theme.AlcoLarmTheme
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlcoLarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClearSignalColors.NearBlack,
                ) {
                    AlcoLarmNavHost()
                }
            }
        }
    }
}
