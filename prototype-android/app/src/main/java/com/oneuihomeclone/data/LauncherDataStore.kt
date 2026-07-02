package com.oneuihomeclone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences store for launcher toggles.
 *
 * A one-shot [SharedPreferencesMigration] copies the legacy v0.1.0
 * `one_ui_home_clone_prefs` file on the first DataStore read. After migration, this
 * store is the only launcher-settings writer and Compose observes [state] directly.
 */
private const val DS_NAME = "one_ui_home_clone_prefs_ds"
private const val LEGACY_SP_NAME = "one_ui_home_clone_prefs"

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DS_NAME,
    produceMigrations = { ctx ->
        listOf(
            SharedPreferencesMigration(
                context = ctx,
                sharedPreferencesName = LEGACY_SP_NAME,
                // Only these keys migrate — anything else in the legacy file is ignored so
                // stale test values can't poison a real user's upgrade.
                keysToMigrate = setOf(
                    "media_page_enabled",
                    "apps_button_enabled",
                    "app_labels_enabled",
                    "widget_labels_enabled",
                    "notifications_swipe",
                    "lock_home_screen_layout",
                    "home_layout_mode",
                    "drawer_sort_mode",
                    "motion_preset",
                    "folder_grid",
                ),
            ),
        )
    },
)

class LauncherDataStore(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.launcherDataStore

    val state: Flow<LauncherState> = dataStore.data
        .catch { cause ->
            // IOException is the documented failure mode (disk full, corrupted proto). Emit
            // defaults rather than crashing the launcher — a broken preferences file must
            // never stop the user from reaching home.
            if (cause is java.io.IOException) emit(emptyPreferences())
            else throw cause
        }
        .map { prefs -> prefs.toLauncherState() }

    suspend fun update(mutator: Mutator.() -> Unit) {
        dataStore.edit { prefs ->
            Mutator(prefs).mutator()
        }
    }

    class Mutator internal constructor(private val prefs: androidx.datastore.preferences.core.MutablePreferences) {
        fun setLauncherState(state: LauncherState) {
            setMediaPageEnabled(state.mediaPageEnabled)
            setAppsButtonEnabled(state.appsButtonEnabled)
            setAppLabelsEnabled(state.appLabelsEnabled)
            setWidgetLabelsEnabled(state.widgetLabelsEnabled)
            setSwipeDownForNotifications(state.swipeDownForNotifications)
            setLockHomeScreenLayout(state.lockHomeScreenLayout)
            setHomeLayoutMode(state.homeLayoutMode)
            setDrawerSortMode(state.drawerSortMode)
            setMotionPreset(state.motionPreset)
            setFolderGrid(state.folderGrid)
        }

        fun setMediaPageEnabled(value: Boolean) { prefs[Keys.MEDIA_PAGE] = value }
        fun setAppsButtonEnabled(value: Boolean) { prefs[Keys.APPS_BUTTON] = value }
        fun setAppLabelsEnabled(value: Boolean) { prefs[Keys.APP_LABELS] = value }
        fun setWidgetLabelsEnabled(value: Boolean) { prefs[Keys.WIDGET_LABELS] = value }
        fun setSwipeDownForNotifications(value: Boolean) { prefs[Keys.NOTIFICATIONS_SWIPE] = value }
        fun setLockHomeScreenLayout(value: Boolean) { prefs[Keys.LOCK_LAYOUT] = value }
        fun setHomeLayoutMode(value: HomeLayoutKey) { prefs[Keys.HOME_LAYOUT] = value.raw }
        fun setDrawerSortMode(value: DrawerSortKey) { prefs[Keys.DRAWER_SORT] = value.raw }
        fun setMotionPreset(value: MotionPresetKey) { prefs[Keys.MOTION_PRESET] = value.raw }
        fun setFolderGrid(value: FolderGridKey) { prefs[Keys.FOLDER_GRID] = value.raw }
    }

    private object Keys {
        val MEDIA_PAGE = booleanPreferencesKey("media_page_enabled")
        val APPS_BUTTON = booleanPreferencesKey("apps_button_enabled")
        val APP_LABELS = booleanPreferencesKey("app_labels_enabled")
        val WIDGET_LABELS = booleanPreferencesKey("widget_labels_enabled")
        val NOTIFICATIONS_SWIPE = booleanPreferencesKey("notifications_swipe")
        val LOCK_LAYOUT = booleanPreferencesKey("lock_home_screen_layout")
        val HOME_LAYOUT = stringPreferencesKey("home_layout_mode")
        val DRAWER_SORT = stringPreferencesKey("drawer_sort_mode")
        val MOTION_PRESET = stringPreferencesKey("motion_preset")
        val FOLDER_GRID = stringPreferencesKey("folder_grid")
    }

    private fun Preferences.toLauncherState(): LauncherState = LauncherState(
        mediaPageEnabled = this[Keys.MEDIA_PAGE] ?: true,
        appsButtonEnabled = this[Keys.APPS_BUTTON] ?: true,
        appLabelsEnabled = this[Keys.APP_LABELS] ?: true,
        widgetLabelsEnabled = this[Keys.WIDGET_LABELS] ?: true,
        swipeDownForNotifications = this[Keys.NOTIFICATIONS_SWIPE] ?: true,
        lockHomeScreenLayout = this[Keys.LOCK_LAYOUT] ?: false,
        homeLayoutMode = HomeLayoutKey.fromRaw(this[Keys.HOME_LAYOUT]),
        drawerSortMode = DrawerSortKey.fromRaw(this[Keys.DRAWER_SORT]),
        motionPreset = MotionPresetKey.fromRaw(this[Keys.MOTION_PRESET]),
        folderGrid = FolderGridKey.fromRaw(this[Keys.FOLDER_GRID]),
    )
}

/** Motion preset — threaded through [LocalMotionScheme] so Standard/Reduced swap affects every transition. */
enum class MotionPresetKey(val raw: String) {
    STANDARD("standard"),
    REDUCED("reduced");

    companion object {
        fun fromRaw(raw: String?): MotionPresetKey =
            entries.firstOrNull { it.raw == raw } ?: STANDARD
    }
}
