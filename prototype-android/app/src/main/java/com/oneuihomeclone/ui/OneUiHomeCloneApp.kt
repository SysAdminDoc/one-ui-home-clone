package com.oneuihomeclone.ui

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.oneuihomeclone.DefaultLauncherState
import com.oneuihomeclone.LauncherApp
import com.oneuihomeclone.R
import com.oneuihomeclone.data.BoundWidget
import com.oneuihomeclone.data.DrawerSortKey
import com.oneuihomeclone.data.FolderGridKey
import com.oneuihomeclone.data.HomeLayoutKey
import com.oneuihomeclone.data.LauncherDataStore
import com.oneuihomeclone.data.LauncherLayoutStore
import com.oneuihomeclone.data.LauncherState
import com.oneuihomeclone.data.MotionPresetKey
import com.oneuihomeclone.data.WidgetPersistence
import com.oneuihomeclone.ui.motion.ProvideMotionScheme
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiBorder
import com.oneuihomeclone.ui.theme.OneUiCard
import com.oneuihomeclone.ui.theme.OneUiPositive
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiSurfaceSoft
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.oneuihomeclone.widgets.PreviewSource
import com.oneuihomeclone.widgets.WidgetBindRequest
import com.oneuihomeclone.widgets.WidgetBindResult
import com.oneuihomeclone.widgets.WidgetPreviewLoader
import com.oneuihomeclone.shouldShowDefaultLauncherPrompt

private fun sampleApps(): List<CloneApp> {
    return listOf(
        CloneApp(id = "sample-gallery", name = "Gallery", color = Color(0xFFFFB84D)),
        CloneApp(id = "sample-camera", name = "Camera", color = Color(0xFF6D8BFF)),
        CloneApp(id = "sample-messages", name = "Messages", color = Color(0xFF45C48B)),
        CloneApp(id = "sample-phone", name = "Phone", color = Color(0xFF35C15E)),
        CloneApp(id = "sample-calendar", name = "Calendar", color = Color(0xFFFF6B6B)),
        CloneApp(id = "sample-clock", name = "Clock", color = Color(0xFF5865F2)),
        CloneApp(id = "sample-settings", name = "Settings", color = Color(0xFF8A94A6)),
        CloneApp(id = "sample-notes", name = "Notes", color = Color(0xFFFFC857)),
        CloneApp(id = "sample-internet", name = "Internet", color = Color(0xFF7B61FF)),
        CloneApp(id = "sample-files", name = "Files", color = Color(0xFF50B5FF)),
        CloneApp(id = "sample-contacts", name = "Contacts", color = Color(0xFF55C6A9)),
        CloneApp(id = "sample-store", name = "Store", color = Color(0xFFFF7F50)),
        CloneApp(id = "sample-health", name = "Health", color = Color(0xFF28B7A1)),
        CloneApp(id = "sample-weather", name = "Weather", color = Color(0xFF65B8FF)),
        CloneApp(id = "sample-music", name = "Music", color = Color(0xFFFF5F8F)),
        CloneApp(id = "sample-reminders", name = "Reminders", color = Color(0xFF1FCE84)),
        CloneApp(id = "sample-calculator", name = "Calculator", color = Color(0xFF627085)),
        CloneApp(id = "sample-smartthings", name = "SmartThings", color = Color(0xFF2EBCF6)),
        CloneApp(id = "sample-recorder", name = "Recorder", color = Color(0xFFFF8D5A)),
        CloneApp(id = "sample-daily", name = "Daily", color = Color(0xFF7A6BFF)),
    )
}

private sealed interface LauncherContextTarget {
    data class App(
        val app: CloneApp,
        val source: AppContextSource,
    ) : LauncherContextTarget

    data class Widget(
        val widget: WidgetTemplateModel,
    ) : LauncherContextTarget
}

private suspend fun loadWidgetProviderTemplates(
    context: Context,
    fallbackWidgets: List<WidgetTemplateModel>,
): List<WidgetTemplateModel> = withContext(Dispatchers.IO) {
    val packageManager = context.packageManager
    val providers = runCatching {
        AppWidgetManager.getInstance(context).getInstalledProviders()
    }.getOrElse { cause ->
        Log.w("OneUiHome/widgets", "Widget provider query failed (${cause.javaClass.simpleName})")
        emptyList()
    }
    val widgetsFallbackLabel = context.getString(R.string.widgets_title)

    val widgets = providers
        .asSequence()
        .filter { info -> info.provider != null }
        .distinctBy { info -> info.provider.flattenToShortString() }
        .sortedWith(
            compareBy<AppWidgetProviderInfo> { info ->
                widgetProviderAppLabel(packageManager, info, widgetsFallbackLabel).lowercase(Locale.getDefault())
            }.thenBy { info ->
                widgetProviderLabel(packageManager, info).lowercase(Locale.getDefault())
            },
        )
        .take(MAX_WIDGET_PROVIDERS_LOADED)
        .map { info ->
            val appLabel = widgetProviderAppLabel(packageManager, info, widgetsFallbackLabel)
            val label = widgetProviderLabel(packageManager, info)
            val spanX = widgetSpanX(info)
            val spanY = widgetSpanY(info)
            val canResizeHorizontal = widgetCanResizeHorizontal(info)
            val canResizeVertical = widgetCanResizeVertical(info)
            WidgetTemplateModel(
                title = label,
                summary = context.getString(R.string.widgets_provider_summary, appLabel),
                category = appLabel,
                span = "$spanX x $spanY",
                accent = fallbackColorFor(info.provider.flattenToShortString()),
                providerInfo = info,
                previewSource = WidgetPreviewLoader.load(context, info),
                spanX = spanX,
                spanY = spanY,
                minSpanX = widgetMinResizeSpanX(info, spanX),
                minSpanY = widgetMinResizeSpanY(info, spanY),
                maxSpanX = if (canResizeHorizontal) 4 else spanX,
                maxSpanY = if (canResizeVertical) 4 else spanY,
                canResizeHorizontal = canResizeHorizontal,
                canResizeVertical = canResizeVertical,
            )
        }
        .toList()

    widgets.ifEmpty { fallbackWidgets }
}

@Composable
fun OneUiHomeCloneApp(
    homeIntentTick: Int = 0,
    recoveryNotice: String? = null,
    defaultLauncherState: DefaultLauncherState = DefaultLauncherState.Unknown,
    onOpenDefaultLauncherSettings: () -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val launcherDataStore = remember(appContext) { LauncherDataStore(appContext) }
    val launcherState: LauncherState? by launcherDataStore.state.collectAsStateWithLifecycle(initialValue = null)
    val widgetPersistence = remember(appContext) { WidgetPersistence(appContext) }
    val layoutStore = remember(appContext) { LauncherLayoutStore(appContext) }
    val coroutineScope = rememberCoroutineScope()
    var hasAppliedPersistedSettings by remember { mutableStateOf(false) }
    val initialPrefs = LauncherState()
    val fallbackApps = remember { sampleApps() }
    val appInventory = remember(appContext, fallbackApps) { LauncherAppInventory(appContext, fallbackApps) }
    var allApps by remember { mutableStateOf(fallbackApps) }
    var appInventoryLoaded by remember { mutableStateOf(false) }
    var hasSeededDeviceApps by remember { mutableStateOf(false) }
    val dockApps = remember(allApps) { allApps.take(4) }
    val fallbackWidgetTemplates = remember {
        listOf(
            WidgetTemplateModel("Calendar", "Month agenda with rounded launcher chrome", "Recommended", "4 x 2", Color(0xFFFF8B7B)),
            WidgetTemplateModel("Weather", "Large conditions card with soft edge highlights", "Recommended", "4 x 2", Color(0xFF62B8FF)),
            WidgetTemplateModel("SmartThings", "Scenes and devices in a compact control stack", "Connected", "4 x 2", Color(0xFF2EBCF6)),
            WidgetTemplateModel("Battery", "Device and buds battery status", "Device", "4 x 1", Color(0xFF5ECB85)),
            WidgetTemplateModel("Music", "Now playing with album art emphasis", "Entertainment", "4 x 2", Color(0xFFFF6F96)),
            WidgetTemplateModel("Reminder list", "Pinned tasks for routines and grocery runs", "Productivity", "4 x 2", Color(0xFFFFC857)),
        )
    }
    var widgetTemplates by remember { mutableStateOf(fallbackWidgetTemplates) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var defaultLauncherPromptDismissed by remember { mutableStateOf(false) }
    var contextTarget by remember { mutableStateOf<LauncherContextTarget?>(null) }
    var contextShortcuts by remember { mutableStateOf(emptyList<LauncherShortcutAction>()) }
    val homeScreenSettingsTitle = stringResource(R.string.settings_title_home_screen)
    val hideAppsTitle = stringResource(R.string.settings_hide_apps)
    val lockLayoutTitle = stringResource(R.string.settings_lock_layout)
    val defaultHomeLabel = stringResource(R.string.home_default_label)
    val fallbackHomePageLabel = stringResource(R.string.home_page_label, 1)
    val finderSettingText = FinderSettingText(
        homeScreenLayout = stringResource(R.string.settings_home_screen_layout),
        homeScreenGrid = stringResource(R.string.settings_home_screen_grid),
        appsScreenGrid = stringResource(R.string.settings_apps_screen_grid),
        folderGrid = stringResource(R.string.settings_folder_grid),
        defaultHomePage = stringResource(R.string.settings_default_home_page),
        visiblePages = stringResource(R.string.settings_visible_pages),
        mediaPage = stringResource(R.string.settings_media_page),
        appsButton = stringResource(R.string.settings_apps_button),
        appLabels = stringResource(R.string.settings_app_labels),
        widgetLabels = stringResource(R.string.settings_widget_labels),
        swipeDownNotifications = stringResource(R.string.settings_swipe_notifications),
        hideApps = hideAppsTitle,
        lockLayout = lockLayoutTitle,
        addNewApps = stringResource(R.string.settings_add_new_apps),
        badgeNotifications = stringResource(R.string.settings_badge_notifications),
        layoutCategory = stringResource(R.string.settings_section_layout),
        behaviorCategory = stringResource(R.string.settings_section_behavior),
        gesturesCategory = stringResource(R.string.settings_category_gestures),
        appsScreenCategory = stringResource(R.string.drawer_section_apps_screen),
        onValue = stringResource(R.string.settings_value_on),
        offValue = stringResource(R.string.state_off),
        noneValue = stringResource(R.string.settings_value_none),
        dotsAndNumberValue = stringResource(R.string.settings_value_dots_and_number),
        appsSortUnavailable = stringResource(R.string.settings_sort_unavailable_home_only),
        hiddenCount = { count -> appContext.getString(R.string.settings_value_hidden_count, count) },
    )
    val finderActionText = FinderActionText(
        homeScreenSettingsTitle = homeScreenSettingsTitle,
        settingsHomeOnlySummary = stringResource(R.string.finder_action_settings_home_only),
        settingsDefaultSummary = stringResource(R.string.finder_action_settings_default),
        wallpapersTitle = stringResource(R.string.edit_wallpapers_style),
        wallpaperLockedSummary = stringResource(R.string.finder_action_wallpaper_locked),
        wallpaperDefaultSummary = stringResource(R.string.finder_action_wallpaper_default),
        widgetsTitle = stringResource(R.string.widgets_title),
        widgetsSummary = stringResource(R.string.finder_action_widgets_summary),
        pageManagerTitle = stringResource(R.string.edit_page_manager),
        pageManagerLockedSummary = stringResource(R.string.finder_action_page_manager_locked),
        pageManagerDefaultSummary = stringResource(R.string.finder_action_page_manager_default),
        mediaGoTitle = stringResource(R.string.finder_action_media_go_title),
        mediaEnableTitle = stringResource(R.string.finder_action_media_enable_title),
        mediaGoSummary = stringResource(R.string.finder_action_media_go_summary),
        mediaEnableSummary = stringResource(R.string.finder_action_media_enable_summary),
        defaultHomeTitle = stringResource(R.string.finder_action_default_home_title),
        defaultHomeSummary = stringResource(R.string.finder_action_default_home_summary),
        manageHiddenTitle = stringResource(R.string.finder_action_manage_hidden_title),
        hideAppsTitle = hideAppsTitle,
        manageHiddenSummary = stringResource(R.string.finder_action_manage_hidden_summary),
        hideAppsSummary = stringResource(R.string.finder_action_hide_apps_summary),
    )
    val contextActionText = LauncherContextActionText(
        appInfo = stringResource(R.string.context_action_app_info),
        appInfoSummary = stringResource(R.string.context_action_app_info_summary),
        appInfoUnavailableSummary = stringResource(R.string.context_action_app_info_unavailable),
        addToHome = stringResource(R.string.context_action_add_to_home),
        addToHomeSummary = stringResource(R.string.context_action_add_to_home_summary),
        addToHomeUnavailableSummary = stringResource(R.string.context_action_add_to_home_unavailable),
        hideApp = stringResource(R.string.action_hide),
        hideAppSummary = stringResource(R.string.context_action_hide_summary),
        restoreApp = stringResource(R.string.action_restore),
        restoreAppSummary = stringResource(R.string.context_action_restore_summary),
        removeFromHome = stringResource(R.string.context_action_remove_from_home),
        removeFromHomeSummary = stringResource(R.string.context_action_remove_from_home_summary),
        widgetSettings = stringResource(R.string.context_action_widget_settings),
        widgetSettingsSummary = stringResource(R.string.context_action_widget_settings_summary),
        widgetSettingsUnavailableSummary = stringResource(R.string.context_action_widget_settings_unavailable),
        removeWidget = stringResource(R.string.context_action_remove_widget),
        removeWidgetSummary = stringResource(R.string.context_action_remove_widget_summary),
        shortcutSummary = stringResource(R.string.context_action_shortcut_summary),
    )

    fun showFeedback(message: String) {
        feedbackMessage = message
    }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2600)
            feedbackMessage = null
        }
    }

    LaunchedEffect(recoveryNotice) {
        if (!recoveryNotice.isNullOrBlank()) {
            feedbackMessage = recoveryNotice
        }
    }

    LaunchedEffect(defaultLauncherState.isDefaultLauncher) {
        if (defaultLauncherState.isDefaultLauncher) {
            defaultLauncherPromptDismissed = false
        }
    }

    LaunchedEffect(contextTarget) {
        val appTarget = contextTarget as? LauncherContextTarget.App
        contextShortcuts = emptyList()
        if (appTarget != null) {
            contextShortcuts = appInventory.loadDynamicShortcuts(appTarget.app)
        }
    }

    val launchSelectedApp: (CloneApp) -> Unit = { app ->
        if (!appInventory.launch(app)) {
            val statusText = app.statusText()
            if (statusText != null) {
                showFeedback(appContext.getString(R.string.feedback_app_unavailable, app.name, statusText))
            } else if (app.launchIntent == null && app.launchTarget == null) {
                showFeedback(appContext.getString(R.string.feedback_app_loading, app.name))
            } else {
                showFeedback(appContext.getString(R.string.feedback_app_open_failed, app.name))
            }
        }
    }
    val clock = rememberStatusClock()

    var activeOverlay by remember { mutableStateOf<OverlayPanel?>(null) }
    var openFolderTarget by remember { mutableStateOf<OpenFolderTarget?>(null) }
    var homeLayoutMode by remember {
        mutableStateOf(
            when (initialPrefs.homeLayoutMode) {
                HomeLayoutKey.HOME_AND_APPS_SCREENS -> HomeLayoutMode.HOME_AND_APPS_SCREENS
                HomeLayoutKey.HOME_SCREEN_ONLY -> HomeLayoutMode.HOME_SCREEN_ONLY
            },
        )
    }
    var lockHomeScreenLayout by remember { mutableStateOf(initialPrefs.lockHomeScreenLayout) }
    val drawerApps = allApps
    var drawerSortMode by remember {
        mutableStateOf(
            when (initialPrefs.drawerSortMode) {
                DrawerSortKey.CUSTOM_ORDER -> DrawerSortMode.CUSTOM_ORDER
                DrawerSortKey.ALPHABETICAL -> DrawerSortMode.ALPHABETICAL
            },
        )
    }
    var drawerPageIndex by remember { mutableIntStateOf(0) }
    var hiddenAppIds by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    val initialRecentSearches = listOf(
        stringResource(R.string.settings_media_page),
        stringResource(R.string.settings_folder_grid),
        stringResource(R.string.widgets_title),
        stringResource(R.string.settings_home_screen_grid),
    )
    var recentSearches by remember {
        mutableStateOf(initialRecentSearches)
    }
    var mediaPageEnabled by remember { mutableStateOf(initialPrefs.mediaPageEnabled) }
    var appsButtonEnabled by remember { mutableStateOf(initialPrefs.appsButtonEnabled) }
    var appLabelsEnabled by remember { mutableStateOf(initialPrefs.appLabelsEnabled) }
    var widgetLabelsEnabled by remember { mutableStateOf(initialPrefs.widgetLabelsEnabled) }
    var swipeDownForNotifications by remember { mutableStateOf(initialPrefs.swipeDownForNotifications) }
    var motionPreset by remember {
        mutableStateOf(
            when (initialPrefs.motionPreset) {
                MotionPresetKey.STANDARD -> MotionPresetMode.STANDARD
                MotionPresetKey.REDUCED -> MotionPresetMode.REDUCED
            },
        )
    }
    var folderGrid by remember {
        mutableStateOf(
            when (initialPrefs.folderGrid) {
                FolderGridKey.GRID_3X4 -> FolderGridMode.GRID_3X4
                FolderGridKey.GRID_4X4 -> FolderGridMode.GRID_4X4
                FolderGridKey.GRID_5X5 -> FolderGridMode.GRID_5X5
            },
        )
    }
    LaunchedEffect(launcherState) {
        val state = launcherState ?: return@LaunchedEffect
        if (hasAppliedPersistedSettings) return@LaunchedEffect
        val toggles = state.toPersistedToggles()
        mediaPageEnabled = toggles.mediaPageEnabled
        appsButtonEnabled = toggles.appsButtonEnabled
        appLabelsEnabled = toggles.appLabelsEnabled
        widgetLabelsEnabled = toggles.widgetLabelsEnabled
        swipeDownForNotifications = toggles.swipeDownForNotifications
        lockHomeScreenLayout = toggles.lockHomeScreenLayout
        homeLayoutMode = toggles.homeLayoutMode
        drawerSortMode = toggles.drawerSortMode
        motionPreset = toggles.motionPreset
        folderGrid = toggles.folderGrid
        hasAppliedPersistedSettings = true
    }
    var settingsFocusTitle by remember { mutableStateOf<String?>(null) }
    var selectedWidgetCategory by remember { mutableStateOf("Recommended") }
    var nextPageId by remember { mutableIntStateOf(3) }
    var nextFolderId by remember { mutableIntStateOf(3) }
    var isHomeItemDragActive by remember { mutableStateOf(false) }
    var homePages by remember {
        mutableStateOf(
            listOf(
                buildHomePage(1, allApps),
                buildHomePage(2, allApps),
            ),
        )
    }
    val latestHomePages by rememberUpdatedState(homePages)
    var defaultHomePageIndex by remember { mutableIntStateOf(0) }
    var pageIndex by remember { mutableIntStateOf(1) }

    LaunchedEffect(appInventory) {
        appInventory.apps().collect { loadedApps ->
            allApps = loadedApps
            appInventoryLoaded = true
        }
    }

    LaunchedEffect(appContext, fallbackWidgetTemplates) {
        widgetTemplates = loadWidgetProviderTemplates(appContext, fallbackWidgetTemplates)
    }

    LaunchedEffect(widgetPersistence, widgetTemplates) {
        widgetPersistence.widgets.collect { boundWidgets ->
            homePages = mergeBoundWidgetsIntoPages(
                pages = latestHomePages,
                boundWidgets = boundWidgets,
                templates = widgetTemplates,
            )
        }
    }

    // Persist user-facing toggles only after the DataStore snapshot initializes Compose
    // state. The first non-null emission is that loaded snapshot, so skip it.
    LaunchedEffect(launcherDataStore) {
        snapshotFlow {
            if (!hasAppliedPersistedSettings) {
                null
            } else {
                PersistedToggles(
                    mediaPageEnabled = mediaPageEnabled,
                    appsButtonEnabled = appsButtonEnabled,
                    appLabelsEnabled = appLabelsEnabled,
                    widgetLabelsEnabled = widgetLabelsEnabled,
                    swipeDownForNotifications = swipeDownForNotifications,
                    lockHomeScreenLayout = lockHomeScreenLayout,
                    homeLayoutMode = homeLayoutMode,
                    drawerSortMode = drawerSortMode,
                    motionPreset = motionPreset,
                    folderGrid = folderGrid,
                )
            }
        }
            .filterNotNull()
            .drop(1)
            .collect { toggles ->
                launcherDataStore.update {
                    setLauncherState(toggles.toLauncherState())
                }
            }
    }

    LaunchedEffect(layoutStore) {
        snapshotFlow {
            if (!hasSeededDeviceApps) {
                null
            } else {
                buildPersistedLauncherLayout(
                    pages = homePages,
                    defaultHomePageIndex = defaultHomePageIndex,
                    hiddenAppIds = hiddenAppIds,
                    recentSearches = recentSearches,
                    nextPageId = nextPageId,
                    nextFolderId = nextFolderId,
                )
            }
        }
            .drop(1)
            .collect { layout ->
                if (layout != null) {
                    layoutStore.save(layout)
                }
            }
    }

    // HOME intent re-entry (user pressed HOME while inside the launcher, or picked us
    // from the home-app picker again). Collapse every overlay + scroll to default page.
    LaunchedEffect(homeIntentTick) {
        if (homeIntentTick > 0) {
            contextTarget = null
            activeOverlay = null
            openFolderTarget = null
            searchQuery = ""
            settingsFocusTitle = null
            drawerPageIndex = 0
            pageIndex = visualIndexForHomePage(defaultHomePageIndex, mediaPageEnabled)
        }
    }

    // Launcher BACK semantics: collapse overlays first, then restore default page,
    // then absorb further back presses — HOME is the bottom of the nav stack.
    BackHandler(enabled = true) {
        when {
            contextTarget != null -> contextTarget = null
            activeOverlay != null -> activeOverlay = null
            openFolderTarget != null -> openFolderTarget = null
            searchQuery.isNotEmpty() -> searchQuery = ""
            pageIndex != visualIndexForHomePage(defaultHomePageIndex, mediaPageEnabled) -> {
                pageIndex = visualIndexForHomePage(defaultHomePageIndex, mediaPageEnabled)
            }
            // else: already on default home with no overlays — absorb the back press.
        }
    }

    val pageCount = totalPageCount(homePages.size, mediaPageEnabled)
    val currentPageIndex = pageIndex.coerceIn(0, pageCount - 1)
    val currentHomePageIndex = homePageIndexFromVisual(currentPageIndex, mediaPageEnabled)
    val isMediaPage = mediaPageEnabled && currentPageIndex == 0
    val visibleHomePages = remember(homePages, hiddenAppIds) {
        applyHiddenAppsToPages(homePages, hiddenAppIds)
    }
    val currentHomePage = currentHomePageIndex?.let(visibleHomePages::getOrNull)
    val widgetTargetHomePageIndex = currentHomePageIndex ?: defaultHomePageIndex
    val widgetTargetPage = homePages.getOrNull(widgetTargetHomePageIndex)
    val openFolder = openFolderTarget?.let { target ->
        visibleHomePages
            .firstOrNull { it.id == target.pageId }
            ?.items
            ?.filterIsInstance<FolderModel>()
            ?.firstOrNull { it.id == target.folderId }
    }
    val visibleDockApps = remember(dockApps, allApps, hiddenAppIds) {
        buildVisibleDockApps(dockApps, allApps, hiddenAppIds)
    }
    val appsScreenApps = remember(drawerApps, drawerSortMode, hiddenAppIds) {
        when (drawerSortMode) {
            DrawerSortMode.CUSTOM_ORDER -> drawerApps.filterNot { it.id in hiddenAppIds }
            DrawerSortMode.ALPHABETICAL -> drawerApps.filterNot { it.id in hiddenAppIds }.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }
    }
    val drawerPages = remember(drawerApps, hiddenAppIds) {
        drawerApps.filterNot { it.id in hiddenAppIds }.chunked(20)
    }
    val filteredApps = remember(searchQuery, appsScreenApps) {
        if (searchQuery.isBlank()) {
            appsScreenApps
        } else {
            appsScreenApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    val widgetCategories = remember(widgetTemplates) { buildWidgetCategories(widgetTemplates) }
    val filteredWidgetTemplates = remember(selectedWidgetCategory, widgetTemplates) {
        filterWidgetsForCategory(widgetTemplates, selectedWidgetCategory)
    }
    val activeBoundWidgetCount = remember(homePages) { boundWidgetCount(homePages) }
    val localizedHomeLayoutTitle = homeLayoutMode.localizedTitle()
    val localizedDrawerSortTitle = drawerSortMode.localizedTitle()
    val defaultFinderHomePageLabel = homePages.getOrNull(defaultHomePageIndex)?.label ?: fallbackHomePageLabel
    val hiddenAppsValue = if (hiddenAppIds.isEmpty()) {
        stringResource(R.string.settings_value_none)
    } else {
        stringResource(R.string.settings_value_hidden_count, hiddenAppIds.size)
    }
    val finderSettings = remember(
        searchQuery,
        homeLayoutMode,
        localizedHomeLayoutTitle,
        lockHomeScreenLayout,
        mediaPageEnabled,
        appsButtonEnabled,
        appLabelsEnabled,
        widgetLabelsEnabled,
        swipeDownForNotifications,
        homePages,
        defaultHomePageIndex,
        defaultFinderHomePageLabel,
        hiddenAppIds,
        hiddenAppsValue,
    ) {
        buildFinderSettingResults(
            query = searchQuery,
            homeLayoutMode = homeLayoutMode,
            lockHomeScreenLayout = lockHomeScreenLayout,
            mediaPageEnabled = mediaPageEnabled,
            appsButtonEnabled = appsButtonEnabled,
            appLabelsEnabled = appLabelsEnabled,
            widgetLabelsEnabled = widgetLabelsEnabled,
            swipeDownForNotifications = swipeDownForNotifications,
            homePageCount = homePages.size,
            defaultHomePageLabel = defaultFinderHomePageLabel,
            hiddenAppCount = hiddenAppIds.size,
            text = finderSettingText,
            homeLayoutModeTitle = localizedHomeLayoutTitle,
            hiddenAppsValue = hiddenAppsValue,
        )
    }
    val finderActions = remember(searchQuery, homeLayoutMode, lockHomeScreenLayout, mediaPageEnabled, hiddenAppIds, finderActionText) {
        buildFinderActionResults(
            query = searchQuery,
            homeLayoutMode = homeLayoutMode,
            lockHomeScreenLayout = lockHomeScreenLayout,
            mediaPageEnabled = mediaPageEnabled,
            hasHiddenApps = hiddenAppIds.isNotEmpty(),
            text = finderActionText,
        )
    }

    LaunchedEffect(appInventoryLoaded, allApps, layoutStore) {
        val hasRealApps = allApps.any { it.launchIntent != null || it.launchTarget != null }
        if (!appInventoryLoaded || !hasRealApps) return@LaunchedEffect

        if (!hasSeededDeviceApps) {
            val persistedLayout = layoutStore.read()
            if (persistedLayout != null) {
                val restoredPages = restorePersistedHomePages(persistedLayout, allApps).ifEmpty {
                    listOf(
                        buildHomePage(1, allApps),
                        buildHomePage(2, allApps),
                    )
                }
                homePages = restoredPages
                hiddenAppIds = reconcileHiddenAppIds(persistedLayout.hiddenAppIds, allApps)
                recentSearches = persistedLayout.recentSearches.ifEmpty { recentSearches }
                defaultHomePageIndex = persistedLayout.defaultHomePageIndex.coerceIn(restoredPages.indices)
                nextPageId = maxOf(persistedLayout.nextPageId, restoredPages.maxOf { it.id + 1 })
                nextFolderId = persistedLayout.nextFolderId.coerceAtLeast(1)
                pageIndex = visualIndexForHomePage(defaultHomePageIndex, mediaPageEnabled)
            } else {
                homePages = listOf(
                    buildHomePage(1, allApps),
                    buildHomePage(2, allApps),
                )
                defaultHomePageIndex = 0
                nextPageId = 3
                nextFolderId = 3
                pageIndex = if (mediaPageEnabled) 1 else 0
            }
            hasSeededDeviceApps = true
        } else {
            val reconciledPages = reconcileHomePagesWithApps(homePages, allApps).ifEmpty {
                listOf(
                    buildHomePage(1, allApps),
                    buildHomePage(2, allApps),
                )
            }
            homePages = reconciledPages
            hiddenAppIds = reconcileHiddenAppIds(hiddenAppIds, allApps)
            defaultHomePageIndex = defaultHomePageIndex.coerceIn(reconciledPages.indices)
            pageIndex = pageIndex.coerceIn(0, totalPageCount(reconciledPages.size, mediaPageEnabled) - 1)
        }
    }

    LaunchedEffect(pageCount) {
        pageIndex = pageIndex.coerceIn(0, pageCount - 1)
    }
    LaunchedEffect(drawerPages.size) {
        drawerPageIndex = drawerPageIndex.coerceIn(0, (drawerPages.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(drawerSortMode) {
        drawerPageIndex = 0
    }

    val updateMediaPageEnabled: (Boolean) -> Unit = { enabled ->
        if (mediaPageEnabled != enabled) {
            val adjustedPage = when {
                mediaPageEnabled && !enabled -> {
                    if (currentPageIndex == 0) defaultHomePageIndex else (currentPageIndex - 1).coerceAtLeast(0)
                }
                !mediaPageEnabled && enabled -> currentPageIndex + 1
                else -> currentPageIndex
            }
            mediaPageEnabled = enabled
            pageIndex = adjustedPage.coerceIn(0, totalPageCount(homePages.size, enabled) - 1)
        }
    }
    val rememberSearch: (String) -> Unit = { query ->
        recentSearches = rememberRecentSearch(query, recentSearches)
    }
    val closeDrawer: () -> Unit = {
        rememberSearch(searchQuery)
        activeOverlay = null
        searchQuery = ""
    }
    fun addWidgetToTargetPage(
        widget: WidgetTemplateModel,
        targetPageId: Int?,
        targetHomePageIndex: Int,
    ): WidgetTemplateModel? {
        if (targetPageId == null) return null
        var placedWidget: WidgetTemplateModel? = null
        homePages = homePages.map { page ->
            if (page.id == targetPageId) {
                val nextWidgets = addWidgetToPage(page.widgets, widget)
                placedWidget = nextWidgets.firstOrNull { it.stableWidgetKey() == widget.stableWidgetKey() }
                page.copy(widgets = nextWidgets)
            } else {
                page
            }
        }
        pageIndex = visualIndexForHomePage(targetHomePageIndex, mediaPageEnabled)
        activeOverlay = null
        return placedWidget
    }

    val addWidgetFromPicker: (WidgetTemplateModel) -> Unit = { widget ->
        val targetPageId = widgetTargetPage?.id
        val targetPageLabel = widgetTargetPage?.label ?: defaultHomeLabel
        val targetHomePageIndex = widgetTargetHomePageIndex
        val providerInfo = widget.providerInfo

        if (providerInfo == null) {
            addWidgetToTargetPage(widget, targetPageId, targetHomePageIndex)
            showFeedback(appContext.getString(R.string.feedback_widget_added, widget.title, targetPageLabel))
        } else if (providerInfo.configure != null) {
            showFeedback(appContext.getString(R.string.feedback_widget_needs_setup, widget.title))
        } else {
            val host = LauncherApp.appWidgetHost()
            val manager = LauncherApp.appWidgetManager()
            if (host == null || manager == null) {
                showFeedback(appContext.getString(R.string.feedback_widget_host_not_ready))
            } else {
                val allocatedId = runCatching { host.allocateAppWidgetId() }.getOrElse { cause ->
                    Log.w("OneUiHome/widgets", "Widget id allocation failed (${cause.javaClass.simpleName})")
                    AppWidgetManager.INVALID_APPWIDGET_ID
                }
                if (allocatedId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    showFeedback(appContext.getString(R.string.feedback_widget_allocate_failed))
                } else {
                    val options = widgetBindOptions(widget)
                    val commitBoundWidget: (Int) -> Unit = { boundId ->
                        val boundModel = widget.copy(hostWidgetId = boundId)
                        val placedWidget = addWidgetToTargetPage(boundModel, targetPageId, targetHomePageIndex)
                        placedWidget?.toBoundWidget(boundId, targetHomePageIndex)?.let { persisted ->
                            coroutineScope.launch { widgetPersistence.add(persisted) }
                        }
                        showFeedback(appContext.getString(R.string.feedback_widget_added, widget.title, targetPageLabel))
                    }
                    val alreadyAllowed = runCatching {
                        if (providerInfo.profile != null) {
                            manager.bindAppWidgetIdIfAllowed(
                                allocatedId,
                                providerInfo.profile,
                                providerInfo.provider,
                                options,
                            )
                        } else {
                            manager.bindAppWidgetIdIfAllowed(allocatedId, providerInfo.provider)
                        }
                    }.getOrDefault(false)

                    if (alreadyAllowed) {
                        commitBoundWidget(allocatedId)
                    } else {
                        val launched = LauncherApp.requestWidgetBind(
                            WidgetBindRequest(
                                allocatedWidgetId = allocatedId,
                                providerInfo = providerInfo,
                                options = options,
                            ),
                        ) { result ->
                            when (result) {
                                is WidgetBindResult.Bound -> commitBoundWidget(result.widgetId)
                                is WidgetBindResult.Declined -> {
                                    deleteWidgetId(result.requestedId)
                                    showFeedback(appContext.getString(R.string.feedback_widget_declined))
                                }
                            }
                        }
                        if (!launched) {
                            deleteWidgetId(allocatedId)
                            showFeedback(appContext.getString(R.string.feedback_widget_bind_unavailable))
                        }
                    }
                }
            }
        }
    }

    val resetWidgets: () -> Unit = {
        val removedCount = boundWidgetCount(homePages)
        LauncherApp.resetWidgetHost()
        homePages = clearBoundWidgetsFromPages(homePages)
        coroutineScope.launch { widgetPersistence.clear() }
        showFeedback(
            if (removedCount == 0) {
                appContext.getString(R.string.feedback_widget_host_reset)
            } else if (removedCount == 1) {
                appContext.getString(R.string.feedback_widget_reset_one)
            } else {
                appContext.getString(R.string.feedback_widget_reset_many, removedCount)
            },
        )
    }

    val removeBoundWidget: (Int) -> Unit = { hostWidgetId ->
        currentHomePage?.let { page ->
            homePages = homePages.map { homePage ->
                if (homePage.id == page.id) {
                    homePage.copy(widgets = removeWidgetFromPage(homePage.widgets, hostWidgetId))
                } else {
                    homePage
                }
            }
            deleteWidgetId(hostWidgetId)
            coroutineScope.launch { widgetPersistence.remove(hostWidgetId) }
            showFeedback(appContext.getString(R.string.feedback_widget_removed))
        }
    }

    val resizeBoundWidget: (Int, Int, Int) -> Unit = resize@{ hostWidgetId, deltaX, deltaY ->
        val pageIndexForPersistence = currentHomePageIndex ?: return@resize
        val page = currentHomePage ?: return@resize
        var resizedWidget: WidgetTemplateModel? = null
        homePages = homePages.map { homePage ->
            if (homePage.id == page.id) {
                val nextWidgets = resizeWidgetInPage(homePage.widgets, hostWidgetId, deltaX, deltaY)
                resizedWidget = nextWidgets.firstOrNull { it.hostWidgetId == hostWidgetId }
                homePage.copy(widgets = nextWidgets)
            } else {
                homePage
            }
        }
        resizedWidget?.toBoundWidget(hostWidgetId, pageIndexForPersistence)?.let { persisted ->
            coroutineScope.launch { widgetPersistence.add(persisted) }
        }
    }

    val moveBoundWidget: (Int, Int, Int) -> Unit = move@{ hostWidgetId, deltaX, deltaY ->
        val pageIndexForPersistence = currentHomePageIndex ?: return@move
        val page = currentHomePage ?: return@move
        var movedWidget: WidgetTemplateModel? = null
        homePages = homePages.map { homePage ->
            if (homePage.id == page.id) {
                val nextWidgets = moveWidgetInPage(homePage.widgets, hostWidgetId, deltaX, deltaY)
                movedWidget = nextWidgets.firstOrNull { it.hostWidgetId == hostWidgetId }
                homePage.copy(widgets = nextWidgets)
            } else {
                homePage
            }
        }
        movedWidget?.toBoundWidget(hostWidgetId, pageIndexForPersistence)?.let { persisted ->
            coroutineScope.launch { widgetPersistence.add(persisted) }
        }
    }

    val openAppActions: (CloneApp, AppContextSource) -> Unit = { app, source ->
        contextTarget = LauncherContextTarget.App(app = app, source = source)
    }
    val openWidgetActions: (WidgetTemplateModel) -> Unit = { widget ->
        contextTarget = LauncherContextTarget.Widget(widget)
    }

    fun addAppToCurrentHome(app: CloneApp) {
        val targetHomePageIndex = currentHomePageIndex ?: defaultHomePageIndex
        val targetPage = homePages.getOrNull(targetHomePageIndex)
        if (!canAddAppToHomePage(targetPage, homePages, app, lockHomeScreenLayout)) {
            showFeedback(appContext.getString(R.string.feedback_app_already_on_home, app.name))
            return
        }
        homePages = homePages.mapIndexed { index, page ->
            if (index == targetHomePageIndex) {
                page.copy(items = addAppToHomePageItems(page.items, app))
            } else {
                page
            }
        }
        pageIndex = visualIndexForHomePage(targetHomePageIndex, mediaPageEnabled)
        activeOverlay = null
        showFeedback(appContext.getString(R.string.feedback_app_added_to_home, app.name))
    }

    fun removeAppFromCurrentHome(app: CloneApp) {
        val targetHomePageIndex = currentHomePageIndex ?: return
        val targetPage = homePages.getOrNull(targetHomePageIndex) ?: return
        homePages = homePages.map { page ->
            if (page.id == targetPage.id) {
                page.copy(items = removeAppFromHomePageItems(page.items, app.id))
            } else {
                page
            }
        }
        showFeedback(appContext.getString(R.string.feedback_app_removed_from_home, app.name))
    }

    fun setAppHidden(app: CloneApp, hidden: Boolean) {
        hiddenAppIds = if (hidden) {
            hiddenAppIds + app.id
        } else {
            hiddenAppIds - app.id
        }
        showFeedback(
            appContext.getString(
                if (hidden) R.string.feedback_app_hidden else R.string.feedback_app_restored,
                app.name,
            ),
        )
    }

    fun removeWidgetFromHome(widget: WidgetTemplateModel) {
        val targetHomePageIndex = currentHomePageIndex ?: return
        val targetPage = homePages.getOrNull(targetHomePageIndex) ?: return
        val stableKey = widget.stableWidgetKey()
        homePages = homePages.map { page ->
            if (page.id == targetPage.id) {
                page.copy(widgets = removeWidgetFromPageByKey(page.widgets, stableKey))
            } else {
                page
            }
        }
        widget.hostWidgetId?.let { hostWidgetId ->
            deleteWidgetId(hostWidgetId)
            coroutineScope.launch { widgetPersistence.remove(hostWidgetId) }
        }
        showFeedback(appContext.getString(R.string.feedback_widget_removed))
    }

    fun openWidgetSettings(widget: WidgetTemplateModel) {
        val intent = widgetConfigureIntent(widget)
        val opened = intent != null && runCatching { appContext.startActivity(intent) }.isSuccess
        if (!opened) {
            showFeedback(appContext.getString(R.string.feedback_widget_settings_unavailable, widget.title))
        }
    }

    val contextActions = remember(
        contextTarget,
        contextShortcuts,
        hiddenAppIds,
        homePages,
        currentHomePageIndex,
        widgetTargetHomePageIndex,
        lockHomeScreenLayout,
        contextActionText,
    ) {
        when (val target = contextTarget) {
            is LauncherContextTarget.App -> {
                val targetPage = homePages.getOrNull(widgetTargetHomePageIndex)
                val canRemoveFromHome = target.source == AppContextSource.HOME && !lockHomeScreenLayout
                buildAppContextActions(
                    source = target.source,
                    isHidden = target.app.id in hiddenAppIds,
                    canOpenAppInfo = target.app.launchTarget != null || target.app.launchIntent?.component != null,
                    canAddToHome = canAddAppToHomePage(targetPage, homePages, target.app, lockHomeScreenLayout),
                    canRemoveFromHome = canRemoveFromHome,
                    shortcuts = contextShortcuts,
                    text = contextActionText,
                )
            }
            is LauncherContextTarget.Widget -> buildWidgetContextActions(
                widget = target.widget,
                canEdit = !lockHomeScreenLayout,
                text = contextActionText,
            )
            null -> emptyList()
        }
    }

    val handleContextAction: (LauncherContextAction) -> Unit = { action ->
        val target = contextTarget
        contextTarget = null
        when (target) {
            is LauncherContextTarget.App -> {
                when (action.type) {
                    LauncherContextActionType.APP_INFO -> {
                        if (!appInventory.openAppInfo(target.app)) {
                            showFeedback(appContext.getString(R.string.feedback_app_info_unavailable, target.app.name))
                        }
                    }
                    LauncherContextActionType.ADD_TO_HOME -> addAppToCurrentHome(target.app)
                    LauncherContextActionType.HIDE_APP -> setAppHidden(target.app, hidden = true)
                    LauncherContextActionType.RESTORE_APP -> setAppHidden(target.app, hidden = false)
                    LauncherContextActionType.REMOVE_FROM_HOME -> removeAppFromCurrentHome(target.app)
                    LauncherContextActionType.SHORTCUT -> {
                        val shortcut = action.shortcut
                        if (shortcut == null || !appInventory.launchShortcut(shortcut)) {
                            showFeedback(appContext.getString(R.string.feedback_shortcut_open_failed, action.title))
                        } else {
                            activeOverlay = null
                            openFolderTarget = null
                            searchQuery = ""
                        }
                    }
                    LauncherContextActionType.WIDGET_SETTINGS,
                    LauncherContextActionType.REMOVE_WIDGET -> Unit
                }
            }
            is LauncherContextTarget.Widget -> {
                when (action.type) {
                    LauncherContextActionType.WIDGET_SETTINGS -> openWidgetSettings(target.widget)
                    LauncherContextActionType.REMOVE_WIDGET -> removeWidgetFromHome(target.widget)
                    LauncherContextActionType.APP_INFO,
                    LauncherContextActionType.ADD_TO_HOME,
                    LauncherContextActionType.HIDE_APP,
                    LauncherContextActionType.RESTORE_APP,
                    LauncherContextActionType.REMOVE_FROM_HOME,
                    LauncherContextActionType.SHORTCUT -> Unit
                }
            }
            null -> Unit
        }
    }

    val motionPresetKey = remember(motionPreset) {
        when (motionPreset) {
            MotionPresetMode.STANDARD -> MotionPresetKey.STANDARD
            MotionPresetMode.REDUCED -> MotionPresetKey.REDUCED
        }
    }
    val showDefaultLauncherPrompt = remember(defaultLauncherState, defaultLauncherPromptDismissed) {
        shouldShowDefaultLauncherPrompt(defaultLauncherState, defaultLauncherPromptDismissed)
    }

    ProvideMotionScheme(presetOverride = motionPresetKey) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF8FBFF),
                        Color(0xFFEFF5FF),
                        Color(0xFFF4F8FD),
                    ),
                    start = Offset.Zero,
                    end = Offset(1200f, 1600f),
                ),
            ),
    ) {
        WallpaperAtmosphere()
        HomeSurface(
            currentHomePage = currentHomePage,
            isMediaPage = isMediaPage,
            dockApps = visibleDockApps,
            pageIndex = currentPageIndex,
            pageCount = pageCount,
            timeText = clock.timeText,
            dateText = clock.dateText,
            homeLayoutMode = homeLayoutMode,
            lockHomeScreenLayout = lockHomeScreenLayout,
            swipeDownForNotifications = swipeDownForNotifications,
            appLabelsEnabled = appLabelsEnabled,
            widgetLabelsEnabled = widgetLabelsEnabled,
            appsButtonEnabled = appsButtonEnabled,
            isHomeItemDragActive = isHomeItemDragActive,
            onOpenDrawer = { activeOverlay = OverlayPanel.DRAWER },
            onOpenNotifications = {
                if (swipeDownForNotifications) {
                    activeOverlay = OverlayPanel.NOTIFICATIONS
                }
            },
            onOpenEditMode = {
                if (!lockHomeScreenLayout) {
                    activeOverlay = OverlayPanel.EDIT_MODE
                }
            },
            onReorderHomeItem = { sourceItemId, targetItemId ->
                currentHomePage?.let { page ->
                    homePages = homePages.map { homePage ->
                        if (homePage.id == page.id) {
                            homePage.copy(items = reorderHomeGridItems(homePage.items, sourceItemId, targetItemId))
                        } else {
                            homePage
                        }
                    }
                }
            },
            onCreateFolder = { sourceItemId, targetItemId ->
                currentHomePage?.let { page ->
                    val newFolderId = "folder-dynamic-$nextFolderId"
                    nextFolderId += 1
                    homePages = homePages.map { homePage ->
                        if (homePage.id == page.id) {
                            homePage.copy(
                                items = createFolderFromHomeGridItems(
                                    items = homePage.items,
                                    sourceItemId = sourceItemId,
                                    targetItemId = targetItemId,
                                    folderId = newFolderId,
                                ),
                            )
                        } else {
                            homePage
                        }
                    }
                }
            },
            onAddAppToFolder = { sourceItemId, folderId ->
                currentHomePage?.let { page ->
                    homePages = homePages.map { homePage ->
                        if (homePage.id == page.id) {
                            homePage.copy(items = addAppToFolder(homePage.items, sourceItemId, folderId))
                        } else {
                            homePage
                        }
                    }
                }
            },
            onHomeItemDragStateChange = { isHomeItemDragActive = it },
            onOpenApp = { app -> launchSelectedApp(app) },
            onOpenAppActions = openAppActions,
            onOpenFolder = { folder ->
                currentHomePage?.let { page ->
                    openFolderTarget = OpenFolderTarget(pageId = page.id, folderId = folder.id)
                    activeOverlay = OverlayPanel.FOLDER
                }
            },
            onMoveWidget = moveBoundWidget,
            onResizeWidget = resizeBoundWidget,
            onRemoveWidget = removeBoundWidget,
            onOpenWidgetActions = openWidgetActions,
            onPageChange = { pageIndex = it },
        )

        AnimatedVisibility(
            visible = showDefaultLauncherPrompt,
            enter = slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(220)) + fadeIn(tween(160)),
            exit = slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(160)) + fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 80.dp),
        ) {
            DefaultLauncherPrompt(
                canOpenSettings = defaultLauncherState.canOpenSettings,
                onOpenSettings = onOpenDefaultLauncherSettings,
                onDismiss = { defaultLauncherPromptDismissed = true },
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == OverlayPanel.DRAWER,
            enter = slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(360, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { it / 5 },
                animationSpec = tween(240, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(160)),
        ) {
            DrawerOverlay(
                query = searchQuery,
                apps = filteredApps,
                appsScreenApps = appsScreenApps,
                drawerPages = drawerPages,
                homeLayoutMode = homeLayoutMode,
                drawerSortMode = drawerSortMode,
                drawerPageIndex = drawerPageIndex,
                hiddenAppCount = hiddenAppIds.size,
                settingResults = finderSettings,
                actionResults = finderActions,
                recentSearches = recentSearches,
                onQueryChange = { searchQuery = it },
                onClose = closeDrawer,
                onOpenSettings = {
                    settingsFocusTitle = null
                    activeOverlay = OverlayPanel.SETTINGS
                },
                onSelectSortMode = {
                    drawerSortMode = it
                },
                onSelectDrawerPage = { drawerPageIndex = it },
                onOpenHideApps = { activeOverlay = OverlayPanel.HIDE_APPS },
                onSelectRecentSearch = { searchQuery = it },
                onOpenSettingResult = { setting ->
                    rememberSearch(setting.title)
                    if (setting.type == FinderSettingType.HIDE_APPS) {
                        settingsFocusTitle = null
                        activeOverlay = OverlayPanel.HIDE_APPS
                    } else {
                        settingsFocusTitle = setting.title
                        activeOverlay = OverlayPanel.SETTINGS
                    }
                },
                onOpenAction = { action ->
                    rememberSearch(if (searchQuery.isBlank()) action.title else searchQuery)
                    when (action.type) {
                        FinderActionType.SETTINGS -> {
                            settingsFocusTitle = homeScreenSettingsTitle
                            activeOverlay = OverlayPanel.SETTINGS
                        }
                        FinderActionType.WALLPAPERS,
                        FinderActionType.PAGE_MANAGER -> {
                            if (lockHomeScreenLayout) {
                                settingsFocusTitle = lockLayoutTitle
                                activeOverlay = OverlayPanel.SETTINGS
                            } else {
                                activeOverlay = OverlayPanel.EDIT_MODE
                            }
                        }
                        FinderActionType.WIDGETS -> {
                            selectedWidgetCategory = "Recommended"
                            activeOverlay = OverlayPanel.WIDGET_PICKER
                        }
                        FinderActionType.MEDIA_PAGE -> {
                            if (!mediaPageEnabled) {
                                updateMediaPageEnabled(true)
                            }
                            pageIndex = 0
                            activeOverlay = null
                            searchQuery = ""
                        }
                        FinderActionType.HOME_PAGE -> {
                            pageIndex = visualIndexForHomePage(defaultHomePageIndex, mediaPageEnabled)
                            activeOverlay = null
                            searchQuery = ""
                        }
                        FinderActionType.HIDE_APPS -> {
                            settingsFocusTitle = null
                            activeOverlay = OverlayPanel.HIDE_APPS
                        }
                    }
                },
                onOpenApp = { app ->
                    rememberSearch(if (searchQuery.isBlank()) app.name else searchQuery)
                    activeOverlay = null
                    searchQuery = ""
                    launchSelectedApp(app)
                },
                onOpenAppActions = openAppActions,
                appLabelsEnabled = appLabelsEnabled,
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == OverlayPanel.NOTIFICATIONS,
            enter = slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(320)) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(220)) + fadeOut(tween(150)),
        ) {
            NotificationShadeOverlay(
                clock = clock,
                onClose = { activeOverlay = null },
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == OverlayPanel.SETTINGS,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(320)) + fadeIn(tween(180)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(140)),
        ) {
            SettingsOverlay(
                mediaPageEnabled = mediaPageEnabled,
                appsButtonEnabled = appsButtonEnabled,
                appLabelsEnabled = appLabelsEnabled,
                widgetLabelsEnabled = widgetLabelsEnabled,
                swipeDownForNotifications = swipeDownForNotifications,
                homeLayoutMode = homeLayoutMode,
                lockHomeScreenLayout = lockHomeScreenLayout,
                motionPreset = motionPreset,
                folderGrid = folderGrid,
                defaultHomePageLabel = homePages.getOrNull(defaultHomePageIndex)?.label ?: fallbackHomePageLabel,
                homePageCount = homePages.size,
                appsScreenSortTitle = localizedDrawerSortTitle,
                hiddenAppCount = hiddenAppIds.size,
                boundWidgetCount = activeBoundWidgetCount,
                defaultLauncherState = defaultLauncherState,
                focusedSettingTitle = settingsFocusTitle,
                onClose = {
                    settingsFocusTitle = null
                    activeOverlay = null
                },
                onMediaPageChange = updateMediaPageEnabled,
                onAppsButtonChange = { appsButtonEnabled = it },
                onAppLabelsChange = { appLabelsEnabled = it },
                onWidgetLabelsChange = { widgetLabelsEnabled = it },
                onSwipeDownChange = { swipeDownForNotifications = it },
                onHomeLayoutModeChange = { homeLayoutMode = it },
                onLockHomeScreenLayoutChange = { lockHomeScreenLayout = it },
                onMotionPresetChange = { motionPreset = it },
                onFolderGridChange = { folderGrid = it },
                onResetWidgets = resetWidgets,
                onOpenDefaultLauncherSettings = onOpenDefaultLauncherSettings,
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == OverlayPanel.EDIT_MODE,
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(260)) + fadeIn(tween(180)),
            exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(180)) + fadeOut(tween(120)),
        ) {
            EditModeTray(
                pages = homePages,
                pageIndex = currentPageIndex,
                mediaPageEnabled = mediaPageEnabled,
                defaultHomePageIndex = defaultHomePageIndex,
                onSelectPage = { pageIndex = it },
                onToggleMediaPage = { updateMediaPageEnabled(!mediaPageEnabled) },
                onAddPage = {
                    val newPage = buildHomePage(nextPageId, allApps)
                    homePages = homePages + newPage
                    pageIndex = visualIndexForHomePage(homePages.size, mediaPageEnabled)
                    nextPageId += 1
                },
                onMoveCurrentPageLeft = {
                    currentHomePageIndex?.let { selectedIndex ->
                        if (selectedIndex > 0) {
                            homePages = moveListItem(homePages, selectedIndex, selectedIndex - 1)
                            defaultHomePageIndex = movedIndexForSwap(defaultHomePageIndex, selectedIndex, selectedIndex - 1)
                            pageIndex = visualIndexForHomePage(selectedIndex - 1, mediaPageEnabled)
                        }
                    }
                },
                onMoveCurrentPageRight = {
                    currentHomePageIndex?.let { selectedIndex ->
                        if (selectedIndex < homePages.lastIndex) {
                            homePages = moveListItem(homePages, selectedIndex, selectedIndex + 1)
                            defaultHomePageIndex = movedIndexForSwap(defaultHomePageIndex, selectedIndex, selectedIndex + 1)
                            pageIndex = visualIndexForHomePage(selectedIndex + 1, mediaPageEnabled)
                        }
                    }
                },
                onOpenWidgetPicker = {
                    selectedWidgetCategory = "Recommended"
                    activeOverlay = OverlayPanel.WIDGET_PICKER
                },
                currentWidgetCount = currentHomePage?.widgets?.size ?: 0,
                onRemoveLastWidget = {
                    currentHomePage?.let { page ->
                        if (page.widgets.isNotEmpty()) {
                            homePages = homePages.map { homePage ->
                                if (homePage.id == page.id) {
                                    homePage.copy(widgets = homePage.widgets.dropLast(1))
                                } else {
                                    homePage
                                }
                            }
                        }
                    }
                },
                onRemoveCurrentPage = {
                    currentHomePageIndex?.let { selectedHomePage ->
                        if (homePages.size > 1) {
                            val updatedPages = homePages.filterIndexed { index, _ -> index != selectedHomePage }
                            val updatedDefaultHomePage = when {
                                defaultHomePageIndex > selectedHomePage -> defaultHomePageIndex - 1
                                defaultHomePageIndex == selectedHomePage -> (selectedHomePage - 1).coerceAtLeast(0)
                                else -> defaultHomePageIndex
                            }.coerceIn(0, updatedPages.lastIndex)
                            homePages = updatedPages
                            defaultHomePageIndex = updatedDefaultHomePage
                            val nextSelectedHomePage = selectedHomePage.coerceAtMost(updatedPages.lastIndex)
                            pageIndex = visualIndexForHomePage(nextSelectedHomePage, mediaPageEnabled)
                        }
                    }
                },
                onSetCurrentPageAsDefault = {
                    currentHomePageIndex?.let { defaultHomePageIndex = it }
                },
                onClose = { activeOverlay = null },
                onOpenSettings = {
                    settingsFocusTitle = null
                    activeOverlay = OverlayPanel.SETTINGS
                },
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == OverlayPanel.HIDE_APPS,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(180)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(140)),
        ) {
            HideAppsOverlay(
                apps = allApps,
                hiddenAppIds = hiddenAppIds,
                onToggleHidden = { app ->
                    setAppHidden(app, hidden = app.id !in hiddenAppIds)
                },
                onOpenAppActions = openAppActions,
                onClose = { activeOverlay = null },
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == OverlayPanel.FOLDER && openFolder != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
        ) {
            openFolder?.let { folder ->
                FolderOverlay(
                    folder = folder,
                    appLabelsEnabled = appLabelsEnabled,
                    folderGrid = folderGrid,
                    onOpenApp = { app -> launchSelectedApp(app) },
                    onOpenAppActions = openAppActions,
                    onRenameFolder = { newTitle ->
                        homePages = homePages.map { page ->
                            if (page.id == openFolderTarget?.pageId) {
                                page.copy(
                                    items = page.items.map { item ->
                                        if (item is FolderModel && item.id == openFolderTarget?.folderId) {
                                            item.copy(title = newTitle.trim().ifBlank { item.title })
                                        } else {
                                            item
                                        }
                                    },
                                )
                            } else {
                                page
                            }
                        }
                    },
                    onClose = {
                        activeOverlay = null
                        openFolderTarget = null
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = activeOverlay == OverlayPanel.WIDGET_PICKER,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(180)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(140)),
        ) {
            WidgetPickerOverlay(
                categories = widgetCategories,
                selectedCategory = selectedWidgetCategory,
                widgets = filteredWidgetTemplates,
                targetPageLabel = widgetTargetPage?.label ?: fallbackHomePageLabel,
                onSelectCategory = { selectedWidgetCategory = it },
                onAddWidget = addWidgetFromPicker,
                onClose = { activeOverlay = null },
            )
        }

        AnimatedVisibility(
            visible = contextTarget != null,
            enter = slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(220)) + fadeIn(tween(140)),
            exit = slideOutVertically(targetOffsetY = { it / 5 }, animationSpec = tween(160)) + fadeOut(tween(120)),
        ) {
            contextTarget?.let { target ->
                when (target) {
                    is LauncherContextTarget.App -> ContextActionSheet(
                        title = target.app.name,
                        summary = target.app.statusText() ?: stringResource(R.string.context_action_app_sheet_summary),
                        app = target.app,
                        widget = null,
                        actions = contextActions,
                        onAction = handleContextAction,
                        onDismiss = { contextTarget = null },
                    )
                    is LauncherContextTarget.Widget -> ContextActionSheet(
                        title = target.widget.title,
                        summary = target.widget.summary,
                        app = null,
                        widget = target.widget,
                        actions = contextActions,
                        onAction = handleContextAction,
                        onDismiss = { contextTarget = null },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(180)) + fadeIn(tween(120)),
            exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(140)) + fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 112.dp),
        ) {
            feedbackMessage?.let { message ->
                SystemFeedbackBanner(message = message)
            }
        }
    }
    }
}


