package com.oneuihomeclone.data

import com.oneuihomeclone.PreviousCrashSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LauncherDiagnosticsTest {

    @Test
    fun buildLauncherDiagnostics_exportsOnlySanitizedCounts() {
        val diagnostics = buildLauncherDiagnostics(
            LauncherDiagnosticsSnapshot(
                versionName = "0.2.5\nleak",
                versionCode = 7,
                buildType = "debug",
                sdkInt = 35,
                targetSdk = 35,
                internetPermissionDeclared = false,
                notificationBadgeMode = "dots_and_number",
                notificationBadgeAccessGranted = true,
                notificationBadgePackageCount = 3,
                notificationBadgeTotalCount = 8,
                defaultLauncherChecked = true,
                isDefaultLauncher = false,
                canOpenDefaultLauncherSettings = true,
                previousCrash = PreviousCrashSummary(
                    timestamp = "2026-07-02 09:00:00.000\nraw second line",
                    thread = "main",
                    versionName = "0.2.2",
                    versionCode = "4",
                    exceptionClass = "java.lang.IllegalStateException",
                ),
                appInventoryLoaded = true,
                appCount = 42,
                launchableAppCount = 40,
                unavailableAppCount = 2,
                restoredPlaceholderAppCount = 1,
                finderIndexedAppCount = 39,
                finderIndexedShortcutCount = 6,
                finderContactsEnabled = true,
                finderContactsPermissionGranted = true,
                finderIndexedContactCount = 7,
                finderRecentSearchCount = 2,
                finderUsageTargetCount = 4,
                finderUsageLaunchCount = 11,
                hiddenAppCount = 3,
                homePageCount = 2,
                folderCount = 1,
                directHomeAppCount = 8,
                folderAppCount = 4,
                homeWidgetCount = 3,
                boundWidgetCount = 2,
                persistedWidgetCount = 2,
                widgetTemplateCount = 12,
                realWidgetProviderCount = 5,
                widgetProviderProfileCount = 3,
                widgetProviderUnavailableProfileCount = 1,
            ),
        )

        assertTrue(diagnostics.contains("apps.total=42"))
        assertTrue(diagnostics.contains("widgets.persisted=2"))
        assertTrue(diagnostics.contains("widgets.providerProfiles=3"))
        assertTrue(diagnostics.contains("widgets.providerProfilesUnavailable=1"))
        assertTrue(diagnostics.contains("badges.notificationAccessGranted=true"))
        assertTrue(diagnostics.contains("badges.packageCount=3"))
        assertTrue(diagnostics.contains("badges.totalCount=8"))
        assertTrue(diagnostics.contains("finder.indexedApps=39"))
        assertTrue(diagnostics.contains("finder.indexedShortcuts=6"))
        assertTrue(diagnostics.contains("finder.contactsEnabled=true"))
        assertTrue(diagnostics.contains("finder.contactsPermissionGranted=true"))
        assertTrue(diagnostics.contains("finder.indexedContacts=7"))
        assertTrue(diagnostics.contains("finder.recentSearches=2"))
        assertTrue(diagnostics.contains("finder.usageTargets=4"))
        assertTrue(diagnostics.contains("finder.usageLaunches=11"))
        assertTrue(diagnostics.contains("defaultLauncher.isDefault=false"))
        assertTrue(diagnostics.contains("previousCrash.exception=java.lang.IllegalStateException"))
        assertTrue(diagnostics.contains("privacy.rawAppNames=false"))
        assertTrue(diagnostics.contains("privacy.rawSearchHistory=false"))
        assertTrue(diagnostics.contains("privacy.rawNotificationText=false"))
        assertTrue(diagnostics.contains("privacy.rawContactNames=false"))
        assertFalse(diagnostics.contains("raw second line"))
        assertFalse(diagnostics.contains("Gmail"))
        assertFalse(diagnostics.contains("recent search"))
    }

    @Test
    fun diagnosticsFileStore_writesSanitizedBundle() = runBlocking {
        val store = LauncherDiagnosticsFileStore(RuntimeEnvironment.getApplication())

        val file = store.export(
            LauncherDiagnosticsSnapshot(
                versionName = "0.2.5",
                versionCode = 7,
                buildType = "debug",
                sdkInt = 35,
                targetSdk = 35,
                internetPermissionDeclared = false,
                notificationBadgeMode = "off",
                notificationBadgeAccessGranted = false,
                notificationBadgePackageCount = 0,
                notificationBadgeTotalCount = 0,
                defaultLauncherChecked = true,
                isDefaultLauncher = true,
                canOpenDefaultLauncherSettings = true,
                previousCrash = null,
                appInventoryLoaded = true,
                appCount = 9,
                launchableAppCount = 8,
                unavailableAppCount = 1,
                restoredPlaceholderAppCount = 0,
                finderIndexedAppCount = 8,
                finderIndexedShortcutCount = 1,
                finderContactsEnabled = false,
                finderContactsPermissionGranted = false,
                finderIndexedContactCount = 0,
                finderRecentSearchCount = 0,
                finderUsageTargetCount = 0,
                finderUsageLaunchCount = 0,
                hiddenAppCount = 0,
                homePageCount = 2,
                folderCount = 1,
                directHomeAppCount = 5,
                folderAppCount = 3,
                homeWidgetCount = 2,
                boundWidgetCount = 1,
                persistedWidgetCount = 1,
                widgetTemplateCount = 4,
                realWidgetProviderCount = 2,
                widgetProviderProfileCount = 1,
                widgetProviderUnavailableProfileCount = 0,
            ),
        )

        val content = file.readText()

        assertTrue(file.name == "one-ui-home-clone-diagnostics.txt")
        assertTrue(content.contains("defaultLauncher.isDefault=true"))
        assertTrue(content.contains("badges.mode=off"))
        assertTrue(content.contains("apps.total=9"))
        assertTrue(content.contains("widgets.realProviders=2"))
        assertTrue(content.contains("widgets.providerProfiles=1"))
        assertTrue(content.contains("privacy.rawAppNames=false"))
        assertTrue(content.contains("privacy.rawNotificationText=false"))
        assertTrue(content.contains("privacy.rawContactNames=false"))
        assertFalse(content.contains("Calendar"))
        assertFalse(content.contains("query="))
    }
}
