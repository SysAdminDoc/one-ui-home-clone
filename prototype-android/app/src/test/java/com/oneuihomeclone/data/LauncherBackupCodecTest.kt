package com.oneuihomeclone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class LauncherBackupCodecTest {

    @Test
    fun encodeDecode_roundTripsSettingsLayoutAndWidgets() {
        val backup = LauncherBackup(
            settings = LauncherState(
                mediaPageEnabled = false,
                appsButtonEnabled = false,
                appLabelsEnabled = false,
                widgetLabelsEnabled = false,
                swipeDownForNotifications = false,
                addNewAppsToHomeScreen = false,
                notificationBadgeMode = NotificationBadgeModeKey.DOTS,
                finderContactsEnabled = true,
                lockHomeScreenLayout = true,
                homeLayoutMode = HomeLayoutKey.HOME_SCREEN_ONLY,
                drawerSortMode = DrawerSortKey.ALPHABETICAL,
                motionPreset = MotionPresetKey.REDUCED,
                folderGrid = FolderGridKey.GRID_5X5,
            ),
            layout = PersistedLauncherLayout(
                pages = listOf(
                    PersistedHomePage(
                        id = 7,
                        label = "Restored",
                        eyebrow = "Day",
                        value = "2",
                        status = "Ready",
                        note = "Note",
                        items = listOf(
                            PersistedHomeItem.App("app-one"),
                            PersistedHomeItem.Folder("folder-one", "Tools", listOf("app-two", "missing-app")),
                        ),
                    ),
                ),
                defaultHomePageIndex = 0,
                hiddenAppIds = setOf("hidden-one", "hidden-two"),
                recentSearches = listOf("Clock", "Weather"),
                nextPageId = 9,
                nextFolderId = 4,
                drawerCustomAppIds = listOf("app-two", "app-one"),
            ),
            widgets = listOf(
                BoundWidget(
                    hostWidgetId = 42,
                    providerPackage = "com.example",
                    providerClass = "com.example.ClockWidget",
                    pageIndex = 0,
                    cellX = 1,
                    cellY = 2,
                    spanX = 3,
                    spanY = 2,
                ),
            ),
            exportedAtMillis = 123456789L,
        )

        assertEquals(backup, LauncherBackupCodec.decode(LauncherBackupCodec.encode(backup)))
    }

    @Test
    fun decode_rejectsCorruptUnknownSchemaAndOversizedPayloads() {
        assertNull(LauncherBackupCodec.decode("{"))
        assertNull(LauncherBackupCodec.decode("""{"schemaVersion":99}"""))
        assertNull(LauncherBackupCodec.decode("x".repeat(512 * 1024 + 1)))
    }

    @Test
    fun validateLauncherRestore_reportsChangedRestoredAndMissingCounts() {
        val snapshot = LauncherRestoreSnapshot(
            settings = LauncherState(),
            layout = layoutWithItems(PersistedHomeItem.App("available")),
            widgets = listOf(widget(hostWidgetId = 1)),
        )
        val backup = LauncherBackup(
            settings = LauncherState(
                appsButtonEnabled = false,
                drawerSortMode = DrawerSortKey.ALPHABETICAL,
            ),
            layout = layoutWithItems(
                PersistedHomeItem.App("available"),
                PersistedHomeItem.Folder("folder", "Tools", listOf("available", "missing")),
            ),
            widgets = listOf(
                widget(hostWidgetId = 2),
                widget(
                    hostWidgetId = 3,
                    providerPackage = "com.missing",
                    providerClass = "com.missing.MissingWidget",
                ),
            ),
            exportedAtMillis = 42L,
        )

        val report = validateLauncherRestore(
            snapshot = snapshot,
            backup = backup,
            availableAppIds = setOf("available"),
            availableWidgetProviderKeys = setOf("com.example/com.example.ClockWidget"),
        )

        assertNotNull(report)
        report ?: return
        assertEquals(2, report.changedSettingCount)
        assertEquals(1, report.restoredPageCount)
        assertEquals(1, report.restoredAppCount)
        assertEquals(2, report.restoredWidgetCount)
        assertEquals(1, report.missingAppCount)
        assertEquals(1, report.missingWidgetProviderCount)
    }

    @Test
    fun validateLauncherRestore_rejectsOversizedCountsAndInvalidWidgets() {
        val snapshot = LauncherRestoreSnapshot(
            settings = LauncherState(),
            layout = layoutWithItems(),
            widgets = emptyList(),
        )
        val oversizedPageBackup = LauncherBackup(
            settings = LauncherState(),
            layout = PersistedLauncherLayout(
                pages = (1..33).map { index ->
                    PersistedHomePage(
                        id = index,
                        label = "Home $index",
                        eyebrow = "",
                        value = "",
                        status = "",
                        note = "",
                        items = emptyList(),
                    )
                },
                defaultHomePageIndex = 0,
                hiddenAppIds = emptySet(),
                recentSearches = emptyList(),
                nextPageId = 34,
                nextFolderId = 1,
            ),
            widgets = emptyList(),
            exportedAtMillis = 1L,
        )
        val invalidWidgetBackup = LauncherBackup(
            settings = LauncherState(),
            layout = layoutWithItems(),
            widgets = listOf(widget(hostWidgetId = 0)),
            exportedAtMillis = 1L,
        )

        assertNull(
            validateLauncherRestore(
                snapshot = snapshot,
                backup = oversizedPageBackup,
                availableAppIds = emptySet(),
                availableWidgetProviderKeys = emptySet(),
            ),
        )
        assertNull(
            validateLauncherRestore(
                snapshot = snapshot,
                backup = invalidWidgetBackup,
                availableAppIds = emptySet(),
                availableWidgetProviderKeys = emptySet(),
            ),
        )
    }

    @Test
    fun applyLauncherRestoreTransaction_rollsBackAfterWidgetFailure() = runBlocking {
        val snapshot = LauncherRestoreSnapshot(
            settings = LauncherState(appLabelsEnabled = false),
            layout = layoutWithItems(PersistedHomeItem.App("before")),
            widgets = listOf(widget(hostWidgetId = 10)),
        )
        val backup = LauncherBackup(
            settings = LauncherState(appsButtonEnabled = false),
            layout = layoutWithItems(PersistedHomeItem.App("after")),
            widgets = listOf(widget(hostWidgetId = 11)),
            exportedAtMillis = 99L,
        )
        var writtenSettings = snapshot.settings
        var writtenLayout = snapshot.layout
        var writtenWidgets = snapshot.widgets

        try {
            applyLauncherRestoreTransaction(
                snapshot = snapshot,
                backup = backup,
                writeSettings = { writtenSettings = it },
                writeLayout = { writtenLayout = it },
                writeWidgets = { throw IllegalStateException("widget write failed") },
            )
            fail("Expected widget write failure")
        } catch (_: IllegalStateException) {
            assertEquals(snapshot.settings, writtenSettings)
            assertEquals(snapshot.layout, writtenLayout)
            assertEquals(snapshot.widgets, writtenWidgets)
        }
    }

    private fun layoutWithItems(vararg items: PersistedHomeItem): PersistedLauncherLayout =
        PersistedLauncherLayout(
            pages = listOf(
                PersistedHomePage(
                    id = 1,
                    label = "Home",
                    eyebrow = "",
                    value = "",
                    status = "",
                    note = "",
                    items = items.toList(),
                ),
            ),
            defaultHomePageIndex = 0,
            hiddenAppIds = emptySet(),
            recentSearches = emptyList(),
            nextPageId = 2,
            nextFolderId = 1,
        )

    private fun widget(
        hostWidgetId: Int,
        providerPackage: String = "com.example",
        providerClass: String = "com.example.ClockWidget",
    ): BoundWidget =
        BoundWidget(
            hostWidgetId = hostWidgetId,
            providerPackage = providerPackage,
            providerClass = providerClass,
            pageIndex = 0,
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2,
        )
}
