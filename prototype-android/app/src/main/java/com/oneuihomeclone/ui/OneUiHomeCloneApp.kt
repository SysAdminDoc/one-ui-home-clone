package com.oneuihomeclone.ui

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
import androidx.core.graphics.drawable.toBitmap
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
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.oneuihomeclone.data.DrawerSortKey
import com.oneuihomeclone.data.HomeLayoutKey
import com.oneuihomeclone.data.LauncherPreferences

private data class CloneApp(
    val id: String,
    val name: String,
    val launchIntent: Intent? = null,
    val icon: ImageBitmap? = null,
    val color: Color,
)

private sealed interface HomeGridItemModel {
    val id: String
}

private data class HomePageModel(
    val id: Int,
    val label: String,
    val eyebrow: String,
    val value: String,
    val status: String,
    val note: String,
    val widgets: List<WidgetTemplateModel>,
    val items: List<HomeGridItemModel>,
)

private data class SettingRowState(
    val title: String,
    val value: String,
)

private data class StatusClock(
    val timeText: String,
    val dateText: String,
    val fullDateText: String,
)

private data class FolderModel(
    override val id: String,
    val title: String,
    val summary: String,
    val apps: List<CloneApp>,
) : HomeGridItemModel

private data class AppItemModel(
    val app: CloneApp,
) : HomeGridItemModel {
    override val id: String = app.id
}

private data class OpenFolderTarget(
    val pageId: Int,
    val folderId: String,
)

private data class FinderSettingResult(
    val title: String,
    val category: String,
    val value: String,
)

private data class FinderActionItem(
    val type: FinderActionType,
    val title: String,
    val summary: String,
)

private data class WidgetTemplateModel(
    val title: String,
    val summary: String,
    val category: String,
    val span: String,
    val accent: Color,
)

private data class NotificationCardModel(
    val title: String,
    val summary: String,
    val timestamp: String,
)

private enum class FinderActionType {
    SETTINGS,
    WALLPAPERS,
    WIDGETS,
    PAGE_MANAGER,
    MEDIA_PAGE,
    HOME_PAGE,
    HIDE_APPS,
}

private enum class HomeLayoutMode(val title: String) {
    HOME_AND_APPS_SCREENS("Home and Apps screens"),
    HOME_SCREEN_ONLY("Home screen only"),
}

private enum class DrawerSortMode(val title: String) {
    CUSTOM_ORDER("Custom order"),
    ALPHABETICAL("Alphabetical order"),
}

private enum class OverlayPanel {
    DRAWER,
    NOTIFICATIONS,
    SETTINGS,
    EDIT_MODE,
    FOLDER,
    WIDGET_PICKER,
    HIDE_APPS,
}

/**
 * Flattened snapshot of persisted toggles for the snapshotFlow pipeline.
 * Equality + hashCode on the data class lets snapshotFlow suppress emissions
 * that encode the same user-visible state.
 */
private data class PersistedToggles(
    val mediaPageEnabled: Boolean,
    val appsButtonEnabled: Boolean,
    val appLabelsEnabled: Boolean,
    val widgetLabelsEnabled: Boolean,
    val swipeDownForNotifications: Boolean,
    val lockHomeScreenLayout: Boolean,
    val homeLayoutMode: HomeLayoutMode,
    val drawerSortMode: DrawerSortMode,
)

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

private fun fallbackColorFor(key: String): Color {
    val palette = listOf(
        Color(0xFFFFB84D),
        Color(0xFF6D8BFF),
        Color(0xFF45C48B),
        Color(0xFF35C15E),
        Color(0xFFFF6B6B),
        Color(0xFF5865F2),
        Color(0xFF8A94A6),
        Color(0xFFFFC857),
        Color(0xFF7B61FF),
        Color(0xFF50B5FF),
        Color(0xFF55C6A9),
        Color(0xFFFF7F50),
    )
    return palette[key.hashCode().absoluteValue % palette.size]
}

private const val MAX_ICONS_LOADED_EAGERLY = 300

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

@Composable
fun OneUiHomeCloneApp(homeIntentTick: Int = 0) {
    val appContext = LocalContext.current.applicationContext
    val preferences = remember(appContext) { LauncherPreferences(appContext) }
    val initialPrefs = remember(preferences) { preferences.snapshot() }
    val fallbackApps = remember { sampleApps() }
    var allApps by remember { mutableStateOf(fallbackApps) }
    var hasSeededDeviceApps by remember { mutableStateOf(false) }
    val dockApps = remember(allApps) { allApps.take(4) }
    val widgetTemplates = remember {
        listOf(
            WidgetTemplateModel("Calendar", "Month agenda with Samsung-style rounded chrome", "Recommended", "4 x 2", Color(0xFFFF8B7B)),
            WidgetTemplateModel("Weather", "Large conditions card with soft edge highlights", "Recommended", "4 x 2", Color(0xFF62B8FF)),
            WidgetTemplateModel("SmartThings", "Scenes and devices in a compact control stack", "Connected", "4 x 2", Color(0xFF2EBCF6)),
            WidgetTemplateModel("Battery", "Device and buds battery status", "Device", "4 x 1", Color(0xFF5ECB85)),
            WidgetTemplateModel("Music", "Now playing with album art emphasis", "Entertainment", "4 x 2", Color(0xFFFF6F96)),
            WidgetTemplateModel("Reminder list", "Pinned tasks for routines and grocery runs", "Productivity", "4 x 2", Color(0xFFFFC857)),
        )
    }
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
    var defaultHomePageIndex by remember { mutableIntStateOf(0) }
    var pageIndex by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        allApps = loadLauncherApps(
            packageManager = appContext.packageManager,
            hostPackageName = appContext.packageName,
            fallbackApps = fallbackApps,
        )
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
    val filteredWidgetTemplates = remember(selectedWidgetCategory, widgetTemplates) {
        if (selectedWidgetCategory == "All") {
            widgetTemplates
        } else {
            widgetTemplates.filter { it.category == selectedWidgetCategory }
        }
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
                categories = listOf("Recommended", "All", "Connected", "Device", "Entertainment", "Productivity"),
                selectedCategory = selectedWidgetCategory,
                widgets = filteredWidgetTemplates,
                targetPageLabel = widgetTargetPage?.label ?: "Home 1",
                onSelectCategory = { selectedWidgetCategory = it },
                onAddWidget = { widget ->
                    widgetTargetPage?.let { targetPage ->
                        homePages = homePages.map { page ->
                            if (page.id == targetPage.id) {
                                page.copy(widgets = addWidgetToPage(page.widgets, widget))
                            } else {
                                page
                            }
                        }
                        pageIndex = visualIndexForHomePage(widgetTargetHomePageIndex, mediaPageEnabled)
                    }
                    activeOverlay = null
                },
                onClose = { activeOverlay = null },
            )
        }
    }
}

private fun totalPageCount(homePageCount: Int, mediaPageEnabled: Boolean): Int {
    return homePageCount + if (mediaPageEnabled) 1 else 0
}

private fun visualIndexForHomePage(homePageIndex: Int, mediaPageEnabled: Boolean): Int {
    return if (mediaPageEnabled) homePageIndex + 1 else homePageIndex
}

private fun homePageIndexFromVisual(pageIndex: Int, mediaPageEnabled: Boolean): Int? {
    return if (mediaPageEnabled) {
        if (pageIndex == 0) null else pageIndex - 1
    } else {
        pageIndex
    }
}

private fun <T> moveListItem(
    items: List<T>,
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex == toIndex) {
        return items
    }
    val mutableItems = items.toMutableList()
    val item = mutableItems.removeAt(fromIndex)
    mutableItems.add(toIndex, item)
    return mutableItems
}

private fun movedIndexForSwap(
    trackedIndex: Int,
    fromIndex: Int,
    toIndex: Int,
): Int {
    return when {
        trackedIndex == fromIndex -> toIndex
        fromIndex < toIndex && trackedIndex in (fromIndex + 1)..toIndex -> trackedIndex - 1
        toIndex < fromIndex && trackedIndex in toIndex until fromIndex -> trackedIndex + 1
        else -> trackedIndex
    }
}

private fun applyHiddenAppsToPages(
    pages: List<HomePageModel>,
    hiddenAppIds: Set<String>,
): List<HomePageModel> {
    if (hiddenAppIds.isEmpty()) {
        return pages
    }

    return pages.map { page ->
        page.copy(
            items = page.items.mapNotNull { item ->
                when (item) {
                    is AppItemModel -> item.takeUnless { it.app.id in hiddenAppIds }
                    is FolderModel -> {
                        val visibleApps = item.apps.filterNot { it.id in hiddenAppIds }
                        if (visibleApps.isEmpty()) {
                            null
                        } else {
                            item.copy(
                                apps = visibleApps,
                                summary = folderSummaryFor(visibleApps),
                            )
                        }
                    }
                }
            },
        )
    }
}

private fun buildVisibleDockApps(
    preferredDockApps: List<CloneApp>,
    allApps: List<CloneApp>,
    hiddenAppIds: Set<String>,
): List<CloneApp> {
    val visiblePreferred = preferredDockApps.filterNot { it.id in hiddenAppIds }
    val visibleFallback = allApps
        .filterNot { it.id in hiddenAppIds || it.id in visiblePreferred.map(CloneApp::id).toSet() }
    return (visiblePreferred + visibleFallback).take(4)
}

private fun addWidgetToPage(
    widgets: List<WidgetTemplateModel>,
    widget: WidgetTemplateModel,
): List<WidgetTemplateModel> {
    return (widgets.filterNot { it.title == widget.title } + widget).takeLast(3)
}

private fun homeItemLabel(item: HomeGridItemModel): String {
    return when (item) {
        is AppItemModel -> item.app.name
        is FolderModel -> item.title
    }
}

private fun folderSummaryFor(apps: List<CloneApp>): String {
    return when (apps.size) {
        0 -> "Empty folder"
        1 -> apps.first().name
        2 -> "${apps[0].name} and ${apps[1].name}"
        else -> "${apps[0].name}, ${apps[1].name}, and ${apps.size - 2} more"
    }
}

private fun previewAppsForPage(page: HomePageModel): List<CloneApp> {
    return page.items.flatMap { item ->
        when (item) {
            is AppItemModel -> listOf(item.app)
            is FolderModel -> item.apps.take(2)
        }
    }.take(4)
}

private fun reorderHomeGridItems(
    items: List<HomeGridItemModel>,
    sourceItemId: String,
    targetItemId: String,
): List<HomeGridItemModel> {
    val sourceIndex = items.indexOfFirst { it.id == sourceItemId }
    val targetIndex = items.indexOfFirst { it.id == targetItemId }
    if (sourceIndex == -1 || targetIndex == -1 || sourceIndex == targetIndex) {
        return items
    }
    return moveListItem(items, sourceIndex, targetIndex)
}

private fun Rect.centerOffset(): Offset = Offset((left + right) / 2f, (top + bottom) / 2f)

private fun distanceSquared(start: Offset, end: Offset): Float {
    val deltaX = start.x - end.x
    val deltaY = start.y - end.y
    return (deltaX * deltaX) + (deltaY * deltaY)
}

private fun isPointInsideInsetRect(
    point: Offset,
    rect: Rect,
    insetFraction: Float,
): Boolean {
    val insetX = rect.width * insetFraction
    val insetY = rect.height * insetFraction
    return point.x in (rect.left + insetX)..(rect.right - insetX) &&
        point.y in (rect.top + insetY)..(rect.bottom - insetY)
}

private fun isCombineTarget(item: HomeGridItemModel): Boolean {
    return item is AppItemModel || item is FolderModel
}

private fun createFolderFromHomeGridItems(
    items: List<HomeGridItemModel>,
    sourceItemId: String,
    targetItemId: String,
    folderId: String,
): List<HomeGridItemModel> {
    val sourceIndex = items.indexOfFirst { it.id == sourceItemId }
    val targetIndex = items.indexOfFirst { it.id == targetItemId }
    if (sourceIndex == -1 || targetIndex == -1 || sourceIndex == targetIndex) {
        return items
    }

    val sourceItem = items.getOrNull(sourceIndex) as? AppItemModel ?: return items
    val targetItem = items.getOrNull(targetIndex) as? AppItemModel ?: return items
    val folderApps = listOf(targetItem.app, sourceItem.app)
    val folder = FolderModel(
        id = folderId,
        title = "New folder",
        summary = folderSummaryFor(folderApps),
        apps = folderApps,
    )
    val insertIndex = minOf(sourceIndex, targetIndex)

    return items.filterIndexed { index, _ ->
        index != sourceIndex && index != targetIndex
    }.toMutableList().apply {
        add(insertIndex, folder)
    }
}

private fun addAppToFolder(
    items: List<HomeGridItemModel>,
    sourceItemId: String,
    folderId: String,
): List<HomeGridItemModel> {
    val sourceIndex = items.indexOfFirst { it.id == sourceItemId }
    val folderIndex = items.indexOfFirst { it.id == folderId }
    if (sourceIndex == -1 || folderIndex == -1 || sourceIndex == folderIndex) {
        return items
    }

    val sourceItem = items.getOrNull(sourceIndex) as? AppItemModel ?: return items
    val folder = items.getOrNull(folderIndex) as? FolderModel ?: return items
    val updatedApps = (folder.apps + sourceItem.app).distinctBy(CloneApp::id)

    return items.mapIndexedNotNull { index, item ->
        when {
            index == sourceIndex -> null
            index == folderIndex -> folder.copy(
                apps = updatedApps,
                summary = folderSummaryFor(updatedApps),
            )
            else -> item
        }
    }
}

private fun buildSeedWidgets(id: Int): List<WidgetTemplateModel> {
    return when ((id - 1) % 3) {
        0 -> listOf(
            WidgetTemplateModel("Calendar", "Month agenda with Samsung-style rounded chrome", "Recommended", "4 x 2", Color(0xFFFF8B7B)),
            WidgetTemplateModel("Weather", "Large conditions card with soft edge highlights", "Recommended", "4 x 2", Color(0xFF62B8FF)),
        )
        1 -> listOf(
            WidgetTemplateModel("Reminder list", "Pinned tasks for routines and grocery runs", "Productivity", "4 x 2", Color(0xFFFFC857)),
            WidgetTemplateModel("Battery", "Device and buds battery status", "Device", "4 x 1", Color(0xFF5ECB85)),
        )
        else -> listOf(
            WidgetTemplateModel("Music", "Now playing with album art emphasis", "Entertainment", "4 x 2", Color(0xFFFF6F96)),
            WidgetTemplateModel("SmartThings", "Scenes and devices in a compact control stack", "Connected", "4 x 2", Color(0xFF2EBCF6)),
        )
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun buildHomePage(id: Int, allApps: List<CloneApp>): HomePageModel {
    val metadata = when ((id - 1) % 3) {
        0 -> listOf(
            "Home $id",
            "Monday",
            "30",
            "73° and bright",
            "Build parity first. Add customization only after the launcher feels native.",
        )
        1 -> listOf(
            "Home $id",
            "Focus",
            "4 blocks",
            "Calendar, tasks, and routines",
            "Samsung's page manager works because every screen has a purpose, not just icons.",
        )
        else -> listOf(
            "Home $id",
            "Evening",
            "3 scenes",
            "Lighting, music, and home controls",
            "A true One UI clone needs dedicated pages for routines, widgets, and media moments.",
        )
    }
    val startIndex = ((id - 1) * 4) % allApps.size
    val folderApps = List(4) { offset -> allApps[(startIndex + offset) % allApps.size] }
    val pageApps = List(11) { offset -> allApps[(startIndex + 4 + offset) % allApps.size] }
    val folder = when ((id - 1) % 3) {
        0 -> FolderModel(id = "folder-seed-$id", title = "Samsung", summary = folderSummaryFor(folderApps), apps = folderApps)
        1 -> FolderModel(id = "folder-seed-$id", title = "Focus", summary = folderSummaryFor(folderApps), apps = folderApps)
        else -> FolderModel(id = "folder-seed-$id", title = "Home life", summary = folderSummaryFor(folderApps), apps = folderApps)
    }

    return HomePageModel(
        id = id,
        label = metadata[0],
        eyebrow = metadata[1],
        value = metadata[2],
        status = metadata[3],
        note = metadata[4],
        widgets = buildSeedWidgets(id),
        items = listOf<HomeGridItemModel>(folder) + pageApps.map(::AppItemModel),
    )
}

private fun buildFinderSettingResults(
    query: String,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    mediaPageEnabled: Boolean,
    appsButtonEnabled: Boolean,
    appLabelsEnabled: Boolean,
    widgetLabelsEnabled: Boolean,
    swipeDownForNotifications: Boolean,
    homePageCount: Int,
    defaultHomePageLabel: String,
    hiddenAppCount: Int,
): List<FinderSettingResult> {
    val settings = listOf(
        FinderSettingResult("Home screen layout", "Layout", homeLayoutMode.title),
        FinderSettingResult("Home screen grid", "Layout", "4x6"),
        FinderSettingResult("Apps screen grid", "Layout", "4x6"),
        FinderSettingResult("Folder grid", "Layout", "3x4"),
        FinderSettingResult("Default home page", "Layout", defaultHomePageLabel),
        FinderSettingResult("Visible pages", "Layout", homePageCount.toString()),
        FinderSettingResult("Media page", "Behavior", if (mediaPageEnabled) "On" else "Off"),
        FinderSettingResult(
            "Apps button on Home screen",
            "Behavior",
            if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) "Unavailable in Home screen only mode" else if (appsButtonEnabled) "On" else "Off",
        ),
        FinderSettingResult("App labels", "Behavior", if (appLabelsEnabled) "On" else "Off"),
        FinderSettingResult("Widget labels", "Behavior", if (widgetLabelsEnabled) "On" else "Off"),
        FinderSettingResult("Swipe down for notification panel", "Gestures", if (swipeDownForNotifications) "On" else "Off"),
        FinderSettingResult("Hide apps", "Apps screen", if (hiddenAppCount == 0) "None" else "$hiddenAppCount hidden"),
        FinderSettingResult("Lock Home screen layout", "Behavior", if (lockHomeScreenLayout) "On" else "Off"),
        FinderSettingResult("Add new apps to Home screen", "Behavior", "On"),
        FinderSettingResult("Badge notifications", "Behavior", "Dots and number"),
    )
    val normalizedQuery = query.trim().lowercase()

    return if (normalizedQuery.isBlank()) {
        settings.take(4)
    } else {
        settings.filter { setting ->
            listOf(setting.title, setting.category, setting.value).any {
                it.lowercase().contains(normalizedQuery)
            }
        }
    }
}

private fun buildFinderActionResults(
    query: String,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    mediaPageEnabled: Boolean,
    hasHiddenApps: Boolean,
): List<FinderActionItem> {
    val actions = listOf(
        FinderActionItem(
            FinderActionType.SETTINGS,
            "Home screen settings",
            if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) {
                "Adjust the launcher while Home screen only mode is active"
            } else {
                "Adjust layout, labels, badges, and gestures"
            },
        ),
        FinderActionItem(
            FinderActionType.WALLPAPERS,
            "Wallpapers and style",
            if (lockHomeScreenLayout) "Layout is locked, so wallpaper controls route through settings first" else "Open edit mode for wallpaper and theme controls",
        ),
        FinderActionItem(FinderActionType.WIDGETS, "Widgets", "Jump into the Samsung-style widget surface"),
        FinderActionItem(
            FinderActionType.PAGE_MANAGER,
            "Page manager",
            if (lockHomeScreenLayout) "Layout is locked, so page management is currently disabled" else "Preview pages, set default home, and add screens",
        ),
        FinderActionItem(
            FinderActionType.MEDIA_PAGE,
            if (mediaPageEnabled) "Go to media page" else "Enable media page",
            if (mediaPageEnabled) "Jump to the Samsung Free-style screen" else "Turn on the left media page and open it",
        ),
        FinderActionItem(FinderActionType.HOME_PAGE, "Go to default home page", "Return to the main home screen immediately"),
        FinderActionItem(
            FinderActionType.HIDE_APPS,
            if (hasHiddenApps) "Manage hidden apps" else "Hide apps",
            if (hasHiddenApps) "Review which apps are hidden from Home and Apps screens" else "Choose which apps disappear from Home and Apps screens",
        ),
    )
    val normalizedQuery = query.trim().lowercase()

    return if (normalizedQuery.isBlank()) {
        actions.take(3)
    } else {
        actions.filter { action ->
            listOf(action.title, action.summary).any { it.lowercase().contains(normalizedQuery) } ||
                when (action.type) {
                    FinderActionType.SETTINGS -> normalizedQuery.contains("setting")
                    FinderActionType.WALLPAPERS -> normalizedQuery.contains("wall") || normalizedQuery.contains("theme")
                    FinderActionType.WIDGETS -> normalizedQuery.contains("widget")
                    FinderActionType.PAGE_MANAGER -> normalizedQuery.contains("page")
                    FinderActionType.MEDIA_PAGE -> normalizedQuery.contains("media") || normalizedQuery.contains("free")
                    FinderActionType.HOME_PAGE -> normalizedQuery.contains("home")
                    FinderActionType.HIDE_APPS -> normalizedQuery.contains("hide") || normalizedQuery.contains("hidden")
                }
        }
    }
}

private fun rememberRecentSearch(
    query: String,
    recentSearches: List<String>,
): List<String> {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isBlank()) {
        return recentSearches
    }

    return listOf(trimmedQuery) + recentSearches.filterNot { it.equals(trimmedQuery, ignoreCase = true) }
        .take(5)
}

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
private fun finderActionIcon(type: FinderActionType) = when (type) {
    FinderActionType.SETTINGS -> Icons.Default.Settings
    FinderActionType.WALLPAPERS -> Icons.Default.Image
    FinderActionType.WIDGETS -> Icons.Default.Widgets
    FinderActionType.PAGE_MANAGER -> Icons.Default.Tune
    FinderActionType.MEDIA_PAGE -> Icons.Default.Apps
    FinderActionType.HOME_PAGE -> Icons.Default.Home
    FinderActionType.HIDE_APPS -> Icons.Default.Tune
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(if (widget.span == "4 x 1") 2 else 3) { thumb ->
                            Box(
                                modifier = Modifier
                                    .size(width = if (widget.span == "4 x 1") 46.dp else 32.dp, height = if (widget.span == "4 x 1") 18.dp else 22.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(widget.accent.copy(alpha = 0.12f + (thumb * 0.04f))),
                            )
                        }
                    }
                }
            }
        }
        repeat(2 - shownWidgets.size) {
            Spacer(Modifier.weight(1f))
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
private fun AppIconBubble(app: CloneApp, size: Dp) {
    if (app.icon != null) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(20.dp),
            color = app.color,
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = app.name.take(1),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PageStrip(
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

@Composable
private fun DrawerOverlay(
    query: String,
    apps: List<CloneApp>,
    appsScreenApps: List<CloneApp>,
    drawerPages: List<List<CloneApp>>,
    homeLayoutMode: HomeLayoutMode,
    drawerSortMode: DrawerSortMode,
    drawerPageIndex: Int,
    hiddenAppCount: Int,
    settingResults: List<FinderSettingResult>,
    actionResults: List<FinderActionItem>,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectSortMode: (DrawerSortMode) -> Unit,
    onSelectDrawerPage: (Int) -> Unit,
    onOpenHideApps: () -> Unit,
    onSelectRecentSearch: (String) -> Unit,
    onOpenSettingResult: (FinderSettingResult) -> Unit,
    onOpenAction: (FinderActionItem) -> Unit,
    onOpenApp: (CloneApp) -> Unit,
    appLabelsEnabled: Boolean,
) {
    val trimmedQuery = query.trim()
    val selectedDrawerPage = drawerPages.getOrNull(drawerPageIndex).orEmpty()
    val isHomeOnly = homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY
    val listState = rememberLazyListState()
    val closeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val drawerCloseGestureEnabled = trimmedQuery.isBlank() &&
        !isHomeOnly &&
        drawerSortMode == DrawerSortMode.CUSTOM_ORDER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        trimmedQuery.isNotBlank() -> "Finder"
                        isHomeOnly -> "Search"
                        else -> "Apps"
                    },
                    color = OneUiText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = "Settings", onClick = onOpenSettings)
            }
            Spacer(Modifier.height(12.dp))
            if (trimmedQuery.isBlank() && !isHomeOnly) {
                AppsScreenControlRow(
                    drawerSortMode = drawerSortMode,
                    hiddenAppCount = hiddenAppCount,
                    onSelectSortMode = onSelectSortMode,
                    onOpenHideApps = onOpenHideApps,
                )
                Spacer(Modifier.height(18.dp))
            }
            if (drawerCloseGestureEnabled) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(closeThresholdPx, onClose) {
                            var totalDrag = 0f
                            var canClose = false

                            detectVerticalDragGestures(
                                onDragStart = {
                                    totalDrag = 0f
                                    canClose = true
                                },
                                onDragEnd = {
                                    if (canClose && totalDrag >= closeThresholdPx) {
                                        onClose()
                                    }
                                    totalDrag = 0f
                                    canClose = false
                                },
                                onDragCancel = {
                                    totalDrag = 0f
                                    canClose = false
                                },
                            ) { change, dragAmount ->
                                if (!canClose) {
                                    return@detectVerticalDragGestures
                                }

                                if (dragAmount < 0f && totalDrag == 0f) {
                                    canClose = false
                                    return@detectVerticalDragGestures
                                }

                                totalDrag = (totalDrag + dragAmount).coerceAtLeast(0f)
                                if (totalDrag >= closeThresholdPx) {
                                    change.consume()
                                }
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        FinderSectionHeader("Apps screen")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Custom order stays paged, just like Samsung's default One UI 7 apps screen.",
                            color = OneUiTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        FinderAppGrid(
                            apps = selectedDrawerPage,
                            showLabels = appLabelsEnabled,
                            onOpenApp = onOpenApp,
                        )
                        if (drawerPages.size > 1) {
                            Spacer(Modifier.height(16.dp))
                            PageStrip(
                                pageIndex = drawerPageIndex,
                                pageCount = drawerPages.size,
                                onPageChange = onSelectDrawerPage,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (recentSearches.isNotEmpty()) {
                            FinderSectionHeader("Recent searches")
                            Spacer(Modifier.height(10.dp))
                            FinderRecentSearches(
                                searches = recentSearches,
                                onSelectSearch = onSelectRecentSearch,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    if (trimmedQuery.isBlank()) {
                        if (isHomeOnly) {
                            item {
                                FinderSectionHeader("Home screen only")
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "All apps live on Home pages in this mode. Finder stays available for search and quick launch.",
                                    color = OneUiTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                            if (appsScreenApps.isNotEmpty()) {
                                item {
                                    FinderSectionHeader("Suggested apps")
                                    Spacer(Modifier.height(12.dp))
                                    FinderAppGrid(
                                        apps = appsScreenApps.take(8),
                                        showLabels = appLabelsEnabled,
                                        onOpenApp = onOpenApp,
                                    )
                                }
                            }
                        } else {
                            item {
                                FinderSectionHeader("Apps screen")
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Alphabetical order switches to a vertically scrolling apps list.",
                                    color = OneUiTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                            alphabeticalAppSections(appsScreenApps).forEach { section ->
                                item(key = "apps-${section.first}") {
                                    FinderSectionHeader(section.first)
                                    Spacer(Modifier.height(10.dp))
                                    FinderAppGrid(
                                        apps = section.second,
                                        showLabels = appLabelsEnabled,
                                        onOpenApp = onOpenApp,
                                    )
                                }
                            }
                        }
                        if (recentSearches.isNotEmpty()) {
                            item {
                                FinderSectionHeader("Recent searches")
                                Spacer(Modifier.height(10.dp))
                                FinderRecentSearches(
                                    searches = recentSearches,
                                    onSelectSearch = onSelectRecentSearch,
                                )
                            }
                        }
                    } else {
                        if (actionResults.isNotEmpty()) {
                            item {
                                FinderSectionHeader("Suggested actions")
                                Spacer(Modifier.height(10.dp))
                                FinderActionList(
                                    actions = actionResults,
                                    onOpenAction = onOpenAction,
                                )
                            }
                        }
                        if (settingResults.isNotEmpty()) {
                            item {
                                FinderSectionHeader("Settings")
                                Spacer(Modifier.height(10.dp))
                                FinderSettingsList(
                                    settings = settingResults,
                                    onOpenSetting = onOpenSettingResult,
                                )
                            }
                        }
                        if (apps.isNotEmpty()) {
                            item {
                                FinderSectionHeader("Apps")
                                Spacer(Modifier.height(12.dp))
                                FinderAppGrid(
                                    apps = apps,
                                    showLabels = appLabelsEnabled,
                                    onOpenApp = onOpenApp,
                                )
                            }
                        }
                        if (apps.isEmpty() && settingResults.isEmpty() && actionResults.isEmpty()) {
                            item {
                                FinderEmptyState(query = query)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(if (trimmedQuery.isBlank()) "Search from the bottom" else "Search apps and settings")
                },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Swipe down or tap Close to return home",
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(12.dp))
            SettingsCapsule(label = "Close", onClick = onClose, accent = false)
        }
    }
}

@Composable
private fun FinderEmptyState(
    query: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = OneUiSurface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No Finder results", color = OneUiText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Try broader terms like \"$query settings\" or \"$query page\" to surface grouped actions and settings results.",
                color = OneUiTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}

private fun alphabeticalAppSections(apps: List<CloneApp>): List<Pair<String, List<CloneApp>>> {
    return apps
        .groupBy { app -> app.name.firstOrNull()?.uppercase() ?: "#" }
        .toSortedMap()
        .map { entry -> entry.key to entry.value }
}

@Composable
private fun AppsScreenControlRow(
    drawerSortMode: DrawerSortMode,
    hiddenAppCount: Int,
    onSelectSortMode: (DrawerSortMode) -> Unit,
    onOpenHideApps: () -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DrawerSortMode.entries.forEach { mode ->
            SettingsCapsule(
                label = mode.title,
                onClick = { onSelectSortMode(mode) },
                accent = drawerSortMode == mode,
            )
        }
        SettingsCapsule(
            label = if (hiddenAppCount == 0) "Hide apps" else "Hide apps ($hiddenAppCount)",
            onClick = onOpenHideApps,
            accent = false,
        )
    }
}

@Composable
private fun DrawerPill(label: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = OneUiSurface,
        shadowElevation = 2.dp,
    ) {
        Text(
            label,
            color = OneUiText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FinderSectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        color = OneUiTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun FinderRecentSearches(
    searches: List<String>,
    onSelectSearch: (String) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        searches.forEach { search ->
            SettingsCapsule(label = search, onClick = { onSelectSearch(search) }, accent = false)
        }
    }
}

@Composable
private fun FinderActionList(
    actions: List<FinderActionItem>,
    onOpenAction: (FinderActionItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.forEach { action ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = OneUiSurface,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAction(action) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(OneUiAccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = finderActionIcon(action.type),
                            contentDescription = null,
                            tint = OneUiAccent,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(action.title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(action.summary, color = OneUiTextSecondary, fontSize = 12.sp)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OneUiTextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FinderSettingsList(
    settings: List<FinderSettingResult>,
    onOpenSetting: (FinderSettingResult) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        settings.forEach { setting ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = OneUiSurface,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSetting(setting) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(setting.title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${setting.category}  •  ${setting.value}",
                            color = OneUiTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OneUiTextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FinderAppGrid(
    apps: List<CloneApp>,
    showLabels: Boolean,
    onOpenApp: (CloneApp) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        apps.chunked(4).forEach { rowApps ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowApps.forEach { app ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenApp(app) }
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIconBubble(app = app, size = 60.dp)
                        if (showLabels) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                app.name,
                                color = OneUiText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                repeat(4 - rowApps.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FolderOverlay(
    folder: FolderModel,
    appLabelsEnabled: Boolean,
    onOpenApp: (CloneApp) -> Unit,
    onRenameFolder: (String) -> Unit,
    onClose: () -> Unit,
) {
    var titleDraft by remember(folder.title) { mutableStateOf(folder.title) }
    val sanitizedTitleDraft = titleDraft.trim()
    val hasTitleChanges = sanitizedTitleDraft.isNotBlank() && sanitizedTitleDraft != folder.title

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClose)
            .background(Color(0x32000000)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            shape = RoundedCornerShape(34.dp),
            color = OneUiCard.copy(alpha = 0.98f),
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = titleDraft,
                            onValueChange = { titleDraft = it.take(24) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = OneUiText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            placeholder = {
                                Text("Folder name", color = OneUiTextSecondary)
                            },
                            shape = RoundedCornerShape(22.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(folder.summary, color = OneUiTextSecondary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    SettingsCapsule(label = "Close", onClick = onClose, accent = false)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsCapsule(
                        label = if (hasTitleChanges) "Save name" else "Folder name",
                        onClick = {
                            val updatedTitle = sanitizedTitleDraft.ifBlank { folder.title }
                            if (updatedTitle != folder.title) {
                                onRenameFolder(updatedTitle)
                            }
                            titleDraft = updatedTitle
                        },
                        accent = hasTitleChanges,
                    )
                    DrawerPill("Samsung folder")
                }
                Spacer(Modifier.height(18.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(258.dp),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(folder.apps) { app ->
                        Column(
                            modifier = Modifier.clickable { onOpenApp(app) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppIconBubble(app = app, size = 60.dp)
                            if (appLabelsEnabled) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = app.name,
                                    color = OneUiText,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Samsung-style folders feel like soft floating sheets with clean spacing and immediate drag targets.",
                    color = OneUiTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun HideAppsOverlay(
    apps: List<CloneApp>,
    hiddenAppIds: Set<String>,
    onToggleHidden: (CloneApp) -> Unit,
    onClose: () -> Unit,
) {
    val hiddenApps = remember(apps, hiddenAppIds) { apps.filter { it.id in hiddenAppIds } }
    val visibleApps = remember(apps, hiddenAppIds) { apps.filterNot { it.id in hiddenAppIds } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hide apps", color = OneUiText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = "Close", onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Hidden apps disappear from Home and Apps screens, which is how Samsung presents the feature.",
                color = OneUiTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(16.dp))
            if (hiddenApps.isNotEmpty()) {
                FinderSectionHeader("Hidden now")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hiddenApps.forEach { app ->
                        SettingsCapsule(
                            label = app.name,
                            onClick = { onToggleHidden(app) },
                            accent = true,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            FinderSectionHeader("Tap apps to hide or restore")
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                lazyItems(visibleApps + hiddenApps) { app ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = OneUiSurface,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleHidden(app) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIconBubble(app = app, size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.name, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (app.id in hiddenAppIds) "Hidden from Home and Apps screens" else "Visible on Home and Apps screens",
                                    color = OneUiTextSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                            SettingsCapsule(
                                label = if (app.id in hiddenAppIds) "Restore" else "Hide",
                                onClick = { onToggleHidden(app) },
                                accent = app.id !in hiddenAppIds,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerOverlay(
    categories: List<String>,
    selectedCategory: String,
    widgets: List<WidgetTemplateModel>,
    targetPageLabel: String,
    onSelectCategory: (String) -> Unit,
    onAddWidget: (WidgetTemplateModel) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Widgets", color = OneUiText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = "Close", onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Recommended first, then grouped by the surfaces Samsung tends to emphasize.",
                color = OneUiTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Adding to $targetPageLabel",
                color = OneUiAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    SettingsCapsule(
                        label = category,
                        onClick = { onSelectCategory(category) },
                        accent = category == selectedCategory,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                lazyItems(widgets) { widget ->
                    WidgetTemplateCard(widget = widget, onAddWidget = onAddWidget)
                }
            }
        }
    }
}

@Composable
private fun WidgetTemplateCard(
    widget: WidgetTemplateModel,
    onAddWidget: (WidgetTemplateModel) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = OneUiSurface,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(widget.title, color = OneUiText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(widget.summary, color = OneUiTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    SettingsCapsule(label = widget.span, accent = false, enabled = false)
                    Spacer(Modifier.height(8.dp))
                    SettingsCapsule(label = "Add", onClick = { onAddWidget(widget) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (widget.span == "4 x 1") 86.dp else 126.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, widget.accent.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(widget.accent.copy(alpha = 0.18f), Color.White),
                                start = Offset.Zero,
                                end = Offset(900f, 400f),
                            ),
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Column {
                        Text(widget.category, color = widget.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(widget.title, color = OneUiText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(if (widget.span == "4 x 1") 3 else 4) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(width = if (widget.span == "4 x 1") 54.dp else 34.dp, height = if (widget.span == "4 x 1") 28.dp else 34.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(widget.accent.copy(alpha = 0.12f + (index * 0.03f))),
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
private fun EditModeTray(
    pages: List<HomePageModel>,
    pageIndex: Int,
    mediaPageEnabled: Boolean,
    defaultHomePageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onToggleMediaPage: () -> Unit,
    onAddPage: () -> Unit,
    onMoveCurrentPageLeft: () -> Unit,
    onMoveCurrentPageRight: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    currentWidgetCount: Int,
    onRemoveLastWidget: () -> Unit,
    onRemoveCurrentPage: () -> Unit,
    onSetCurrentPageAsDefault: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val selectedHomePageIndex = homePageIndexFromVisual(pageIndex, mediaPageEnabled)
    val selectedIsMedia = mediaPageEnabled && pageIndex == 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x20000000)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(34.dp),
            color = OneUiSurface,
            shadowElevation = 10.dp,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Home screen", color = OneUiText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    SettingsCapsule(label = "Done", onClick = onClose, accent = false)
                }
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditTile(
                            title = "Wallpapers and style",
                            icon = Icons.Default.Image,
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            supportingText = "Preview only",
                        )
                        EditTile("Widgets", Icons.Default.Widgets, modifier = Modifier.weight(1f), onClick = onOpenWidgetPicker)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditTile("Home settings", Icons.Default.Settings, modifier = Modifier.weight(1f), onClick = onOpenSettings)
                        EditTile(
                            title = "Page manager",
                            icon = Icons.Default.Tune,
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            supportingText = "Preview below",
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                PageManagerPanel(
                    pages = pages,
                    pageIndex = pageIndex,
                    mediaPageEnabled = mediaPageEnabled,
                    defaultHomePageIndex = defaultHomePageIndex,
                    onSelectPage = onSelectPage,
                    onAddPage = onAddPage,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsCapsule(
                        label = if (mediaPageEnabled) "Hide media page" else "Show media page",
                        onClick = onToggleMediaPage,
                        accent = mediaPageEnabled,
                    )
                    if (!selectedIsMedia) {
                        SettingsCapsule(
                            label = if (selectedHomePageIndex == defaultHomePageIndex) "Default home" else "Set as home",
                            onClick = onSetCurrentPageAsDefault,
                            accent = selectedHomePageIndex != defaultHomePageIndex,
                        )
                        if (selectedHomePageIndex != null && selectedHomePageIndex > 0) {
                            SettingsCapsule(
                                label = "Move left",
                                onClick = onMoveCurrentPageLeft,
                                accent = false,
                            )
                        }
                        if (selectedHomePageIndex != null && selectedHomePageIndex < pages.lastIndex) {
                            SettingsCapsule(
                                label = "Move right",
                                onClick = onMoveCurrentPageRight,
                                accent = false,
                            )
                        }
                        if (currentWidgetCount > 0) {
                            SettingsCapsule(
                                label = "Remove widget",
                                onClick = onRemoveLastWidget,
                                accent = false,
                            )
                        }
                        if (pages.size > 1) {
                            SettingsCapsule(
                                label = "Remove page",
                                onClick = onRemoveCurrentPage,
                                accent = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageManagerPanel(
    pages: List<HomePageModel>,
    pageIndex: Int,
    mediaPageEnabled: Boolean,
    defaultHomePageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = OneUiSurfaceSoft,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text("Pages", color = OneUiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Samsung-style page controls need real thumbnails, a default home marker, and a dedicated media surface.",
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (mediaPageEnabled) {
                    PagePreviewTile(
                        title = "Media",
                        subtitle = "Samsung Free",
                        selected = pageIndex == 0,
                        onClick = { onSelectPage(0) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFEEF4FF), Color(0xFFDDEBFF)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = OneUiAccent)
                        }
                    }
                }

                pages.forEachIndexed { index, page ->
                    PagePreviewTile(
                        title = page.label,
                        subtitle = if (index == defaultHomePageIndex) "Default home" else "Tap to preview",
                        selected = pageIndex == visualIndexForHomePage(index, mediaPageEnabled),
                        onClick = { onSelectPage(visualIndexForHomePage(index, mediaPageEnabled)) },
                    ) {
                        HomePagePreview(
                            items = page.items.take(4),
                            isDefaultHome = index == defaultHomePageIndex,
                        )
                    }
                }

                PagePreviewTile(
                    title = "New page",
                    subtitle = "Add",
                    selected = false,
                    onClick = onAddPage,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(OneUiAccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = OneUiAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun PagePreviewTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.width(132.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) Color.White else OneUiSurface,
        border = if (selected) BorderStroke(1.dp, OneUiAccent.copy(alpha = 0.32f)) else null,
        shadowElevation = if (selected) 4.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
            ) {
                preview()
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = OneUiText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = if (selected) OneUiAccent else OneUiTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomePagePreview(
    items: List<HomeGridItemModel>,
    isDefaultHome: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF3F6FB),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "8:42",
                    color = OneUiTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                if (isDefaultHome) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = OneUiAccent, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("Widget", color = OneUiTextSecondary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(2).forEach { item ->
                    PreviewItem(item = item, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.drop(2).take(2).forEach { item ->
                    PreviewItem(item = item, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PreviewItem(
    item: HomeGridItemModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (item) {
            is AppItemModel -> {
                if (item.app.icon != null) {
                    Image(
                        bitmap = item.app.icon,
                        contentDescription = item.app.name,
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(item.app.color),
                    )
                }
            }

            is FolderModel -> {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, OneUiBorder),
                ) {
                    Column(Modifier.padding(3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(2) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                item.apps.drop(row * 2).take(2).forEach { app ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(3.dp))
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
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(3.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x22000000)),
        )
    }
}

@Composable
private fun EditTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.widthIn(min = 150.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (enabled) OneUiSurfaceSoft else OneUiSurfaceSoft.copy(alpha = 0.76f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (enabled) OneUiAccentSoft else OneUiAccentSoft.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (enabled) OneUiAccent else OneUiTextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = if (enabled) OneUiText else OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                supportingText?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, color = OneUiTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun NotificationShadeOverlay(
    clock: StatusClock,
    onClose: () -> Unit,
) {
    val notifications = remember {
        listOf(
            NotificationCardModel(
                title = "Launcher parity checkpoint",
                summary = "Home icons, dock targets, and folder entries now respond like real surfaces instead of dead mocks.",
                timestamp = "Just now",
            ),
            NotificationCardModel(
                title = "Finder cleanup",
                summary = "The extra drawer control that never changed state was removed to keep the prototype honest.",
                timestamp = "2 min ago",
            ),
            NotificationCardModel(
                title = "Notification gesture active",
                summary = "Swipe down from the home surface now opens this Samsung-style mock shade when the setting is enabled.",
                timestamp = "5 min ago",
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground.copy(alpha = 0.98f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(clock.timeText, color = OneUiText, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(clock.fullDateText, color = OneUiTextSecondary, fontSize = 13.sp)
                }
                SettingsCapsule(label = "Close", onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = OneUiSurfaceSoft,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(OneUiAccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = OneUiAccent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifications", color = OneUiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "This keeps the swipe-down toggle testable instead of leaving it as display-only state.",
                            color = OneUiTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                lazyItems(notifications) { notification ->
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = OneUiSurface,
                        shadowElevation = 3.dp,
                    ) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    notification.title,
                                    color = OneUiText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(notification.timestamp, color = OneUiTextSecondary, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                notification.summary,
                                color = OneUiTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsOverlay(
    mediaPageEnabled: Boolean,
    appsButtonEnabled: Boolean,
    appLabelsEnabled: Boolean,
    widgetLabelsEnabled: Boolean,
    swipeDownForNotifications: Boolean,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    defaultHomePageLabel: String,
    homePageCount: Int,
    appsScreenSortTitle: String,
    hiddenAppCount: Int,
    focusedSettingTitle: String?,
    onClose: () -> Unit,
    onMediaPageChange: (Boolean) -> Unit,
    onAppsButtonChange: (Boolean) -> Unit,
    onAppLabelsChange: (Boolean) -> Unit,
    onWidgetLabelsChange: (Boolean) -> Unit,
    onSwipeDownChange: (Boolean) -> Unit,
    onHomeLayoutModeChange: (HomeLayoutMode) -> Unit,
    onLockHomeScreenLayoutChange: (Boolean) -> Unit,
) {
    val layoutRows = remember(defaultHomePageLabel, homePageCount, homeLayoutMode, appsScreenSortTitle, hiddenAppCount) {
        listOf(
            SettingRowState("Home screen layout", homeLayoutMode.title),
            SettingRowState("Home screen grid", "4x6"),
            SettingRowState("Apps screen grid", "4x6"),
            SettingRowState(
                "Apps screen sort",
                if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) "Unavailable in Home screen only mode" else appsScreenSortTitle,
            ),
            SettingRowState("Hide apps", if (hiddenAppCount == 0) "None" else "$hiddenAppCount hidden"),
            SettingRowState("Folder grid", "3x4"),
            SettingRowState("Default home page", defaultHomePageLabel),
            SettingRowState("Visible pages", homePageCount.toString()),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Home screen settings", color = OneUiText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = "Close", onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
            ) {
                if (!focusedSettingTitle.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = OneUiAccentSoft,
                        ) {
                            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                                Text("Jumped from Finder", color = OneUiAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    focusedSettingTitle,
                                    color = OneUiText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "A later pass can scroll directly to this section or row, but the prototype now preserves the Finder handoff.",
                                    color = OneUiTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
                }
                item {
                    SettingsSection(
                        title = "Layout",
                        summary = "Match Samsung defaults first",
                        rows = layoutRows,
                    )
                }
                item {
                    SettingsModeCard(
                        title = "Home screen layout",
                        selectedMode = homeLayoutMode,
                        onSelectMode = onHomeLayoutModeChange,
                    )
                }
                item { SettingsToggleCard("Media page", mediaPageEnabled, onMediaPageChange) }
                if (homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS) {
                    item { SettingsToggleCard("Apps button on Home screen", appsButtonEnabled, onAppsButtonChange) }
                }
                item { SettingsToggleCard("App labels", appLabelsEnabled, onAppLabelsChange) }
                item { SettingsToggleCard("Widget labels", widgetLabelsEnabled, onWidgetLabelsChange) }
                item { SettingsToggleCard("Swipe down for notification panel", swipeDownForNotifications, onSwipeDownChange) }
                item { SettingsToggleCard("Lock Home screen layout", lockHomeScreenLayout, onLockHomeScreenLayoutChange) }
                item {
                    SettingsSection(
                        title = "Behavior",
                        summary = "Samsung naming and expected defaults",
                        rows = listOf(
                            SettingRowState("Add new apps to Home screen", "On"),
                            SettingRowState("Badge notifications", "Dots and number"),
                            SettingRowState("About Home screen", "One UI Home clone prototype"),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    summary: String,
    rows: List<SettingRowState>,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = OneUiSurface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(title, color = OneUiText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(summary, color = OneUiTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            rows.forEachIndexed { index, row ->
                Column {
                    Text(row.title, color = OneUiText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(row.value, color = OneUiAccent, fontSize = 12.sp)
                }
                if (index != rows.lastIndex) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(OneUiBorder))
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsModeCard(
    title: String,
    selectedMode: HomeLayoutMode,
    onSelectMode: (HomeLayoutMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = OneUiSurface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Switch between Samsung's traditional Home and Apps screens setup and the simpler Home screen only layout.",
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeLayoutMode.entries.forEach { mode ->
                    SettingsCapsule(
                        label = mode.title,
                        onClick = { onSelectMode(mode) },
                        accent = selectedMode == mode,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = OneUiSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsCapsule(
    label: String,
    onClick: () -> Unit = {},
    accent: Boolean = true,
    enabled: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (accent) OneUiAccentSoft else OneUiSurface,
        shadowElevation = if (accent) 0.dp else 2.dp,
    ) {
        Text(
            label,
            color = if (accent) OneUiAccent else OneUiText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
