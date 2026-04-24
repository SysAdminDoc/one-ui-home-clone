package com.oneuihomeclone.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent store of widget IDs the user has bound to this launcher.
 *
 * Scaffolding for v0.2.x real AppWidgetHost bind flow. Stored shape is versioned JSON so
 * the future cell-layout + profile fields can migrate without a wipe. Not yet wired into
 * the widget picker UI — that happens in the follow-up iteration that actually exercises
 * `ACTION_APPWIDGET_BIND` and builds the CellLayout drop handoff.
 *
 * Uses a separate DataStore file from launcher prefs so widget data can be wiped
 * (e.g. "Reset widgets" settings action) without touching toggle state.
 */
private const val WIDGETS_DS_NAME = "one_ui_home_clone_widgets"
private const val SCHEMA_VERSION = 1

private val Context.widgetsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = WIDGETS_DS_NAME,
)

/** Persistable description of a bound widget. Android-framework types (RemoteViews, Drawable) are deliberately excluded — they're re-resolved on rebind from the provider info. */
data class BoundWidget(
    val hostWidgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val pageIndex: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int,
)

class WidgetPersistence(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.widgetsDataStore

    val widgets: Flow<List<BoundWidget>> = dataStore.data
        .catch { cause ->
            // Corrupt JSON or disk failure — empty the in-memory view rather than crashing
            // the launcher. A follow-up `clear()` call is the user-visible recovery path.
            Log.w(TAG, "Failed to read widget store — returning empty list", cause)
            emit(emptyPreferences())
        }
        .map { prefs -> decode(prefs[Keys.SCHEMA], prefs[Keys.WIDGETS_JSON]) }

    suspend fun add(widget: BoundWidget) {
        dataStore.edit { prefs ->
            val current = decode(prefs[Keys.SCHEMA], prefs[Keys.WIDGETS_JSON]).toMutableList()
            // Replace existing entry with the same hostWidgetId (rebind case). Avoids a
            // duplicate stripe in the on-disk list after `deleteAppWidgetId` + re-add.
            current.removeAll { it.hostWidgetId == widget.hostWidgetId }
            current.add(widget)
            if (prefs[Keys.SCHEMA] == null) prefs[Keys.SCHEMA] = SCHEMA_VERSION
            prefs[Keys.WIDGETS_JSON] = encode(current)
        }
    }

    suspend fun remove(hostWidgetId: Int) {
        dataStore.edit { prefs ->
            val current = decode(prefs[Keys.SCHEMA], prefs[Keys.WIDGETS_JSON]).toMutableList()
            val removed = current.removeAll { it.hostWidgetId == hostWidgetId }
            if (removed) {
                prefs[Keys.WIDGETS_JSON] = encode(current)
            }
        }
    }

    suspend fun clear() {
        // Wipe everything including the schema stamp — a truly empty store needs no
        // version. Next `add()` will stamp the current SCHEMA_VERSION.
        dataStore.edit { prefs -> prefs.clear() }
    }

    private object Keys {
        val SCHEMA = intPreferencesKey("schema_version")
        val WIDGETS_JSON = stringPreferencesKey("bound_widgets_json")
    }

    companion object {
        private const val TAG = "WidgetPersistence"

        /** Defense-in-depth against self-inflicted DoS from a corrupt store. Worst realistic
         *  widget count: ~5 pages x 4 cols x 5 rows = 100 widgets. 1024 entries is ~10x that
         *  headroom and a 128 KB JSON ceiling cuts off runaway payloads well before OOM. */
        private const val MAX_DECODE_BYTES = 128 * 1024
        private const val MAX_DECODE_ENTRIES = 1024

        internal fun encode(list: List<BoundWidget>): String {
            val arr = JSONArray()
            list.forEach { w ->
                arr.put(
                    JSONObject()
                        .put("hostWidgetId", w.hostWidgetId)
                        .put("providerPackage", w.providerPackage)
                        .put("providerClass", w.providerClass)
                        .put("pageIndex", w.pageIndex)
                        .put("cellX", w.cellX)
                        .put("cellY", w.cellY)
                        .put("spanX", w.spanX)
                        .put("spanY", w.spanY),
                )
            }
            return arr.toString()
        }

        internal fun decode(schema: Int?, json: String?): List<BoundWidget> {
            if (json.isNullOrBlank()) return emptyList()
            if (json.length > MAX_DECODE_BYTES) {
                Log.w(TAG, "Widget JSON exceeds ${MAX_DECODE_BYTES}B cap (${json.length}B) — discarding")
                return emptyList()
            }
            // Version dispatch. schema=null is treated as SCHEMA_VERSION since v0.2.0 was
            // the first writer and the null branch can only occur on a partial write.
            return when (schema ?: SCHEMA_VERSION) {
                1 -> decodeV1(json)
                else -> {
                    Log.w(TAG, "Unknown widget schema version $schema — returning empty list")
                    emptyList()
                }
            }
        }

        private fun decodeV1(json: String): List<BoundWidget> = runCatching {
            val arr = JSONArray(json)
            if (arr.length() > MAX_DECODE_ENTRIES) {
                Log.w(TAG, "Widget JSON has ${arr.length()} entries > $MAX_DECODE_ENTRIES cap — discarding")
                return@runCatching emptyList<BoundWidget>()
            }
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        BoundWidget(
                            hostWidgetId = o.getInt("hostWidgetId"),
                            providerPackage = o.getString("providerPackage"),
                            providerClass = o.getString("providerClass"),
                            pageIndex = o.optInt("pageIndex", 0),
                            cellX = o.optInt("cellX", 0),
                            cellY = o.optInt("cellY", 0),
                            spanX = o.optInt("spanX", 1),
                            spanY = o.optInt("spanY", 1),
                        ),
                    )
                }
            }
        }.getOrElse { cause ->
            Log.w(TAG, "Discarding malformed widget JSON (${cause.javaClass.simpleName})")
            emptyList()
        }
    }
}
