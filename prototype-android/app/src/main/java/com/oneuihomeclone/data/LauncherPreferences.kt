package com.oneuihomeclone.data

/** Immutable snapshot of persisted user toggles. */
data class LauncherState(
    val mediaPageEnabled: Boolean = true,
    val appsButtonEnabled: Boolean = true,
    val appLabelsEnabled: Boolean = true,
    val widgetLabelsEnabled: Boolean = true,
    val swipeDownForNotifications: Boolean = true,
    val addNewAppsToHomeScreen: Boolean = true,
    val notificationBadgeMode: NotificationBadgeModeKey = NotificationBadgeModeKey.OFF,
    val lockHomeScreenLayout: Boolean = false,
    val homeLayoutMode: HomeLayoutKey = HomeLayoutKey.HOME_AND_APPS_SCREENS,
    val drawerSortMode: DrawerSortKey = DrawerSortKey.CUSTOM_ORDER,
    val motionPreset: MotionPresetKey = MotionPresetKey.STANDARD,
    val folderGrid: FolderGridKey = FolderGridKey.GRID_3X4,
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

enum class FolderGridKey(val raw: String) {
    GRID_3X4("3x4"),
    GRID_4X4("4x4"),
    GRID_5X5("5x5");

    companion object {
        fun fromRaw(raw: String?): FolderGridKey =
            entries.firstOrNull { it.raw == raw } ?: GRID_3X4
    }
}

enum class NotificationBadgeModeKey(val raw: String) {
    OFF("off"),
    DOTS("dots"),
    DOTS_AND_NUMBER("dots_and_number");

    companion object {
        fun fromRaw(raw: String?): NotificationBadgeModeKey =
            entries.firstOrNull { it.raw == raw } ?: OFF
    }
}
