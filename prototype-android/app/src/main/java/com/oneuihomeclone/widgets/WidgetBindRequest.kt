package com.oneuihomeclone.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
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
 * extra in the outbound Intent. On `parseResult`, the system echoes the extras back (both
 * for RESULT_OK and RESULT_CANCELED — AOSP `AppWidgetServiceImpl` copies them), so we can
 * always recover the ID and deallocate on cancel. If the system strips the extra (OEM
 * divergence), we fall back to `EXTRA_APPWIDGET_ID` which every bind dialog echoes.
 */
private const val EXTRA_ALLOCATED_ID_INTERNAL = "com.oneuihomeclone.widgets.ALLOCATED_ID"

data class WidgetBindRequest(
    val allocatedWidgetId: Int,
    val providerInfo: AppWidgetProviderInfo,
    val options: Bundle? = null,
)

sealed class WidgetBindResult {
    /** User approved the bind. [widgetId] is stable and safe to persist + render. */
    data class Bound(val widgetId: Int) : WidgetBindResult()

    /** User declined or the system denied — widget id has already been deallocated. */
    data class Declined(val requestedId: Int) : WidgetBindResult()
}

class WidgetBindContract : ActivityResultContract<WidgetBindRequest, WidgetBindResult>() {

    override fun createIntent(context: Context, input: WidgetBindRequest): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.allocatedWidgetId)
            putExtra(EXTRA_ALLOCATED_ID_INTERNAL, input.allocatedWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, input.providerInfo.provider)
            // minSdk=28, so the Lollipop-era profile extra is always available.
            if (input.providerInfo.profile != null) {
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,
                    input.providerInfo.profile as UserHandle,
                )
            }
            input.options?.let { putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, it) }
        }

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
