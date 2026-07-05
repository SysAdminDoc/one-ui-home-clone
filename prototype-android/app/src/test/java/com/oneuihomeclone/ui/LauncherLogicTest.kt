package com.oneuihomeclone.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oneuihomeclone.data.BoundWidget
import com.oneuihomeclone.data.DrawerSortKey
import com.oneuihomeclone.data.FolderGridKey
import com.oneuihomeclone.data.HomeLayoutKey
import com.oneuihomeclone.data.LauncherState
import com.oneuihomeclone.data.MotionPresetKey
import com.oneuihomeclone.data.NotificationBadgeModeKey
import com.oneuihomeclone.data.PersistedHomeItem
import com.oneuihomeclone.data.PersistedHomePage
import com.oneuihomeclone.data.PersistedLauncherLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLogicTest {

    private fun app(id: String, name: String = id, packageName: String? = null) =
        CloneApp(
            id = id,
            name = name,
            packageName = packageName,
            color = Color.Gray,
        )

    private fun shortcut(
        id: String,
        shortLabel: String,
        longLabel: String? = null,
        packageName: String = "com.example",
        isEnabled: Boolean = true,
    ) = LauncherShortcutAction(
        id = id,
        packageName = packageName,
        shortLabel = shortLabel,
        longLabel = longLabel,
        isEnabled = isEnabled,
        disabledMessage = null,
        user = null,
    )

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
        summary: String = "$title summary",
        category: String = "Recommended",
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
        restoredProviderPackage: String? = null,
        restoredProviderClass: String? = null,
    ) =
        WidgetTemplateModel(
            title = title,
            summary = summary,
            category = category,
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
            restoredProviderPackage = restoredProviderPackage,
            restoredProviderClass = restoredProviderClass,
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
    fun resolveLauncherLayoutContract_phonePortraitUsesOneUiPhoneGrid() {
        val contract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915)

        assertEquals(LauncherFormFactor.PHONE_PORTRAIT, contract.formFactor)
        assertFalse(contract.isLandscape)
        assertEquals("4x5", contract.homeGridLabel)
        assertEquals("4x5", contract.appsGridLabel)
        assertEquals(20, contract.appsPageSize)
        assertEquals(440.dp, contract.homeMaxWidth)
    }

    @Test
    fun resolveLauncherLayoutContract_phoneLandscapeUsesCompactFiveColumnGrid() {
        val contract = resolveLauncherLayoutContract(widthDp = 891, heightDp = 411)

        assertEquals(LauncherFormFactor.PHONE_LANDSCAPE, contract.formFactor)
        assertTrue(contract.isLandscape)
        assertEquals("5x3", contract.homeGridLabel)
        assertEquals("5x3", contract.appsGridLabel)
        assertEquals(5, contract.widgetGridColumns)
        assertEquals(3, contract.widgetGridMaxRows)
        assertEquals(15, contract.appsPageSize)
    }

    @Test
    fun resolveLauncherLayoutContract_foldableUsesBoundedFiveColumnSurfaces() {
        val contract = resolveLauncherLayoutContract(widthDp = 673, heightDp = 841)

        assertEquals(LauncherFormFactor.FOLDABLE, contract.formFactor)
        assertEquals("5x5", contract.homeGridLabel)
        assertEquals("5x5", contract.appsGridLabel)
        assertEquals(640.dp, contract.settingsMaxWidth)
        assertEquals(560.dp, contract.folderMaxWidth)
    }

    @Test
    fun resolveLauncherLayoutContract_tabletUsesSixColumnSurfaces() {
        val contract = resolveLauncherLayoutContract(widthDp = 1280, heightDp = 800)

        assertEquals(LauncherFormFactor.TABLET, contract.formFactor)
        assertTrue(contract.isLandscape)
        assertEquals("6x5", contract.homeGridLabel)
        assertEquals("6x5", contract.appsGridLabel)
        assertEquals(30, contract.appsPageSize)
        assertEquals(760.dp, contract.drawerMaxWidth)
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
    fun applyNotificationBadges_clearsWhenOffOrPermissionRevoked() {
        val badgedApp = app("mail", packageName = "com.example.mail")
            .copy(notificationBadge = NotificationBadgeState(count = 4, showNumber = true))

        val offResult = applyNotificationBadges(
            apps = listOf(badgedApp),
            countsByPackage = mapOf("com.example.mail" to 4),
            mode = NotificationBadgeMode.OFF,
            accessGranted = true,
        )
        val revokedResult = applyNotificationBadges(
            apps = listOf(badgedApp),
            countsByPackage = mapOf("com.example.mail" to 4),
            mode = NotificationBadgeMode.DOTS_AND_NUMBER,
            accessGranted = false,
        )

        assertEquals(NotificationBadgeState.None, offResult.single().notificationBadge)
        assertEquals(NotificationBadgeState.None, revokedResult.single().notificationBadge)
    }

    @Test
    fun applyNotificationBadges_usesDotsWithoutCounts() {
        val result = applyNotificationBadges(
            apps = listOf(app("mail", packageName = "com.example.mail")),
            countsByPackage = mapOf("com.example.mail" to 3),
            mode = NotificationBadgeMode.DOTS,
            accessGranted = true,
        )

        assertEquals(NotificationBadgeState(count = 3, showNumber = false), result.single().notificationBadge)
    }

    @Test
    fun applyNotificationBadges_usesNumberModeAndIgnoresUnknownPackages() {
        val result = applyNotificationBadges(
            apps = listOf(
                app("mail", packageName = "com.example.mail"),
                app("clock", packageName = "com.example.clock"),
            ),
            countsByPackage = mapOf("com.example.mail" to 120, "com.unknown" to 8),
            mode = NotificationBadgeMode.DOTS_AND_NUMBER,
            accessGranted = true,
        )

        assertEquals(NotificationBadgeState(count = 120, showNumber = true), result[0].notificationBadge)
        assertEquals(NotificationBadgeState.None, result[1].notificationBadge)
    }

    @Test
    fun addAppToHomePageItems_appendsOnlyWhenMissingAndSpaceAvailable() {
        val original = listOf<HomeGridItemModel>(appItem("a"))
        val result = addAppToHomePageItems(original, app("b", "Beta"))

        assertEquals(listOf("a", "b"), result.map(HomeGridItemModel::id))
        assertEquals(result, addAppToHomePageItems(result, app("b", "Beta")))
    }

    @Test
    fun placeNewAppsOnHomePages_usesFirstPageWithRoom() {
        val pages = listOf(
            page(1, List(MAX_HOME_GRID_ITEMS) { index -> appItem("full-$index") }),
            page(2, listOf(appItem("existing"))),
        )

        val result = placeNewAppsOnHomePages(
            pages = pages,
            newApps = listOf(app("new", "New app")),
            nextPageId = 3,
            enabled = true,
            layoutLocked = false,
        )

        assertEquals(2, result.pages.size)
        assertEquals(List(MAX_HOME_GRID_ITEMS) { "full-$it" }, result.pages[0].items.map(HomeGridItemModel::id))
        assertEquals(listOf("existing", "new"), result.pages[1].items.map(HomeGridItemModel::id))
        assertEquals(3, result.nextPageId)
        assertEquals(setOf("new"), result.handledAppIds)
        assertEquals(setOf("new"), result.placedAppIds)
    }

    @Test
    fun placeNewAppsOnHomePages_createsPageOnlyWhenEveryPageIsFull() {
        val fullPage = page(1, List(MAX_HOME_GRID_ITEMS) { index -> appItem("full-$index") })

        val result = placeNewAppsOnHomePages(
            pages = listOf(fullPage),
            newApps = listOf(app("new", "New app")),
            nextPageId = 2,
            enabled = true,
            layoutLocked = false,
        )

        assertEquals(2, result.pages.size)
        assertEquals("Home 2", result.pages[1].label)
        assertEquals(listOf("new"), result.pages[1].items.map(HomeGridItemModel::id))
        assertEquals(3, result.nextPageId)
    }

    @Test
    fun placeNewAppsOnHomePages_skipsAppsAlreadyOnHomeOrInFolders() {
        val pages = listOf(page(1, listOf(appItem("direct"), folder("folder", "nested"))))

        val result = placeNewAppsOnHomePages(
            pages = pages,
            newApps = listOf(app("direct"), app("nested"), app("new")),
            nextPageId = 2,
            enabled = true,
            layoutLocked = false,
        )

        assertEquals(listOf("direct", "folder", "new"), result.pages.single().items.map(HomeGridItemModel::id))
        assertEquals(setOf("new"), result.placedAppIds)
        assertEquals(setOf("direct", "nested", "new"), result.handledAppIds)
    }

    @Test
    fun placeNewAppsOnHomePages_settingOffHandlesWithoutPlacement() {
        val pages = listOf(page(1))

        val result = placeNewAppsOnHomePages(
            pages = pages,
            newApps = listOf(app("new")),
            nextPageId = 2,
            enabled = false,
            layoutLocked = false,
        )

        assertEquals(pages, result.pages)
        assertEquals(2, result.nextPageId)
        assertEquals(setOf("new"), result.handledAppIds)
        assertTrue(result.placedAppIds.isEmpty())
    }

    @Test
    fun placeNewAppsOnHomePages_keepsInstallingAppsPending() {
        val installing = app("installing").copy(statusLabel = "Installing", installProgressPercent = 42, isLaunchable = false)

        val result = placeNewAppsOnHomePages(
            pages = listOf(page(1)),
            newApps = listOf(installing),
            nextPageId = 2,
            enabled = true,
            layoutLocked = false,
        )

        assertTrue(result.pages.single().items.isEmpty())
        assertTrue(result.handledAppIds.isEmpty())
        assertTrue(result.placedAppIds.isEmpty())
    }

    @Test
    fun removeAppFromHomePageItems_removesDirectShortcutOnly() {
        val items: List<HomeGridItemModel> = listOf(appItem("a"), folder("folder", "b"))
        val result = removeAppFromHomePageItems(items, "a")

        assertEquals(listOf("folder"), result.map(HomeGridItemModel::id))
        assertEquals(items, removeAppFromHomePageItems(items, "b"))
    }

    @Test
    fun buildAppContextActions_drawerAppIncludesAddHideAndAppInfo() {
        val actions = buildAppContextActions(
            source = AppContextSource.DRAWER,
            isHidden = false,
            canOpenAppInfo = true,
            canAddToHome = true,
            canRemoveFromHome = false,
            shortcuts = emptyList(),
        )

        assertEquals(
            listOf(
                LauncherContextActionType.APP_INFO,
                LauncherContextActionType.ADD_TO_HOME,
                LauncherContextActionType.HIDE_APP,
            ),
            actions.map(LauncherContextAction::type),
        )
        assertTrue(actions.first { it.type == LauncherContextActionType.ADD_TO_HOME }.enabled)
    }

    @Test
    fun buildAppContextActions_hiddenAppUsesRestore() {
        val actions = buildAppContextActions(
            source = AppContextSource.HIDE_APPS,
            isHidden = true,
            canOpenAppInfo = false,
            canAddToHome = false,
            canRemoveFromHome = false,
            shortcuts = emptyList(),
        )

        assertTrue(actions.any { it.type == LauncherContextActionType.RESTORE_APP })
        assertTrue(actions.none { it.type == LauncherContextActionType.HIDE_APP })
    }

    @Test
    fun buildAppContextActions_homeAppIncludesRemoveWhenEditable() {
        val actions = buildAppContextActions(
            source = AppContextSource.HOME,
            isHidden = false,
            canOpenAppInfo = true,
            canAddToHome = false,
            canRemoveFromHome = true,
            shortcuts = emptyList(),
        )

        assertTrue(actions.any { it.type == LauncherContextActionType.REMOVE_FROM_HOME })
        assertTrue(actions.none { it.type == LauncherContextActionType.ADD_TO_HOME })
    }

    @Test
    fun buildWidgetContextActions_disablesUnavailableSettingsAndLockedRemove() {
        val actions = buildWidgetContextActions(widget("Calendar", hostWidgetId = 42), canEdit = false)

        assertEquals(
            listOf(LauncherContextActionType.WIDGET_SETTINGS, LauncherContextActionType.REMOVE_WIDGET),
            actions.map(LauncherContextAction::type),
        )
        assertTrue(actions.none(LauncherContextAction::enabled))
    }

    @Test
    fun buildFinderShortcutResults_returnsEmptyForBlankQuery() {
        val messages = app("messages", "Messages")

        assertTrue(
            buildFinderShortcutResults(
                query = "   ",
                shortcutsByApp = mapOf(messages to listOf(shortcut("compose", "Compose"))),
            ).isEmpty(),
        )
    }

    @Test
    fun buildFinderShortcutResults_matchesShortcutAndAppText() {
        val messages = app("messages", "Messages")
        val results = buildFinderShortcutResults(
            query = "messages",
            shortcutsByApp = mapOf(
                messages to listOf(
                    shortcut(
                        id = "compose",
                        shortLabel = "Compose",
                        longLabel = "Start a new message",
                    ),
                ),
            ),
        )

        assertEquals(1, results.size)
        assertEquals(FinderActionType.APP_SHORTCUT, results.single().type)
        assertEquals("Compose", results.single().title)
        assertEquals("Messages - Start a new message", results.single().summary)
        assertEquals("compose", results.single().shortcut?.id)
    }

    @Test
    fun buildFinderShortcutResults_skipsDisabledDedupesAndCapsResults() {
        val messages = app("messages", "Messages")
        val shortcuts = List(MAX_FINDER_SHORTCUT_RESULTS + 3) { index ->
            shortcut(id = "shortcut-$index", shortLabel = "Open $index")
        } + shortcut(id = "shortcut-0", shortLabel = "Open duplicate") +
            shortcut(id = "disabled", shortLabel = "Open disabled", isEnabled = false)

        val results = buildFinderShortcutResults(
            query = "open",
            shortcutsByApp = mapOf(messages to shortcuts),
        )

        assertEquals(MAX_FINDER_SHORTCUT_RESULTS, results.size)
        assertEquals(
            (0 until MAX_FINDER_SHORTCUT_RESULTS).map { "shortcut-$it" },
            results.map { it.shortcut?.id },
        )
        assertTrue(results.none { it.title == "Open disabled" })
    }

    @Test
    fun selectedSeslInteropAssessment_recordsCostsAndKeepsComposeToggle() {
        val assessment = selectedSeslInteropAssessment()

        assertEquals("SettingsToggleCard", assessment.composeImplementation)
        assertEquals("dev.oneuiproject.oneui.preference.SwitchBarPreference", assessment.seslComponent)
        assertEquals(SeslInteropCost.HIGH, assessment.binaryCost)
        assertEquals(SeslInteropCost.HIGH, assessment.dependencyCost)
        assertEquals(SeslInteropCost.HIGH, assessment.lifecycleRisk)
        assertEquals(SeslInteropCost.MEDIUM, assessment.fidelityGain)
        assertEquals(SeslInteropDecision.KEEP_COMPOSE, assessment.decision)
        assertTrue(assessment.requiresAuthenticatedPackageRegistry)
        assertTrue(assessment.needsAndroidViewBridge)
        assertFalse(assessment.shouldAdoptSesl())
        assertTrue(assessment.evidence.any { it.contains("SwitchBarPreference") })
    }

    @Test
    fun filterWidgetsForPicker_filtersByCategoryAndQuery() {
        val widgets = listOf(
            widget("Calendar", summary = "Month agenda", category = "Google Calendar"),
            widget("Battery", summary = "Device status", category = "Device"),
            widget("Weather", summary = "Forecast", category = "Weather"),
        )

        assertEquals(
            listOf("Calendar"),
            filterWidgetsForPicker(widgets, selectedCategory = "All", query = "calendar")
                .map { it.title },
        )
        assertEquals(
            listOf("Battery"),
            filterWidgetsForPicker(widgets, selectedCategory = "Device", query = "status")
                .map { it.title },
        )
        assertTrue(filterWidgetsForPicker(widgets, selectedCategory = "Weather", query = "calendar").isEmpty())
    }

    @Test
    fun filterWidgetsForPicker_matchesRestoredProviderMetadata() {
        val restored = widget(
            "ClockWidget",
            category = "Restored",
            restoredProviderPackage = "com.example.clock",
            restoredProviderClass = "com.example.clock.ClockWidget",
        )

        assertEquals(
            listOf(restored),
            filterWidgetsForPicker(listOf(restored), selectedCategory = "All", query = "example clock"),
        )
        assertTrue(restored.isProviderUnavailable())
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
    fun restorePersistedHomePages_reconcilesAppsAndKeepsMissingPlaceholders() {
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

        val items = restored.single().items
        assertEquals(listOf("keep", "missing", "folder"), items.map { it.id })
        assertEquals("Current label", (items[0] as AppItemModel).app.name)
        val missing = (items[1] as AppItemModel).app
        assertTrue(missing.isRestoredPlaceholder)
        assertFalse(missing.isLaunchable)
        assertEquals("Not installed", missing.statusLabel)
        val folder = items[2] as FolderModel
        assertEquals(listOf("missing"), folder.apps.map { it.id })
        assertTrue(folder.apps.single().isRestoredPlaceholder)
        assertTrue(restored.single().widgets.isNotEmpty())
    }

    @Test
    fun reconcileHomePagesWithApps_replacesCurrentAppRecordsAndKeepsPlaceholders() {
        val pages = listOf(
            page(
                id = 1,
                items = listOf(
                    appItem("old", "Old name"),
                    AppItemModel(restoredMissingAppPlaceholder("gone")),
                    folder("folder", "old").copy(
                        apps = listOf(app("old", "Old name"), restoredMissingAppPlaceholder("gone-folder")),
                    ),
                ),
            ),
        )

        val result = reconcileHomePagesWithApps(pages, listOf(app("old", "New name")))

        assertEquals("New name", ((result.single().items[0] as AppItemModel).app.name))
        val missing = (result.single().items[1] as AppItemModel).app
        assertEquals("gone", missing.id)
        assertTrue(missing.isRestoredPlaceholder)
        val folder = result.single().items[2] as FolderModel
        assertEquals(listOf("old", "gone-folder"), folder.apps.map { it.id })
        assertEquals("New name", folder.apps.first().name)
        assertTrue(folder.apps.last().isRestoredPlaceholder)
    }

    @Test
    fun boundWidgetsFromPages_preservesRestoredProviderMetadata() {
        val pages = listOf(
            page(
                id = 1,
                widgets = listOf(
                    widget("Restored", hostWidgetId = 77).copy(
                        restoredProviderPackage = "com.example",
                        restoredProviderClass = "com.example.ClockWidget",
                    ),
                ),
            ),
        )

        assertEquals(
            listOf(
                BoundWidget(
                    hostWidgetId = 77,
                    providerPackage = "com.example",
                    providerClass = "com.example.ClockWidget",
                    pageIndex = 0,
                    cellX = 0,
                    cellY = 0,
                    spanX = 2,
                    spanY = 1,
                ),
            ),
            boundWidgetsFromPages(pages),
        )
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
            addNewAppsToHomeScreen = false,
            notificationBadgeMode = NotificationBadgeModeKey.DOTS_AND_NUMBER,
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
        assertEquals(NotificationBadgeMode.DOTS_AND_NUMBER, toggles.notificationBadgeMode)
        assertEquals(state, toggles.toLauncherState())
    }

    @Test
    fun buildFinderSettingResults_reflectsAddNewAppsState() {
        val results = buildFinderSettingResults(
            query = "add new",
            homeLayoutMode = HomeLayoutMode.HOME_AND_APPS_SCREENS,
            lockHomeScreenLayout = false,
            mediaPageEnabled = true,
            appsButtonEnabled = true,
            appLabelsEnabled = true,
            widgetLabelsEnabled = true,
            swipeDownForNotifications = true,
            addNewAppsToHomeScreen = false,
            homePageCount = 2,
            defaultHomePageLabel = "Home 1",
            hiddenAppCount = 0,
        )

        assertEquals(FinderSettingType.ADD_NEW_APPS, results.single().type)
        assertEquals("Off", results.single().value)
    }

    @Test
    fun buildFinderSettingResults_reflectsNotificationBadgeMode() {
        val results = buildFinderSettingResults(
            query = "badge",
            homeLayoutMode = HomeLayoutMode.HOME_AND_APPS_SCREENS,
            lockHomeScreenLayout = false,
            mediaPageEnabled = true,
            appsButtonEnabled = true,
            appLabelsEnabled = true,
            widgetLabelsEnabled = true,
            swipeDownForNotifications = true,
            homePageCount = 2,
            defaultHomePageLabel = "Home 1",
            hiddenAppCount = 0,
            notificationBadgeModeValue = "Dots",
        )

        assertEquals(FinderSettingType.BADGE_NOTIFICATIONS, results.single().type)
        assertEquals("Dots", results.single().value)
    }
}
