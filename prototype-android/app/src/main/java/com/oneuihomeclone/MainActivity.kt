package com.oneuihomeclone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.oneuihomeclone.data.LauncherDataStore
import com.oneuihomeclone.data.LauncherLayoutStore
import com.oneuihomeclone.data.WidgetPersistence
import com.oneuihomeclone.ui.OneUiHomeCloneApp
import com.oneuihomeclone.ui.SafeRecoveryCheckingScreen
import com.oneuihomeclone.ui.SafeRecoveryScreen
import com.oneuihomeclone.ui.theme.OneUiHomeCloneTheme
import com.oneuihomeclone.widgets.WidgetBindContract
import com.oneuihomeclone.widgets.WidgetBindRequest
import com.oneuihomeclone.widgets.WidgetBindResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    /**
     * Bumped every time the system delivers a HOME intent (user pressed HOME or
     * selected the launcher from the home-app picker). Compose observes this to reset
     * overlay state and scroll back to the default home page.
     */
    private var homeIntentTick by mutableIntStateOf(0)
    private var recoveryGate by mutableStateOf<RecoveryGate>(RecoveryGate.Checking)
    private var defaultLauncherState by mutableStateOf(DefaultLauncherState.Unknown)

    /**
     * ActivityResultLauncher for `ACTION_APPWIDGET_BIND`. Registered before `setContent`
     * so the lifecycle owner is in CREATED state — registration during/after RESUMED
     * throws `IllegalStateException`. The lambda is hot-swapped per request via
     * `LauncherApp`'s pending bind callback so callers from deep in Compose can await
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

        // Keep disk IO off the main thread, but do not mount the full launcher tree
        // until the previous crash log check opens recovery mode or releases Home.
        setContent {
            OneUiHomeCloneTheme {
                when (val gate = recoveryGate) {
                    RecoveryGate.Checking -> SafeRecoveryCheckingScreen()
                    is RecoveryGate.Ready -> {
                        OneUiHomeCloneApp(
                            homeIntentTick = homeIntentTick,
                            recoveryNotice = gate.recoveryNotice,
                            defaultLauncherState = defaultLauncherState,
                            onOpenDefaultLauncherSettings = ::openDefaultLauncherSettings,
                        )
                    }
                    is RecoveryGate.SafeMode -> {
                        SafeRecoveryScreen(
                            summary = gate.summary,
                            actionMessage = gate.actionMessage,
                            actionInProgress = gate.actionInProgress,
                            onResetLayout = ::resetLayoutFromSafeMode,
                            onResetSettings = ::resetSettingsFromSafeMode,
                            onClearWidgets = ::clearWidgetsFromSafeMode,
                            onExportDiagnostics = { exportDiagnosticsFromSafeMode(gate.summary) },
                            onContinue = ::continueFromSafeMode,
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            val summary = withContext(Dispatchers.IO) {
                LauncherApp.consumePreviousCrashLog()
            }
            if (summary != null) {
                Log.w(TAG, "Previous crash recovered: ${summary.toLogLine()}")
                recoveryGate = RecoveryGate.SafeMode(summary)
            } else {
                recoveryGate = RecoveryGate.Ready()
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

    override fun onResume() {
        super.onResume()
        refreshDefaultLauncherState()
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

    private fun continueFromSafeMode() {
        recoveryGate = RecoveryGate.Ready(getString(R.string.feedback_recovery_continue))
    }

    private fun refreshDefaultLauncherState() {
        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) {
                queryDefaultLauncherState(applicationContext)
            }
            defaultLauncherState = state
        }
    }

    private fun openDefaultLauncherSettings() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { createDefaultLauncherSettingsIntent(applicationContext) }
            }
            result.fold(
                onSuccess = { intent ->
                    runCatching { startActivity(intent) }
                        .onFailure { cause -> Log.e(TAG, "Default launcher settings launch failed", cause) }
                },
                onFailure = { cause -> Log.e(TAG, "Default launcher settings launch failed", cause) },
            )
        }
    }

    private fun resetLayoutFromSafeMode() {
        runRecoveryAction(
            actionLogName = "reset-layout",
            actionLabel = getString(R.string.safe_mode_reset_layout),
            successMessage = getString(R.string.safe_mode_layout_reset_done),
        ) {
            LauncherLayoutStore(applicationContext).clear()
        }
    }

    private fun resetSettingsFromSafeMode() {
        runRecoveryAction(
            actionLogName = "reset-settings",
            actionLabel = getString(R.string.safe_mode_reset_settings),
            successMessage = getString(R.string.safe_mode_settings_reset_done),
        ) {
            LauncherDataStore(applicationContext).clear()
        }
    }

    private fun clearWidgetsFromSafeMode() {
        runRecoveryAction(
            actionLogName = "clear-widgets",
            actionLabel = getString(R.string.safe_mode_clear_widgets),
            successMessage = getString(R.string.safe_mode_widgets_cleared_done),
        ) {
            LauncherApp.resetWidgetHost()
            WidgetPersistence(applicationContext).clear()
        }
    }

    private fun exportDiagnosticsFromSafeMode(summary: PreviousCrashSummary) {
        val current = recoveryGate as? RecoveryGate.SafeMode ?: return
        val actionLabel = getString(R.string.safe_mode_export_diagnostics)
        recoveryGate = current.copy(
            actionInProgress = true,
            actionMessage = getString(R.string.safe_mode_action_running),
        )
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    LauncherApp.writeRecoveryDiagnostics(summary)?.name
                        ?: error("LauncherApp unavailable")
                }
            }
            recoveryGate = result.fold(
                onSuccess = { fileName ->
                    Log.w(TAG, "Recovery action completed: export-diagnostics")
                    current.copy(
                        actionInProgress = false,
                        actionMessage = getString(R.string.safe_mode_diagnostics_exported, fileName),
                    )
                },
                onFailure = { cause ->
                    Log.e(TAG, "Recovery action failed: export-diagnostics", cause)
                    current.copy(
                        actionInProgress = false,
                        actionMessage = getString(R.string.safe_mode_action_failed, actionLabel),
                    )
                },
            )
        }
    }

    private fun runRecoveryAction(
        actionLogName: String,
        actionLabel: String,
        successMessage: String,
        block: suspend () -> Unit,
    ) {
        val current = recoveryGate as? RecoveryGate.SafeMode ?: return
        recoveryGate = current.copy(
            actionInProgress = true,
            actionMessage = getString(R.string.safe_mode_action_running),
        )
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { block() } }
            recoveryGate = result.fold(
                onSuccess = {
                    Log.w(TAG, "Recovery action completed: $actionLogName")
                    current.copy(actionInProgress = false, actionMessage = successMessage)
                },
                onFailure = { cause ->
                    Log.e(TAG, "Recovery action failed: $actionLogName", cause)
                    current.copy(
                        actionInProgress = false,
                        actionMessage = getString(R.string.safe_mode_action_failed, actionLabel),
                    )
                },
            )
        }
    }
}

/** Callback shape returned after an ACTION_APPWIDGET_BIND flow completes. */
typealias WidgetBindCallback = (WidgetBindResult) -> Unit

private sealed interface RecoveryGate {
    data object Checking : RecoveryGate

    data class Ready(val recoveryNotice: String? = null) : RecoveryGate

    data class SafeMode(
        val summary: PreviousCrashSummary,
        val actionMessage: String? = null,
        val actionInProgress: Boolean = false,
    ) : RecoveryGate
}
