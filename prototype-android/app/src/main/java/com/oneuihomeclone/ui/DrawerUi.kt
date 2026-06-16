package com.oneuihomeclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun DrawerOverlay(
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
private fun finderActionIcon(type: FinderActionType) = when (type) {
    FinderActionType.SETTINGS -> Icons.Default.Settings
    FinderActionType.WALLPAPERS -> Icons.Default.Image
    FinderActionType.WIDGETS -> Icons.Default.Widgets
    FinderActionType.PAGE_MANAGER -> Icons.Default.Tune
    FinderActionType.MEDIA_PAGE -> Icons.Default.Apps
    FinderActionType.HOME_PAGE -> Icons.Default.Home
    FinderActionType.HIDE_APPS -> Icons.Default.Tune
}
