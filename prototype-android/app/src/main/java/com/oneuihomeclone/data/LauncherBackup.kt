package com.oneuihomeclone.data

import android.content.Context
import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LauncherBackup(
    val settings: LauncherState,
    val layout: PersistedLauncherLayout,
    val widgets: List<BoundWidget>,
    val exportedAtMillis: Long,
)

data class LauncherRestoreSnapshot(
    val settings: LauncherState,
    val layout: PersistedLauncherLayout,
    val widgets: List<BoundWidget>,
)

data class LauncherRestoreReport(
    val changedSettingCount: Int,
    val restoredPageCount: Int,
    val restoredAppCount: Int,
    val restoredWidgetCount: Int,
    val missingAppCount: Int,
    val missingWidgetProviderCount: Int,
)

sealed class LauncherBackupImportResult {
    data class Success(val backup: LauncherBackup) : LauncherBackupImportResult()
    object Missing : LauncherBackupImportResult()
    object Invalid : LauncherBackupImportResult()
}

class LauncherBackupFileStore(context: Context) {
    private val appContext = context.applicationContext

    val backupFileName: String = BACKUP_FILE_NAME

    suspend fun export(backup: LauncherBackup): File = withContext(Dispatchers.IO) {
        val file = backupFile()
        file.parentFile?.mkdirs()
        file.writeText(LauncherBackupCodec.encode(backup), Charsets.UTF_8)
        file
    }

    suspend fun import(): LauncherBackup? = withContext(Dispatchers.IO) {
        when (val result = importResult()) {
            is LauncherBackupImportResult.Success -> result.backup
            LauncherBackupImportResult.Invalid,
            LauncherBackupImportResult.Missing,
            -> null
        }
    }

    suspend fun importResult(): LauncherBackupImportResult = withContext(Dispatchers.IO) {
        val file = backupFile()
        if (!file.exists()) return@withContext LauncherBackupImportResult.Missing
        LauncherBackupCodec.decode(file.readText(Charsets.UTF_8))?.let(LauncherBackupImportResult::Success)
            ?: LauncherBackupImportResult.Invalid
    }

    suspend fun exportPreRestoreSnapshot(backup: LauncherBackup): File = withContext(Dispatchers.IO) {
        val file = preRestoreSnapshotFile()
        file.parentFile?.mkdirs()
        file.writeText(LauncherBackupCodec.encode(backup), Charsets.UTF_8)
        file
    }

    private fun backupFile(): File {
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: appContext.filesDir
        return File(dir, BACKUP_FILE_NAME)
    }

    private fun preRestoreSnapshotFile(): File =
        File(appContext.filesDir, PRE_RESTORE_SNAPSHOT_FILE_NAME)

    private companion object {
        private const val BACKUP_FILE_NAME = "one-ui-home-clone-backup.json"
        private const val PRE_RESTORE_SNAPSHOT_FILE_NAME = "one-ui-home-clone-pre-restore.json"
    }
}

internal fun LauncherRestoreSnapshot.toBackup(exportedAtMillis: Long): LauncherBackup =
    LauncherBackup(
        settings = settings,
        layout = layout,
        widgets = widgets,
        exportedAtMillis = exportedAtMillis,
    )

internal fun validateLauncherRestore(
    snapshot: LauncherRestoreSnapshot,
    backup: LauncherBackup,
    availableAppIds: Set<String>,
    availableWidgetProviderKeys: Set<String>,
): LauncherRestoreReport? {
    val pageCount = backup.layout.pages.size
    val itemCount = backup.layout.pages.sumOf { page -> page.items.size }
    if (pageCount > MAX_RESTORE_PAGES || itemCount > MAX_RESTORE_ITEMS) return null
    if (backup.widgets.size > MAX_RESTORE_WIDGETS) return null
    if (backup.widgets.any { it.hostWidgetId <= 0 || it.providerPackage.isBlank() || it.providerClass.isBlank() }) {
        return null
    }

    val appIds = backup.layout.pages
        .flatMap { page -> page.items.flatMap(PersistedHomeItem::restoreAppIds) }
        .filter(String::isNotBlank)
        .distinct()
    val widgetProviderKeys = backup.widgets
        .map(BoundWidget::restoreProviderKey)
        .distinct()

    return LauncherRestoreReport(
        changedSettingCount = changedSettingCount(snapshot.settings, backup.settings),
        restoredPageCount = pageCount,
        restoredAppCount = appIds.count { it in availableAppIds },
        restoredWidgetCount = backup.widgets.size,
        missingAppCount = appIds.count { it !in availableAppIds },
        missingWidgetProviderCount = widgetProviderKeys.count { it !in availableWidgetProviderKeys },
    )
}

internal suspend fun applyLauncherRestoreTransaction(
    snapshot: LauncherRestoreSnapshot,
    backup: LauncherBackup,
    writeSettings: suspend (LauncherState) -> Unit,
    writeLayout: suspend (PersistedLauncherLayout) -> Unit,
    writeWidgets: suspend (List<BoundWidget>) -> Unit,
) {
    try {
        writeSettings(backup.settings)
        writeLayout(backup.layout)
        writeWidgets(backup.widgets)
    } catch (cause: Throwable) {
        runCatching {
            writeSettings(snapshot.settings)
            writeLayout(snapshot.layout)
            writeWidgets(snapshot.widgets)
        }.onFailure(cause::addSuppressed)
        throw cause
    }
}

private fun PersistedHomeItem.restoreAppIds(): List<String> =
    when (this) {
        is PersistedHomeItem.App -> listOf(appId)
        is PersistedHomeItem.Folder -> appIds
    }

private fun BoundWidget.restoreProviderKey(): String =
    "$providerPackage/$providerClass"

private fun changedSettingCount(before: LauncherState, after: LauncherState): Int =
    listOf(
        before.mediaPageEnabled != after.mediaPageEnabled,
        before.appsButtonEnabled != after.appsButtonEnabled,
        before.appLabelsEnabled != after.appLabelsEnabled,
        before.widgetLabelsEnabled != after.widgetLabelsEnabled,
        before.swipeDownForNotifications != after.swipeDownForNotifications,
        before.addNewAppsToHomeScreen != after.addNewAppsToHomeScreen,
        before.notificationBadgeMode != after.notificationBadgeMode,
        before.lockHomeScreenLayout != after.lockHomeScreenLayout,
        before.homeLayoutMode != after.homeLayoutMode,
        before.drawerSortMode != after.drawerSortMode,
        before.motionPreset != after.motionPreset,
        before.folderGrid != after.folderGrid,
    ).count { it }

private const val MAX_RESTORE_PAGES = 32
private const val MAX_RESTORE_ITEMS_PER_PAGE = 128
private const val MAX_RESTORE_ITEMS = MAX_RESTORE_PAGES * MAX_RESTORE_ITEMS_PER_PAGE
private const val MAX_RESTORE_WIDGETS = 1024

object LauncherBackupCodec {
    private const val SCHEMA_VERSION = 1
    private const val MAX_BACKUP_BYTES = 512 * 1024

    fun encode(backup: LauncherBackup): String =
        JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAtMillis", backup.exportedAtMillis)
            .put("settings", encodeSettings(backup.settings))
            .put("layoutSchemaVersion", 1)
            .put("layout", JSONObject(LauncherLayoutStore.encode(backup.layout)))
            .put("widgetsSchemaVersion", 1)
            .put("widgets", JSONArray(WidgetPersistence.encode(backup.widgets)))
            .toString(2)

    fun decode(json: String): LauncherBackup? {
        if (json.isBlank() || json.length > MAX_BACKUP_BYTES) return null
        return runCatching {
            val root = JSONObject(json)
            if (root.optInt("schemaVersion", -1) != SCHEMA_VERSION) return null
            val layout = LauncherLayoutStore.decode(
                schema = root.optInt("layoutSchemaVersion", 1),
                json = root.optJSONObject("layout")?.toString(),
            ) ?: return null
            LauncherBackup(
                settings = decodeSettings(root.optJSONObject("settings")),
                layout = layout,
                widgets = WidgetPersistence.decode(
                    schema = root.optInt("widgetsSchemaVersion", 1),
                    json = root.optJSONArray("widgets")?.toString(),
                ),
                exportedAtMillis = root.optLong("exportedAtMillis", 0L),
            )
        }.getOrNull()
    }

    private fun encodeSettings(state: LauncherState): JSONObject =
        JSONObject()
            .put("mediaPageEnabled", state.mediaPageEnabled)
            .put("appsButtonEnabled", state.appsButtonEnabled)
            .put("appLabelsEnabled", state.appLabelsEnabled)
            .put("widgetLabelsEnabled", state.widgetLabelsEnabled)
            .put("swipeDownForNotifications", state.swipeDownForNotifications)
            .put("addNewAppsToHomeScreen", state.addNewAppsToHomeScreen)
            .put("notificationBadgeMode", state.notificationBadgeMode.raw)
            .put("finderContactsEnabled", state.finderContactsEnabled)
            .put("lockHomeScreenLayout", state.lockHomeScreenLayout)
            .put("homeLayoutMode", state.homeLayoutMode.raw)
            .put("drawerSortMode", state.drawerSortMode.raw)
            .put("motionPreset", state.motionPreset.raw)
            .put("folderGrid", state.folderGrid.raw)

    private fun decodeSettings(obj: JSONObject?): LauncherState =
        LauncherState(
            mediaPageEnabled = obj?.optBoolean("mediaPageEnabled", true) ?: true,
            appsButtonEnabled = obj?.optBoolean("appsButtonEnabled", true) ?: true,
            appLabelsEnabled = obj?.optBoolean("appLabelsEnabled", true) ?: true,
            widgetLabelsEnabled = obj?.optBoolean("widgetLabelsEnabled", true) ?: true,
            swipeDownForNotifications = obj?.optBoolean("swipeDownForNotifications", true) ?: true,
            addNewAppsToHomeScreen = obj?.optBoolean("addNewAppsToHomeScreen", true) ?: true,
            notificationBadgeMode = NotificationBadgeModeKey.fromRaw(obj?.optString("notificationBadgeMode")),
            finderContactsEnabled = obj?.optBoolean("finderContactsEnabled", false) ?: false,
            lockHomeScreenLayout = obj?.optBoolean("lockHomeScreenLayout", false) ?: false,
            homeLayoutMode = HomeLayoutKey.fromRaw(obj?.optString("homeLayoutMode")),
            drawerSortMode = DrawerSortKey.fromRaw(obj?.optString("drawerSortMode")),
            motionPreset = MotionPresetKey.fromRaw(obj?.optString("motionPreset")),
            folderGrid = FolderGridKey.fromRaw(obj?.optString("folderGrid")),
        )
}
