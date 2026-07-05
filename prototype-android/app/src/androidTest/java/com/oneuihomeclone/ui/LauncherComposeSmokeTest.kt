package com.oneuihomeclone.ui

import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oneuihomeclone.PreviousCrashSummary
import com.oneuihomeclone.data.BoundWidget
import com.oneuihomeclone.data.LauncherBackup
import com.oneuihomeclone.data.LauncherBackupFileStore
import com.oneuihomeclone.data.LauncherBackupImportResult
import com.oneuihomeclone.data.LauncherDataStore
import com.oneuihomeclone.data.LauncherState
import com.oneuihomeclone.data.PersistedLauncherLayout
import com.oneuihomeclone.ui.theme.OneUiHomeCloneTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    private fun setLauncherContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OneUiHomeCloneTheme(content)
        }
    }
}
