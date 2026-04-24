package com.oneuihomeclone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.oneuihomeclone.ui.OneUiHomeCloneApp
import com.oneuihomeclone.ui.motion.ProvideMotionScheme
import com.oneuihomeclone.ui.theme.OneUiHomeCloneTheme
import com.oneuihomeclone.widgets.WidgetBindContract
import com.oneuihomeclone.widgets.WidgetBindRequest
import com.oneuihomeclone.widgets.WidgetBindResult

class MainActivity : ComponentActivity() {

    /**
     * Bumped every time the system delivers a HOME intent (user pressed HOME or
     * selected the launcher from the home-app picker). Compose observes this to reset
     * overlay state and scroll back to the default home page.
     */
    private var homeIntentTick by mutableIntStateOf(0)

    /**
     * ActivityResultLauncher for `ACTION_APPWIDGET_BIND`. Registered before `setContent`
     * so the lifecycle owner is in CREATED state — registration during/after RESUMED
     * throws `IllegalStateException`. The lambda is hot-swapped per request via
     * [LauncherApp.pendingWidgetBindCallback] so callers from deep in Compose can await
     * the result without a ViewModel plumbing pass.
     */
    private lateinit var widgetBindLauncher: ActivityResultLauncher<WidgetBindRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        widgetBindLauncher = registerForActivityResult(WidgetBindContract()) { result ->
            val pending = LauncherApp.consumePendingWidgetBindCallback()
            if (pending != null) {
                runCatching { pending(result) }
                    .onFailure { Log.e(TAG, "Widget bind callback threw", it) }
            } else {
                Log.w(TAG, "Widget bind result had no pending callback: $result")
            }
        }
        LauncherApp.registerWidgetBindLauncher(widgetBindLauncher)

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
                ProvideMotionScheme {
                    OneUiHomeCloneApp(
                        homeIntentTick = homeIntentTick,
                    )
                }
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

    override fun onDestroy() {
        // The ActivityResultLauncher is owned by this Activity; when the instance goes
        // away, drop the companion-held reference so a stale launcher from a finished
        // Activity can't be invoked. Also flush any in-flight bind callback — its
        // closure pins Compose state on an Activity that is finishing, which would leak
        // the composition tree across a rotation that happens while the bind dialog is up.
        LauncherApp.clearWidgetBindLauncher(widgetBindLauncher)
        LauncherApp.cancelPendingWidgetBind()
        super.onDestroy()
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

/** Callback shape returned after an ACTION_APPWIDGET_BIND flow completes. */
typealias WidgetBindCallback = (WidgetBindResult) -> Unit
