package com.oneuihomeclone.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.appwidget.AppWidgetProviderInfo
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.oneuihomeclone.LauncherApp
import com.oneuihomeclone.data.BoundWidget
import com.oneuihomeclone.data.PersistedHomeItem
import com.oneuihomeclone.data.PersistedHomePage
import com.oneuihomeclone.data.PersistedLauncherLayout
import com.oneuihomeclone.widgets.PreviewSource
import java.util.Locale

internal fun totalPageCount(homePageCount: Int, mediaPageEnabled: Boolean): Int {
    return homePageCount + if (mediaPageEnabled) 1 else 0
}

internal fun visualIndexForHomePage(homePageIndex: Int, mediaPageEnabled: Boolean): Int {
    return if (mediaPageEnabled) homePageIndex + 1 else homePageIndex
}

internal fun homePageIndexFromVisual(pageIndex: Int, mediaPageEnabled: Boolean): Int? {
    return if (mediaPageEnabled) {
        if (pageIndex == 0) null else pageIndex - 1
    } else {
        pageIndex
    }
}

internal fun <T> moveListItem(
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

internal fun movedIndexForSwap(
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

internal fun applyHiddenAppsToPages(
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

internal fun applyNotificationBadges(
    apps: List<CloneApp>,
    countsByPackage: Map<String, Int>,
    mode: NotificationBadgeMode,
    accessGranted: Boolean,
): List<CloneApp> {
    if (apps.isEmpty()) return apps
    if (!accessGranted || mode == NotificationBadgeMode.OFF || countsByPackage.isEmpty()) {
        return apps.map { app ->
            if (app.notificationBadge == NotificationBadgeState.None) app else app.copy(notificationBadge = NotificationBadgeState.None)
        }
    }

    val showNumber = mode == NotificationBadgeMode.DOTS_AND_NUMBER
    return apps.map { app ->
        val count = app.notificationPackageName()
            ?.let { packageName -> countsByPackage[packageName] }
            ?.coerceAtLeast(0)
            ?: 0
        val nextBadge = if (count > 0) {
            NotificationBadgeState(count = count, showNumber = showNumber)
        } else {
            NotificationBadgeState.None
        }
        if (app.notificationBadge == nextBadge) app else app.copy(notificationBadge = nextBadge)
    }
}

private fun CloneApp.notificationPackageName(): String? =
    packageName
        ?: launchTarget?.componentName?.packageName
        ?: launchIntent?.component?.packageName

internal fun homeGridContainsApp(items: List<HomeGridItemModel>, appId: String): Boolean =
    items.any { item ->
        when (item) {
            is AppItemModel -> item.app.id == appId
            is FolderModel -> item.apps.any { app -> app.id == appId }
        }
    }

internal fun homePagesContainApp(pages: List<HomePageModel>, appId: String): Boolean =
    pages.any { page -> homeGridContainsApp(page.items, appId) }

internal fun canAddAppToHomePage(
    page: HomePageModel?,
    pages: List<HomePageModel>,
    app: CloneApp,
    layoutLocked: Boolean,
): Boolean =
    page != null &&
        !layoutLocked &&
        !homePagesContainApp(pages, app.id) &&
        page.items.size < MAX_HOME_GRID_ITEMS

internal fun addAppToHomePageItems(
    items: List<HomeGridItemModel>,
    app: CloneApp,
): List<HomeGridItemModel> {
    if (homeGridContainsApp(items, app.id) || items.size >= MAX_HOME_GRID_ITEMS) {
        return items
    }
    return items + AppItemModel(app)
}

internal data class AutoAddNewAppsResult(
    val pages: List<HomePageModel>,
    val nextPageId: Int,
    val handledAppIds: Set<String>,
    val placedAppIds: Set<String>,
)

internal fun placeNewAppsOnHomePages(
    pages: List<HomePageModel>,
    newApps: List<CloneApp>,
    nextPageId: Int,
    enabled: Boolean,
    layoutLocked: Boolean,
): AutoAddNewAppsResult {
    val distinctNewApps = newApps.distinctBy(CloneApp::id)
    if (distinctNewApps.isEmpty()) {
        return AutoAddNewAppsResult(
            pages = pages,
            nextPageId = nextPageId,
            handledAppIds = emptySet(),
            placedAppIds = emptySet(),
        )
    }
    if (!enabled || layoutLocked) {
        return AutoAddNewAppsResult(
            pages = pages,
            nextPageId = nextPageId,
            handledAppIds = distinctNewApps.mapTo(mutableSetOf(), CloneApp::id),
            placedAppIds = emptySet(),
        )
    }

    val mutablePages = pages.toMutableList()
    var nextAvailablePageId = maxOf(nextPageId, (pages.maxOfOrNull(HomePageModel::id) ?: 0) + 1)
    val handledAppIds = mutableSetOf<String>()
    val placedAppIds = mutableSetOf<String>()

    distinctNewApps
        .filter { app -> app.isLaunchable && !app.isRestoredPlaceholder }
        .forEach { app ->
            handledAppIds += app.id
            if (homePagesContainApp(mutablePages, app.id)) {
                return@forEach
            }
            var targetPageIndex = mutablePages.indexOfFirst { page -> page.items.size < MAX_HOME_GRID_ITEMS }
            if (targetPageIndex == -1) {
                mutablePages += buildAutoAddedHomePage(nextAvailablePageId)
                nextAvailablePageId += 1
                targetPageIndex = mutablePages.lastIndex
            }
            val targetPage = mutablePages[targetPageIndex]
            val nextItems = addAppToHomePageItems(targetPage.items, app)
            if (nextItems.size > targetPage.items.size) {
                placedAppIds += app.id
            }
            mutablePages[targetPageIndex] = targetPage.copy(items = nextItems)
        }

    return AutoAddNewAppsResult(
        pages = mutablePages,
        nextPageId = nextAvailablePageId,
        handledAppIds = handledAppIds,
        placedAppIds = placedAppIds,
    )
}

internal fun removeAppFromHomePageItems(
    items: List<HomeGridItemModel>,
    appId: String,
): List<HomeGridItemModel> =
    items.filterNot { item -> item is AppItemModel && item.app.id == appId }

internal fun buildAppContextActions(
    source: AppContextSource,
    isHidden: Boolean,
    canOpenAppInfo: Boolean,
    canAddToHome: Boolean,
    canRemoveFromHome: Boolean,
    shortcuts: List<LauncherShortcutAction>,
    text: LauncherContextActionText = LauncherContextActionText(),
): List<LauncherContextAction> =
    buildList {
        add(
            LauncherContextAction(
                type = LauncherContextActionType.APP_INFO,
                title = text.appInfo,
                summary = if (canOpenAppInfo) text.appInfoSummary else text.appInfoUnavailableSummary,
                enabled = canOpenAppInfo,
            ),
        )
        shortcuts.take(MAX_CONTEXT_SHORTCUTS).forEach { shortcut ->
            add(
                LauncherContextAction(
                    type = LauncherContextActionType.SHORTCUT,
                    title = shortcut.shortLabel,
                    summary = shortcut.longLabel?.takeIf { it != shortcut.shortLabel }
                        ?: shortcut.disabledMessage
                        ?: text.shortcutSummary,
                    enabled = shortcut.isEnabled,
                    shortcut = shortcut,
                ),
            )
        }
        if (source != AppContextSource.HOME && !isHidden) {
            add(
                LauncherContextAction(
                    type = LauncherContextActionType.ADD_TO_HOME,
                    title = text.addToHome,
                    summary = if (canAddToHome) text.addToHomeSummary else text.addToHomeUnavailableSummary,
                    enabled = canAddToHome,
                ),
            )
        }
        if (canRemoveFromHome) {
            add(
                LauncherContextAction(
                    type = LauncherContextActionType.REMOVE_FROM_HOME,
                    title = text.removeFromHome,
                    summary = text.removeFromHomeSummary,
                ),
            )
        }
        add(
            if (isHidden) {
                LauncherContextAction(
                    type = LauncherContextActionType.RESTORE_APP,
                    title = text.restoreApp,
                    summary = text.restoreAppSummary,
                )
            } else {
                LauncherContextAction(
                    type = LauncherContextActionType.HIDE_APP,
                    title = text.hideApp,
                    summary = text.hideAppSummary,
                )
            },
        )
    }

internal fun buildVisibleDockApps(
    preferredDockApps: List<CloneApp>,
    allApps: List<CloneApp>,
    hiddenAppIds: Set<String>,
): List<CloneApp> {
    val visiblePreferred = preferredDockApps.filterNot { it.id in hiddenAppIds }
    val visibleFallback = allApps
        .filterNot { it.id in hiddenAppIds || it.id in visiblePreferred.map(CloneApp::id).toSet() }
    return (visiblePreferred + visibleFallback).take(4)
}

internal fun addWidgetToPage(
    widgets: List<WidgetTemplateModel>,
    widget: WidgetTemplateModel,
    columns: Int = 4,
    maxRows: Int = 6,
): List<WidgetTemplateModel> {
    return placeWidgetsInGrid(
        widgets.filterNot { it.stableWidgetKey() == widget.stableWidgetKey() } + widget,
        columns = columns,
        maxRows = maxRows,
    )
}

internal fun removeWidgetFromPage(
    widgets: List<WidgetTemplateModel>,
    hostWidgetId: Int,
): List<WidgetTemplateModel> =
    widgets.filterNot { it.hostWidgetId == hostWidgetId }

internal fun removeWidgetFromPageByKey(
    widgets: List<WidgetTemplateModel>,
    stableWidgetKey: String,
): List<WidgetTemplateModel> =
    widgets.filterNot { it.stableWidgetKey() == stableWidgetKey }

internal fun buildWidgetContextActions(
    widget: WidgetTemplateModel,
    canEdit: Boolean,
    text: LauncherContextActionText = LauncherContextActionText(),
): List<LauncherContextAction> =
    buildList {
        val canOpenSettings = widgetConfigureIntent(widget) != null
        add(
            LauncherContextAction(
                type = LauncherContextActionType.WIDGET_SETTINGS,
                title = text.widgetSettings,
                summary = if (canOpenSettings) text.widgetSettingsSummary else text.widgetSettingsUnavailableSummary,
                enabled = canOpenSettings,
            ),
        )
        add(
            LauncherContextAction(
                type = LauncherContextActionType.REMOVE_WIDGET,
                title = text.removeWidget,
                summary = text.removeWidgetSummary,
                enabled = canEdit,
            ),
        )
    }

internal fun resizeWidgetInPage(
    widgets: List<WidgetTemplateModel>,
    hostWidgetId: Int,
    deltaX: Int,
    deltaY: Int,
    columns: Int = 4,
    maxRows: Int = 6,
): List<WidgetTemplateModel> =
    placeWidgetsInGrid(
        widgets.map { widget ->
            if (widget.hostWidgetId != hostWidgetId) {
                widget
            } else {
                val nextSpanX = if (widget.canResizeHorizontal) {
                    (widget.spanX + deltaX).coerceIn(widget.minSpanX, widget.maxSpanX)
                } else {
                    widget.spanX
                }
                val nextSpanY = if (widget.canResizeVertical) {
                    (widget.spanY + deltaY).coerceIn(widget.minSpanY, widget.maxSpanY)
                } else {
                    widget.spanY
                }
                widget.copy(
                    span = widgetSpanLabel(nextSpanX, nextSpanY),
                    spanX = nextSpanX,
                    spanY = nextSpanY,
                )
            }
        },
        columns = columns,
        maxRows = maxRows,
    )

internal fun moveWidgetInPage(
    widgets: List<WidgetTemplateModel>,
    hostWidgetId: Int,
    deltaX: Int,
    deltaY: Int,
    columns: Int = 4,
    maxRows: Int = 6,
): List<WidgetTemplateModel> =
    placeWidgetsInGrid(
        widgets.map { widget ->
            if (widget.hostWidgetId == hostWidgetId) {
                widget.copy(
                    cellX = widget.cellX + deltaX,
                    cellY = widget.cellY + deltaY,
                )
            } else {
                widget
            }
        },
        columns = columns,
        maxRows = maxRows,
    )

internal fun placeWidgetsInGrid(
    widgets: List<WidgetTemplateModel>,
    columns: Int = 4,
    maxRows: Int = 6,
): List<WidgetTemplateModel> {
    if (widgets.isEmpty()) return emptyList()
    val occupied = mutableSetOf<Pair<Int, Int>>()
    val placed = mutableListOf<WidgetTemplateModel>()
    widgets
        .map { it.normalizedWidgetBounds(columns, maxRows) }
        .sortedWith(compareBy<WidgetTemplateModel> { it.cellY }.thenBy { it.cellX }.thenBy { it.stableWidgetKey() })
        .forEach { widget ->
            val preferred = widget.cellX to widget.cellY
            val cell = if (canPlaceWidget(preferred.first, preferred.second, widget, columns, maxRows, occupied)) {
                preferred
            } else {
                firstAvailableWidgetCell(widget, columns, maxRows, occupied)
            }
            placed += widget.copy(cellX = cell.first, cellY = cell.second)
            markWidgetCells(cell.first, cell.second, widget, occupied)
        }
    return placed.sortedWith(compareBy<WidgetTemplateModel> { it.cellY }.thenBy { it.cellX }.thenBy { it.stableWidgetKey() })
}

internal fun buildWidgetCategories(widgets: List<WidgetTemplateModel>): List<String> {
    val providerCategories = widgets
        .map { it.category }
        .filterNot { it == "Recommended" || it == "All" }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    val templateCategories = widgets
        .map { it.category }
        .filterNot { it == "All" }
        .distinct()
        .filter { it !in providerCategories }
    return (listOf("Recommended", "All") + providerCategories + templateCategories)
        .distinct()
}

internal fun filterWidgetsForCategory(
    widgets: List<WidgetTemplateModel>,
    selectedCategory: String,
): List<WidgetTemplateModel> {
    return when (selectedCategory) {
        "All" -> widgets
        "Recommended" -> widgets.filter { it.category == "Recommended" }.ifEmpty { widgets.take(8) }
        else -> widgets.filter { it.category == selectedCategory }
    }
}

internal fun filterWidgetsForPicker(
    widgets: List<WidgetTemplateModel>,
    selectedCategory: String,
    query: String,
): List<WidgetTemplateModel> {
    val categoryMatches = filterWidgetsForCategory(widgets, selectedCategory)
    val terms = query.trim()
        .lowercase(Locale.getDefault())
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (terms.isEmpty()) return categoryMatches

    return categoryMatches.filter { widget ->
        val searchable = widget.widgetSearchText().lowercase(Locale.getDefault())
        terms.all(searchable::contains)
    }
}

internal fun WidgetTemplateModel.requiresConfiguration(): Boolean =
    providerInfo?.configure != null

internal fun WidgetTemplateModel.isProviderUnavailable(): Boolean =
    providerInfo == null && restoredProviderPackage != null

internal fun WidgetTemplateModel.hasPreviewFallback(): Boolean =
    providerInfo != null && previewSource == PreviewSource.Empty

private fun WidgetTemplateModel.widgetSearchText(): String =
    listOfNotNull(
        title,
        summary,
        category,
        span,
        providerInfo?.provider?.packageName,
        providerInfo?.provider?.className,
        restoredProviderPackage,
        restoredProviderClass,
    ).joinToString(" ")

internal fun mergeBoundWidgetsIntoPages(
    pages: List<HomePageModel>,
    boundWidgets: List<BoundWidget>,
    templates: List<WidgetTemplateModel>,
): List<HomePageModel> {
    if (boundWidgets.isEmpty() || pages.isEmpty()) return pages
    val byPage = boundWidgets.groupBy { it.pageIndex.coerceIn(0, pages.lastIndex) }
    return pages.mapIndexed { index, page ->
        val restoredWidgets = byPage[index]
            .orEmpty()
            .map { bound -> bound.toWidgetModel(templates) }
        if (restoredWidgets.isEmpty()) {
            page
        } else {
            val volatileWidgets = page.widgets.filter { it.hostWidgetId == null }
            page.copy(widgets = (volatileWidgets + restoredWidgets).distinctBy { it.stableWidgetKey() }.takeLast(3))
        }
    }
}

internal fun BoundWidget.toWidgetModel(templates: List<WidgetTemplateModel>): WidgetTemplateModel {
    val providerKey = "$providerPackage/$providerClass"
    val template = templates.firstOrNull { it.providerKey() == providerKey }
    return if (template != null) {
        template.copy(
            hostWidgetId = hostWidgetId,
            cellX = cellX,
            cellY = cellY,
            span = widgetSpanLabel(spanX, spanY),
            spanX = spanX,
            spanY = spanY,
        )
    } else {
        WidgetTemplateModel(
            title = providerClass.substringAfterLast('.'),
            summary = "Restored widget from ${providerPackage.substringAfterLast('.')}",
            category = providerPackage.substringAfterLast('.').replaceFirstChar(Char::titlecase),
            span = widgetSpanLabel(spanX, spanY),
            accent = fallbackColorFor(providerKey),
            hostWidgetId = hostWidgetId,
            cellX = cellX,
            cellY = cellY,
            spanX = spanX,
            spanY = spanY,
            minSpanX = 1,
            minSpanY = 1,
            maxSpanX = 4,
            maxSpanY = 4,
            restoredProviderPackage = providerPackage,
            restoredProviderClass = providerClass,
        )
    }
}

internal fun WidgetTemplateModel.toBoundWidget(
    widgetId: Int,
    pageIndex: Int,
): BoundWidget? {
    val provider = providerInfo?.provider
    val providerPackage = provider?.packageName ?: restoredProviderPackage ?: return null
    val providerClass = provider?.className ?: restoredProviderClass ?: return null
    return BoundWidget(
        hostWidgetId = widgetId,
        providerPackage = providerPackage,
        providerClass = providerClass,
        pageIndex = pageIndex,
        cellX = cellX,
        cellY = cellY,
        spanX = spanX,
        spanY = spanY,
    )
}

internal fun WidgetTemplateModel.providerKey(): String? =
    providerInfo?.provider?.let { provider -> "${provider.packageName}/${provider.className}" }
        ?: restoredProviderPackage?.let { providerPackage ->
            restoredProviderClass?.let { providerClass -> "$providerPackage/$providerClass" }
        }

internal fun WidgetTemplateModel.stableWidgetKey(): String {
    hostWidgetId?.let { return "bound:$it" }
    providerKey()?.let { return "provider:$it" }
    return "template:$title"
}

internal fun widgetBindOptions(widget: WidgetTemplateModel): Bundle =
    Bundle().apply {
        val minWidth = widget.spanX.coerceAtLeast(1) * 72
        val minHeight = widget.spanY.coerceAtLeast(1) * 72
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth * 2)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight * 2)
    }

internal fun widgetConfigureIntent(widget: WidgetTemplateModel): Intent? {
    val widgetId = widget.hostWidgetId ?: return null
    val configure = widget.providerInfo?.configure ?: return null
    return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
        component = configure
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

internal fun widgetSpanLabel(spanX: Int, spanY: Int): String = "$spanX x $spanY"

private fun WidgetTemplateModel.normalizedWidgetBounds(
    columns: Int,
    maxRows: Int,
): WidgetTemplateModel {
    val normalizedMinX = minSpanX.coerceIn(1, columns)
    val normalizedMinY = minSpanY.coerceIn(1, maxRows)
    val normalizedMaxX = maxSpanX.coerceIn(normalizedMinX, columns)
    val normalizedMaxY = maxSpanY.coerceIn(normalizedMinY, maxRows)
    val normalizedSpanX = spanX.coerceIn(normalizedMinX, normalizedMaxX)
    val normalizedSpanY = spanY.coerceIn(normalizedMinY, normalizedMaxY)
    return copy(
        cellX = cellX.coerceIn(0, columns - normalizedSpanX),
        cellY = cellY.coerceIn(0, maxRows - normalizedSpanY),
        span = widgetSpanLabel(normalizedSpanX, normalizedSpanY),
        spanX = normalizedSpanX,
        spanY = normalizedSpanY,
        minSpanX = normalizedMinX,
        minSpanY = normalizedMinY,
        maxSpanX = normalizedMaxX,
        maxSpanY = normalizedMaxY,
    )
}

private fun firstAvailableWidgetCell(
    widget: WidgetTemplateModel,
    columns: Int,
    maxRows: Int,
    occupied: Set<Pair<Int, Int>>,
): Pair<Int, Int> {
    for (row in 0..(maxRows - widget.spanY)) {
        for (column in 0..(columns - widget.spanX)) {
            if (canPlaceWidget(column, row, widget, columns, maxRows, occupied)) {
                return column to row
            }
        }
    }
    return 0 to (maxRows - widget.spanY).coerceAtLeast(0)
}

private fun canPlaceWidget(
    cellX: Int,
    cellY: Int,
    widget: WidgetTemplateModel,
    columns: Int,
    maxRows: Int,
    occupied: Set<Pair<Int, Int>>,
): Boolean {
    if (cellX < 0 || cellY < 0 || cellX + widget.spanX > columns || cellY + widget.spanY > maxRows) {
        return false
    }
    return widgetCellRange(cellX, cellY, widget).none { it in occupied }
}

private fun markWidgetCells(
    cellX: Int,
    cellY: Int,
    widget: WidgetTemplateModel,
    occupied: MutableSet<Pair<Int, Int>>,
) {
    occupied += widgetCellRange(cellX, cellY, widget)
}

private fun widgetCellRange(
    cellX: Int,
    cellY: Int,
    widget: WidgetTemplateModel,
): List<Pair<Int, Int>> =
    buildList {
        for (x in cellX until cellX + widget.spanX) {
            for (y in cellY until cellY + widget.spanY) {
                add(x to y)
            }
        }
    }

internal fun boundWidgetCount(pages: List<HomePageModel>): Int =
    pages.sumOf { page -> page.widgets.count { it.hostWidgetId != null } }

internal fun clearBoundWidgetsFromPages(pages: List<HomePageModel>): List<HomePageModel> =
    pages.map { page ->
        page.copy(widgets = page.widgets.filter { it.hostWidgetId == null })
    }

internal fun boundWidgetsFromPages(pages: List<HomePageModel>): List<BoundWidget> =
    pages.flatMapIndexed { pageIndex, page ->
        page.widgets.mapNotNull { widget ->
            widget.hostWidgetId?.let { widgetId -> widget.toBoundWidget(widgetId, pageIndex) }
        }
    }

internal fun buildPersistedLauncherLayout(
    pages: List<HomePageModel>,
    defaultHomePageIndex: Int,
    hiddenAppIds: Set<String>,
    recentSearches: List<String>,
    nextPageId: Int,
    nextFolderId: Int,
): PersistedLauncherLayout =
    PersistedLauncherLayout(
        pages = pages.map { page ->
            PersistedHomePage(
                id = page.id,
                label = page.label,
                eyebrow = page.eyebrow,
                value = page.value,
                status = page.status,
                note = page.note,
                items = page.items.map { item ->
                    when (item) {
                        is AppItemModel -> PersistedHomeItem.App(item.app.id)
                        is FolderModel -> PersistedHomeItem.Folder(
                            id = item.id,
                            title = item.title,
                            appIds = item.apps.map(CloneApp::id),
                        )
                    }
                },
            )
        },
        defaultHomePageIndex = defaultHomePageIndex,
        hiddenAppIds = hiddenAppIds,
        recentSearches = recentSearches,
        nextPageId = nextPageId,
        nextFolderId = nextFolderId,
    )

internal fun restorePersistedHomePages(
    layout: PersistedLauncherLayout,
    allApps: List<CloneApp>,
): List<HomePageModel> {
    val appsById = allApps.associateBy(CloneApp::id)
    return layout.pages.map { page ->
        val restoredItems = page.items.map { item ->
            when (item) {
                is PersistedHomeItem.App -> AppItemModel(
                    appsById[item.appId] ?: restoredMissingAppPlaceholder(item.appId),
                )
                is PersistedHomeItem.Folder -> {
                    val apps = item.appIds
                        .map { appId -> appsById[appId] ?: restoredMissingAppPlaceholder(appId) }
                        .distinctBy(CloneApp::id)
                    FolderModel(
                        id = item.id,
                        title = item.title.ifBlank { "Folder" },
                        summary = folderSummaryFor(apps),
                        apps = apps,
                    )
                }
            }
        }
        HomePageModel(
            id = page.id,
            label = page.label.ifBlank { "Home ${page.id}" },
            eyebrow = page.eyebrow,
            value = page.value,
            status = page.status,
            note = page.note,
            widgets = buildSeedWidgets(page.id),
            items = restoredItems,
        )
    }
}

internal fun reconcileHomePagesWithApps(
    pages: List<HomePageModel>,
    allApps: List<CloneApp>,
): List<HomePageModel> {
    if (pages.isEmpty()) return pages
    val appsById = allApps.associateBy(CloneApp::id)
    return pages.mapNotNull { page ->
        val reconciledItems = page.items.mapNotNull { item ->
            when (item) {
                is AppItemModel -> appsById[item.app.id]?.let(::AppItemModel)
                    ?: item.takeIf { it.app.isRestoredPlaceholder }
                is FolderModel -> {
                    val apps = item.apps
                        .mapNotNull { app -> appsById[app.id] ?: app.takeIf { it.isRestoredPlaceholder } }
                        .distinctBy(CloneApp::id)
                    if (apps.isEmpty()) {
                        null
                    } else {
                        item.copy(apps = apps, summary = folderSummaryFor(apps))
                    }
                }
            }
        }
        if (reconciledItems.isEmpty() && page.widgets.isEmpty()) {
            null
        } else {
            page.copy(items = reconciledItems)
        }
    }
}

internal fun reconcileHiddenAppIds(hiddenAppIds: Set<String>, allApps: List<CloneApp>): Set<String> {
    val appIds = allApps.mapTo(mutableSetOf(), CloneApp::id)
    return hiddenAppIds.filterTo(mutableSetOf()) { it in appIds }
}

internal fun deleteWidgetId(widgetId: Int) {
    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
    runCatching { LauncherApp.appWidgetHost()?.deleteAppWidgetId(widgetId) }
        .onFailure { Log.w("OneUiHome/widgets", "Widget id $widgetId delete failed (${it.javaClass.simpleName})") }
}

internal fun homeItemLabel(item: HomeGridItemModel): String {
    return when (item) {
        is AppItemModel -> item.app.name
        is FolderModel -> item.title
    }
}

internal fun restoredMissingAppPlaceholder(appId: String): CloneApp =
    CloneApp(
        id = appId,
        name = restoredMissingAppLabel(appId),
        color = fallbackColorFor(appId),
        statusLabel = "Not installed",
        isLaunchable = false,
        isRestoredPlaceholder = true,
    )

private fun restoredMissingAppLabel(appId: String): String {
    val component = appId.substringAfter(':', appId)
    val className = component.substringAfter('/', "")
    val packageName = component.substringBefore('/', component)
    val raw = className.substringAfterLast('.').takeIf { it.isNotBlank() }
        ?: packageName.substringAfterLast('.').takeIf { it.isNotBlank() }
        ?: appId
    return raw
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
}

internal fun restoredPlaceholderCount(pages: List<HomePageModel>): Int =
    pages.sumOf { page ->
        page.items.sumOf { item ->
            when (item) {
                is AppItemModel -> if (item.app.isRestoredPlaceholder) 1 else 0
                is FolderModel -> item.apps.count { it.isRestoredPlaceholder }
            }
        }
    }

internal fun folderSummaryFor(apps: List<CloneApp>): String {
    return when (apps.size) {
        0 -> "Empty folder"
        1 -> apps.first().name
        2 -> "${apps[0].name} and ${apps[1].name}"
        else -> "${apps[0].name}, ${apps[1].name}, and ${apps.size - 2} more"
    }
}

internal fun previewAppsForPage(page: HomePageModel): List<CloneApp> {
    return page.items.flatMap { item ->
        when (item) {
            is AppItemModel -> listOf(item.app)
            is FolderModel -> item.apps.take(2)
        }
    }.take(4)
}

internal fun reorderHomeGridItems(
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

internal fun Rect.centerOffset(): Offset = Offset((left + right) / 2f, (top + bottom) / 2f)

internal fun distanceSquared(start: Offset, end: Offset): Float {
    val deltaX = start.x - end.x
    val deltaY = start.y - end.y
    return (deltaX * deltaX) + (deltaY * deltaY)
}

internal fun isPointInsideInsetRect(
    point: Offset,
    rect: Rect,
    insetFraction: Float,
): Boolean {
    val insetX = rect.width * insetFraction
    val insetY = rect.height * insetFraction
    return point.x in (rect.left + insetX)..(rect.right - insetX) &&
        point.y in (rect.top + insetY)..(rect.bottom - insetY)
}

internal fun isCombineTarget(item: HomeGridItemModel): Boolean {
    return item is AppItemModel || item is FolderModel
}

internal fun createFolderFromHomeGridItems(
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

internal fun addAppToFolder(
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

internal fun buildSeedWidgets(id: Int): List<WidgetTemplateModel> {
    return when ((id - 1) % 3) {
        0 -> listOf(
            WidgetTemplateModel("Calendar", "Month agenda with rounded launcher chrome", "Recommended", "4 x 2", Color(0xFFFF8B7B)),
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

internal fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

internal fun buildHomePage(id: Int, allApps: List<CloneApp>): HomePageModel {
    val metadata = when ((id - 1) % 3) {
        0 -> listOf("Home $id", "Monday", "30", "73° and bright", "Build parity first. Add customization only after the launcher feels native.")
        1 -> listOf("Home $id", "Focus", "4 blocks", "Calendar, tasks, and routines", "Every screen should have a purpose, not just icons.")
        else -> listOf("Home $id", "Evening", "3 scenes", "Lighting, music, and home controls", "Dedicated pages keep routines, widgets, and media moments easy to reach.")
    }
    val startIndex = ((id - 1) * 4) % allApps.size
    val folderApps = List(4) { offset -> allApps[(startIndex + offset) % allApps.size] }
    val pageApps = List(11) { offset -> allApps[(startIndex + 4 + offset) % allApps.size] }
    val folder = when ((id - 1) % 3) {
        0 -> FolderModel(id = "folder-seed-$id", title = "Tools", summary = folderSummaryFor(folderApps), apps = folderApps)
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

internal fun buildAutoAddedHomePage(id: Int): HomePageModel =
    HomePageModel(
        id = id,
        label = "Home $id",
        eyebrow = "New",
        value = "Apps",
        status = "Added automatically",
        note = "Newly installed apps land here when existing Home pages are full.",
        widgets = emptyList(),
        items = emptyList(),
    )

internal data class FinderSettingText(
    val homeScreenLayout: String = "Home screen layout",
    val homeScreenGrid: String = "Home screen grid",
    val appsScreenGrid: String = "Apps screen grid",
    val folderGrid: String = "Folder grid",
    val defaultHomePage: String = "Default home page",
    val visiblePages: String = "Visible pages",
    val mediaPage: String = "Media page",
    val appsButton: String = "Apps button on Home screen",
    val appLabels: String = "App labels",
    val widgetLabels: String = "Widget labels",
    val swipeDownNotifications: String = "Swipe down for notification panel",
    val hideApps: String = "Hide apps",
    val lockLayout: String = "Lock Home screen layout",
    val addNewApps: String = "Add new apps to Home screen",
    val badgeNotifications: String = "Badge notifications",
    val layoutCategory: String = "Layout",
    val behaviorCategory: String = "Behavior",
    val gesturesCategory: String = "Gestures",
    val appsScreenCategory: String = "Apps screen",
    val onValue: String = "On",
    val offValue: String = "Off",
    val noneValue: String = "None",
    val dotsValue: String = "Dots",
    val dotsAndNumberValue: String = "Dots and number",
    val appsSortUnavailable: String = "Unavailable in Home screen only mode",
    val hiddenCount: (Int) -> String = { count -> "$count hidden" },
)

internal data class FinderActionText(
    val homeScreenSettingsTitle: String = "Home screen settings",
    val settingsHomeOnlySummary: String = "Adjust the launcher while Home screen only mode is active",
    val settingsDefaultSummary: String = "Adjust layout, labels, badges, and gestures",
    val wallpapersTitle: String = "Wallpapers and style",
    val wallpaperLockedSummary: String = "Layout is locked, so wallpaper controls route through settings first",
    val wallpaperDefaultSummary: String = "Open edit mode for wallpaper and theme controls",
    val widgetsTitle: String = "Widgets",
    val widgetsSummary: String = "Open the widget picker",
    val pageManagerTitle: String = "Page manager",
    val pageManagerLockedSummary: String = "Layout is locked, so page management is currently disabled",
    val pageManagerDefaultSummary: String = "Preview pages, set default home, and add screens",
    val mediaGoTitle: String = "Go to media page",
    val mediaEnableTitle: String = "Enable media page",
    val mediaGoSummary: String = "Jump to the media page",
    val mediaEnableSummary: String = "Turn on the left media page and open it",
    val defaultHomeTitle: String = "Go to default home page",
    val defaultHomeSummary: String = "Return to the main home screen immediately",
    val manageHiddenTitle: String = "Manage hidden apps",
    val hideAppsTitle: String = "Hide apps",
    val manageHiddenSummary: String = "Review which apps are hidden from Home and Apps screens",
    val hideAppsSummary: String = "Choose which apps disappear from Home and Apps screens",
)

internal fun buildFinderSettingResults(
    query: String,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    mediaPageEnabled: Boolean,
    appsButtonEnabled: Boolean,
    appLabelsEnabled: Boolean,
    widgetLabelsEnabled: Boolean,
    swipeDownForNotifications: Boolean,
    addNewAppsToHomeScreen: Boolean = true,
    homePageCount: Int,
    defaultHomePageLabel: String,
    hiddenAppCount: Int,
    text: FinderSettingText = FinderSettingText(),
    homeLayoutModeTitle: String = homeLayoutMode.title,
    hiddenAppsValue: String = if (hiddenAppCount == 0) text.noneValue else text.hiddenCount(hiddenAppCount),
    homeScreenGridValue: String = "4x6",
    appsScreenGridValue: String = "4x6",
    folderGridValue: String = "3x4",
    notificationBadgeModeValue: String = text.offValue,
): List<FinderSettingResult> {
    val settings = listOf(
        FinderSettingResult(FinderSettingType.HOME_SCREEN_LAYOUT, text.homeScreenLayout, text.layoutCategory, homeLayoutModeTitle),
        FinderSettingResult(FinderSettingType.HOME_SCREEN_GRID, text.homeScreenGrid, text.layoutCategory, homeScreenGridValue),
        FinderSettingResult(FinderSettingType.APPS_SCREEN_GRID, text.appsScreenGrid, text.layoutCategory, appsScreenGridValue),
        FinderSettingResult(FinderSettingType.FOLDER_GRID, text.folderGrid, text.layoutCategory, folderGridValue),
        FinderSettingResult(FinderSettingType.DEFAULT_HOME_PAGE, text.defaultHomePage, text.layoutCategory, defaultHomePageLabel),
        FinderSettingResult(FinderSettingType.VISIBLE_PAGES, text.visiblePages, text.layoutCategory, homePageCount.toString()),
        FinderSettingResult(FinderSettingType.MEDIA_PAGE, text.mediaPage, text.behaviorCategory, if (mediaPageEnabled) text.onValue else text.offValue),
        FinderSettingResult(
            FinderSettingType.APPS_BUTTON,
            text.appsButton,
            text.behaviorCategory,
            if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) text.appsSortUnavailable else if (appsButtonEnabled) text.onValue else text.offValue,
        ),
        FinderSettingResult(FinderSettingType.APP_LABELS, text.appLabels, text.behaviorCategory, if (appLabelsEnabled) text.onValue else text.offValue),
        FinderSettingResult(FinderSettingType.WIDGET_LABELS, text.widgetLabels, text.behaviorCategory, if (widgetLabelsEnabled) text.onValue else text.offValue),
        FinderSettingResult(FinderSettingType.SWIPE_DOWN_NOTIFICATIONS, text.swipeDownNotifications, text.gesturesCategory, if (swipeDownForNotifications) text.onValue else text.offValue),
        FinderSettingResult(FinderSettingType.HIDE_APPS, text.hideApps, text.appsScreenCategory, hiddenAppsValue),
        FinderSettingResult(FinderSettingType.LOCK_LAYOUT, text.lockLayout, text.behaviorCategory, if (lockHomeScreenLayout) text.onValue else text.offValue),
        FinderSettingResult(FinderSettingType.ADD_NEW_APPS, text.addNewApps, text.behaviorCategory, if (addNewAppsToHomeScreen) text.onValue else text.offValue),
        FinderSettingResult(FinderSettingType.BADGE_NOTIFICATIONS, text.badgeNotifications, text.behaviorCategory, notificationBadgeModeValue),
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

internal fun buildFinderActionResults(
    query: String,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    mediaPageEnabled: Boolean,
    hasHiddenApps: Boolean,
    text: FinderActionText = FinderActionText(),
): List<FinderActionItem> {
    val actions = listOf(
        FinderActionItem(
            FinderActionType.SETTINGS,
            text.homeScreenSettingsTitle,
            if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) text.settingsHomeOnlySummary else text.settingsDefaultSummary,
        ),
        FinderActionItem(
            FinderActionType.WALLPAPERS,
            text.wallpapersTitle,
            if (lockHomeScreenLayout) text.wallpaperLockedSummary else text.wallpaperDefaultSummary,
        ),
        FinderActionItem(FinderActionType.WIDGETS, text.widgetsTitle, text.widgetsSummary),
        FinderActionItem(
            FinderActionType.PAGE_MANAGER,
            text.pageManagerTitle,
            if (lockHomeScreenLayout) text.pageManagerLockedSummary else text.pageManagerDefaultSummary,
        ),
        FinderActionItem(
            FinderActionType.MEDIA_PAGE,
            if (mediaPageEnabled) text.mediaGoTitle else text.mediaEnableTitle,
            if (mediaPageEnabled) text.mediaGoSummary else text.mediaEnableSummary,
        ),
        FinderActionItem(FinderActionType.HOME_PAGE, text.defaultHomeTitle, text.defaultHomeSummary),
        FinderActionItem(
            FinderActionType.HIDE_APPS,
            if (hasHiddenApps) text.manageHiddenTitle else text.hideAppsTitle,
            if (hasHiddenApps) text.manageHiddenSummary else text.hideAppsSummary,
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
                    FinderActionType.APP_SHORTCUT -> false
                }
        }
    }
}

internal fun buildFinderShortcutResults(
    query: String,
    shortcutsByApp: Map<CloneApp, List<LauncherShortcutAction>>,
): List<FinderActionItem> {
    val terms = finderSearchTerms(query)
    if (terms.isEmpty()) return emptyList()

    return shortcutsByApp.asSequence()
        .flatMap { (app, shortcuts) ->
            shortcuts.asSequence()
                .filter(LauncherShortcutAction::isEnabled)
                .map { shortcut -> app to shortcut }
        }
        .distinctBy { (app, shortcut) -> shortcut.finderStableKey(app.id) }
        .filter { (app, shortcut) ->
            val searchable = shortcut.finderSearchText(app).lowercase(Locale.getDefault())
            terms.all(searchable::contains)
        }
        .take(MAX_FINDER_SHORTCUT_RESULTS)
        .map { (app, shortcut) ->
            FinderActionItem(
                type = FinderActionType.APP_SHORTCUT,
                title = shortcut.shortLabel,
                summary = shortcut.finderSummary(app),
                shortcut = shortcut,
            )
        }
        .toList()
}

internal fun rememberRecentSearch(
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

private fun finderSearchTerms(query: String): List<String> =
    query.trim()
        .lowercase(Locale.getDefault())
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)

private fun LauncherShortcutAction.finderStableKey(appId: String): String =
    "$appId:$packageName:$id:${user?.hashCode() ?: 0}"

private fun LauncherShortcutAction.finderSearchText(app: CloneApp): String =
    listOfNotNull(
        app.name,
        app.profileBadge,
        packageName,
        shortLabel,
        longLabel,
    ).joinToString(" ")

private fun LauncherShortcutAction.finderSummary(app: CloneApp): String {
    val detail = longLabel
        ?.takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals(shortLabel, ignoreCase = true) }
    return if (detail == null) app.name else "${app.name} - $detail"
}

internal fun widgetProviderLabel(
    packageManager: PackageManager,
    info: AppWidgetProviderInfo,
): String {
    return runCatching { info.loadLabel(packageManager)?.toString().orEmpty() }
        .getOrDefault("")
        .ifBlank { info.provider.className.substringAfterLast('.') }
}

internal fun widgetProviderAppLabel(
    packageManager: PackageManager,
    info: AppWidgetProviderInfo,
    fallbackLabel: String = "Widgets",
): String {
    val packageName = info.provider?.packageName.orEmpty()
    if (packageName.isBlank()) return fallbackLabel
    return runCatching {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar(Char::titlecase))
}

internal fun widgetSpanX(info: AppWidgetProviderInfo): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val cells = info.targetCellWidth
        if (cells > 0) return cells.coerceIn(1, 4)
    }
    return if (info.minWidth > 0) ((info.minWidth + 71) / 72).coerceIn(1, 4) else 2
}

internal fun widgetSpanY(info: AppWidgetProviderInfo): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val cells = info.targetCellHeight
        if (cells > 0) return cells.coerceIn(1, 4)
    }
    return if (info.minHeight > 0) ((info.minHeight + 71) / 72).coerceIn(1, 4) else 2
}

internal fun widgetCanResizeHorizontal(info: AppWidgetProviderInfo): Boolean =
    info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0

internal fun widgetCanResizeVertical(info: AppWidgetProviderInfo): Boolean =
    info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0

internal fun widgetMinResizeSpanX(info: AppWidgetProviderInfo, fallbackSpan: Int): Int =
    if (widgetCanResizeHorizontal(info) && info.minResizeWidth > 0) {
        ((info.minResizeWidth + 71) / 72).coerceIn(1, fallbackSpan.coerceAtLeast(1))
    } else {
        fallbackSpan.coerceAtLeast(1)
    }

internal fun widgetMinResizeSpanY(info: AppWidgetProviderInfo, fallbackSpan: Int): Int =
    if (widgetCanResizeVertical(info) && info.minResizeHeight > 0) {
        ((info.minResizeHeight + 71) / 72).coerceIn(1, fallbackSpan.coerceAtLeast(1))
    } else {
        fallbackSpan.coerceAtLeast(1)
    }

internal fun alphabeticalAppSections(apps: List<CloneApp>): List<Pair<String, List<CloneApp>>> {
    return apps
        .groupBy { app -> app.name.firstOrNull()?.uppercase() ?: "#" }
        .toSortedMap()
        .map { entry -> entry.key to entry.value }
}

internal fun fallbackColorFor(key: String): Color {
    val palette = listOf(
        Color(0xFFFFB84D), Color(0xFF6D8BFF), Color(0xFF45C48B), Color(0xFF35C15E),
        Color(0xFFFF6B6B), Color(0xFF5865F2), Color(0xFF8A94A6), Color(0xFFFFC857),
        Color(0xFF7B61FF), Color(0xFF50B5FF), Color(0xFF55C6A9), Color(0xFFFF7F50),
    )
    return palette[key.hashCode().absoluteValue % palette.size]
}

private val Int.absoluteValue: Int get() = if (this < 0) -this else this
