package com.oneuihomeclone.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Zero-dep persistence for user-facing launcher toggles.
 *
 * SharedPreferences is fine for v0.1.0 — eight boolean/enum flags, no observability need
 * beyond "write on toggle, read on launch". DataStore migration is scheduled for v0.2.0
 * when we need typed async flows for widget state and pending-operation persistence.
 */
class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun snapshot(): LauncherState = LauncherState(
        mediaPageEnabled = prefs.getBoolean(KEY_MEDIA_PAGE, true),
        appsButtonEnabled = prefs.getBoolean(KEY_APPS_BUTTON, true),
        appLabelsEnabled = prefs.getBoolean(KEY_APP_LABELS, true),
        widgetLabelsEnabled = prefs.getBoolean(KEY_WIDGET_LABELS, true),
        swipeDownForNotifications = prefs.getBoolean(KEY_NOTIFICATIONS_SWIPE, true),
        lockHomeScreenLayout = prefs.getBoolean(KEY_LOCK_LAYOUT, false),
        homeLayoutMode = HomeLayoutKey.fromRaw(prefs.getString(KEY_HOME_LAYOUT, null)),
        drawerSortMode = DrawerSortKey.fromRaw(prefs.getString(KEY_DRAWER_SORT, null)),
        motionPreset = MotionPresetKey.fromRaw(prefs.getString(KEY_MOTION_PRESET, null)),
    )

    fun update(mutator: (Editor) -> Unit) {
        prefs.edit {
            mutator(Editor(this))
        }
    }

    class Editor internal constructor(private val editor: SharedPreferences.Editor) {
        fun setMediaPageEnabled(value: Boolean): Editor = apply { editor.putBoolean(KEY_MEDIA_PAGE, value) }
        fun setAppsButtonEnabled(value: Boolean): Editor = apply { editor.putBoolean(KEY_APPS_BUTTON, value) }
        fun setAppLabelsEnabled(value: Boolean): Editor = apply { editor.putBoolean(KEY_APP_LABELS, value) }
        fun setWidgetLabelsEnabled(value: Boolean): Editor = apply { editor.putBoolean(KEY_WIDGET_LABELS, value) }
        fun setSwipeDownForNotifications(value: Boolean): Editor = apply { editor.putBoolean(KEY_NOTIFICATIONS_SWIPE, value) }
        fun setLockHomeScreenLayout(value: Boolean): Editor = apply { editor.putBoolean(KEY_LOCK_LAYOUT, value) }
        fun setHomeLayoutMode(value: HomeLayoutKey): Editor = apply { editor.putString(KEY_HOME_LAYOUT, value.raw) }
        fun setDrawerSortMode(value: DrawerSortKey): Editor = apply { editor.putString(KEY_DRAWER_SORT, value.raw) }
        fun setMotionPreset(value: MotionPresetKey): Editor = apply { editor.putString(KEY_MOTION_PRESET, value.raw) }
    }

    companion object {
        private const val PREFS_NAME = "one_ui_home_clone_prefs"

        private const val KEY_MEDIA_PAGE = "media_page_enabled"
        private const val KEY_APPS_BUTTON = "apps_button_enabled"
        private const val KEY_APP_LABELS = "app_labels_enabled"
        private const val KEY_WIDGET_LABELS = "widget_labels_enabled"
        private const val KEY_NOTIFICATIONS_SWIPE = "notifications_swipe"
        private const val KEY_LOCK_LAYOUT = "lock_home_screen_layout"
        private const val KEY_HOME_LAYOUT = "home_layout_mode"
        private const val KEY_DRAWER_SORT = "drawer_sort_mode"
        private const val KEY_MOTION_PRESET = "motion_preset"
    }
}

/** Immutable snapshot of persisted user toggles read at launcher start / on resume. */
data class LauncherState(
    val mediaPageEnabled: Boolean,
    val appsButtonEnabled: Boolean,
    val appLabelsEnabled: Boolean,
    val widgetLabelsEnabled: Boolean,
    val swipeDownForNotifications: Boolean,
    val lockHomeScreenLayout: Boolean,
    val homeLayoutMode: HomeLayoutKey,
    val drawerSortMode: DrawerSortKey,
    val motionPreset: MotionPresetKey = MotionPresetKey.STANDARD,
)

/**
 * Decouples persistence keys from Compose enum types so Compose code can use its own
 * display-friendly enum without leaking raw string tokens into UI logic.
 */
enum class HomeLayoutKey(val raw: String) {
    HOME_AND_APPS_SCREENS("home_and_apps"),
    HOME_SCREEN_ONLY("home_only");

    companion object {
        fun fromRaw(raw: String?): HomeLayoutKey =
            entries.firstOrNull { it.raw == raw } ?: HOME_AND_APPS_SCREENS
    }
}

enum class DrawerSortKey(val raw: String) {
    CUSTOM_ORDER("custom"),
    ALPHABETICAL("alphabetical");

    companion object {
        fun fromRaw(raw: String?): DrawerSortKey =
            entries.firstOrNull { it.raw == raw } ?: CUSTOM_ORDER
    }
}
