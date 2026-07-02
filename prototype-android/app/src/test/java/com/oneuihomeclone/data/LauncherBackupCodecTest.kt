package com.oneuihomeclone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    fun decode_rejectsUnknownSchemaAndOversizedPayloads() {
        assertNull(LauncherBackupCodec.decode("""{"schemaVersion":99}"""))
        assertNull(LauncherBackupCodec.decode("x".repeat(512 * 1024 + 1)))
    }
}
