package com.oneuihomeclone.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.oneuihomeclone.LauncherApp

/**
 * Packaged request/result pair for `ACTION_APPWIDGET_BIND`.
 *
 * Non-system launchers cannot `BIND_APPWIDGET` silently — they must ask the system UI,
 * which prompts the user on install. We model it with an [ActivityResultContract] so the
 * Activity owns the lifecycle-scoped [ActivityResultLauncher] and Compose code only
 * passes the request object through. On cancel we deallocate the widget id so the host
 * doesn't leak phantom allocations across repeated attempts.
 *
 * **Process-death / rotation robustness:** the [ActivityResultRegistry] restores only the
 * request key across a process kill, not instance state on the contract object. We avoid
 * storing anything on the contract itself and instead smuggle [allocatedWidgetId] as an
 * extra in the outbound Intent. Some bind-dialog implementations echo custom extras
 * verbatim on the result Intent; the canonical fallback path is `EXTRA_APPWIDGET_ID`,
 * which every AOSP-derived bind dialog copies into the result on RESULT_OK and most
 * copy on RESULT_CANCELED as well. If both are stripped (`intent == null` + both extras
 * missing), deallocation short-circuits with a `Log.w` rather than touching a random ID.
 */
private const val EXTRA_ALLOCATED_ID_INTERNAL = "com.oneuihomeclone.widgets.ALLOCATED_ID"

data class WidgetBindRequest(
    val allocatedWidgetId: Int,
    val providerInfo: AppWidgetProviderInfo,
    val options: Bundle? = null,
    val bindActivity: ComponentName? = null,
)

sealed class WidgetBindResult {
    /** User approved the bind. [widgetId] is stable and safe to persist + render. */
    data class Bound(val widgetId: Int) : WidgetBindResult()

    /** User declined or the system denied — widget id has already been deallocated. */
    data class Declined(val requestedId: Int) : WidgetBindResult()
}

class WidgetBindContract : ActivityResultContract<WidgetBindRequest, WidgetBindResult>() {

    override fun createIntent(context: Context, input: WidgetBindRequest): Intent =
        input.toBindIntent()

    override fun parseResult(resultCode: Int, intent: Intent?): WidgetBindResult {
        val allocatedId = readAllocatedId(intent)
        return if (resultCode == Activity.RESULT_OK) {
            val boundId = intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
            if (boundId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                deallocate(allocatedId, reason = "result OK but INVALID_APPWIDGET_ID returned")
                WidgetBindResult.Declined(allocatedId)
            } else {
                WidgetBindResult.Bound(boundId)
            }
        } else {
            deallocate(allocatedId, reason = "resultCode=$resultCode (user canceled or system denied)")
            WidgetBindResult.Declined(allocatedId)
        }
    }

    private fun readAllocatedId(intent: Intent?): Int {
        if (intent == null) return AppWidgetManager.INVALID_APPWIDGET_ID
        val internal = intent.getIntExtra(EXTRA_ALLOCATED_ID_INTERNAL, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (internal != AppWidgetManager.INVALID_APPWIDGET_ID) return internal
        // OEM fallback — some forks strip non-framework extras. EXTRA_APPWIDGET_ID is
        // echoed by every canonical bind dialog implementation.
        return intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    private fun deallocate(widgetId: Int, reason: String) {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "Cannot deallocate — allocated id not recoverable from result intent ($reason)")
            return
        }
        runCatching { LauncherApp.appWidgetHost()?.deleteAppWidgetId(widgetId) }
            .onFailure { Log.w(TAG, "Widget id $widgetId deallocation failed: $reason", it) }
    }

    companion object {
        private const val TAG = "WidgetBindContract"
    }
}

internal fun WidgetBindRequest.toBindIntent(): Intent =
    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
        bindActivity?.let(::setComponent)
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedWidgetId)
        putExtra(EXTRA_ALLOCATED_ID_INTERNAL, allocatedWidgetId)
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
        // minSdk=28, so the Lollipop-era profile extra is always available.
        if (providerInfo.profile != null) {
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,
                providerInfo.profile as UserHandle,
            )
        }
        options?.let { putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, it) }
    }

@Suppress("DEPRECATION")
internal fun WidgetBindRequest.withResolvedSystemBindActivity(context: Context): WidgetBindRequest? {
    val resolver = context.packageManager
    val resolvedInfo = resolver.resolveActivity(toBindIntent(), PackageManager.MATCH_DEFAULT_ONLY)
    val activityInfo = resolvedInfo?.activityInfo
    if (activityInfo == null) {
        Log.w(TAG, "No activity resolves ACTION_APPWIDGET_BIND")
        return null
    }
    val appInfo = activityInfo.applicationInfo
    val isSystemHandler = appInfo != null &&
        (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
    if (!isSystemHandler) {
        Log.w(TAG, "Refusing non-system widget bind handler ${activityInfo.packageName}/${activityInfo.name}")
        return null
    }
    return copy(bindActivity = ComponentName(activityInfo.packageName, activityInfo.name))
}

private const val TAG = "WidgetBindContract"
