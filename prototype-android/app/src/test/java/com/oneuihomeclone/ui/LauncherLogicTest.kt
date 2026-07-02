package com.oneuihomeclone.ui

import androidx.compose.ui.graphics.Color
import com.oneuihomeclone.data.BoundWidget
import com.oneuihomeclone.data.DrawerSortKey
import com.oneuihomeclone.data.FolderGridKey
import com.oneuihomeclone.data.HomeLayoutKey
import com.oneuihomeclone.data.LauncherState
import com.oneuihomeclone.data.MotionPresetKey
import com.oneuihomeclone.data.PersistedHomeItem
import com.oneuihomeclone.data.PersistedHomePage
import com.oneuihomeclone.data.PersistedLauncherLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLogicTest {

    private fun app(id: String, name: String = id) =
        CloneApp(id = id, name = name, color = Color.Gray)

    private fun appItem(id: String, name: String = id) =
        AppItemModel(app(id, name))

    private fun folder(id: String, vararg appIds: String) =
        FolderModel(
            id = id,
            title = "Folder $id",
            summary = "${appIds.size} apps",
            apps = appIds.map { app(it) },
        )

    private fun page(
        id: Int,
        items: List<HomeGridItemModel> = emptyList(),
        widgets: List<WidgetTemplateModel> = emptyList(),
    ) = HomePageModel(
        id = id,
        label = "Home $id",
        eyebrow = "",
        value = "",
        status = "",
        note = "",
        widgets = widgets,
        items = items,
    )

    private fun widget(
        title: String,
        hostWidgetId: Int? = null,
        cellX: Int = 0,
        cellY: Int = 0,
        spanX: Int = 2,
        spanY: Int = 1,
        minSpanX: Int = 1,
        minSpanY: Int = 1,
        maxSpanX: Int = 4,
        maxSpanY: Int = 4,
        canResizeHorizontal: Boolean = true,
        canResizeVertical: Boolean = true,
    ) =
        WidgetTemplateModel(
            title = title,
            summary = "$title summary",
            category = "Recommended",
            span = "$spanX x $spanY",
            accent = Color.Gray,
            hostWidgetId = hostWidgetId,
            cellX = cellX,
            cellY = cellY,
            spanX = spanX,
            spanY = spanY,
            minSpanX = minSpanX,
            minSpanY = minSpanY,
            maxSpanX = maxSpanX,
            maxSpanY = maxSpanY,
            canResizeHorizontal = canResizeHorizontal,
            canResizeVertical = canResizeVertical,
        )

    @Test
    fun totalPageCount_withoutMediaPage() {
        assertEquals(3, totalPageCount(3, mediaPageEnabled = false))
    }

    @Test
    fun totalPageCount_withMediaPage() {
        assertEquals(4, totalPageCount(3, mediaPageEnabled = true))
    }

    @Test
    fun visualIndexForHomePage_withoutMediaPage() {
        assertEquals(0, visualIndexForHomePage(0, mediaPageEnabled = false))
        assertEquals(2, visualIndexForHomePage(2, mediaPageEnabled = false))
    }

    @Test
    fun visualIndexForHomePage_withMediaPage() {
        assertEquals(1, visualIndexForHomePage(0, mediaPageEnabled = true))
        assertEquals(3, visualIndexForHomePage(2, mediaPageEnabled = true))
    }

    @Test
    fun homePageIndexFromVisual_withoutMediaPage() {
        assertEquals(0, homePageIndexFromVisual(0, mediaPageEnabled = false))
        assertEquals(2, homePageIndexFromVisual(2, mediaPageEnabled = false))
    }

    @Test
    fun homePageIndexFromVisual_mediaPageIndex_returnsNull() {
        assertNull(homePageIndexFromVisual(0, mediaPageEnabled = true))
    }

    @Test
    fun homePageIndexFromVisual_withMediaPage_offset() {
        assertEquals(0, homePageIndexFromVisual(1, mediaPageEnabled = true))
        assertEquals(2, homePageIndexFromVisual(3, mediaPageEnabled = true))
    }

    @Test
    fun moveListItem_swapAdjacent() {
        val items = listOf("A", "B", "C")
        assertEquals(listOf("B", "A", "C"), moveListItem(items, 0, 1))
    }

    @Test
    fun moveListItem_sameIndex_noChange() {
        val items = listOf("A", "B", "C")
        assertEquals(items, moveListItem(items, 1, 1))
    }

    @Test
    fun moveListItem_moveToEnd() {
        val items = listOf("A", "B", "C")
        assertEquals(listOf("B", "C", "A"), moveListItem(items, 0, 2))
    }

    @Test
    fun movedIndexForSwap_trackedIsMoved() {
        assertEquals(2, movedIndexForSwap(trackedIndex = 0, fromIndex = 0, toIndex = 2))
    }

    @Test
    fun movedIndexForSwap_trackedUnaffected() {
        assertEquals(3, movedIndexForSwap(trackedIndex = 3, fromIndex = 0, toIndex = 1))
    }

    @Test
    fun movedIndexForSwap_trackedBetweenForwardSwap() {
        assertEquals(0, movedIndexForSwap(trackedIndex = 1, fromIndex = 0, toIndex = 2))
    }

    @Test
    fun reorderHomeGridItems_swapByIds() {
        val items: List<HomeGridItemModel> = listOf(
            appItem("a"),
            appItem("b"),
            appItem("c"),
        )
        val result = reorderHomeGridItems(items, "a", "c")
        assertEquals(listOf("b", "c", "a"), result.map { it.id })
    }

    @Test
    fun reorderHomeGridItems_unknownId_noChange() {
        val items: List<HomeGridItemModel> = listOf(appItem("a"), appItem("b"))
        val result = reorderHomeGridItems(items, "a", "missing")
        assertEquals(items, result)
    }

    @Test
    fun alphabeticalAppSections_groupsByFirstLetter() {
        val apps = listOf(app("a1", "Alpha"), app("b1", "Beta"), app("a2", "Apex"))
        val sections = alphabeticalAppSections(apps)
        assertEquals(2, sections.size)
        assertEquals("A", sections[0].first)
        assertEquals(2, sections[0].second.size)
        assertEquals("B", sections[1].first)
        assertEquals(1, sections[1].second.size)
    }

    @Test
    fun alphabeticalAppSections_emptyList() {
        assertTrue(alphabeticalAppSections(emptyList()).isEmpty())
    }

    @Test
    fun cloneAppStatusText_includesInstallProgress() {
        val installing = app("installing").copy(statusLabel = "Installing", installProgressPercent = 42)

        assertEquals("Installing 42%", installing.statusText())
    }

    @Test
    fun cloneAppAccessibilityLabel_includesProfileAndStatus() {
        val workApp = app("work", "Mail").copy(profileBadge = "Work", statusLabel = "Unavailable")

        assertEquals("Mail, Work, Unavailable", workApp.accessibilityLabel())
    }

    @Test
    fun applyHiddenAppsToPages_removesHiddenApps() {
        val items: List<HomeGridItemModel> = listOf(appItem("a"), appItem("b"), appItem("c"))
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, setOf("b"))
        assertEquals(2, result[0].items.size)
        assertEquals(listOf("a", "c"), result[0].items.map { it.id })
    }

    @Test
    fun applyHiddenAppsToPages_removesEmptyFolders() {
        val items: List<HomeGridItemModel> = listOf(
            appItem("a"),
            folder("f1", "b"),
        )
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, setOf("b"))
        assertEquals(1, result[0].items.size)
        assertEquals("a", result[0].items[0].id)
    }

    @Test
    fun applyHiddenAppsToPages_filtersAppsInsideFolders() {
        val items: List<HomeGridItemModel> = listOf(
            folder("f1", "a", "b", "c"),
        )
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, setOf("b"))
        val folder = result[0].items[0] as FolderModel
        assertEquals(2, folder.apps.size)
        assertEquals(listOf("a", "c"), folder.apps.map { it.id })
    }

    @Test
    fun applyHiddenAppsToPages_noHiddenIds_unchanged() {
        val items: List<HomeGridItemModel> = listOf(appItem("a"))
        val pages = listOf(page(1, items))
        val result = applyHiddenAppsToPages(pages, emptySet())
        assertEquals(pages, result)
    }

    @Test
    fun boundWidgetCount_countsOnlyHostBackedWidgets() {
        val pages = listOf(
            page(1, widgets = listOf(widget("Calendar"), widget("Clock", hostWidgetId = 42))),
            page(2, widgets = listOf(widget("Weather", hostWidgetId = 99))),
        )

        assertEquals(2, boundWidgetCount(pages))
    }

    @Test
    fun clearBoundWidgetsFromPages_keepsSyntheticSeedWidgets() {
        val seed = widget("Calendar")
        val pages = listOf(
            page(1, widgets = listOf(seed, widget("Clock", hostWidgetId = 42))),
            page(2, widgets = listOf(widget("Weather", hostWidgetId = 99))),
        )

        val result = clearBoundWidgetsFromPages(pages)

        assertEquals(listOf(seed), result[0].widgets)
        assertTrue(result[1].widgets.isEmpty())
    }

    @Test
    fun addWidgetToPage_placesWidgetsWithoutOverlap() {
        val result = addWidgetToPage(
            widgets = listOf(widget("Calendar", hostWidgetId = 1, spanX = 2, spanY = 1)),
            widget = widget("Weather", hostWidgetId = 2, spanX = 2, spanY = 1),
        )

        assertEquals(2, result.size)
        assertEquals(0, result[0].cellX)
        assertEquals(0, result[0].cellY)
        assertEquals(2, result[1].cellX)
        assertEquals(0, result[1].cellY)
    }

    @Test
    fun addWidgetToPage_keepsMoreThanThreeBoundWidgets() {
        val result = listOf(
            widget("One", hostWidgetId = 1),
            widget("Two", hostWidgetId = 2),
            widget("Three", hostWidgetId = 3),
        ).let { widgets ->
            addWidgetToPage(widgets, widget("Four", hostWidgetId = 4))
        }

        assertEquals(4, result.size)
    }

    @Test
    fun resizeWidgetInPage_clampsToWidgetLimits() {
        val expanded = resizeWidgetInPage(
            widgets = listOf(
                widget(
                    title = "Calendar",
                    hostWidgetId = 42,
                    spanX = 2,
                    spanY = 2,
                    minSpanX = 1,
                    minSpanY = 1,
                    maxSpanX = 3,
                    maxSpanY = 3,
                ),
            ),
            hostWidgetId = 42,
            deltaX = 5,
            deltaY = 5,
        )

        assertEquals(3, expanded.single().spanX)
        assertEquals(3, expanded.single().spanY)

        val shrunk = resizeWidgetInPage(expanded, hostWidgetId = 42, deltaX = -5, deltaY = -5)

        assertEquals(1, shrunk.single().spanX)
        assertEquals(1, shrunk.single().spanY)
    }

    @Test
    fun moveWidgetInPage_updatesCellWithinGrid() {
        val result = moveWidgetInPage(
            widgets = listOf(widget("Calendar", hostWidgetId = 42, cellX = 1, cellY = 1, spanX = 2, spanY = 2)),
            hostWidgetId = 42,
            deltaX = 1,
            deltaY = 1,
        )

        assertEquals(2, result.single().cellX)
        assertEquals(2, result.single().cellY)
    }

    @Test
    fun mergeBoundWidgetsIntoPages_restoresCellsAndSpans() {
        val result = mergeBoundWidgetsIntoPages(
            pages = listOf(page(1)),
            boundWidgets = listOf(
                BoundWidget(
                    hostWidgetId = 42,
                    providerPackage = "com.example",
                    providerClass = "com.example.ClockWidget",
                    pageIndex = 0,
                    cellX = 2,
                    cellY = 1,
                    spanX = 2,
                    spanY = 3,
                ),
            ),
            templates = emptyList(),
        )

        val restored = result.single().widgets.single()
        assertEquals(42, restored.hostWidgetId)
        assertEquals(2, restored.cellX)
        assertEquals(1, restored.cellY)
        assertEquals(2, restored.spanX)
        assertEquals(3, restored.spanY)
    }

    @Test
    fun buildPersistedLauncherLayout_capturesPageFoldersHiddenAndRecents() {
        val pages = listOf(
            page(
                id = 7,
                items = listOf(
                    appItem("a"),
                    folder("tools", "b", "c"),
                ),
            ),
        )

        val layout = buildPersistedLauncherLayout(
            pages = pages,
            defaultHomePageIndex = 0,
            hiddenAppIds = setOf("c"),
            recentSearches = listOf("Widgets"),
            nextPageId = 8,
            nextFolderId = 3,
        )

        assertEquals(7, layout.pages.single().id)
        assertEquals(PersistedHomeItem.App("a"), layout.pages.single().items[0])
        assertEquals(PersistedHomeItem.Folder("tools", "Folder tools", listOf("b", "c")), layout.pages.single().items[1])
        assertEquals(setOf("c"), layout.hiddenAppIds)
        assertEquals(listOf("Widgets"), layout.recentSearches)
    }

    @Test
    fun restorePersistedHomePages_reconcilesAppsAndDropsEmptyFolders() {
        val layout = PersistedLauncherLayout(
            pages = listOf(
                PersistedHomePage(
                    id = 3,
                    label = "Home 3",
                    eyebrow = "Day",
                    value = "2",
                    status = "Ready",
                    note = "Note",
                    items = listOf(
                        PersistedHomeItem.App("keep"),
                        PersistedHomeItem.App("missing"),
                        PersistedHomeItem.Folder("folder", "Folder", listOf("missing")),
                    ),
                ),
            ),
            defaultHomePageIndex = 0,
            hiddenAppIds = emptySet(),
            recentSearches = emptyList(),
            nextPageId = 4,
            nextFolderId = 2,
        )

        val restored = restorePersistedHomePages(layout, listOf(app("keep", "Current label")))

        assertEquals(listOf("keep"), restored.single().items.map { it.id })
        assertEquals("Current label", (restored.single().items.single() as AppItemModel).app.name)
        assertTrue(restored.single().widgets.isNotEmpty())
    }

    @Test
    fun reconcileHomePagesWithApps_replacesCurrentAppRecordsAndPrunesMissing() {
        val pages = listOf(
            page(
                id = 1,
                items = listOf(
                    appItem("old", "Old name"),
                    folder("folder", "old", "gone"),
                ),
            ),
        )

        val result = reconcileHomePagesWithApps(pages, listOf(app("old", "New name")))

        assertEquals("New name", ((result.single().items[0] as AppItemModel).app.name))
        val folder = result.single().items[1] as FolderModel
        assertEquals(listOf("old"), folder.apps.map { it.id })
        assertEquals("New name", folder.apps.single().name)
    }

    @Test
    fun reconcileHiddenAppIds_prunesRemovedApps() {
        assertEquals(
            setOf("a"),
            reconcileHiddenAppIds(setOf("a", "missing"), listOf(app("a"))),
        )
    }

    @Test
    fun launcherAppStableId_includesProfileSerial() {
        assertEquals(
            "10:com.example/.MainActivity",
            launcherAppStableId(
                userSerial = 10,
                packageName = "com.example",
                className = ".MainActivity",
            ),
        )
    }

    @Test
    fun normalizedLauncherAppRecords_filtersHostAndBlankComponents() {
        val records = listOf(
            LauncherAppRecord(0, "com.oneuihomeclone", ".MainActivity", "Host"),
            LauncherAppRecord(0, "", ".MissingPackage", "Broken"),
            LauncherAppRecord(0, "com.example", "", "Broken"),
            LauncherAppRecord(0, "com.example", ".MainActivity", "Example"),
        )

        val result = normalizedLauncherAppRecords(records, hostPackageName = "com.oneuihomeclone")

        assertEquals(listOf("0:com.example/.MainActivity"), result.map { it.stableId() })
    }

    @Test
    fun normalizedLauncherAppRecords_dedupesPerProfileComponent() {
        val records = listOf(
            LauncherAppRecord(0, "com.example", ".MainActivity", "Example"),
            LauncherAppRecord(0, "com.example", ".MainActivity", "Duplicate"),
            LauncherAppRecord(10, "com.example", ".MainActivity", "Work Example"),
        )

        val result = normalizedLauncherAppRecords(records, hostPackageName = "com.oneuihomeclone")

        assertEquals(
            listOf("0:com.example/.MainActivity", "10:com.example/.MainActivity"),
            result.map { it.stableId() },
        )
    }

    @Test
    fun normalizedLauncherAppRecords_sortsByDisplayLabel() {
        val records = listOf(
            LauncherAppRecord(0, "com.zeta", ".MainActivity", "Zeta"),
            LauncherAppRecord(0, "com.alpha", ".MainActivity", "Alpha"),
            LauncherAppRecord(0, "com.blanklabel", ".MainActivity", ""),
        )

        val result = normalizedLauncherAppRecords(records, hostPackageName = "com.oneuihomeclone")

        assertEquals(
            listOf("Alpha", "Blanklabel", "Zeta"),
            result.map { it.displayLabel() },
        )
    }

    @Test
    fun launcherStateToggleMapping_roundTripsAllValues() {
        val state = LauncherState(
            mediaPageEnabled = false,
            appsButtonEnabled = false,
            appLabelsEnabled = false,
            widgetLabelsEnabled = false,
            swipeDownForNotifications = false,
            lockHomeScreenLayout = true,
            homeLayoutMode = HomeLayoutKey.HOME_SCREEN_ONLY,
            drawerSortMode = DrawerSortKey.ALPHABETICAL,
            motionPreset = MotionPresetKey.REDUCED,
            folderGrid = FolderGridKey.GRID_5X5,
        )

        val toggles = state.toPersistedToggles()

        assertEquals(HomeLayoutMode.HOME_SCREEN_ONLY, toggles.homeLayoutMode)
        assertEquals(DrawerSortMode.ALPHABETICAL, toggles.drawerSortMode)
        assertEquals(MotionPresetMode.REDUCED, toggles.motionPreset)
        assertEquals(FolderGridMode.GRID_5X5, toggles.folderGrid)
        assertEquals(state, toggles.toLauncherState())
    }
}
