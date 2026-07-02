package com.oneuihomeclone.ui

import android.content.ComponentName
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.UserHandle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.oneuihomeclone.data.DrawerSortKey
import com.oneuihomeclone.data.FolderGridKey
import com.oneuihomeclone.data.HomeLayoutKey
import com.oneuihomeclone.data.LauncherState
import com.oneuihomeclone.data.MotionPresetKey
import com.oneuihomeclone.widgets.PreviewSource

internal data class CloneApp(
    val id: String,
    val name: String,
    val launchIntent: Intent? = null,
    val launchTarget: LauncherAppLaunchTarget? = null,
    val icon: ImageBitmap? = null,
    val color: Color,
    val profileBadge: String? = null,
    val statusLabel: String? = null,
    val installProgressPercent: Int? = null,
    val isLaunchable: Boolean = true,
)

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
)

internal data class FinderActionItem(
    val type: FinderActionType,
    val title: String,
    val summary: String,
)

internal data class WidgetTemplateModel(
    val title: String,
    val summary: String,
    val category: String,
    val span: String,
    val accent: Color,
    val providerInfo: AppWidgetProviderInfo? = null,
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

internal const val MAX_ICONS_LOADED_EAGERLY = 300
internal const val MAX_WIDGET_PROVIDERS_LOADED = 150
