package com.oneuihomeclone.ui

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object LauncherVisualAssetCache {
    private val appIconCache = object : LruCache<String, ImageBitmap>(MAX_APP_ICON_CACHE_ENTRIES) {}

    suspend fun loadAppIcon(context: Context, app: CloneApp): ImageBitmap? = withContext(Dispatchers.IO) {
        val target = app.launchTarget ?: return@withContext app.icon
        synchronized(appIconCache) {
            appIconCache.get(app.id)?.let { return@withContext it }
        }

        val appContext = context.applicationContext
        val launcherApps = appContext.getSystemService(LauncherApps::class.java) ?: return@withContext null
        val loadedIcon = runCatching {
            val info = launcherApps.resolveActivity(Intent.makeMainActivity(target.componentName), target.user)
                ?: return@runCatching null
            appContext.packageManager
                .getUserBadgedIcon(info.getIcon(0), target.user)
                .toBitmap(width = APP_ICON_SIZE_PX, height = APP_ICON_SIZE_PX, config = Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }.getOrNull()

        if (loadedIcon != null) {
            synchronized(appIconCache) {
                appIconCache.put(app.id, loadedIcon)
            }
        }
        loadedIcon
    }

    private const val APP_ICON_SIZE_PX = 144
}
