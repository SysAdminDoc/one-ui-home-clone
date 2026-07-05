package com.oneuihomeclone.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WidgetConfigureContractTest {

    @Test
    fun createIntent_targetsConfigureActivityAndCarriesWidgetId() {
        val configureActivity = ComponentName("com.example.widgets", "com.example.widgets.ConfigureActivity")
        val intent = WidgetConfigureContract().createIntent(
            RuntimeEnvironment.getApplication(),
            WidgetConfigureRequest(widgetId = 42, configureActivity = configureActivity),
        )

        assertEquals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE, intent.action)
        assertEquals(configureActivity, intent.component)
        assertEquals(42, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    }

    @Test
    fun parseResult_returnsConfiguredOnlyForOkWithWidgetId() {
        val result = WidgetConfigureContract().parseResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42),
        )

        assertEquals(WidgetConfigureResult.Configured(42), result)
    }

    @Test
    fun withFallbackWidgetId_restoresPendingIdWhenProviderOmitsResultData() {
        val result = WidgetConfigureResult.Declined(AppWidgetManager.INVALID_APPWIDGET_ID)
            .withFallbackWidgetId(42)

        assertEquals(WidgetConfigureResult.Declined(42), result)
    }
}
