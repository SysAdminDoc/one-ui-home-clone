package com.oneuihomeclone.baselineprofile

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun launcherCoreJourneys() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
    ) {
        val uiDevice = device
        uiDevice.pressHome()
        startActivityAndWait(launcherIntent())
        uiDevice.waitForIdle()

        openAppsDrawer(uiDevice)
        searchFinder(uiDevice)
        launchFirstVisibleApp(uiDevice)
    }

    private fun openAppsDrawer(device: UiDevice) {
        val width = device.displayWidth
        val height = device.displayHeight
        device.swipe(width / 2, (height * 0.82f).toInt(), width / 2, (height * 0.28f).toInt(), 28)
        device.waitForIdle()
    }

    private fun searchFinder(device: UiDevice) {
        val searchField = device.wait(
            Until.findObject(By.text("Search from the bottom")),
            UI_WAIT_MS,
        ) ?: device.findObject(By.text("Search apps and settings"))
        searchField?.click()
        device.waitForIdle()
        device.executeShellCommand("input text settings")
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private fun launchFirstVisibleApp(device: UiDevice) {
        val app = device.wait(
            Until.findObject(By.clickable(true).hasDescendant(By.descContains("Settings"))),
            UI_WAIT_MS,
        ) ?: device.findObject(By.clickable(true).hasDescendant(By.descContains("Calendar")))
        app?.click()
        device.waitForIdle()
        device.pressHome()
        device.waitForIdle()
    }

    private fun launcherIntent(): Intent = Intent(Intent.ACTION_MAIN).apply {
        component = ComponentName(TARGET_PACKAGE, "$TARGET_PACKAGE.MainActivity")
        addCategory(Intent.CATEGORY_LAUNCHER)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private companion object {
        private const val TARGET_PACKAGE = "com.oneuihomeclone"
        private const val UI_WAIT_MS = 3_000L
    }
}
