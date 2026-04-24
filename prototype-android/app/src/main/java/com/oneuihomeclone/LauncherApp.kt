package com.oneuihomeclone

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Process
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.oneuihomeclone.widgets.WidgetBindRequest
import com.oneuihomeclone.widgets.WidgetBindResult
import kotlin.system.exitProcess
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Application subclass. Two jobs:
 *
 *  1. Crash guard — register an `UncaughtExceptionHandler` that writes a minimal crash
 *     log to the app's `filesDir` before delegating to the default handler. The next
 *     cold start reads + surfaces the log so the user knows something went wrong and
 *     we know where to look. Without this, a crash silently drops the user back to the
 *     prior launcher with no post-mortem trail.
 *
 *  2. `AppWidgetHost` lifecycle — a launcher app must own an `AppWidgetHost` instance
 *     for its process lifetime. We allocate one here at `onCreate` so every Activity
 *     can share it; the companion exposes `start()` / `stop()` for the Activity's
 *     lifecycle to call. Actual widget binding (allocate widget id, bind permission,
 *     view inflation) is staged for v0.2.x — this plumbing exists so that work can
 *     slot in without lifecycle rewrites.
 */
class LauncherApp : Application() {

    private val crashLogFile: File
        get() = File(filesDir, CRASH_LOG_NAME)

    override fun onCreate() {
        super.onCreate()
        // Publish widgetHost BEFORE instance so any reader observing a non-null instance
        // also sees the host — tiny window otherwise produces spurious nulls. Volatile
        // writes give release-semantics to the reads in the companion accessors.
        widgetHost = AppWidgetHost(applicationContext, APP_WIDGET_HOST_ID)
        instance = this
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrashLog(thread, throwable) }
                .onFailure { Log.e(TAG, "Failed to persist crash log", it) }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                // No prior handler — without an explicit kill, the process becomes a
                // zombie showing a frozen home surface. Mirror what the default handler
                // does: terminate.
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                throwable.printStackTrace(pw)
            }
            sw.toString()
        }
        val timestamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).format(Date())
        // Intentionally record the exception class only, not message. Stack trace is
        // sufficient for root-cause in a crash log and `throwable.message` can embed
        // user search strings, file paths, or drag-op package names as features land.
        // `filesDir` is 0700 today so exposure is bounded, but the principle holds as
        // soon as any crash-report exfil path ships.
        crashLogFile.writeText(
            buildString {
                append("timestamp=").append(timestamp).append('\n')
                append("thread=").append(thread.name).append('\n')
                append("build.versionName=").append(BuildConfig.VERSION_NAME).append('\n')
                append("build.versionCode=").append(BuildConfig.VERSION_CODE).append('\n')
                append("exception=").append(throwable.javaClass.name).append('\n')
                append("---\n")
                append(stackTrace)
            },
        )
    }

    /**
     * Reads + clears the crash log written on a prior crash. Returns null if no log is
     * present. Caller is responsible for surfacing the log (toast / banner / error
     * reporter). Clearing on read guarantees we don't re-notify the user on subsequent
     * cold starts of the same crash.
     */
    fun consumePreviousCrashLog(): String? {
        val file = crashLogFile
        if (!file.exists() || file.length() == 0L) return null
        val content = runCatching { file.readText() }.getOrNull()
        file.delete()
        return content
    }

    companion object {
        private const val TAG = "LauncherApp"
        private const val CRASH_LOG_NAME = "crash-log.txt"
        private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"

        /**
         * App widget host id. 2048 dodges Lawnchair's 1024 so users migrating between
         * launchers don't collide on widget ownership in `AppWidgetHost.deleteHost()`.
         */
        const val APP_WIDGET_HOST_ID: Int = 2048

        @Volatile
        private var instance: LauncherApp? = null

        @Volatile
        private var widgetHost: AppWidgetHost? = null

        @Volatile
        private var widgetBindLauncher: ActivityResultLauncher<WidgetBindRequest>? = null

        /**
         * Callback that should fire when the current bind flow completes. AtomicReference
         * so two near-simultaneous requests can't read stale data — the second caller
         * observes null and falls back to its own error path rather than silently
         * stealing the first caller's result.
         */
        private val pendingWidgetBindCallback: AtomicReference<((WidgetBindResult) -> Unit)?> =
            AtomicReference(null)

        fun appWidgetHost(): AppWidgetHost? = widgetHost

        fun appWidgetManager(): AppWidgetManager? =
            instance?.let { AppWidgetManager.getInstance(it) }

        fun consumePreviousCrashLog(): String? = instance?.consumePreviousCrashLog()

        /** Called once per Activity creation so Compose-layer code can dispatch a bind. */
        internal fun registerWidgetBindLauncher(launcher: ActivityResultLauncher<WidgetBindRequest>) {
            widgetBindLauncher = launcher
        }

        /**
         * Drops the reference if (and only if) [launcher] matches the current one.
         * Guards against a late [onDestroy] from an old Activity unregistering a newer
         * Activity's launcher during a configuration-change race.
         */
        internal fun clearWidgetBindLauncher(launcher: ActivityResultLauncher<WidgetBindRequest>) {
            if (widgetBindLauncher === launcher) widgetBindLauncher = null
        }

        /**
         * Issues a bind request. Returns true if the Activity was able to launch the
         * system chooser; false if there is no Activity-scoped launcher right now (e.g.
         * request issued before Activity.onCreate or after onDestroy).
         */
        fun requestWidgetBind(
            request: WidgetBindRequest,
            callback: (WidgetBindResult) -> Unit,
        ): Boolean {
            val launcher = widgetBindLauncher ?: return false
            // A prior-in-flight callback would mean someone started a second bind before
            // the first returned. This shouldn't happen in UI flow — but if it does, we
            // surface Declined for the older request so neither waits forever.
            pendingWidgetBindCallback.getAndSet(callback)?.let { stale ->
                runCatching { stale(WidgetBindResult.Declined(AppWidgetManager.INVALID_APPWIDGET_ID)) }
                    .onFailure { Log.w(TAG, "Stale bind callback threw during superseding request", it) }
            }
            return runCatching { launcher.launch(request) }.fold(
                onSuccess = { true },
                onFailure = { cause ->
                    Log.e(TAG, "Widget bind launcher.launch() failed", cause)
                    pendingWidgetBindCallback.set(null)
                    false
                },
            )
        }

        internal fun consumePendingWidgetBindCallback(): ((WidgetBindResult) -> Unit)? =
            pendingWidgetBindCallback.getAndSet(null)

        /**
         * Called from `Activity.onDestroy` to prevent a callback left by a dying Activity
         * from firing on a dead Compose tree — the callback's closure would pin the old
         * Activity + composition, leaking them until the next bind result arrives.
         * Delivers [WidgetBindResult.Declined] so any awaiting coroutine / continuation
         * completes rather than hanging forever.
         */
        internal fun cancelPendingWidgetBind() {
            val pending = pendingWidgetBindCallback.getAndSet(null) ?: return
            runCatching { pending(WidgetBindResult.Declined(AppWidgetManager.INVALID_APPWIDGET_ID)) }
                .onFailure { Log.w(TAG, "Pending widget bind callback threw during cancel", it) }
        }
    }
}
