package com.oneuihomeclone.ui

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.oneuihomeclone.LauncherApp
import com.oneuihomeclone.R
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
import com.oneuihomeclone.widgets.PreviewSource
import com.oneuihomeclone.widgets.WidgetPreviewLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt


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
internal fun rememberStatusClock(): StatusClock {
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
internal fun WallpaperAtmosphere() {
    val context = LocalContext.current
    // Decode the wallpaper off the main thread. First-frame render uses the gradient
    // glyphs alone; the real wallpaper fades in once the IO work completes. This avoids
    // a 200+ ms main-thread hitch on devices with large (≥3MP) wallpapers.
    val wallpaperBitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { readSystemWallpaper(context) }
    }

    Box(Modifier.fillMaxSize()) {
        // When the user has set us as HOME launcher we can read the system wallpaper
        // directly — render it as the full-bleed backdrop, then layer the One UI soft
        // contrast wash on top. When no wallpaper is accessible, the app-level
        // gradient remains the fallback.
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
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.06f),
                            Color(0xFFEAF2FF).copy(alpha = 0.32f),
                        ),
                    ),
                ),
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
internal fun HomeSurface(
    layoutContract: LauncherLayoutContract,
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
    onOpenAppActions: (CloneApp, AppContextSource) -> Unit,
    onOpenFolder: (FolderModel) -> Unit,
    onMoveWidget: (Int, Int, Int) -> Unit,
    onResizeWidget: (Int, Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onOpenWidgetActions: (WidgetTemplateModel) -> Unit,
    onPageChange: (Int) -> Unit,
) {
    val drawerGestureEnabled = homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = layoutContract.homeHorizontalPadding, vertical = layoutContract.homeVerticalPadding)
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
        contentAlignment = Alignment.TopCenter,
    ) {
        if (layoutContract.isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = layoutContract.homeMaxWidth),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                ) {
                    StatusRow(
                        timeText = timeText,
                        dateText = dateText,
                        homeLayoutMode = homeLayoutMode,
                        lockHomeScreenLayout = lockHomeScreenLayout,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (isMediaPage) {
                        MediaPageHero()
                    } else {
                        currentHomePage?.let { WidgetHeroCard(it) }
                        currentHomePage?.takeIf { it.widgets.isNotEmpty() }?.let { page ->
                            Spacer(Modifier.height(10.dp))
                            WidgetGrid(
                                layoutContract = layoutContract,
                                widgets = page.widgets,
                                showLabels = widgetLabelsEnabled,
                                canEdit = !lockHomeScreenLayout,
                                onMoveWidget = onMoveWidget,
                                onResizeWidget = onResizeWidget,
                                onRemoveWidget = onRemoveWidget,
                                onOpenWidgetActions = onOpenWidgetActions,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                ) {
                    if (isMediaPage) {
                        MediaPageCards()
                    } else {
                        HomeGrid(
                            layoutContract = layoutContract,
                            items = currentHomePage?.items.orEmpty(),
                            showLabels = appLabelsEnabled,
                            compactLayout = currentHomePage?.widgets?.isNotEmpty() == true,
                            canOrganize = !lockHomeScreenLayout,
                            onReorderItem = onReorderHomeItem,
                            onCreateFolder = onCreateFolder,
                            onAddAppToFolder = onAddAppToFolder,
                            onDragStateChange = onHomeItemDragStateChange,
                            onOpenApp = onOpenApp,
                            onOpenAppActions = onOpenAppActions,
                            onOpenFolder = onOpenFolder,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    PageStrip(
                        pageIndex = pageIndex,
                        pageCount = pageCount,
                        onPageChange = onPageChange,
                    )
                    Spacer(Modifier.height(8.dp))
                    SearchBarButton(
                        label = if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) stringResource(R.string.home_search_apps) else stringResource(R.string.drawer_title_finder),
                        onOpenDrawer = onOpenDrawer,
                    )
                    Spacer(Modifier.height(8.dp))
                    DockBar(
                        apps = dockApps,
                        showLabels = appLabelsEnabled,
                        appsButtonEnabled = homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS && appsButtonEnabled,
                        onOpenApp = onOpenApp,
                        onOpenAppActions = onOpenAppActions,
                        onOpenDrawer = onOpenDrawer,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = layoutContract.homeMaxWidth),
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
                        WidgetGrid(
                            layoutContract = layoutContract,
                            widgets = page.widgets,
                            showLabels = widgetLabelsEnabled,
                            canEdit = !lockHomeScreenLayout,
                            onMoveWidget = onMoveWidget,
                            onResizeWidget = onResizeWidget,
                            onRemoveWidget = onRemoveWidget,
                            onOpenWidgetActions = onOpenWidgetActions,
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    HomeGrid(
                        layoutContract = layoutContract,
                        items = currentHomePage?.items.orEmpty(),
                        showLabels = appLabelsEnabled,
                        compactLayout = currentHomePage?.widgets?.isNotEmpty() == true,
                        canOrganize = !lockHomeScreenLayout,
                        onReorderItem = onReorderHomeItem,
                        onCreateFolder = onCreateFolder,
                        onAddAppToFolder = onAddAppToFolder,
                        onDragStateChange = onHomeItemDragStateChange,
                        onOpenApp = onOpenApp,
                        onOpenAppActions = onOpenAppActions,
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
                SearchBarButton(
                    label = if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) stringResource(R.string.home_search_apps) else stringResource(R.string.drawer_title_finder),
                    onOpenDrawer = onOpenDrawer,
                )
                Spacer(Modifier.height(14.dp))
                DockBar(
                    apps = dockApps,
                    showLabels = appLabelsEnabled,
                    appsButtonEnabled = homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS && appsButtonEnabled,
                    onOpenApp = onOpenApp,
                    onOpenAppActions = onOpenAppActions,
                    onOpenDrawer = onOpenDrawer,
                )
            }
        }
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
        Surface(color = OneUiCard, shape = OneUiControlShape, shadowElevation = 1.dp) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(OneUiMicroShape).background(OneUiPositive))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (lockHomeScreenLayout) stringResource(R.string.home_layout_locked) else homeLayoutMode.localizedTitle(),
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
        shape = OneUiPanelShape,
        color = OneUiCard,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(stringResource(R.string.settings_media_page), color = OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.home_media_hub), color = OneUiText, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.home_media_body),
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
                body = stringResource(R.string.home_media_card_body),
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
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
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
        shape = OneUiPanelShape,
        color = OneUiCard,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(page.eyebrow, color = OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(page.value, color = OneUiText, fontSize = 50.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(OneUiMicroShape).background(OneUiAccent))
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
private fun WidgetGrid(
    layoutContract: LauncherLayoutContract,
    widgets: List<WidgetTemplateModel>,
    showLabels: Boolean,
    canEdit: Boolean,
    onMoveWidget: (Int, Int, Int) -> Unit,
    onResizeWidget: (Int, Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onOpenWidgetActions: (WidgetTemplateModel) -> Unit,
) {
    val gridColumns = layoutContract.widgetGridColumns
    val maxRows = layoutContract.widgetGridMaxRows
    val placedWidgets = remember(widgets, gridColumns, maxRows) {
        placeWidgetsInGrid(widgets, columns = gridColumns, maxRows = maxRows)
    }
    val gridRows = (placedWidgets.maxOfOrNull { it.cellY + it.spanY } ?: 1).coerceIn(1, maxRows)
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier
            .fillMaxWidth()
            .height((gridRows * layoutContract.widgetGridCellHeight.value).dp),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = placedWidgets,
            key = { widget -> widget.stableWidgetKey() },
            span = { widget -> GridItemSpan(widget.spanX.coerceIn(1, gridColumns)) },
        ) { widget ->
            WidgetGridTile(
                layoutContract = layoutContract,
                widget = widget,
                showLabels = showLabels,
                canEdit = canEdit,
                onMoveWidget = onMoveWidget,
                onResizeWidget = onResizeWidget,
                onRemoveWidget = onRemoveWidget,
                onOpenWidgetActions = onOpenWidgetActions,
            )
        }
    }
}

@Composable
private fun WidgetGridTile(
    layoutContract: LauncherLayoutContract,
    widget: WidgetTemplateModel,
    showLabels: Boolean,
    canEdit: Boolean,
    onMoveWidget: (Int, Int, Int) -> Unit,
    onResizeWidget: (Int, Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onOpenWidgetActions: (WidgetTemplateModel) -> Unit,
) {
    val hostWidgetId = widget.hostWidgetId
    val boundProviderMissing = hostWidgetId != null && widget.providerInfo == null
    val tileHeight = (layoutContract.widgetGridCellHeight.value - 6f).coerceAtLeast(58f)
    Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onOpenWidgetActions(widget) },
                    role = Role.Button,
                )
                .height((widget.spanY.coerceIn(1, layoutContract.widgetGridMaxRows) * tileHeight).dp),
        shape = OneUiPanelShape,
        color = OneUiCard,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(widget.accent.copy(alpha = 0.12f), Color.White.copy(alpha = 0.82f)),
                        start = Offset.Zero,
                        end = Offset(900f, 260f),
                    ),
                )
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showLabels) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = widget.title,
                            color = OneUiText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = widget.span,
                            color = OneUiTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (hostWidgetId != null && canEdit) {
                    WidgetGridControls(
                        layoutContract = layoutContract,
                        widget = widget,
                        onMoveWidget = onMoveWidget,
                        onResizeWidget = onResizeWidget,
                        onRemoveWidget = onRemoveWidget,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (boundProviderMissing) {
                WidgetUnavailablePane(
                    widget = widget,
                    onRemoveWidget = {
                        hostWidgetId?.let(onRemoveWidget)
                    },
                )
            } else {
                WidgetPreviewPane(
                    widget = widget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    compact = false,
                )
            }
        }
    }
}

@Composable
private fun WidgetGridControls(
    layoutContract: LauncherLayoutContract,
    widget: WidgetTemplateModel,
    onMoveWidget: (Int, Int, Int) -> Unit,
    onResizeWidget: (Int, Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    val hostWidgetId = widget.hostWidgetId ?: return
    val moveLeftDescription = stringResource(R.string.a11y_widget_move_left)
    val moveRightDescription = stringResource(R.string.a11y_widget_move_right)
    val moveUpDescription = stringResource(R.string.a11y_widget_move_up)
    val moveDownDescription = stringResource(R.string.a11y_widget_move_down)
    val resizeNarrowerDescription = stringResource(R.string.a11y_widget_resize_narrower)
    val resizeWiderDescription = stringResource(R.string.a11y_widget_resize_wider)
    val resizeShorterDescription = stringResource(R.string.a11y_widget_resize_shorter)
    val resizeTallerDescription = stringResource(R.string.a11y_widget_resize_taller)
    val removeDescription = stringResource(R.string.a11y_widget_remove)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetGridControlButton(
            label = "<",
            contentDescription = moveLeftDescription,
            enabled = widget.cellX > 0,
            onClick = { onMoveWidget(hostWidgetId, -1, 0) },
        )
        WidgetGridControlButton(
            label = ">",
            contentDescription = moveRightDescription,
            enabled = widget.cellX + widget.spanX < layoutContract.widgetGridColumns,
            onClick = { onMoveWidget(hostWidgetId, 1, 0) },
        )
        WidgetGridControlButton(
            label = "^",
            contentDescription = moveUpDescription,
            enabled = widget.cellY > 0,
            onClick = { onMoveWidget(hostWidgetId, 0, -1) },
        )
        WidgetGridControlButton(
            label = "v",
            contentDescription = moveDownDescription,
            enabled = widget.cellY + widget.spanY < layoutContract.widgetGridMaxRows,
            onClick = { onMoveWidget(hostWidgetId, 0, 1) },
        )
        WidgetGridControlButton(
            label = "-",
            contentDescription = resizeNarrowerDescription,
            enabled = widget.canResizeHorizontal && widget.spanX > widget.minSpanX,
            onClick = { onResizeWidget(hostWidgetId, -1, 0) },
        )
        WidgetGridControlButton(
            label = "+",
            contentDescription = resizeWiderDescription,
            enabled = widget.canResizeHorizontal && widget.spanX < widget.maxSpanX,
            onClick = { onResizeWidget(hostWidgetId, 1, 0) },
        )
        WidgetGridControlButton(
            label = "V-",
            contentDescription = resizeShorterDescription,
            enabled = widget.canResizeVertical && widget.spanY > widget.minSpanY,
            onClick = { onResizeWidget(hostWidgetId, 0, -1) },
        )
        WidgetGridControlButton(
            label = "V+",
            contentDescription = resizeTallerDescription,
            enabled = widget.canResizeVertical && widget.spanY < widget.maxSpanY,
            onClick = { onResizeWidget(hostWidgetId, 0, 1) },
        )
        WidgetGridControlButton(
            label = "X",
            contentDescription = removeDescription,
            enabled = true,
            onClick = { onRemoveWidget(hostWidgetId) },
        )
    }
}

@Composable
private fun WidgetGridControlButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(26.dp),
        shape = OneUiMicroShape,
        color = if (enabled) OneUiSurface else OneUiSurfaceSoft.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, OneUiBorder.copy(alpha = if (enabled) 0.5f else 0.25f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { this.contentDescription = contentDescription }
                .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (enabled) OneUiText else OneUiTextSecondary.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WidgetUnavailablePane(
    widget: WidgetTemplateModel,
    onRemoveWidget: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(OneUiPanelShape)
            .background(Color.White.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_widget_unavailable),
            color = OneUiText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = widget.summary,
            color = OneUiTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = OneUiControlShape,
            color = OneUiSurface,
            border = BorderStroke(1.dp, OneUiBorder.copy(alpha = 0.45f)),
        ) {
            Text(
                text = stringResource(R.string.action_remove),
                color = OneUiText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onRemoveWidget)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
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
                shape = OneUiPanelShape,
                color = OneUiCard,
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(widget.accent.copy(alpha = 0.14f), Color.White),
                                start = Offset.Zero,
                                end = Offset(900f, 260f),
                            ),
                        )
                        .heightIn(min = 168.dp)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    if (showLabels) {
                        Text(
                            text = widget.category,
                            color = widget.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = widget.title,
                        color = OneUiText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(OneUiControlShape)
                            .background(Color.White.copy(alpha = 0.58f)),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        WidgetPreviewPane(
                            widget = widget,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(
                                    width = if (widget.spanY == 1) 88.dp else 94.dp,
                                    height = if (widget.spanY == 1) 42.dp else 50.dp,
                                ),
                            compact = true,
                        )
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
internal fun WidgetPreviewPane(
    widget: WidgetTemplateModel,
    modifier: Modifier,
    compact: Boolean,
) {
    val context = LocalContext.current.applicationContext
    val hostWidgetId = widget.hostWidgetId
    val providerInfo = widget.providerInfo
    val resolvedPreview: PreviewSource by produceState(
        initialValue = widget.previewSource,
        key1 = providerInfo?.provider?.flattenToShortString(),
        key2 = widget.previewSource,
    ) {
        if (providerInfo != null && hostWidgetId == null && widget.previewSource == PreviewSource.Empty) {
            value = withContext(Dispatchers.IO) {
                WidgetPreviewLoader.load(context, providerInfo)
            }
        }
    }
    when {
        hostWidgetId != null && providerInfo != null -> BoundWidgetPreview(
            widgetId = hostWidgetId,
            providerInfo = providerInfo,
            modifier = modifier,
        )
        resolvedPreview is PreviewSource.RemoteLayout -> RemoteLayoutPreview(
            preview = resolvedPreview as PreviewSource.RemoteLayout,
            modifier = modifier,
        )
        resolvedPreview is PreviewSource.PreviewImage -> DrawableWidgetPreview(
            preview = resolvedPreview,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
        resolvedPreview is PreviewSource.ProviderIcon -> DrawableWidgetPreview(
            preview = resolvedPreview,
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
            .clip(OneUiPanelShape)
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
            .clip(OneUiPanelShape)
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
                .clip(OneUiPanelShape)
                .background(Color.White.copy(alpha = 0.82f)),
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = modifier
                .clip(OneUiPanelShape)
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
            .clip(OneUiPanelShape)
            .background(widget.accent.copy(alpha = 0.1f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        repeat(barCount) { thumb ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (thumb == 0) 1f else 0.72f)
                    .height(if (compact) 8.dp else 12.dp)
                    .clip(OneUiMicroShape)
                    .background(widget.accent.copy(alpha = 0.14f + (thumb * 0.04f))),
            )
        }
    }
}

@Composable
private fun HomeGrid(
    layoutContract: LauncherLayoutContract,
    items: List<HomeGridItemModel>,
    showLabels: Boolean,
    compactLayout: Boolean,
    canOrganize: Boolean,
    onReorderItem: (String, String) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onAddAppToFolder: (String, String) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onOpenApp: (CloneApp) -> Unit,
    onOpenAppActions: (CloneApp, AppContextSource) -> Unit,
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
            .height(if (compactLayout) layoutContract.compactHomeGridHeight else layoutContract.homeGridHeight),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(layoutContract.homeGridColumns),
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
                        .clip(OneUiPanelShape)
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
                                    } else if (!hasDragged && sourceItem is AppItemModel) {
                                        onOpenAppActions(sourceItem.app, AppContextSource.HOME)
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
                                is AppItemModel -> Modifier
                                    .semantics { contentDescription = item.app.accessibilityLabel() }
                                    .combinedClickable(
                                        role = Role.Button,
                                        onClick = { onOpenApp(item.app) },
                                        onLongClick = { onOpenAppActions(item.app, AppContextSource.HOME) },
                                    )
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
                            color = if (item is AppItemModel && !item.app.isLaunchable) OneUiTextSecondary else OneUiText,
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
                        if (item is AppItemModel) {
                            AppStatusText(item.app)
                        }
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
    val openFolderDescription = stringResource(R.string.a11y_open_folder, folder.title)
    val folderBadge = remember(folder.apps) {
        val badgedApps = folder.apps.filter { it.notificationBadge.isVisible }
        if (badgedApps.isEmpty()) {
            NotificationBadgeState.None
        } else {
            NotificationBadgeState(
                count = badgedApps.sumOf { it.notificationBadge.count },
                showNumber = badgedApps.any { it.notificationBadge.showNumber },
            )
        }
    }
    Surface(
        modifier = Modifier.size(62.dp),
        shape = OneUiIconShape,
        color = OneUiCard.copy(alpha = 0.94f),
        shadowElevation = 1.dp,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = openFolderDescription }
                    .clickable(role = Role.Button) { onOpenFolder(folder) }
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
                                        contentDescription = app.accessibilityLabel(),
                                        modifier = Modifier.size(18.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(18.dp)
                                            .clip(OneUiMicroShape)
                                            .background(app.color),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            NotificationBadge(
                badge = folderBadge,
                modifier = Modifier.align(Alignment.TopStart),
            )
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
            val pageDescription = stringResource(R.string.a11y_page_indicator, index + 1, pageCount)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (selected) 20.dp else 7.dp, height = 7.dp)
                    .clip(OneUiMicroShape)
                    .background(if (selected) OneUiText.copy(alpha = 0.76f) else OneUiTextSecondary.copy(alpha = 0.22f))
                    .semantics {
                        contentDescription = pageDescription
                        this.selected = selected
                    }
                    .clickable(role = Role.Button) { onPageChange(index) },
            )
        }
    }
}

@Composable
private fun SearchBarButton(
    label: String,
    onOpenDrawer: () -> Unit,
) {
    val openDescription = stringResource(R.string.a11y_open_label, label)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OneUiControlShape,
        color = OneUiCard,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = openDescription }
                .clickable(role = Role.Button, onClick = onOpenDrawer)
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
    onOpenAppActions: (CloneApp, AppContextSource) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val openAppsDescription = stringResource(R.string.a11y_open_apps_screen)
    val appsLabel = stringResource(R.string.apps)
    val dockItems: List<CloneApp?> = if (appsButtonEnabled) {
        apps + listOf<CloneApp?>(null)
    } else {
        apps
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OneUiPanelShape,
        color = OneUiCard,
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dockItems.forEach { app ->
                val isAppsButton = app == null
                Column(
                    modifier = if (isAppsButton) {
                        Modifier
                    } else {
                        Modifier.combinedClickable(
                            role = Role.Button,
                            onClick = { app?.let(onOpenApp) },
                            onLongClick = { app?.let { onOpenAppActions(it, AppContextSource.DOCK) } },
                        )
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isAppsButton) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = OneUiIconShape,
                            color = OneUiSurfaceSoft,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .semantics { contentDescription = openAppsDescription }
                                    .clickable(role = Role.Button, onClick = onOpenDrawer),
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
                            text = if (isAppsButton) appsLabel else app?.name.orEmpty(),
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
