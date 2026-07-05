package com.oneuihomeclone.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Environment
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oneuihomeclone.DefaultLauncherState
import com.oneuihomeclone.PreviousCrashSummary
import com.oneuihomeclone.R
import com.oneuihomeclone.data.BoundWidget
import com.oneuihomeclone.data.LauncherBackup
import com.oneuihomeclone.data.LauncherBackupFileStore
import com.oneuihomeclone.data.LauncherBackupImportResult
import com.oneuihomeclone.data.LauncherDataStore
import com.oneuihomeclone.data.LauncherState
import com.oneuihomeclone.data.PersistedLauncherLayout
import com.oneuihomeclone.ui.theme.OneUiHomeCloneTheme
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherComposeSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsTogglePersistsAndOverlayFiresCallback() = runBlocking {
        val store = LauncherDataStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.clear()
        store.update { setAppLabelsEnabled(false) }
        assertFalse(store.state.first().appLabelsEnabled)
        store.recordFinderUsage("app:clock", nowMillis = 100L)
        assertEquals(1, store.finderUsageStats.first().targetCount)
        store.clearFinderUsageStats()
        assertEquals(0, store.finderUsageStats.first().targetCount)

        var appLabelsEnabled = true
        setLauncherContent {
            SettingsToggleCard(
                title = "App labels",
                checked = appLabelsEnabled,
                summary = "Show names under launcher icons.",
                onCheckedChange = { appLabelsEnabled = it },
            )
        }

        composeRule.onNodeWithText("App labels").performClick()
        composeRule.runOnIdle { assertFalse(appLabelsEnabled) }
        store.clear()
    }

    @Test
    fun finderQuerySurfacesAndOpensActionResult() {
        var openedSettingsAction = false
        setLauncherContent {
            var query by remember { mutableStateOf("settings") }
            DrawerOverlay(
                layoutContract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915),
                query = query,
                apps = emptyList(),
                appsScreenApps = emptyList(),
                drawerPages = listOf(emptyList()),
                homeLayoutMode = HomeLayoutMode.HOME_AND_APPS_SCREENS,
                drawerSortMode = DrawerSortMode.CUSTOM_ORDER,
                drawerPageIndex = 0,
                hiddenAppCount = 0,
                settingResults = emptyList(),
                actionResults = if (query.isBlank()) {
                    emptyList()
                } else {
                    listOf(FinderActionItem(FinderActionType.SETTINGS, "Open Home settings", "Adjust layout"))
                },
                shortcutResults = emptyList(),
                contactResults = emptyList(),
                recentSearches = emptyList(),
                onQueryChange = { query = it },
                onClose = {},
                onOpenSettings = {},
                onSelectSortMode = {},
                onSelectDrawerPage = {},
                onOpenHideApps = {},
                onSelectRecentSearch = {},
                onOpenSettingResult = {},
                onOpenAction = { openedSettingsAction = it.type == FinderActionType.SETTINGS },
                onOpenContact = {},
                onOpenApp = {},
                onOpenAppActions = { _, _ -> },
                appLabelsEnabled = true,
            )
        }

        composeRule.onNodeWithContentDescription("Open Home settings, Adjust layout")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertTrue(openedSettingsAction) }
    }

    @Test
    fun drawerCustomOrderLongPressSelectsAndTapReordersApp() {
        val apps = listOf(
            CloneApp("alpha", "Alpha", color = Color(0xFF4A88FF)),
            CloneApp("beta", "Beta", color = Color(0xFFFF8B7B)),
        )
        var startedWith: String? = null
        var movedPair: Pair<String, String>? = null
        var openedApp: String? = null
        setLauncherContent {
            var reorderSource by remember { mutableStateOf<String?>(null) }
            DrawerOverlay(
                layoutContract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915),
                query = "",
                apps = apps,
                appsScreenApps = apps,
                drawerPages = listOf(apps),
                homeLayoutMode = HomeLayoutMode.HOME_AND_APPS_SCREENS,
                drawerSortMode = DrawerSortMode.CUSTOM_ORDER,
                drawerPageIndex = 0,
                hiddenAppCount = 0,
                settingResults = emptyList(),
                actionResults = emptyList(),
                shortcutResults = emptyList(),
                contactResults = emptyList(),
                recentSearches = emptyList(),
                onQueryChange = {},
                onClose = {},
                onOpenSettings = {},
                onSelectSortMode = {},
                onSelectDrawerPage = {},
                onOpenHideApps = {},
                onSelectRecentSearch = {},
                onOpenSettingResult = {},
                onOpenAction = {},
                onOpenContact = {},
                onOpenApp = { openedApp = it.id },
                onOpenAppActions = { _, _ -> },
                drawerReorderSourceAppId = reorderSource,
                onStartDrawerReorder = {
                    startedWith = it.id
                    reorderSource = it.id
                },
                onReorderDrawerApp = { sourceId, targetId ->
                    movedPair = sourceId to targetId
                    reorderSource = null
                },
                onCancelDrawerReorder = { reorderSource = null },
                appLabelsEnabled = true,
            )
        }

        composeRule.onNodeWithContentDescription("Alpha")
            .assertIsDisplayed()
            .performTouchInput { longClick() }
        composeRule.onNodeWithText("Done").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Alpha, selected for Apps screen reorder")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Beta").performClick()
        composeRule.runOnIdle {
            assertEquals("alpha", startedWith)
            assertEquals("alpha" to "beta", movedPair)
            assertEquals(null, openedApp)
        }
    }

    @Test
    fun widgetPickerSearchShowsHealthState() {
        setLauncherContent {
            var query by remember { mutableStateOf("") }
            val widgets = listOf(
                WidgetTemplateModel("Calendar", "Month view", "Recommended", "4 x 2", Color(0xFFFF8B7B)),
                WidgetTemplateModel(
                    title = "Missing clock",
                    summary = "Restored widget from an uninstalled provider",
                    category = "Clock",
                    span = "2 x 2",
                    accent = Color(0xFF62B8FF),
                    restoredProviderPackage = "com.missing.clock",
                    restoredProviderClass = "com.missing.clock.ClockWidget",
                ),
            ).filter { widget -> widget.title.contains(query, ignoreCase = true) }
            WidgetPickerOverlay(
                layoutContract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915),
                categories = listOf("Recommended", "Clock"),
                selectedCategory = "Recommended",
                searchQuery = query,
                widgets = widgets,
                providerWarning = null,
                targetPageLabel = "Home",
                onSelectCategory = {},
                onSearchQueryChange = { query = it },
                onAddWidget = {},
                onClose = {},
            )
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("clock")
        composeRule.onAllNodesWithText("Calendar").assertCountEquals(0)
        composeRule.onAllNodesWithText("Missing clock").assertCountEquals(2)
        composeRule.onAllNodesWithText("Unavailable").assertCountEquals(2)
    }

    @Test
    fun backupFileStoreExportsAndImportsFakeBackup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = LauncherBackupFileStore(context)
        val backupFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            store.backupFileName,
        )
        runBlocking {
            backupFile.delete()
            val backup = LauncherBackup(
                settings = LauncherState(appLabelsEnabled = false),
                layout = PersistedLauncherLayout(
                    pages = emptyList(),
                    defaultHomePageIndex = 0,
                    hiddenAppIds = emptySet(),
                    recentSearches = listOf("settings"),
                    nextPageId = 1,
                    nextFolderId = 1,
                ),
                widgets = listOf(
                    BoundWidget(
                        hostWidgetId = 1,
                        providerPackage = "com.example",
                        providerClass = "com.example.ClockWidget",
                        pageIndex = 0,
                        cellX = 0,
                        cellY = 0,
                        spanX = 2,
                        spanY = 2,
                    ),
                ),
                exportedAtMillis = 123L,
            )

            store.export(backup)
            val imported = store.importResult()
            assertTrue(imported is LauncherBackupImportResult.Success)
            backupFile.delete()
        }
    }

    @Test
    fun defaultLauncherPromptDismissesForSession() {
        var visible by mutableStateOf(true)
        setLauncherContent {
            if (visible) {
                DefaultLauncherPrompt(
                    canOpenSettings = true,
                    onOpenSettings = {},
                    onDismiss = { visible = false },
                )
            }
        }

        composeRule.onNodeWithText("Later").performClick()
        composeRule.onAllNodesWithText("Later").assertCountEquals(0)
    }

    @Test
    fun safeRecoveryResetActionsInvokeCallbacks() {
        var resetLayout = false
        var resetSettings = false
        var clearWidgets = false
        setLauncherContent {
            SafeRecoveryScreen(
                summary = PreviousCrashSummary(
                    timestamp = "2026-07-05T12:00:00",
                    thread = "main",
                    versionName = "0.2.3",
                    versionCode = "5",
                    exceptionClass = "java.lang.IllegalStateException",
                ),
                actionMessage = null,
                actionInProgress = false,
                onResetLayout = { resetLayout = true },
                onResetSettings = { resetSettings = true },
                onClearWidgets = { clearWidgets = true },
                onExportDiagnostics = {},
                onContinue = {},
            )
        }

        composeRule.onNodeWithText("Reset layout").performClick()
        composeRule.onNodeWithText("Reset settings").performClick()
        composeRule.onNodeWithText("Clear widgets").performClick()
        composeRule.runOnIdle {
            assertTrue(resetLayout)
            assertTrue(resetSettings)
            assertTrue(clearWidgets)
        }
    }

    @Test
    fun pluralResourcesResolveForLocaleStress() {
        val englishContext = localizedContext("en")
        assertEquals("1 hidden app", englishContext.hiddenAppsCountText(1))
        assertEquals("2 hidden apps", englishContext.hiddenAppsCountText(2))
        assertEquals("Reset 2 widgets.", englishContext.widgetResetFeedback(2))

        val pseudoContext = localizedContext("en-XA")
        assertEquals(
            "[!! 2 visible pages !!]",
            pseudoContext.resources.getQuantityString(R.plurals.settings_visible_page_count, 2, 2),
        )
        assertTrue(
            pseudoContext.getString(
                R.string.settings_clear_finder_history_summary,
                "[!! 1 target !!]",
                "[!! 2 launches !!]",
            ).startsWith("[!!"),
        )

        val arabicContext = localizedContext("ar")
        assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, arabicContext.resources.configuration.layoutDirection)
        assertTrue(arabicContext.resources.getQuantityString(R.plurals.settings_visible_page_count, 3, 3).contains("صفحات"))
    }

    @Test
    fun settingsOverlayRendersPseudoLocaleAndRtlPrimaryControls() {
        setLauncherContent(localeTag = "en-XA", layoutDirection = androidx.compose.ui.unit.LayoutDirection.Rtl) {
            SettingsOverlay(
                layoutContract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915),
                mediaPageEnabled = true,
                appsButtonEnabled = true,
                appLabelsEnabled = true,
                widgetLabelsEnabled = true,
                swipeDownForNotifications = true,
                addNewAppsToHomeScreen = true,
                notificationBadgeMode = NotificationBadgeMode.DOTS_AND_NUMBER,
                notificationBadgePermissionGranted = true,
                notificationBadgeActiveAppCount = 1,
                notificationBadgeActiveCount = 2,
                finderContactsEnabled = false,
                finderContactsPermissionGranted = false,
                homeLayoutMode = HomeLayoutMode.HOME_AND_APPS_SCREENS,
                lockHomeScreenLayout = false,
                motionPreset = MotionPresetMode.STANDARD,
                folderGrid = FolderGridMode.GRID_3X4,
                defaultHomePageLabel = "Home",
                homePageCount = 3,
                appsScreenSortTitle = "Custom order",
                hiddenAppCount = 2,
                boundWidgetCount = 2,
                finderUsageTargetCount = 1,
                finderUsageLaunchCount = 2,
                backupFileName = "backup.json",
                diagnosticsFileName = "diagnostics.txt",
                defaultLauncherState = DefaultLauncherState.Unknown,
                focusedSettingTitle = null,
                onClose = {},
                onMediaPageChange = {},
                onAppsButtonChange = {},
                onAppLabelsChange = {},
                onWidgetLabelsChange = {},
                onSwipeDownChange = {},
                onAddNewAppsToHomeScreenChange = {},
                onNotificationBadgeModeChange = {},
                onOpenNotificationBadgeSettings = {},
                onFinderContactsEnabledChange = {},
                onRequestFinderContactsPermission = {},
                onHomeLayoutModeChange = {},
                onLockHomeScreenLayoutChange = {},
                onMotionPresetChange = {},
                onFolderGridChange = {},
                onResetWidgets = {},
                onClearFinderUsageStats = {},
                onExportBackup = {},
                onImportBackup = {},
                onExportDiagnostics = {},
                onOpenDefaultLauncherSettings = {},
            )
        }

        composeRule.onNodeWithText("[!! Home screen settings !!]").assertIsDisplayed()
        composeRule.onNodeWithText("[!! Close !!]").assertIsDisplayed()
        composeRule.onNodeWithText("[!! 3 visible pages !!]").assertIsDisplayed()
        composeRule.onAllNodes(hasText("[!! 2 hidden apps !!]", substring = true)).assertCountEquals(1)
    }

    @Test
    fun drawerFinderRendersPseudoLocaleAndRtlSearchControls() {
        val apps = listOf(CloneApp("clock", "Clock", color = Color(0xFF4A88FF)))
        setLauncherContent(localeTag = "en-XA", layoutDirection = androidx.compose.ui.unit.LayoutDirection.Rtl) {
            DrawerOverlay(
                layoutContract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915),
                query = "settings",
                apps = apps,
                appsScreenApps = apps,
                drawerPages = listOf(apps),
                homeLayoutMode = HomeLayoutMode.HOME_AND_APPS_SCREENS,
                drawerSortMode = DrawerSortMode.CUSTOM_ORDER,
                drawerPageIndex = 0,
                hiddenAppCount = 2,
                settingResults = listOf(
                    FinderSettingResult(
                        type = FinderSettingType.HOME_SCREEN_LAYOUT,
                        title = "Home screen layout",
                        category = "Layout",
                        value = "Home and Apps screens",
                    ),
                ),
                actionResults = emptyList(),
                shortcutResults = emptyList(),
                contactResults = emptyList(),
                recentSearches = emptyList(),
                onQueryChange = {},
                onClose = {},
                onOpenSettings = {},
                onSelectSortMode = {},
                onSelectDrawerPage = {},
                onOpenHideApps = {},
                onSelectRecentSearch = {},
                onOpenSettingResult = {},
                onOpenAction = {},
                onOpenContact = {},
                onOpenApp = {},
                onOpenAppActions = { _, _ -> },
                appLabelsEnabled = true,
            )
        }

        composeRule.onNodeWithText("[!! Finder !!]").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Home screen layout, Layout, Home and Apps screens").assertIsDisplayed()
    }

    @Test
    fun widgetPickerRendersPseudoLocaleAndRtlPrimaryControls() {
        setLauncherContent(localeTag = "en-XA", layoutDirection = androidx.compose.ui.unit.LayoutDirection.Rtl) {
            WidgetPickerOverlay(
                layoutContract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915),
                categories = listOf("Recommended"),
                selectedCategory = "Recommended",
                searchQuery = "",
                widgets = listOf(WidgetTemplateModel("Calendar", "Month view", "Recommended", "4 x 2", Color(0xFFFF8B7B))),
                providerWarning = null,
                targetPageLabel = "Home",
                onSelectCategory = {},
                onSearchQueryChange = {},
                onAddWidget = {},
                onClose = {},
            )
        }

        composeRule.onNodeWithText("[!! Widgets !!]").assertIsDisplayed()
        composeRule.onNodeWithText("[!! Close !!]").assertIsDisplayed()
        composeRule.onNodeWithText("[!! Search widgets !!]").assertIsDisplayed()
    }

    @Test
    fun homeSurfaceRendersPseudoLocaleAndRtlPrimaryControls() {
        val app = CloneApp("clock", "Clock", color = Color(0xFF4A88FF))
        setLauncherContent(localeTag = "en-XA", layoutDirection = androidx.compose.ui.unit.LayoutDirection.Rtl) {
            HomeSurface(
                layoutContract = resolveLauncherLayoutContract(widthDp = 412, heightDp = 915),
                currentHomePage = HomePageModel(
                    id = 1,
                    label = "Home",
                    eyebrow = "Now",
                    value = "Ready",
                    status = "Online",
                    note = "Locale smoke",
                    widgets = emptyList(),
                    items = listOf(AppItemModel(app)),
                ),
                isMediaPage = false,
                dockApps = listOf(app),
                pageIndex = 0,
                pageCount = 1,
                timeText = "6:00",
                dateText = "Sunday",
                homeLayoutMode = HomeLayoutMode.HOME_AND_APPS_SCREENS,
                lockHomeScreenLayout = false,
                swipeDownForNotifications = true,
                appLabelsEnabled = true,
                widgetLabelsEnabled = true,
                appsButtonEnabled = true,
                isHomeItemDragActive = false,
                onOpenDrawer = {},
                onOpenNotifications = {},
                onOpenEditMode = {},
                onReorderHomeItem = { _, _ -> },
                onCreateFolder = { _, _ -> },
                onAddAppToFolder = { _, _ -> },
                onHomeItemDragStateChange = {},
                onOpenApp = {},
                onOpenAppActions = { _, _ -> },
                onOpenFolder = {},
                onMoveWidget = { _, _, _ -> },
                onResizeWidget = { _, _, _ -> },
                onRemoveWidget = {},
                onOpenWidgetActions = {},
                onPageChange = {},
            )
        }

        composeRule.onNodeWithText("[!! Finder !!]").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Apps screen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clock").assertIsDisplayed()
    }

    private fun setLauncherContent(
        localeTag: String = "en",
        layoutDirection: androidx.compose.ui.unit.LayoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        val localizedContext = localizedContext(localeTag)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection,
            ) {
                OneUiHomeCloneTheme(content)
            }
        }
    }

    private fun localizedContext(localeTag: String): Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locale = Locale.forLanguageTag(localeTag)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
