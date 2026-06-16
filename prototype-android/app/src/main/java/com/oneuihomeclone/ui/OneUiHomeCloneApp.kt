package com.oneuihomeclone.ui

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.core.graphics.drawable.toBitmap
import com.oneuihomeclone.LauncherApp
import com.oneuihomeclone.data.BoundWidget
import com.oneuihomeclone.data.DrawerSortKey
import com.oneuihomeclone.data.FolderGridKey
import com.oneuihomeclone.data.HomeLayoutKey
import com.oneuihomeclone.data.LauncherPreferences
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.oneuihomeclone.widgets.PreviewSource
import com.oneuihomeclone.widgets.WidgetBindRequest
import com.oneuihomeclone.widgets.WidgetBindResult
import com.oneuihomeclone.widgets.WidgetPreviewLoader

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
        CloneApp(id = "sample-samsung-free", name = "Samsung Free", color = Color(0xFF7A6BFF)),
    )
}

private suspend fun loadLauncherApps(
    packageManager: PackageManager,
    hostPackageName: String,
    fallbackApps: List<CloneApp>,
): List<CloneApp> = withContext(Dispatchers.IO) {
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val iconSizePx = 144
    val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        .filter { resolveInfo -> resolveInfo.activityInfo?.packageName != hostPackageName }
        .distinctBy { resolveInfo ->
            "${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}"
        }
        .sortedBy { resolveInfo ->
            resolveInfo.loadLabel(packageManager)?.toString()?.lowercase(Locale.getDefault()).orEmpty()
        }

    val apps = resolveInfos.mapIndexed { index, resolveInfo ->
        val activityInfo = resolveInfo.activityInfo
        val componentId = "${activityInfo.packageName}/${activityInfo.name}"
        val label = resolveInfo.loadLabel(packageManager)?.toString().orEmpty().ifBlank {
            activityInfo.packageName.substringAfterLast('.').replaceFirstChar(Char::titlecase)
        }
        // Cap eager icon decoding at MAX_ICONS_LOADED_EAGERLY: an ARGB_8888 144x144
        // bitmap is ~82 KB, so 300 icons ≈ 24 MB. Devices with 400+ installed apps
        // (or a hostile app registering many LAUNCHER-category aliases) could otherwise
        // OOM the Compose snapshot. Entries beyond the cap render with their color
        // swatch and first letter — lazy icon load is scheduled for v0.2.x.
        val iconBitmap = if (index < MAX_ICONS_LOADED_EAGERLY) {
            runCatching {
                resolveInfo
                    .loadIcon(packageManager)
                    .toBitmap(width = iconSizePx, height = iconSizePx, config = Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }.getOrNull()
        } else {
            null
        }

        CloneApp(
            id = componentId,
            name = label,
            launchIntent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(activityInfo.packageName, activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            icon = iconBitmap,
            color = fallbackColorFor(componentId),
        )
    }

    apps.ifEmpty { fallbackApps }
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

    val widgets = providers
        .asSequence()
        .filter { info -> info.provider != null }
        .distinctBy { info -> info.provider.flattenToShortString() }
        .sortedWith(
            compareBy<AppWidgetProviderInfo> { info ->
                widgetProviderAppLabel(packageManager, info).lowercase(Locale.getDefault())
            }.thenBy { info ->
                widgetProviderLabel(packageManager, info).lowercase(Locale.getDefault())
            },
        )
        .take(MAX_WIDGET_PROVIDERS_LOADED)
        .map { info ->
            val appLabel = widgetProviderAppLabel(packageManager, info)
            val label = widgetProviderLabel(packageManager, info)
            val spanX = widgetSpanX(info)
            val spanY = widgetSpanY(info)
            WidgetTemplateModel(
                title = label,
                summary = "Provided by $appLabel",
                category = appLabel,
                span = "$spanX x $spanY",
                accent = fallbackColorFor(info.provider.flattenToShortString()),
                providerInfo = info,
                previewSource = WidgetPreviewLoader.load(context, info),
                spanX = spanX,
                spanY = spanY,
            )
        }
        .toList()

    widgets.ifEmpty { fallbackWidgets }
}

@Composable
fun OneUiHomeCloneApp(homeIntentTick: Int = 0) {
    val appContext = LocalContext.current.applicationContext
    val preferences = remember(appContext) { LauncherPreferences(appContext) }
    val widgetPersistence = remember(appContext) { WidgetPersistence(appContext) }
    val coroutineScope = rememberCoroutineScope()
    val initialPrefs = remember(preferences) { preferences.snapshot() }
    val fallbackApps = remember { sampleApps() }
    var allApps by remember { mutableStateOf(fallbackApps) }
    var hasSeededDeviceApps by remember { mutableStateOf(false) }
    val dockApps = remember(allApps) { allApps.take(4) }
    val fallbackWidgetTemplates = remember {
        listOf(
            WidgetTemplateModel("Calendar", "Month agenda with Samsung-style rounded chrome", "Recommended", "4 x 2", Color(0xFFFF8B7B)),
            WidgetTemplateModel("Weather", "Large conditions card with soft edge highlights", "Recommended", "4 x 2", Color(0xFF62B8FF)),
            WidgetTemplateModel("SmartThings", "Scenes and devices in a compact control stack", "Connected", "4 x 2", Color(0xFF2EBCF6)),
            WidgetTemplateModel("Battery", "Device and buds battery status", "Device", "4 x 1", Color(0xFF5ECB85)),
            WidgetTemplateModel("Music", "Now playing with album art emphasis", "Entertainment", "4 x 2", Color(0xFFFF6F96)),
            WidgetTemplateModel("Reminder list", "Pinned tasks for routines and grocery runs", "Productivity", "4 x 2", Color(0xFFFFC857)),
        )
    }
    var widgetTemplates by remember { mutableStateOf(fallbackWidgetTemplates) }
    val launchSelectedApp = remember(appContext) {
        { app: CloneApp ->
            val launchIntent = app.launchIntent
            if (launchIntent == null) {
                Toast.makeText(appContext, "${app.name} is a prototype surface for now.", Toast.LENGTH_SHORT).show()
            } else {
                runCatching { appContext.startActivity(Intent(launchIntent)) }
                    .onFailure {
                        Toast.makeText(appContext, "Couldn't open ${app.name}.", Toast.LENGTH_SHORT).show()
                    }
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
    var recentSearches by remember {
        mutableStateOf(listOf("Media page", "Folder grid", "Widgets", "Home screen grid"))
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

    LaunchedEffect(Unit) {
        allApps = loadLauncherApps(
            packageManager = appContext.packageManager,
            hostPackageName = appContext.packageName,
            fallbackApps = fallbackApps,
        )
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

    // Persist user-facing toggles via snapshotFlow so the first emission (on composition
    // entry) can be discarded — there's no reason to rewrite SharedPreferences with the
    // values we just read from it. Further emissions fire only on genuine state changes.
    LaunchedEffect(preferences) {
        snapshotFlow {
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
            .drop(1)
            .collect { toggles ->
                preferences.update { editor ->
                    editor
                        .setMediaPageEnabled(toggles.mediaPageEnabled)
                        .setAppsButtonEnabled(toggles.appsButtonEnabled)
                        .setAppLabelsEnabled(toggles.appLabelsEnabled)
                        .setWidgetLabelsEnabled(toggles.widgetLabelsEnabled)
                        .setSwipeDownForNotifications(toggles.swipeDownForNotifications)
                        .setLockHomeScreenLayout(toggles.lockHomeScreenLayout)
                        .setHomeLayoutMode(
                            when (toggles.homeLayoutMode) {
                                HomeLayoutMode.HOME_AND_APPS_SCREENS -> HomeLayoutKey.HOME_AND_APPS_SCREENS
                                HomeLayoutMode.HOME_SCREEN_ONLY -> HomeLayoutKey.HOME_SCREEN_ONLY
                            },
                        )
                        .setDrawerSortMode(
                            when (toggles.drawerSortMode) {
                                DrawerSortMode.CUSTOM_ORDER -> DrawerSortKey.CUSTOM_ORDER
                                DrawerSortMode.ALPHABETICAL -> DrawerSortKey.ALPHABETICAL
                            },
                        )
                        .setMotionPreset(
                            when (toggles.motionPreset) {
                                MotionPresetMode.STANDARD -> MotionPresetKey.STANDARD
                                MotionPresetMode.REDUCED -> MotionPresetKey.REDUCED
                            },
                        )
                        .setFolderGrid(
                            when (toggles.folderGrid) {
                                FolderGridMode.GRID_3X4 -> FolderGridKey.GRID_3X4
                                FolderGridMode.GRID_4X4 -> FolderGridKey.GRID_4X4
                                FolderGridMode.GRID_5X5 -> FolderGridKey.GRID_5X5
                            },
                        )
                }
            }
    }

    // HOME intent re-entry (user pressed HOME while inside the launcher, or picked us
    // from the home-app picker again). Collapse every overlay + scroll to default page.
    LaunchedEffect(homeIntentTick) {
        if (homeIntentTick > 0) {
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
    val finderSettings = remember(
        searchQuery,
        homeLayoutMode,
        lockHomeScreenLayout,
        mediaPageEnabled,
        appsButtonEnabled,
        appLabelsEnabled,
        widgetLabelsEnabled,
        swipeDownForNotifications,
        homePages,
        defaultHomePageIndex,
        hiddenAppIds,
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
            defaultHomePageLabel = homePages.getOrNull(defaultHomePageIndex)?.label ?: "Home 1",
            hiddenAppCount = hiddenAppIds.size,
        )
    }
    val finderActions = remember(searchQuery, homeLayoutMode, lockHomeScreenLayout, mediaPageEnabled, hiddenAppIds) {
        buildFinderActionResults(
            query = searchQuery,
            homeLayoutMode = homeLayoutMode,
            lockHomeScreenLayout = lockHomeScreenLayout,
            mediaPageEnabled = mediaPageEnabled,
            hasHiddenApps = hiddenAppIds.isNotEmpty(),
        )
    }

    LaunchedEffect(allApps) {
        if (!hasSeededDeviceApps && allApps.any { it.launchIntent != null }) {
            homePages = listOf(
                buildHomePage(1, allApps),
                buildHomePage(2, allApps),
            )
            defaultHomePageIndex = 0
            nextPageId = 3
            nextFolderId = 3
            pageIndex = if (mediaPageEnabled) 1 else 0
            hasSeededDeviceApps = true
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
    ) {
        if (targetPageId == null) return
        homePages = homePages.map { page ->
            if (page.id == targetPageId) {
                page.copy(widgets = addWidgetToPage(page.widgets, widget))
            } else {
                page
            }
        }
        pageIndex = visualIndexForHomePage(targetHomePageIndex, mediaPageEnabled)
        activeOverlay = null
    }

    val addWidgetFromPicker: (WidgetTemplateModel) -> Unit = { widget ->
        val targetPageId = widgetTargetPage?.id
        val targetPageLabel = widgetTargetPage?.label ?: "Home"
        val targetHomePageIndex = widgetTargetHomePageIndex
        val providerInfo = widget.providerInfo

        if (providerInfo == null) {
            addWidgetToTargetPage(widget, targetPageId, targetHomePageIndex)
        } else {
            val host = LauncherApp.appWidgetHost()
            val manager = LauncherApp.appWidgetManager()
            if (host == null || manager == null) {
                Toast.makeText(appContext, "Widget host is not ready yet.", Toast.LENGTH_SHORT).show()
            } else {
                val allocatedId = runCatching { host.allocateAppWidgetId() }.getOrElse { cause ->
                    Log.w("OneUiHome/widgets", "Widget id allocation failed (${cause.javaClass.simpleName})")
                    AppWidgetManager.INVALID_APPWIDGET_ID
                }
                if (allocatedId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Toast.makeText(appContext, "Couldn't allocate a widget slot.", Toast.LENGTH_SHORT).show()
                } else {
                    val options = widgetBindOptions(widget)
                    val commitBoundWidget: (Int) -> Unit = { boundId ->
                        val boundModel = widget.copy(hostWidgetId = boundId)
                        addWidgetToTargetPage(boundModel, targetPageId, targetHomePageIndex)
                        boundModel.toBoundWidget(boundId, targetHomePageIndex)?.let { persisted ->
                            coroutineScope.launch { widgetPersistence.add(persisted) }
                        }
                        Toast.makeText(appContext, "${widget.title} added to $targetPageLabel.", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(appContext, "Widget was not added.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        if (!launched) {
                            deleteWidgetId(allocatedId)
                            Toast.makeText(appContext, "Widget picker is not ready yet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val motionPresetKey = remember(motionPreset) {
        when (motionPreset) {
            MotionPresetMode.STANDARD -> MotionPresetKey.STANDARD
            MotionPresetMode.REDUCED -> MotionPresetKey.REDUCED
        }
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
            onOpenFolder = { folder ->
                currentHomePage?.let { page ->
                    openFolderTarget = OpenFolderTarget(pageId = page.id, folderId = folder.id)
                    activeOverlay = OverlayPanel.FOLDER
                }
            },
            onPageChange = { pageIndex = it },
        )

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
                    if (setting.title == "Hide apps") {
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
                            settingsFocusTitle = "Home screen settings"
                            activeOverlay = OverlayPanel.SETTINGS
                        }
                        FinderActionType.WALLPAPERS,
                        FinderActionType.PAGE_MANAGER -> {
                            if (lockHomeScreenLayout) {
                                settingsFocusTitle = "Lock Home screen layout"
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
                defaultHomePageLabel = homePages.getOrNull(defaultHomePageIndex)?.label ?: "Home 1",
                homePageCount = homePages.size,
                appsScreenSortTitle = drawerSortMode.title,
                hiddenAppCount = hiddenAppIds.size,
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
                    hiddenAppIds = hiddenAppIds.toggle(app.id)
                },
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
                targetPageLabel = widgetTargetPage?.label ?: "Home 1",
                onSelectCategory = { selectedWidgetCategory = it },
                onAddWidget = addWidgetFromPicker,
                onClose = { activeOverlay = null },
            )
        }
    }
    }
}


private fun replaceBoundWidgetView(
    container: FrameLayout,
    widgetId: Int,
    providerInfo: AppWidgetProviderInfo,
) {
    container.removeAllViews()
    val resolvedInfo = LauncherApp.appWidgetManager()?.getAppWidgetInfo(widgetId) ?: providerInfo
    val hostView = runCatching {
        LauncherApp.appWidgetHost()?.createView(container.context, widgetId, resolvedInfo)
    }.getOrElse { cause ->
        Log.w("OneUiHome/widgets", "Bound widget view failed (${cause.javaClass.simpleName})")
        null
    } ?: return
    container.addView(hostView, matchParentLayoutParams())
}

private fun replaceRemotePreview(
    container: FrameLayout,
    preview: PreviewSource.RemoteLayout,
) {
    container.removeAllViews()
    val previewView = runCatching {
        RemoteViews(preview.providerPackage, preview.layoutResId).apply(container.context, container)
    }.getOrElse { cause ->
        Log.w("OneUiHome/widgets", "Remote widget preview failed (${cause.javaClass.simpleName})")
        null
    } ?: return
    container.addView(previewView, matchParentLayoutParams())
}

private fun matchParentLayoutParams(): FrameLayout.LayoutParams =
    FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

@Composable
private fun rememberStatusClock(): StatusClock {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now()
            now = current
            val millisUntilNextMinute = (((60 - current.second) * 1000L) - (current.nano / 1_000_000L)).coerceAtLeast(250L)
            delay(millisUntilNextMinute)
        }
    }

    val locale = Locale.getDefault()
    return remember(now, locale) {
        StatusClock(
            timeText = now.format(DateTimeFormatter.ofPattern("h:mm", locale)),
            dateText = now.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale)),
            fullDateText = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)),
        )
    }
}

@Composable
private fun WallpaperAtmosphere() {
    val context = LocalContext.current
    // Decode the wallpaper off the main thread. First-frame render uses the gradient
    // glyphs alone; the real wallpaper fades in once the IO work completes. This avoids
    // a 200+ ms main-thread hitch on devices with large (≥3MP) wallpapers.
    val wallpaperBitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { readSystemWallpaper(context) }
    }

    val transition = rememberInfiniteTransition(label = "wallpaper")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(Modifier.fillMaxSize()) {
        // When the user has set us as HOME launcher we can read the system wallpaper
        // directly — render it as the full-bleed backdrop, then layer the One UI soft
        // gradient glyphs on top. When no wallpaper is accessible we rely on the
        // themes.xml transparent background + our glyphs alone, which matches the
        // previous prototype feel.
        wallpaperBitmap?.let { wallpaper ->
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 18.dp)
                .size(220.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(Color(0x33B5D0FF)),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Color(0x2AF7C6A3)),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 150.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0x22A78BFA)),
        )
    }
}

/**
 * Best-effort system wallpaper read. The active HOME launcher gets implicit access at
 * runtime without declaring READ_WALLPAPER_INTERNAL (signature-only) or
 * MANAGE_EXTERNAL_STORAGE (which we deliberately do not request for privacy reasons).
 * Lint flags this statically; runCatching handles the SecurityException on OEM skins
 * that decline the access and fall back to our gradient glyphs.
 */
@SuppressLint("MissingPermission")
private fun readSystemWallpaper(context: Context): ImageBitmap? = runCatching {
    val manager = WallpaperManager.getInstance(context) ?: return@runCatching null
    val drawable = manager.peekDrawable() ?: manager.peekFastDrawable() ?: return@runCatching null
    val source = (drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
    // Copy so we own the memory — WallpaperManager may recycle the backing bitmap when
    // the user changes wallpaper (live wallpapers in particular). An ARGB_8888 copy keeps
    // colour fidelity; caller drops the reference when the composition leaves scope.
    val safeCopy = source.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
    safeCopy.asImageBitmap()
}.onFailure { failure ->
    // Log class name only — no message, no stack (avoid leaking OEM wallpaper path data
    // to logcat on restricted OEM skins). Useful signal in bug reports.
    android.util.Log.w("OneUiHome/wallpaper", failure.javaClass.simpleName)
}.getOrNull()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeSurface(
    currentHomePage: HomePageModel?,
    isMediaPage: Boolean,
    dockApps: List<CloneApp>,
    pageIndex: Int,
    pageCount: Int,
    timeText: String,
    dateText: String,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    swipeDownForNotifications: Boolean,
    appLabelsEnabled: Boolean,
    widgetLabelsEnabled: Boolean,
    appsButtonEnabled: Boolean,
    isHomeItemDragActive: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenEditMode: () -> Unit,
    onReorderHomeItem: (String, String) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onAddAppToFolder: (String, String) -> Unit,
    onHomeItemDragStateChange: (Boolean) -> Unit,
    onOpenApp: (CloneApp) -> Unit,
    onOpenFolder: (FolderModel) -> Unit,
    onPageChange: (Int) -> Unit,
) {
    val drawerGestureEnabled = homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .pointerInput(onOpenEditMode, isHomeItemDragActive) {
                if (isHomeItemDragActive) {
                    return@pointerInput
                }
                detectTapGestures(onLongPress = { onOpenEditMode() })
            }
            .pointerInput(swipeDownForNotifications, drawerGestureEnabled, onOpenNotifications, onOpenDrawer, isHomeItemDragActive) {
                if (isHomeItemDragActive || (!swipeDownForNotifications && !drawerGestureEnabled)) {
                    return@pointerInput
                }
                var totalDrag = 0f
                var handled = false
                detectVerticalDragGestures(
                    onDragEnd = {
                        totalDrag = 0f
                        handled = false
                    },
                    onDragCancel = {
                        totalDrag = 0f
                        handled = false
                    },
                ) { _, dragAmount ->
                    if (handled) {
                        return@detectVerticalDragGestures
                    }
                    totalDrag += dragAmount
                    if (swipeDownForNotifications && totalDrag >= 160f) {
                        handled = true
                        onOpenNotifications()
                    } else if (drawerGestureEnabled && totalDrag <= -160f) {
                        handled = true
                        onOpenDrawer()
                    }
                }
            },
    ) {
        StatusRow(
            timeText = timeText,
            dateText = dateText,
            homeLayoutMode = homeLayoutMode,
            lockHomeScreenLayout = lockHomeScreenLayout,
        )
        Spacer(Modifier.height(18.dp))
        if (isMediaPage) {
            MediaPageHero()
            Spacer(Modifier.height(16.dp))
            MediaPageCards()
            Spacer(Modifier.weight(1f))
        } else {
            currentHomePage?.let { WidgetHeroCard(it) }
            currentHomePage?.takeIf { it.widgets.isNotEmpty() }?.let { page ->
                Spacer(Modifier.height(14.dp))
                WidgetPreviewStrip(
                    widgets = page.widgets,
                    showLabels = widgetLabelsEnabled,
                )
            }
            Spacer(Modifier.height(22.dp))
            HomeGrid(
                items = currentHomePage?.items.orEmpty(),
                showLabels = appLabelsEnabled,
                compactLayout = currentHomePage?.widgets?.isNotEmpty() == true,
                canOrganize = !lockHomeScreenLayout,
                onReorderItem = onReorderHomeItem,
                onCreateFolder = onCreateFolder,
                onAddAppToFolder = onAddAppToFolder,
                onDragStateChange = onHomeItemDragStateChange,
                onOpenApp = onOpenApp,
                onOpenFolder = onOpenFolder,
            )
            Spacer(Modifier.weight(1f))
        }
        PageStrip(
            pageIndex = pageIndex,
            pageCount = pageCount,
            onPageChange = onPageChange,
        )
        Spacer(Modifier.height(18.dp))
        SearchPill(
            label = if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) "Search apps" else "Finder",
            onOpenDrawer = onOpenDrawer,
        )
        Spacer(Modifier.height(14.dp))
        DockBar(
            apps = dockApps,
            showLabels = appLabelsEnabled,
            appsButtonEnabled = homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS && appsButtonEnabled,
            onOpenApp = onOpenApp,
            onOpenDrawer = onOpenDrawer,
        )
    }
}

@Composable
private fun StatusRow(
    timeText: String,
    dateText: String,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(timeText, color = OneUiText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(dateText, color = OneUiTextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Surface(color = OneUiCard, shape = RoundedCornerShape(18.dp), shadowElevation = 2.dp) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(OneUiPositive))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (lockHomeScreenLayout) "Layout locked" else homeLayoutMode.title,
                    color = OneUiText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun MediaPageHero() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = OneUiCard,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text("Media page", color = OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text("Samsung Free", color = OneUiText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "News, watch, listen, and play surfaces belong here instead of crowding the default home page.",
                color = OneUiText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrawerPill("Daily briefing")
                DrawerPill("Podcasts")
                DrawerPill("Videos")
            }
        }
    }
}

@Composable
private fun MediaPageCards() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MediaMiniCard(
                title = "For you",
                body = "Morning briefing, weather, and commute timing",
                modifier = Modifier.weight(1f),
            )
            MediaMiniCard(
                title = "Listen",
                body = "Resume playlists and podcasts from where you left off",
                modifier = Modifier.weight(1f),
            )
        }
        MediaMiniCard(
            title = "Play next",
            body = "A Samsung-like media page needs stacked cards, soft depth, and clear swipe destinations.",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MediaMiniCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = OneUiSurface,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(body, color = OneUiTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun WidgetHeroCard(page: HomePageModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = OneUiCard,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(page.eyebrow, color = OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(page.value, color = OneUiText, fontSize = 50.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(OneUiAccent))
                Spacer(Modifier.width(8.dp))
                Text(page.status, color = OneUiText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                page.note,
                color = OneUiTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun WidgetPreviewStrip(
    widgets: List<WidgetTemplateModel>,
    showLabels: Boolean,
) {
    val shownWidgets = widgets.take(2)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        shownWidgets.forEach { widget ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = OneUiCard,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(widget.accent.copy(alpha = 0.14f), Color.White),
                                start = Offset.Zero,
                                end = Offset(900f, 260f),
                            ),
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        if (showLabels) {
                            Text(
                                text = widget.category,
                                color = widget.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(
                            text = widget.title,
                            color = OneUiText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = widget.summary,
                            color = OneUiTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    WidgetPreviewPane(
                        widget = widget,
                        modifier = Modifier.size(
                            width = if (widget.spanY == 1) 74.dp else 80.dp,
                            height = if (widget.spanY == 1) 46.dp else 72.dp,
                        ),
                        compact = true,
                    )
                }
            }
        }
        repeat(2 - shownWidgets.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun WidgetPreviewPane(
    widget: WidgetTemplateModel,
    modifier: Modifier,
    compact: Boolean,
) {
    val hostWidgetId = widget.hostWidgetId
    val providerInfo = widget.providerInfo
    when {
        hostWidgetId != null && providerInfo != null -> BoundWidgetPreview(
            widgetId = hostWidgetId,
            providerInfo = providerInfo,
            modifier = modifier,
        )
        widget.previewSource is PreviewSource.RemoteLayout -> RemoteLayoutPreview(
            preview = widget.previewSource,
            modifier = modifier,
        )
        widget.previewSource is PreviewSource.PreviewImage -> DrawableWidgetPreview(
            preview = widget.previewSource,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
        widget.previewSource is PreviewSource.ProviderIcon -> DrawableWidgetPreview(
            preview = widget.previewSource,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        else -> SyntheticWidgetPreview(
            widget = widget,
            modifier = modifier,
            compact = compact,
        )
    }
}

@Composable
private fun BoundWidgetPreview(
    widgetId: Int,
    providerInfo: AppWidgetProviderInfo,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.82f)),
        factory = { context ->
            FrameLayout(context).apply {
                replaceBoundWidgetView(this, widgetId, providerInfo)
            }
        },
        update = { container ->
            val tag = "bound:$widgetId:${providerInfo.provider.flattenToShortString()}"
            if (container.tag != tag) {
                container.tag = tag
                replaceBoundWidgetView(container, widgetId, providerInfo)
            }
        },
    )
}

@Composable
private fun RemoteLayoutPreview(
    preview: PreviewSource.RemoteLayout,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.82f)),
        factory = { context ->
            FrameLayout(context).apply {
                replaceRemotePreview(this, preview)
            }
        },
        update = { container ->
            val tag = "remote:${preview.providerPackage}:${preview.layoutResId}"
            if (container.tag != tag) {
                container.tag = tag
                replaceRemotePreview(container, preview)
            }
        },
    )
}

@Composable
private fun DrawableWidgetPreview(
    preview: PreviewSource,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val density = LocalDensity.current
    val drawable = when (preview) {
        is PreviewSource.PreviewImage -> preview.drawable
        is PreviewSource.ProviderIcon -> preview.drawable
        else -> null
    }
    val bitmap = remember(drawable, density) {
        drawable?.let {
            runCatching {
                it.toBitmap(width = 320, height = 180, config = Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.82f)),
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.82f)),
        )
    }
}

@Composable
internal fun SyntheticWidgetPreview(
    widget: WidgetTemplateModel,
    modifier: Modifier,
    compact: Boolean,
) {
    val barCount = if (compact) {
        if (widget.spanY == 1) 2 else 3
    } else {
        if (widget.spanY == 1) 3 else 4
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(widget.accent.copy(alpha = 0.1f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        repeat(barCount) { thumb ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (thumb == 0) 1f else 0.72f)
                    .height(if (compact) 8.dp else 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(widget.accent.copy(alpha = 0.14f + (thumb * 0.04f))),
            )
        }
    }
}

@Composable
private fun HomeGrid(
    items: List<HomeGridItemModel>,
    showLabels: Boolean,
    compactLayout: Boolean,
    canOrganize: Boolean,
    onReorderItem: (String, String) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onAddAppToFolder: (String, String) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onOpenApp: (CloneApp) -> Unit,
    onOpenFolder: (FolderModel) -> Unit,
) {
    val iconScale by animateFloatAsState(targetValue = 1f, label = "grid")
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var hoverTargetItemId by remember { mutableStateOf<String?>(null) }
    var combineHoverTargetItemId by remember { mutableStateOf<String?>(null) }
    var combineReadyTargetItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hasDragged by remember { mutableStateOf(false) }

    fun resetDragState() {
        draggingItemId = null
        hoverTargetItemId = null
        combineHoverTargetItemId = null
        combineReadyTargetItemId = null
        dragOffset = Offset.Zero
        hasDragged = false
        onDragStateChange(false)
    }

    LaunchedEffect(draggingItemId, combineHoverTargetItemId) {
        combineReadyTargetItemId = if (draggingItemId != null) combineHoverTargetItemId else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compactLayout) 304.dp else 356.dp),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(items, key = { item -> item.id }) { item ->
                val isDragging = draggingItemId == item.id
                val isHoverTarget = hoverTargetItemId == item.id
                val isCombineReady = combineReadyTargetItemId == item.id
                val cardScale by animateFloatAsState(
                    targetValue = when {
                        isDragging -> 1.06f
                        isCombineReady -> 1.04f
                        else -> iconScale
                    },
                    label = "grid-item-scale",
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            when {
                                isCombineReady -> OneUiAccentSoft.copy(alpha = 0.9f)
                                isHoverTarget -> OneUiAccentSoft.copy(alpha = 0.5f)
                                else -> Color.Transparent
                            },
                        )
                        .padding(vertical = 4.dp)
                        .graphicsLayer {
                            scaleX = cardScale
                            scaleY = cardScale
                            alpha = if (isDragging) 0.58f else 1f
                        }
                        .onGloballyPositioned { coordinates ->
                            itemBounds[item.id] = coordinates.boundsInRoot()
                        }
                        .pointerInput(item.id, items, canOrganize) {
                            if (!canOrganize) {
                                return@pointerInput
                            }

                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingItemId = item.id
                                    hoverTargetItemId = null
                                    combineHoverTargetItemId = null
                                    combineReadyTargetItemId = null
                                    dragOffset = Offset.Zero
                                    hasDragged = false
                                    onDragStateChange(true)
                                },
                                onDragCancel = { resetDragState() },
                                onDragEnd = {
                                    val sourceItemId = draggingItemId
                                    val targetItemId = hoverTargetItemId
                                    val sourceItem = items.firstOrNull { it.id == sourceItemId }
                                    val targetItem = items.firstOrNull { it.id == targetItemId }

                                    if (hasDragged && sourceItemId != null && targetItemId != null && sourceItemId != targetItemId) {
                                        when {
                                            sourceItem is AppItemModel && combineReadyTargetItemId == targetItemId && targetItem is AppItemModel ->
                                                onCreateFolder(sourceItemId, targetItemId)

                                            sourceItem is AppItemModel && combineReadyTargetItemId == targetItemId && targetItem is FolderModel ->
                                                onAddAppToFolder(sourceItemId, targetItemId)

                                            else -> onReorderItem(sourceItemId, targetItemId)
                                        }
                                    }

                                    resetDragState()
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                hasDragged = true
                                dragOffset += dragAmount

                                val sourceBounds = itemBounds[item.id] ?: return@detectDragGesturesAfterLongPress
                                val draggedCenter = sourceBounds.centerOffset() + dragOffset
                                val nearestTarget = items
                                    .asSequence()
                                    .filter { it.id != item.id }
                                    .mapNotNull { target ->
                                        itemBounds[target.id]?.let { bounds -> target to bounds }
                                    }
                                    .minByOrNull { (_, bounds) ->
                                        distanceSquared(draggedCenter, bounds.centerOffset())
                                    }

                                hoverTargetItemId = nearestTarget?.first?.id
                                combineHoverTargetItemId = nearestTarget
                                    ?.takeIf { (target, bounds) ->
                                        item is AppItemModel &&
                                            isCombineTarget(target) &&
                                            isPointInsideInsetRect(draggedCenter, bounds, insetFraction = 0.22f)
                                    }
                                    ?.first
                                    ?.id
                            }
                        }
                        .then(
                            when (item) {
                                is AppItemModel -> Modifier.clickable { onOpenApp(item.app) }
                                is FolderModel -> Modifier
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (item) {
                        is FolderModel -> FolderBubble(folder = item, onOpenFolder = onOpenFolder)
                        is AppItemModel -> AppIconBubble(app = item.app, size = 62.dp)
                    }
                    if (showLabels) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = homeItemLabel(item),
                            color = OneUiText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.White.copy(alpha = 0.45f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 3f,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderBubble(
    folder: FolderModel,
    onOpenFolder: (FolderModel) -> Unit,
) {
    Surface(
        modifier = Modifier.size(62.dp),
        shape = RoundedCornerShape(22.dp),
        color = OneUiCard.copy(alpha = 0.94f),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onOpenFolder(folder) }
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            folder.apps.chunked(2).forEach { rowApps ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowApps.forEach { app ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.name,
                                    modifier = Modifier.size(18.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(app.color),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun PageStrip(
    pageIndex: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == pageIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (selected) 20.dp else 7.dp, height = 7.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) OneUiText.copy(alpha = 0.76f) else OneUiTextSecondary.copy(alpha = 0.22f))
                    .clickable { onPageChange(index) },
            )
        }
    }
}

@Composable
private fun SearchPill(
    label: String,
    onOpenDrawer: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = OneUiCard,
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenDrawer)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = OneUiTextSecondary)
            Spacer(Modifier.width(12.dp))
            Text(label, color = OneUiTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun DockBar(
    apps: List<CloneApp>,
    showLabels: Boolean,
    appsButtonEnabled: Boolean,
    onOpenApp: (CloneApp) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val dockItems: List<CloneApp?> = if (appsButtonEnabled) {
        apps + listOf<CloneApp?>(null)
    } else {
        apps
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        color = OneUiCard,
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dockItems.forEach { app ->
                val isAppsButton = app == null
                Column(
                    modifier = if (isAppsButton) Modifier else Modifier.clickable { app?.let(onOpenApp) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isAppsButton) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = OneUiSurfaceSoft,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().clickable(onClick = onOpenDrawer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = OneUiText)
                            }
                        }
                    } else {
                        app?.let { AppIconBubble(app = it, size = 56.dp) }
                    }
                    if (showLabels) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isAppsButton) "Apps" else app?.name.orEmpty(),
                            color = OneUiText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}


