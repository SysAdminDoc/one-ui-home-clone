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
        val file = backupFile()
        if (!file.exists()) return@withContext null
        LauncherBackupCodec.decode(file.readText(Charsets.UTF_8))
    }

    private fun backupFile(): File {
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: appContext.filesDir
        return File(dir, BACKUP_FILE_NAME)
    }

    private companion object {
        private const val BACKUP_FILE_NAME = "one-ui-home-clone-backup.json"
    }
}

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
            lockHomeScreenLayout = obj?.optBoolean("lockHomeScreenLayout", false) ?: false,
            homeLayoutMode = HomeLayoutKey.fromRaw(obj?.optString("homeLayoutMode")),
            drawerSortMode = DrawerSortKey.fromRaw(obj?.optString("drawerSortMode")),
            motionPreset = MotionPresetKey.fromRaw(obj?.optString("motionPreset")),
            folderGrid = FolderGridKey.fromRaw(obj?.optString("folderGrid")),
        )
}
