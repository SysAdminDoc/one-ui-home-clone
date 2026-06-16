package com.oneuihomeclone.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.oneuihomeclone.widgets.PreviewSource

internal data class CloneApp(
    val id: String,
    val name: String,
    val launchIntent: Intent? = null,
    val icon: ImageBitmap? = null,
    val color: Color,
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
    val spanX: Int = 4,
    val spanY: Int = 2,
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

internal const val MAX_ICONS_LOADED_EAGERLY = 300
internal const val MAX_WIDGET_PROVIDERS_LOADED = 150
