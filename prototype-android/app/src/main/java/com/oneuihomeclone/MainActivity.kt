package com.oneuihomeclone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.oneuihomeclone.ui.OneUiHomeCloneApp
import com.oneuihomeclone.ui.theme.OneUiHomeCloneTheme

class MainActivity : ComponentActivity() {

    /**
     * Bumped every time the system delivers a HOME intent (user pressed HOME or
     * selected the launcher from the home-app picker). Compose observes this to reset
     * overlay state and scroll back to the default home page.
     */
    private var homeIntentTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Surface a toast if the launcher died on the previous run. The log file is
        // cleared atomically in consume — user only sees this once per crash.
        LauncherApp.consumePreviousCrashLog()?.let { log ->
            Log.w(TAG, "Previous crash:\n$log")
            Toast.makeText(
                this,
                "One UI Home Clone recovered from a crash on its previous run.",
                Toast.LENGTH_LONG,
            ).show()
        }

        setContent {
            OneUiHomeCloneTheme {
                OneUiHomeCloneApp(
                    homeIntentTick = homeIntentTick,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // AppWidgetHost documentation requires matched start/stop across the Activity
        // lifecycle — without this, widget update broadcasts are dropped.
        runCatching { LauncherApp.appWidgetHost()?.startListening() }
            .onFailure { Log.e(TAG, "Widget host startListening failed", it) }
    }

    override fun onStop() {
        super.onStop()
        runCatching { LauncherApp.appWidgetHost()?.stopListening() }
            .onFailure { Log.e(TAG, "Widget host stopListening failed", it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // We're registered as HOME + DEFAULT — any re-entry (HOME key, launcher picker)
        // arrives here because launchMode=singleTask. Increment tick so Compose can
        // observe via LaunchedEffect(homeIntentTick) and collapse overlays.
        if (Intent.ACTION_MAIN == intent.action &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            homeIntentTick += 1
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
