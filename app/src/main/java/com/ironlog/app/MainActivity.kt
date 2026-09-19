package com.ironlog.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ironlog.app.ui.navigation.AppNavigation
import com.ironlog.app.ui.theme.ProYouTheme
import com.ironlog.app.util.SettingsStore
import com.ironlog.app.util.SettingsStore.ThemeConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsStore: SettingsStore

    private var latestIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        latestIntentState = Intent(intent)
        setContent {
            val themeConfig by settingsStore.themeConfig.collectAsStateWithLifecycle(initialValue = ThemeConfig.DARK)
            ProYouTheme(themeConfig = themeConfig) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(startIntent = latestIntentState)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Dedup: ignore re-delivery of an intent we've already processed (system can
        // re-fire the same intent on resume, which would cause the navigation graph to
        // re-trigger deep-link side effects).
        val current = latestIntentState
        if (current != null &&
            current.action == intent.action &&
            current.data == intent.data &&
            sameExtras(current, intent)
        ) {
            return
        }
        latestIntentState = Intent(intent)
    }

    private fun sameExtras(a: Intent, b: Intent): Boolean {
        val aBundle = a.extras ?: return b.extras == null
        val bBundle = b.extras ?: return false
        if (aBundle.size() != bBundle.size()) return false
        for (key in aBundle.keySet()) {
            @Suppress("DEPRECATION")
            if (aBundle.get(key) != bBundle.get(key)) return false
        }
        return true
    }
}

