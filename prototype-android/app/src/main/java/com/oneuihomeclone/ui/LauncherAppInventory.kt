package com.oneuihomeclone.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Process
import android.graphics.Bitmap
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.oneuihomeclone.R

internal data class LauncherAppRecord(
    val userSerial: Long,
    val packageName: String,
    val className: String,
    val label: String,
)

internal fun launcherAppStableId(
    userSerial: Long,
    packageName: String,
    className: String,
): String = "$userSerial:$packageName/$className"

internal fun LauncherAppRecord.stableId(): String =
    launcherAppStableId(userSerial, packageName, className)

internal fun LauncherAppRecord.displayLabel(): String {
    return label.ifBlank {
        packageName.substringAfterLast('.').ifBlank { "App" }.replaceFirstChar(Char::titlecase)
    }
}

internal fun normalizedLauncherAppRecords(
    records: List<LauncherAppRecord>,
    hostPackageName: String,
): List<LauncherAppRecord> {
    return records
        .asSequence()
        .filter { record ->
            record.packageName.isNotBlank() &&
                record.className.isNotBlank() &&
                record.packageName != hostPackageName
        }
        .distinctBy(LauncherAppRecord::stableId)
        .sortedWith(
            compareBy<LauncherAppRecord> { it.displayLabel().lowercase(Locale.getDefault()) }
                .thenBy { it.packageName }
                .thenBy { it.className }
                .thenBy { it.userSerial },
        )
        .toList()
}

internal class LauncherAppInventory(
    context: Context,
    private val fallbackApps: List<CloneApp>,
) {
    private val appContext = context.applicationContext
    private val launcherApps: LauncherApps? = appContext.getSystemService(LauncherApps::class.java)
    private val userManager: UserManager? = appContext.getSystemService(UserManager::class.java)
    private val packageManager = appContext.packageManager
    private val currentUserSerial: Long by lazy { userSerial(Process.myUserHandle()) }

    fun apps(): Flow<List<CloneApp>> = callbackFlow {
        val service = launcherApps
        if (service == null) {
            trySend(fallbackApps)
            close()
            return@callbackFlow
        }

        val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun refresh() {
            refreshScope.launch {
                trySend(loadApps())
            }
        }

        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()

            override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()

            override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = refresh()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = refresh()

            override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) = refresh()

            override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) = refresh()
        }

        service.registerCallback(callback)
        refresh()

        awaitClose {
            service.unregisterCallback(callback)
            refreshScope.cancel()
        }
    }

    suspend fun loadApps(): List<CloneApp> = withContext(Dispatchers.IO) {
        val service = launcherApps ?: return@withContext fallbackApps
        val loadedApps = runCatching {
            val sources = service.profiles.flatMap { user ->
                val userSerial = userSerial(user)
                runCatching {
                    service.getActivityList(null, user).map { info ->
                        LauncherActivitySource(
                            record = LauncherAppRecord(
                                userSerial = userSerial,
                                packageName = info.componentName.packageName,
                                className = info.componentName.className,
                                label = info.label?.toString().orEmpty(),
                            ),
                            activityInfo = info,
                            user = user,
                        )
                    }
                }.getOrElse { cause ->
                    Log.w(TAG, "Launcher profile query failed (${cause.javaClass.simpleName})")
                    emptyList()
                }
            }
            normalizedSources(sources).mapIndexed { index, source ->
                source.toCloneApp(index)
            }
        }.getOrElse { cause ->
            Log.w(TAG, "Launcher app query failed (${cause.javaClass.simpleName})")
            emptyList()
        }

        loadedApps.ifEmpty { fallbackApps }
    }

    fun launch(app: CloneApp): Boolean {
        if (!app.isLaunchable) return false

        val target = app.launchTarget
        if (target != null) {
            val service = launcherApps
            if (service != null) {
                val launched = runCatching {
                    service.startMainActivity(target.componentName, target.user, null, null)
                }.isSuccess
                if (launched) return true
            }
        }

        val launchIntent = app.launchIntent ?: return false
        return runCatching { appContext.startActivity(Intent(launchIntent)) }.isSuccess
    }

    fun openAppInfo(app: CloneApp): Boolean {
        val target = app.launchTarget
        val service = launcherApps
        if (target != null && service != null) {
            val opened = runCatching {
                service.startAppDetailsActivity(target.componentName, target.user, null, null)
            }.isSuccess
            if (opened) return true
        }

        val packageName = target?.componentName?.packageName
            ?: app.launchIntent?.component?.packageName
            ?: return false
        val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { appContext.startActivity(detailsIntent) }.isSuccess
    }

    suspend fun loadDynamicShortcuts(app: CloneApp): List<LauncherShortcutAction> = withContext(Dispatchers.IO) {
        val target = app.launchTarget ?: return@withContext emptyList()
        val service = launcherApps ?: return@withContext emptyList()
        val hasPermission = runCatching { service.hasShortcutHostPermission() }.getOrDefault(false)
        if (!hasPermission) return@withContext emptyList()

        runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(target.componentName.packageName)
                .setActivity(target.componentName)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
            service.getShortcuts(query, target.user).orEmpty()
                .asSequence()
                .filter { shortcut -> shortcut.`package` == target.componentName.packageName }
                .distinctBy { shortcut -> shortcut.id }
                .sortedWith(compareBy({ it.rank }, { it.shortLabel?.toString().orEmpty() }))
                .take(MAX_CONTEXT_SHORTCUTS)
                .map { shortcut ->
                    LauncherShortcutAction(
                        id = shortcut.id,
                        packageName = shortcut.`package`,
                        shortLabel = shortcut.shortLabel?.toString()?.ifBlank { shortcut.id } ?: shortcut.id,
                        longLabel = shortcut.longLabel?.toString()?.ifBlank { null },
                        isEnabled = shortcut.isEnabled,
                        disabledMessage = shortcut.disabledMessage?.toString()?.ifBlank { null },
                        user = target.user,
                    )
                }
                .toList()
        }.getOrElse { cause ->
            Log.w(TAG, "Shortcut query failed (${cause.javaClass.simpleName})")
            emptyList()
        }
    }

    fun launchShortcut(shortcut: LauncherShortcutAction): Boolean {
        val service = launcherApps ?: return false
        return runCatching {
            service.startShortcut(shortcut.packageName, shortcut.id, null, null, shortcut.user)
        }.isSuccess
    }

    private fun normalizedSources(sources: List<LauncherActivitySource>): List<LauncherActivitySource> {
        val sourceById = sources
            .filter { source ->
                source.record.packageName.isNotBlank() &&
                    source.record.className.isNotBlank() &&
                    source.record.packageName != appContext.packageName
            }
            .distinctBy { it.record.stableId() }
            .associateBy { it.record.stableId() }
        return normalizedLauncherAppRecords(
            records = sources.map(LauncherActivitySource::record),
            hostPackageName = appContext.packageName,
        ).mapNotNull { record -> sourceById[record.stableId()] }
    }

    private fun userSerial(user: UserHandle): Long {
        return runCatching { userManager?.getSerialNumberForUser(user) ?: user.hashCode().toLong() }
            .getOrDefault(user.hashCode().toLong())
    }

    private fun LauncherActivitySource.toCloneApp(index: Int): CloneApp {
        val component = activityInfo.componentName
        val componentId = record.stableId()
        val loadingProgress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { activityInfo.loadingProgress }.getOrDefault(1f)
        } else {
            1f
        }
        val installProgressPercent = loadingProgress
            .takeIf { it >= 0f && it < 1f }
            ?.let { (it * 100).roundToInt().coerceIn(0, 99) }
        val isPackageEnabled = runCatching {
            launcherApps?.isPackageEnabled(component.packageName, user) ?: true
        }.getOrDefault(true)
        val isActivityEnabled = runCatching {
            launcherApps?.isActivityEnabled(component, user) ?: true
        }.getOrDefault(true)
        val isSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED != 0 }
                .getOrDefault(false)
        } else {
            false
        }
        val statusLabel = when {
            installProgressPercent != null -> appContext.getString(R.string.app_status_installing)
            !isPackageEnabled || !isActivityEnabled || isSuspended -> appContext.getString(R.string.app_status_unavailable)
            else -> null
        }
        val profileBadge = profileBadgeFor(user, record.userSerial)
        val displayLabel = packageManager.getUserBadgedLabel(record.displayLabel(), user).toString()
        val iconBitmap = if (index < MAX_ICONS_LOADED_EAGERLY) {
            runCatching {
                packageManager
                    .getUserBadgedIcon(activityInfo.getIcon(0), user)
                    .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX, config = Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }.getOrNull()
        } else {
            null
        }
        return CloneApp(
            id = componentId,
            name = displayLabel,
            launchIntent = Intent.makeMainActivity(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            launchTarget = LauncherAppLaunchTarget(
                componentName = component,
                user = user,
            ),
            icon = iconBitmap,
            color = fallbackColorFor(componentId),
            profileBadge = profileBadge,
            statusLabel = statusLabel,
            installProgressPercent = installProgressPercent,
            isLaunchable = statusLabel == null,
        )
    }

    private data class LauncherActivitySource(
        val record: LauncherAppRecord,
        val activityInfo: LauncherActivityInfo,
        val user: UserHandle,
    )

    private fun profileBadgeFor(user: UserHandle, userSerial: Long): String? {
        if (userSerial == currentUserSerial) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val userType = runCatching { launcherApps?.getLauncherUserInfo(user)?.userType }.getOrNull()
            val labelRes = when (userType) {
                UserManager.USER_TYPE_PROFILE_MANAGED -> R.string.profile_badge_work
                UserManager.USER_TYPE_PROFILE_PRIVATE -> R.string.profile_badge_private
                UserManager.USER_TYPE_PROFILE_CLONE -> R.string.profile_badge_clone
                else -> R.string.profile_badge_profile
            }
            return appContext.getString(labelRes)
        }
        return appContext.getString(R.string.profile_badge_profile)
    }

    private companion object {
        private const val TAG = "OneUiHome/apps"
        private const val ICON_SIZE_PX = 144
    }
}
