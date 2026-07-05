package com.oneuihomeclone.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import com.oneuihomeclone.LauncherApp

private const val EXTRA_CONFIGURE_ID_INTERNAL = "com.oneuihomeclone.widgets.CONFIGURE_ID"

data class WidgetConfigureRequest(
    val widgetId: Int,
    val configureActivity: ComponentName,
)

sealed class WidgetConfigureResult {
    abstract val widgetId: Int

    data class Configured(override val widgetId: Int) : WidgetConfigureResult()

    data class Declined(override val widgetId: Int) : WidgetConfigureResult()
}

class WidgetConfigureContract : ActivityResultContract<WidgetConfigureRequest, WidgetConfigureResult>() {

    override fun createIntent(context: Context, input: WidgetConfigureRequest): Intent =
        input.toConfigureIntent()

    override fun parseResult(resultCode: Int, intent: Intent?): WidgetConfigureResult {
        val widgetId = readWidgetId(intent)
        return if (resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetConfigureResult.Configured(widgetId)
        } else {
            deallocateWidgetId(widgetId, reason = "configure resultCode=$resultCode")
            WidgetConfigureResult.Declined(widgetId)
        }
    }

    private fun readWidgetId(intent: Intent?): Int {
        if (intent == null) return AppWidgetManager.INVALID_APPWIDGET_ID
        val internal = intent.getIntExtra(EXTRA_CONFIGURE_ID_INTERNAL, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (internal != AppWidgetManager.INVALID_APPWIDGET_ID) return internal
        return intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }
}

internal fun WidgetConfigureRequest.toConfigureIntent(): Intent =
    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
        component = configureActivity
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        putExtra(EXTRA_CONFIGURE_ID_INTERNAL, widgetId)
    }

internal fun WidgetConfigureResult.withFallbackWidgetId(fallbackWidgetId: Int): WidgetConfigureResult {
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) return this
    return when (this) {
        is WidgetConfigureResult.Configured -> WidgetConfigureResult.Configured(fallbackWidgetId)
        is WidgetConfigureResult.Declined -> WidgetConfigureResult.Declined(fallbackWidgetId)
    }
}

internal fun deallocateWidgetId(widgetId: Int, reason: String) {
    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        Log.w(TAG, "Cannot deallocate widget id: $reason")
        return
    }
    runCatching { LauncherApp.appWidgetHost()?.deleteAppWidgetId(widgetId) }
        .onFailure { Log.w(TAG, "Widget id $widgetId deallocation failed: $reason", it) }
}

private const val TAG = "WidgetConfigureContract"
