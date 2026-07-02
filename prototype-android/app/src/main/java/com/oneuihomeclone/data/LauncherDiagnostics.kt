package com.oneuihomeclone.data

import android.content.Context
import android.os.Environment
import com.oneuihomeclone.PreviousCrashSummary
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LauncherDiagnosticsSnapshot(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val sdkInt: Int,
    val targetSdk: Int,
    val internetPermissionDeclared: Boolean,
    val defaultLauncherChecked: Boolean,
    val isDefaultLauncher: Boolean,
    val canOpenDefaultLauncherSettings: Boolean,
    val previousCrash: PreviousCrashSummary?,
    val appInventoryLoaded: Boolean,
    val appCount: Int,
    val launchableAppCount: Int,
    val unavailableAppCount: Int,
    val restoredPlaceholderAppCount: Int,
    val hiddenAppCount: Int,
    val homePageCount: Int,
    val folderCount: Int,
    val directHomeAppCount: Int,
    val folderAppCount: Int,
    val homeWidgetCount: Int,
    val boundWidgetCount: Int,
    val persistedWidgetCount: Int,
    val widgetTemplateCount: Int,
    val realWidgetProviderCount: Int,
)

class LauncherDiagnosticsFileStore(context: Context) {
    private val appContext = context.applicationContext

    val diagnosticsFileName: String = DIAGNOSTICS_FILE_NAME

    suspend fun export(snapshot: LauncherDiagnosticsSnapshot): File = withContext(Dispatchers.IO) {
        val file = diagnosticsFile()
        file.parentFile?.mkdirs()
        file.writeText(buildLauncherDiagnostics(snapshot), Charsets.UTF_8)
        file
    }

    private fun diagnosticsFile(): File {
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: appContext.filesDir
        return File(dir, DIAGNOSTICS_FILE_NAME)
    }

    private companion object {
        private const val DIAGNOSTICS_FILE_NAME = "one-ui-home-clone-diagnostics.txt"
    }
}

internal fun buildLauncherDiagnostics(snapshot: LauncherDiagnosticsSnapshot): String = buildString {
    appendLine("One UI Home Clone diagnostics")
    appendLine("versionName=${snapshot.versionName.diagnosticValue()}")
    appendLine("versionCode=${snapshot.versionCode}")
    appendLine("buildType=${snapshot.buildType.diagnosticValue()}")
    appendLine("sdk=${snapshot.sdkInt}")
    appendLine("targetSdk=${snapshot.targetSdk}")
    appendLine("permissions.internetDeclared=${snapshot.internetPermissionDeclared}")
    appendLine("defaultLauncher.checked=${snapshot.defaultLauncherChecked}")
    appendLine("defaultLauncher.isDefault=${snapshot.isDefaultLauncher}")
    appendLine("defaultLauncher.canOpenSettings=${snapshot.canOpenDefaultLauncherSettings}")
    appendLine("previousCrash.present=${snapshot.previousCrash != null}")
    appendLine("previousCrash.timestamp=${snapshot.previousCrash?.timestamp.diagnosticValue()}")
    appendLine("previousCrash.thread=${snapshot.previousCrash?.thread.diagnosticValue()}")
    appendLine("previousCrash.versionName=${snapshot.previousCrash?.versionName.diagnosticValue()}")
    appendLine("previousCrash.versionCode=${snapshot.previousCrash?.versionCode.diagnosticValue()}")
    appendLine("previousCrash.exception=${snapshot.previousCrash?.exceptionClass.diagnosticValue()}")
    appendLine("appInventory.loaded=${snapshot.appInventoryLoaded}")
    appendLine("apps.total=${snapshot.appCount}")
    appendLine("apps.launchable=${snapshot.launchableAppCount}")
    appendLine("apps.unavailable=${snapshot.unavailableAppCount}")
    appendLine("apps.restoredPlaceholders=${snapshot.restoredPlaceholderAppCount}")
    appendLine("apps.hidden=${snapshot.hiddenAppCount}")
    appendLine("layout.homePages=${snapshot.homePageCount}")
    appendLine("layout.folders=${snapshot.folderCount}")
    appendLine("layout.directHomeApps=${snapshot.directHomeAppCount}")
    appendLine("layout.folderApps=${snapshot.folderAppCount}")
    appendLine("widgets.onHome=${snapshot.homeWidgetCount}")
    appendLine("widgets.boundOnHome=${snapshot.boundWidgetCount}")
    appendLine("widgets.persisted=${snapshot.persistedWidgetCount}")
    appendLine("widgets.templates=${snapshot.widgetTemplateCount}")
    appendLine("widgets.realProviders=${snapshot.realWidgetProviderCount}")
    appendLine("privacy.rawAppNames=false")
    appendLine("privacy.rawSearchHistory=false")
}

private fun String?.diagnosticValue(): String =
    this
        ?.lineSequence()
        ?.firstOrNull()
        ?.take(160)
        ?.ifBlank { null }
        ?: "unknown"
