package com.oneuihomeclone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.R
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun DrawerOverlay(
    layoutContract: LauncherLayoutContract,
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
    shortcutResults: List<FinderActionItem>,
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
    onOpenAppActions: (CloneApp, AppContextSource) -> Unit,
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
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = layoutContract.drawerMaxWidth)
                .fillMaxHeight()
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = layoutContract.overlayHorizontalPadding, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        trimmedQuery.isNotBlank() -> stringResource(R.string.drawer_title_finder)
                        isHomeOnly -> stringResource(R.string.drawer_title_search)
                        else -> stringResource(R.string.apps)
                    },
                    color = OneUiText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = stringResource(R.string.settings), onClick = onOpenSettings)
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
                        FinderSectionHeader(stringResource(R.string.drawer_section_apps_screen))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.drawer_custom_order_summary),
                            color = OneUiTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        FinderAppGrid(
                            columns = layoutContract.appsGridColumns,
                            apps = selectedDrawerPage,
                            showLabels = appLabelsEnabled,
                            onOpenApp = onOpenApp,
                            onOpenAppActions = onOpenAppActions,
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
                            FinderSectionHeader(stringResource(R.string.drawer_recent_searches))
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
                                FinderSectionHeader(stringResource(R.string.drawer_home_only))
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    stringResource(R.string.drawer_home_only_summary),
                                    color = OneUiTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                            if (appsScreenApps.isNotEmpty()) {
                                item {
                                    FinderSectionHeader(stringResource(R.string.drawer_suggested_apps))
                                    Spacer(Modifier.height(12.dp))
                                    FinderAppGrid(
                                        columns = layoutContract.appsGridColumns,
                                        apps = appsScreenApps.take(layoutContract.appsGridColumns * 2),
                                        showLabels = appLabelsEnabled,
                                        onOpenApp = onOpenApp,
                                        onOpenAppActions = onOpenAppActions,
                                    )
                                }
                            }
                        } else {
                            item {
                                FinderSectionHeader(stringResource(R.string.drawer_section_apps_screen))
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    stringResource(R.string.drawer_alphabetical_summary),
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
                                        columns = layoutContract.appsGridColumns,
                                        apps = section.second,
                                        showLabels = appLabelsEnabled,
                                        onOpenApp = onOpenApp,
                                        onOpenAppActions = onOpenAppActions,
                                    )
                                }
                            }
                        }
                        if (recentSearches.isNotEmpty()) {
                            item {
                                FinderSectionHeader(stringResource(R.string.drawer_recent_searches))
                                Spacer(Modifier.height(10.dp))
                                FinderRecentSearches(
                                    searches = recentSearches,
                                    onSelectSearch = onSelectRecentSearch,
                                )
                            }
                        }
                    } else {
                        if (shortcutResults.isNotEmpty()) {
                            item {
                                FinderSectionHeader(stringResource(R.string.drawer_section_app_shortcuts))
                                Spacer(Modifier.height(10.dp))
                                FinderActionList(
                                    actions = shortcutResults,
                                    onOpenAction = onOpenAction,
                                )
                            }
                        }
                        if (actionResults.isNotEmpty()) {
                            item {
                                FinderSectionHeader(stringResource(R.string.drawer_suggested_actions))
                                Spacer(Modifier.height(10.dp))
                                FinderActionList(
                                    actions = actionResults,
                                    onOpenAction = onOpenAction,
                                )
                            }
                        }
                        if (settingResults.isNotEmpty()) {
                            item {
                                FinderSectionHeader(stringResource(R.string.settings))
                                Spacer(Modifier.height(10.dp))
                                FinderSettingsList(
                                    settings = settingResults,
                                    onOpenSetting = onOpenSettingResult,
                                )
                            }
                        }
                        if (apps.isNotEmpty()) {
                            item {
                                FinderSectionHeader(stringResource(R.string.apps))
                                Spacer(Modifier.height(12.dp))
                                FinderAppGrid(
                                    columns = layoutContract.appsGridColumns,
                                    apps = apps,
                                    showLabels = appLabelsEnabled,
                                    onOpenApp = onOpenApp,
                                    onOpenAppActions = onOpenAppActions,
                                )
                            }
                        }
                        if (apps.isEmpty() && settingResults.isEmpty() && actionResults.isEmpty() && shortcutResults.isEmpty()) {
                            item {
                                FinderEmptyState()
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
                    Text(if (trimmedQuery.isBlank()) stringResource(R.string.drawer_search_from_bottom) else stringResource(R.string.drawer_search_apps_settings))
                },
                singleLine = true,
                shape = OneUiControlShape,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search)) },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.drawer_close_hint),
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(12.dp))
            SettingsCapsule(label = stringResource(R.string.action_close), onClick = onClose, accent = false)
        }
    }
}

@Composable
private fun FinderEmptyState(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.drawer_no_matches), color = OneUiText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.drawer_no_matches_summary),
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
                label = mode.localizedTitle(),
                onClick = { onSelectSortMode(mode) },
                accent = drawerSortMode == mode,
            )
        }
        SettingsCapsule(
            label = if (hiddenAppCount == 0) stringResource(R.string.settings_hide_apps) else stringResource(R.string.drawer_hide_apps_count, hiddenAppCount),
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
            val actionDescription = stringResource(R.string.a11y_finder_action_result, action.title, action.summary)
            Surface(
                shape = OneUiPanelShape,
                color = OneUiSurface,
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .semantics { contentDescription = actionDescription }
                        .clickable(role = Role.Button) { onOpenAction(action) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(OneUiIconShape)
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
                        contentDescription = stringResource(R.string.action_open),
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
            val settingDescription = stringResource(R.string.a11y_finder_setting_result, setting.title, setting.category, setting.value)
            Surface(
                shape = OneUiPanelShape,
                color = OneUiSurface,
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .semantics { contentDescription = settingDescription }
                        .clickable(role = Role.Button) { onOpenSetting(setting) }
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
                        contentDescription = stringResource(R.string.action_open),
                        tint = OneUiTextSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FinderAppGrid(
    columns: Int,
    apps: List<CloneApp>,
    showLabels: Boolean,
    onOpenApp: (CloneApp) -> Unit,
    onOpenAppActions: (CloneApp, AppContextSource) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        apps.chunked(columns).forEach { rowApps ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowApps.forEach { app ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = app.accessibilityLabel() }
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { onOpenApp(app) },
                                onLongClick = { onOpenAppActions(app, AppContextSource.DRAWER) },
                            )
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIconBubble(app = app, size = 60.dp)
                        if (showLabels) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                app.name,
                                color = if (app.isLaunchable) OneUiText else OneUiTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                            AppStatusText(app)
                        }
                    }
                }
                repeat(columns - rowApps.size) {
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
    FinderActionType.APP_SHORTCUT -> Icons.Default.Apps
}
