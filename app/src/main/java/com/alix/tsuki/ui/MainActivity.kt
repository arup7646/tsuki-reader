package com.alix.tsuki.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.alix.tsuki.TsukiApp
import com.alix.tsuki.data.model.ThemeMode
import com.alix.tsuki.ui.navigation.TsukiNavHost
import com.alix.tsuki.ui.theme.TsukiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TsukiApp

        setContent {
            val themeMode by app.preferences.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

            TsukiTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TsukiNavHost()
                }
            }
        }
    }
}
