package com.oneuihomeclone.ui

import android.appwidget.AppWidgetManager
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
): List<WidgetTemplateModel> {
    return (widgets.filterNot { it.stableWidgetKey() == widget.stableWidgetKey() } + widget).takeLast(3)
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
            span = "$spanX x $spanY",
            spanX = spanX,
            spanY = spanY,
        )
    } else {
        WidgetTemplateModel(
            title = providerClass.substringAfterLast('.'),
            summary = "Restored widget from ${providerPackage.substringAfterLast('.')}",
            category = providerPackage.substringAfterLast('.').replaceFirstChar(Char::titlecase),
            span = "$spanX x $spanY",
            accent = fallbackColorFor(providerKey),
            hostWidgetId = hostWidgetId,
            spanX = spanX,
            spanY = spanY,
        )
    }
}

internal fun WidgetTemplateModel.toBoundWidget(
    widgetId: Int,
    pageIndex: Int,
): BoundWidget? {
    val provider = providerInfo?.provider ?: return null
    return BoundWidget(
        hostWidgetId = widgetId,
        providerPackage = provider.packageName,
        providerClass = provider.className,
        pageIndex = pageIndex,
        cellX = 0,
        cellY = 0,
        spanX = spanX,
        spanY = spanY,
    )
}

internal fun WidgetTemplateModel.providerKey(): String? =
    providerInfo?.provider?.let { provider -> "${provider.packageName}/${provider.className}" }

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

internal fun deleteWidgetId(widgetId: Int) {
    runCatching { LauncherApp.appWidgetHost()?.deleteAppWidgetId(widgetId) }
}

internal fun homeItemLabel(item: HomeGridItemModel): String {
    return when (item) {
        is AppItemModel -> item.app.name
        is FolderModel -> item.title
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

internal fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

internal fun buildHomePage(id: Int, allApps: List<CloneApp>): HomePageModel {
    val metadata = when ((id - 1) % 3) {
        0 -> listOf("Home $id", "Monday", "30", "73° and bright", "Build parity first. Add customization only after the launcher feels native.")
        1 -> listOf("Home $id", "Focus", "4 blocks", "Calendar, tasks, and routines", "Samsung's page manager works because every screen has a purpose, not just icons.")
        else -> listOf("Home $id", "Evening", "3 scenes", "Lighting, music, and home controls", "A true One UI clone needs dedicated pages for routines, widgets, and media moments.")
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

internal fun buildFinderSettingResults(
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

internal fun buildFinderActionResults(
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
            if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) "Adjust the launcher while Home screen only mode is active" else "Adjust layout, labels, badges, and gestures",
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
): String {
    val packageName = info.provider?.packageName.orEmpty()
    if (packageName.isBlank()) return "Widgets"
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
