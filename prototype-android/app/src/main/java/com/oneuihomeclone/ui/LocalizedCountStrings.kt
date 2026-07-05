package com.oneuihomeclone.ui

import android.content.Context
import androidx.annotation.PluralsRes
import com.oneuihomeclone.R
import com.oneuihomeclone.data.LauncherRestoreReport

private fun Context.quantityText(@PluralsRes resId: Int, count: Int): String =
    resources.getQuantityString(resId, count, count)

internal fun Context.hiddenAppsCountText(count: Int): String =
    quantityText(R.plurals.settings_hidden_app_count, count)

internal fun Context.widgetResetFeedback(count: Int): String =
    quantityText(R.plurals.feedback_widget_reset_count, count)

internal fun Context.backupRestoreSummary(report: LauncherRestoreReport): String =
    getString(
        R.string.feedback_backup_restored_summary,
        quantityText(R.plurals.feedback_backup_changed_setting_count, report.changedSettingCount),
        quantityText(R.plurals.feedback_backup_restored_page_count, report.restoredPageCount),
        quantityText(R.plurals.feedback_backup_restored_app_count, report.restoredAppCount),
        quantityText(R.plurals.feedback_backup_restored_widget_count, report.restoredWidgetCount),
        quantityText(R.plurals.feedback_backup_missing_app_count, report.missingAppCount),
        quantityText(R.plurals.feedback_backup_missing_widget_provider_count, report.missingWidgetProviderCount),
    )
