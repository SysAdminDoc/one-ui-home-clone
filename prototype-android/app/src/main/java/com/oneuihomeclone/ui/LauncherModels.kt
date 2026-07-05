package com.oneuihomeclone.ui

import android.content.ComponentName
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.net.Uri
import android.os.UserHandle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.oneuihomeclone.data.DrawerSortKey
import com.oneuihomeclone.data.FolderGridKey
import com.oneuihomeclone.data.HomeLayoutKey
import com.oneuihomeclone.data.LauncherState
import com.oneuihomeclone.data.MotionPresetKey
import com.oneuihomeclone.data.NotificationBadgeModeKey
import com.oneuihomeclone.widgets.PreviewSource

internal data class CloneApp(
    val id: String,
    val name: String,
    val packageName: String? = null,
    val launchIntent: Intent? = null,
    val launchTarget: LauncherAppLaunchTarget? = null,
    val icon: ImageBitmap? = null,
    val color: Color,
    val profileBadge: String? = null,
    val statusLabel: String? = null,
    val installProgressPercent: Int? = null,
    val notificationBadge: NotificationBadgeState = NotificationBadgeState.None,
    val isLaunchable: Boolean = true,
    val isRestoredPlaceholder: Boolean = false,
)

internal data class NotificationBadgeState(
    val count: Int,
    val showNumber: Boolean,
) {
    val isVisible: Boolean = count > 0

    companion object {
        val None = NotificationBadgeState(count = 0, showNumber = false)
    }
}

internal fun CloneApp.statusText(): String? =
    installProgressPercent?.let { progress ->
        statusLabel?.let { "$it $progress%" } ?: "$progress%"
    } ?: statusLabel

internal fun CloneApp.accessibilityLabel(): String =
    listOfNotNull(name, profileBadge, statusText()).joinToString(", ")

internal data class LauncherAppLaunchTarget(
    val componentName: ComponentName,
    val user: UserHandle,
)

internal data class LauncherShortcutAction(
    val id: String,
    val packageName: String,
    val shortLabel: String,
    val longLabel: String?,
    val isEnabled: Boolean,
    val disabledMessage: String?,
    val user: UserHandle?,
)

internal sealed interface HomeGridItemModel {
    val id: String
}

internal data class HomePageModel(
    val id: Int,
    val label: String,
    val eyebrow: String,
    val value: String,
    val status: String,
    val note: String,
    val widgets: List<WidgetTemplateModel>,
    val items: List<HomeGridItemModel>,
)

internal data class SettingRowState(
    val title: String,
    val value: String,
)

internal data class StatusClock(
    val timeText: String,
    val dateText: String,
    val fullDateText: String,
)

internal data class FolderModel(
    override val id: String,
    val title: String,
    val summary: String,
    val apps: List<CloneApp>,
) : HomeGridItemModel

internal data class AppItemModel(
    val app: CloneApp,
) : HomeGridItemModel {
    override val id: String = app.id
}

internal data class OpenFolderTarget(
    val pageId: Int,
    val folderId: String,
)

internal data class FinderSettingResult(
    val type: FinderSettingType,
    val title: String,
    val category: String,
    val value: String,
    val usageKey: String = "setting:${type.name}",
)

internal data class FinderActionItem(
    val type: FinderActionType,
    val title: String,
    val summary: String,
    val shortcut: LauncherShortcutAction? = null,
    val usageKey: String = "action:${type.name}",
)

internal data class FinderContactResult(
    val id: String,
    val displayName: String,
    val subtitle: String?,
    val lookupUri: Uri,
)

internal enum class AppContextSource {
    HOME,
    DOCK,
    DRAWER,
    FOLDER,
    HIDE_APPS,
}

internal enum class LauncherContextActionType {
    APP_INFO,
    ADD_TO_HOME,
    HIDE_APP,
    RESTORE_APP,
    REMOVE_FROM_HOME,
    WIDGET_SETTINGS,
    REMOVE_WIDGET,
    SHORTCUT,
}

internal data class LauncherContextAction(
    val type: LauncherContextActionType,
    val title: String,
    val summary: String,
    val enabled: Boolean = true,
    val shortcut: LauncherShortcutAction? = null,
)

internal data class LauncherContextActionText(
    val appInfo: String = "App info",
    val appInfoSummary: String = "Open Android's app details screen",
    val appInfoUnavailableSummary: String = "App details are unavailable for this sample app",
    val addToHome: String = "Add to Home",
    val addToHomeSummary: String = "Place this app on the current Home page",
    val addToHomeUnavailableSummary: String = "This app is already on Home or the page is full",
    val hideApp: String = "Hide",
    val hideAppSummary: String = "Hide from Home and Apps screens",
    val restoreApp: String = "Restore",
    val restoreAppSummary: String = "Show in Home and Apps screens again",
    val removeFromHome: String = "Remove from Home",
    val removeFromHomeSummary: String = "Keep the app installed and remove only this Home shortcut",
    val widgetSettings: String = "Widget settings",
    val widgetSettingsSummary: String = "Open this widget's configuration screen",
    val widgetSettingsUnavailableSummary: String = "This widget does not expose settings",
    val removeWidget: String = "Remove widget",
    val removeWidgetSummary: String = "Remove this widget from Home",
    val shortcutSummary: String = "App shortcut",
)

internal data class WidgetTemplateModel(
    val title: String,
    val summary: String,
    val category: String,
    val span: String,
    val accent: Color,
    val providerInfo: AppWidgetProviderInfo? = null,
    val profileBadge: String? = null,
    val profileUserSerial: Long? = null,
    val previewSource: PreviewSource = PreviewSource.Empty,
    val hostWidgetId: Int? = null,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 4,
    val spanY: Int = 2,
    val minSpanX: Int = 1,
    val minSpanY: Int = 1,
    val maxSpanX: Int = 4,
    val maxSpanY: Int = 4,
    val canResizeHorizontal: Boolean = true,
    val canResizeVertical: Boolean = true,
    val restoredProviderPackage: String? = null,
    val restoredProviderClass: String? = null,
)

internal data class NotificationCardModel(
    val title: String,
    val summary: String,
    val timestamp: String,
)

internal enum class FinderActionType {
    SETTINGS,
    WALLPAPERS,
    WIDGETS,
    PAGE_MANAGER,
    MEDIA_PAGE,
    HOME_PAGE,
    HIDE_APPS,
    APP_SHORTCUT,
}

internal enum class FinderSettingType {
    HOME_SCREEN_LAYOUT,
    HOME_SCREEN_GRID,
    APPS_SCREEN_GRID,
    FOLDER_GRID,
    DEFAULT_HOME_PAGE,
    VISIBLE_PAGES,
    MEDIA_PAGE,
    APPS_BUTTON,
    APP_LABELS,
    WIDGET_LABELS,
    SWIPE_DOWN_NOTIFICATIONS,
    HIDE_APPS,
    LOCK_LAYOUT,
    ADD_NEW_APPS,
    BADGE_NOTIFICATIONS,
    FINDER_CONTACTS,
}

internal enum class HomeLayoutMode(val title: String) {
    HOME_AND_APPS_SCREENS("Home and Apps screens"),
    HOME_SCREEN_ONLY("Home screen only"),
}

internal enum class DrawerSortMode(val title: String) {
    CUSTOM_ORDER("Custom order"),
    ALPHABETICAL("Alphabetical order"),
}

internal enum class MotionPresetMode(val title: String) {
    STANDARD("Standard"),
    REDUCED("Reduced"),
}

internal enum class FolderGridMode(val title: String, val columns: Int, val rows: Int) {
    GRID_3X4("3x4", 3, 4),
    GRID_4X4("4x4", 4, 4),
    GRID_5X5("5x5", 5, 5),
}

internal enum class NotificationBadgeMode(val title: String) {
    OFF("Off"),
    DOTS("Dots"),
    DOTS_AND_NUMBER("Dots and number"),
}

internal enum class OverlayPanel {
    DRAWER,
    NOTIFICATIONS,
    SETTINGS,
    EDIT_MODE,
    FOLDER,
    WIDGET_PICKER,
    HIDE_APPS,
}

internal data class PersistedToggles(
    val mediaPageEnabled: Boolean,
    val appsButtonEnabled: Boolean,
    val appLabelsEnabled: Boolean,
    val widgetLabelsEnabled: Boolean,
    val swipeDownForNotifications: Boolean,
    val addNewAppsToHomeScreen: Boolean,
    val notificationBadgeMode: NotificationBadgeMode,
    val finderContactsEnabled: Boolean,
    val lockHomeScreenLayout: Boolean,
    val homeLayoutMode: HomeLayoutMode,
    val drawerSortMode: DrawerSortMode,
    val motionPreset: MotionPresetMode,
    val folderGrid: FolderGridMode,
)

internal fun LauncherState.toPersistedToggles(): PersistedToggles = PersistedToggles(
    mediaPageEnabled = mediaPageEnabled,
    appsButtonEnabled = appsButtonEnabled,
    appLabelsEnabled = appLabelsEnabled,
    widgetLabelsEnabled = widgetLabelsEnabled,
    swipeDownForNotifications = swipeDownForNotifications,
    addNewAppsToHomeScreen = addNewAppsToHomeScreen,
    notificationBadgeMode = when (notificationBadgeMode) {
        NotificationBadgeModeKey.OFF -> NotificationBadgeMode.OFF
        NotificationBadgeModeKey.DOTS -> NotificationBadgeMode.DOTS
        NotificationBadgeModeKey.DOTS_AND_NUMBER -> NotificationBadgeMode.DOTS_AND_NUMBER
    },
    finderContactsEnabled = finderContactsEnabled,
    lockHomeScreenLayout = lockHomeScreenLayout,
    homeLayoutMode = when (homeLayoutMode) {
        HomeLayoutKey.HOME_AND_APPS_SCREENS -> HomeLayoutMode.HOME_AND_APPS_SCREENS
        HomeLayoutKey.HOME_SCREEN_ONLY -> HomeLayoutMode.HOME_SCREEN_ONLY
    },
    drawerSortMode = when (drawerSortMode) {
        DrawerSortKey.CUSTOM_ORDER -> DrawerSortMode.CUSTOM_ORDER
        DrawerSortKey.ALPHABETICAL -> DrawerSortMode.ALPHABETICAL
    },
    motionPreset = when (motionPreset) {
        MotionPresetKey.STANDARD -> MotionPresetMode.STANDARD
        MotionPresetKey.REDUCED -> MotionPresetMode.REDUCED
    },
    folderGrid = when (folderGrid) {
        FolderGridKey.GRID_3X4 -> FolderGridMode.GRID_3X4
        FolderGridKey.GRID_4X4 -> FolderGridMode.GRID_4X4
        FolderGridKey.GRID_5X5 -> FolderGridMode.GRID_5X5
    },
)

internal fun PersistedToggles.toLauncherState(): LauncherState = LauncherState(
    mediaPageEnabled = mediaPageEnabled,
    appsButtonEnabled = appsButtonEnabled,
    appLabelsEnabled = appLabelsEnabled,
    widgetLabelsEnabled = widgetLabelsEnabled,
    swipeDownForNotifications = swipeDownForNotifications,
    addNewAppsToHomeScreen = addNewAppsToHomeScreen,
    notificationBadgeMode = when (notificationBadgeMode) {
        NotificationBadgeMode.OFF -> NotificationBadgeModeKey.OFF
        NotificationBadgeMode.DOTS -> NotificationBadgeModeKey.DOTS
        NotificationBadgeMode.DOTS_AND_NUMBER -> NotificationBadgeModeKey.DOTS_AND_NUMBER
    },
    finderContactsEnabled = finderContactsEnabled,
    lockHomeScreenLayout = lockHomeScreenLayout,
    homeLayoutMode = when (homeLayoutMode) {
        HomeLayoutMode.HOME_AND_APPS_SCREENS -> HomeLayoutKey.HOME_AND_APPS_SCREENS
        HomeLayoutMode.HOME_SCREEN_ONLY -> HomeLayoutKey.HOME_SCREEN_ONLY
    },
    drawerSortMode = when (drawerSortMode) {
        DrawerSortMode.CUSTOM_ORDER -> DrawerSortKey.CUSTOM_ORDER
        DrawerSortMode.ALPHABETICAL -> DrawerSortKey.ALPHABETICAL
    },
    motionPreset = when (motionPreset) {
        MotionPresetMode.STANDARD -> MotionPresetKey.STANDARD
        MotionPresetMode.REDUCED -> MotionPresetKey.REDUCED
    },
    folderGrid = when (folderGrid) {
        FolderGridMode.GRID_3X4 -> FolderGridKey.GRID_3X4
        FolderGridMode.GRID_4X4 -> FolderGridKey.GRID_4X4
        FolderGridMode.GRID_5X5 -> FolderGridKey.GRID_5X5
    },
)

internal const val MAX_WIDGET_PROVIDERS_LOADED = 150
internal const val MAX_HOME_GRID_ITEMS = 16
internal const val MAX_CONTEXT_SHORTCUTS = 4
internal const val MAX_FINDER_SHORTCUT_RESULTS = 8
internal const val MAX_FINDER_SHORTCUT_APPS_SCANNED = 96
internal const val MAX_FINDER_CONTACT_RESULTS = 8
internal const val MAX_DEVICE_CONTACT_ROWS = 32
internal const val MAX_APP_ICON_CACHE_ENTRIES = 96
