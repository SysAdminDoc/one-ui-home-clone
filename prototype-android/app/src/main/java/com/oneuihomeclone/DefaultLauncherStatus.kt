package com.oneuihomeclone

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

data class DefaultLauncherState(
    val checked: Boolean,
    val isDefaultLauncher: Boolean,
    val canOpenSettings: Boolean,
) {
    companion object {
        val Unknown = DefaultLauncherState(
            checked = false,
            isDefaultLauncher = true,
            canOpenSettings = false,
        )
    }
}

internal fun shouldShowDefaultLauncherPrompt(
    state: DefaultLauncherState,
    dismissedForSession: Boolean,
): Boolean = state.checked && !state.isDefaultLauncher && !dismissedForSession

fun queryDefaultLauncherState(context: Context): DefaultLauncherState {
    val appContext = context.applicationContext
    val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appContext.getSystemService(RoleManager::class.java)
    } else {
        null
    }
    val roleAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching { roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true }.getOrDefault(false)
    } else {
        false
    }
    val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleAvailable) {
        runCatching { roleManager?.isRoleHeld(RoleManager.ROLE_HOME) }.getOrNull()
    } else {
        null
    }
    val isDefault = roleHeld ?: appContext.resolvesAsDefaultHome()
    return DefaultLauncherState(
        checked = true,
        isDefaultLauncher = isDefault,
        canOpenSettings = roleAvailable || appContext.canResolveDefaultLauncherSettings(),
    )
}

fun createDefaultLauncherSettingsIntent(context: Context): Intent {
    val appContext = context.applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = appContext.getSystemService(RoleManager::class.java)
        val canRequestHomeRole = runCatching {
            roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }.getOrDefault(false)
        if (canRequestHomeRole && roleManager != null) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
    }

    val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
    if (appContext.canResolve(homeSettings)) return homeSettings
    val defaultApps = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    if (appContext.canResolve(defaultApps)) return defaultApps
    return Intent(Settings.ACTION_SETTINGS)
}

private fun Context.resolvesAsDefaultHome(): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == packageName
}

private fun Context.canResolveDefaultLauncherSettings(): Boolean =
    canResolve(Intent(Settings.ACTION_HOME_SETTINGS)) ||
        canResolve(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) ||
        canResolve(Intent(Settings.ACTION_SETTINGS))

private fun Context.canResolve(intent: Intent): Boolean =
    packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
