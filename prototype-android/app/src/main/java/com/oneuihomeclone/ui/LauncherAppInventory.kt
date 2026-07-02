package com.oneuihomeclone.ui

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.UserHandle
import android.os.UserManager
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
        val iconBitmap = if (index < MAX_ICONS_LOADED_EAGERLY) {
            runCatching {
                activityInfo
                    .getIcon(0)
                    .toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX, config = Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            }.getOrNull()
        } else {
            null
        }
        return CloneApp(
            id = componentId,
            name = record.displayLabel(),
            launchIntent = Intent.makeMainActivity(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            launchTarget = LauncherAppLaunchTarget(
                componentName = component,
                user = user,
            ),
            icon = iconBitmap,
            color = fallbackColorFor(componentId),
        )
    }

    private data class LauncherActivitySource(
        val record: LauncherAppRecord,
        val activityInfo: LauncherActivityInfo,
        val user: UserHandle,
    )

    private companion object {
        private const val TAG = "OneUiHome/apps"
        private const val ICON_SIZE_PX = 144
    }
}
