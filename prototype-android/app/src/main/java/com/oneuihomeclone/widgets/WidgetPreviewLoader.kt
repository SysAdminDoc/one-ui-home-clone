package com.oneuihomeclone.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.core.content.res.ResourcesCompat

/**
 * Resolves a widget preview with the correct hierarchy:
 *
 * 1. `previewLayout` (API 31+, Android 12+) — a full RemoteViews XML, rendered at its
 *    natural widget size. This is what modern widgets advertise and what the picker
 *    should show where available.
 * 2. `previewImage` — a drawable resource provided by the widget. Legacy fallback for
 *    pre-API-31 widgets or widgets that never adopted the new manifest attribute.
 * 3. Provider icon — last resort, just so the picker never shows an empty cell.
 *
 * Loader is pure and synchronous; the caller is responsible for rendering (RemoteViews
 * `.apply` must go through the main thread because inflation may touch themed attrs).
 *
 * **Trust boundary:** callers must source `info` from [android.appwidget.AppWidgetManager]'s
 * `getInstalledProviders` / `getInstalledProvidersForProfile`, never from an untrusted
 * Parcelable (e.g. an Intent extra). A caller that parcels an attacker-controlled
 * `AppWidgetProviderInfo` could direct this loader to dereference `info.provider.packageName`
 * against an arbitrary package. The [loadDrawableFromProvider] helper resolves strictly
 * through [PackageManager.getResourcesForApplication] — any package missing from the
 * current user's install set surfaces as `NameNotFoundException` and falls back to
 * [PreviewSource.Empty], but this is not a substitute for source-side validation.
 */
sealed class PreviewSource {
    /** Inflatable layout — resolve via `RemoteViews(providerPackage, layoutResId).apply(ctx, parent)`. */
    data class RemoteLayout(val providerPackage: String, val layoutResId: Int) : PreviewSource()

    /** Drawable preview image. */
    data class PreviewImage(val drawable: Drawable) : PreviewSource()

    /** Provider icon as last-resort fallback. */
    data class ProviderIcon(val drawable: Drawable) : PreviewSource()

    /** Nothing loadable — caller should render a generic widget tile. */
    data object Empty : PreviewSource()
}

object WidgetPreviewLoader {

    private const val TAG = "WidgetPreviewLoader"

    fun load(context: Context, info: AppWidgetProviderInfo): PreviewSource {
        val providerPackage = info.provider?.packageName ?: return PreviewSource.Empty
        // Prefer previewLayout on API 31+ where declared.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val layoutId = info.previewLayout
            if (layoutId != 0) {
                return PreviewSource.RemoteLayout(
                    providerPackage = providerPackage,
                    layoutResId = layoutId,
                )
            }
        }
        // previewImage fallback.
        if (info.previewImage != 0) {
            val drawable = runCatching {
                loadDrawableFromProvider(context, providerPackage, info.previewImage)
            }.getOrNull()
            if (drawable != null) return PreviewSource.PreviewImage(drawable)
        }
        // Provider icon fallback.
        if (info.icon != 0) {
            val drawable = runCatching {
                loadDrawableFromProvider(context, providerPackage, info.icon)
            }.getOrNull()
            if (drawable != null) return PreviewSource.ProviderIcon(drawable)
        }
        return PreviewSource.Empty
    }

    private fun loadDrawableFromProvider(context: Context, pkg: String, resId: Int): Drawable? {
        val pm = context.packageManager
        val resources = try {
            pm.getResourcesForApplication(pkg)
        } catch (_: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package $pkg not found while loading widget preview")
            return null
        }
        // minSdk=28 guarantees the themed-getDrawable overload; ResourcesCompat keeps the
        // themed path explicit and works on the widget-provider's Resources.
        return runCatching { ResourcesCompat.getDrawable(resources, resId, context.theme) }.getOrNull()
    }
}
