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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val LAYOUT_DS_NAME = "one_ui_home_clone_layout"
private const val LAYOUT_SCHEMA_VERSION = 1

private val Context.launcherLayoutDataStore: DataStore<Preferences> by preferencesDataStore(
    name = LAYOUT_DS_NAME,
)

data class PersistedLauncherLayout(
    val pages: List<PersistedHomePage>,
    val defaultHomePageIndex: Int,
    val hiddenAppIds: Set<String>,
    val recentSearches: List<String>,
    val nextPageId: Int,
    val nextFolderId: Int,
    val drawerCustomAppIds: List<String> = emptyList(),
)

data class PersistedHomePage(
    val id: Int,
    val label: String,
    val eyebrow: String,
    val value: String,
    val status: String,
    val note: String,
    val items: List<PersistedHomeItem>,
)

sealed interface PersistedHomeItem {
    data class App(val appId: String) : PersistedHomeItem
    data class Folder(
        val id: String,
        val title: String,
        val appIds: List<String>,
    ) : PersistedHomeItem
}

class LauncherLayoutStore(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.launcherLayoutDataStore

    val state: Flow<PersistedLauncherLayout?> = dataStore.data
        .catch { cause ->
            Log.w(TAG, "Failed to read launcher layout store - returning empty state", cause)
            emit(emptyPreferences())
        }
        .map { prefs -> decode(prefs[Keys.SCHEMA], prefs[Keys.LAYOUT_JSON]) }

    suspend fun read(): PersistedLauncherLayout? = state.first()

    suspend fun save(layout: PersistedLauncherLayout) {
        dataStore.edit { prefs ->
            prefs[Keys.SCHEMA] = LAYOUT_SCHEMA_VERSION
            prefs[Keys.LAYOUT_JSON] = encode(layout)
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private object Keys {
        val SCHEMA = intPreferencesKey("schema_version")
        val LAYOUT_JSON = stringPreferencesKey("layout_json")
    }

    companion object {
        private const val TAG = "LauncherLayoutStore"
        private const val MAX_DECODE_BYTES = 256 * 1024
        private const val MAX_PAGES = 32
        private const val MAX_ITEMS_PER_PAGE = 128
        private const val MAX_RECENTS = 12
        private const val MAX_DRAWER_ORDER = 2048

        internal fun encode(layout: PersistedLauncherLayout): String =
            JSONObject()
                .put("pages", JSONArray().also { pages ->
                    layout.pages.take(MAX_PAGES).forEach { page -> pages.put(encodePage(page)) }
                })
                .put("defaultHomePageIndex", layout.defaultHomePageIndex)
                .put("hiddenAppIds", JSONArray(layout.hiddenAppIds.toList().sorted()))
                .put("recentSearches", JSONArray(layout.recentSearches.take(MAX_RECENTS)))
                .put("nextPageId", layout.nextPageId)
                .put("nextFolderId", layout.nextFolderId)
                .put("drawerCustomAppIds", JSONArray(layout.drawerCustomAppIds.distinct().take(MAX_DRAWER_ORDER)))
                .toString()

        internal fun decode(schema: Int?, json: String?): PersistedLauncherLayout? {
            if (json.isNullOrBlank()) return null
            if (json.length > MAX_DECODE_BYTES) {
                Log.w(TAG, "Layout JSON exceeds ${MAX_DECODE_BYTES}B cap (${json.length}B) - discarding")
                return null
            }
            return when (schema ?: LAYOUT_SCHEMA_VERSION) {
                1 -> decodeV1(json)
                else -> {
                    Log.w(TAG, "Unknown layout schema version $schema - returning empty state")
                    null
                }
            }
        }

        private fun encodePage(page: PersistedHomePage): JSONObject =
            JSONObject()
                .put("id", page.id)
                .put("label", page.label)
                .put("eyebrow", page.eyebrow)
                .put("value", page.value)
                .put("status", page.status)
                .put("note", page.note)
                .put("items", JSONArray().also { arr ->
                    page.items.take(MAX_ITEMS_PER_PAGE).forEach { item ->
                        arr.put(
                            when (item) {
                                is PersistedHomeItem.App -> JSONObject()
                                    .put("type", "app")
                                    .put("appId", item.appId)
                                is PersistedHomeItem.Folder -> JSONObject()
                                    .put("type", "folder")
                                    .put("id", item.id)
                                    .put("title", item.title)
                                    .put("appIds", JSONArray(item.appIds))
                            },
                        )
                    }
                })

        private fun decodeV1(json: String): PersistedLauncherLayout? = runCatching {
            val root = JSONObject(json)
            val pagesArray = root.optJSONArray("pages") ?: JSONArray()
            val pages = buildList {
                for (i in 0 until minOf(pagesArray.length(), MAX_PAGES)) {
                    decodePage(pagesArray.optJSONObject(i))?.let(::add)
                }
            }
            PersistedLauncherLayout(
                pages = pages,
                defaultHomePageIndex = root.optInt("defaultHomePageIndex", 0),
                hiddenAppIds = root.optJSONArray("hiddenAppIds").toStringSet(),
                recentSearches = root.optJSONArray("recentSearches").toStringList(MAX_RECENTS),
                nextPageId = root.optInt("nextPageId", pages.maxOfOrNull { it.id + 1 } ?: 1),
                nextFolderId = root.optInt("nextFolderId", 1),
                drawerCustomAppIds = root.optJSONArray("drawerCustomAppIds").toStringList(MAX_DRAWER_ORDER).distinct(),
            )
        }.getOrElse { cause ->
            Log.w(TAG, "Discarding malformed layout JSON (${cause.javaClass.simpleName})")
            null
        }

        private fun decodePage(obj: JSONObject?): PersistedHomePage? {
            if (obj == null) return null
            val id = obj.optInt("id", -1)
            if (id <= 0) return null
            val itemsArray = obj.optJSONArray("items") ?: JSONArray()
            return PersistedHomePage(
                id = id,
                label = obj.optString("label", "Home $id"),
                eyebrow = obj.optString("eyebrow"),
                value = obj.optString("value"),
                status = obj.optString("status"),
                note = obj.optString("note"),
                items = buildList {
                    for (i in 0 until minOf(itemsArray.length(), MAX_ITEMS_PER_PAGE)) {
                        decodeItem(itemsArray.optJSONObject(i))?.let(::add)
                    }
                },
            )
        }

        private fun decodeItem(obj: JSONObject?): PersistedHomeItem? {
            if (obj == null) return null
            return when (obj.optString("type")) {
                "app" -> {
                    val appId = obj.optString("appId")
                    if (appId.isBlank()) null else PersistedHomeItem.App(appId)
                }
                "folder" -> {
                    val id = obj.optString("id")
                    if (id.isBlank()) {
                        null
                    } else {
                        PersistedHomeItem.Folder(
                            id = id,
                            title = obj.optString("title", "Folder"),
                            appIds = obj.optJSONArray("appIds").toStringList(MAX_ITEMS_PER_PAGE),
                        )
                    }
                }
                else -> null
            }
        }

        private fun JSONArray?.toStringList(limit: Int): List<String> {
            if (this == null) return emptyList()
            return buildList {
                for (i in 0 until minOf(length(), limit)) {
                    val value = optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        }

        private fun JSONArray?.toStringSet(): Set<String> =
            toStringList(MAX_ITEMS_PER_PAGE * MAX_PAGES).toSet()
    }
}
