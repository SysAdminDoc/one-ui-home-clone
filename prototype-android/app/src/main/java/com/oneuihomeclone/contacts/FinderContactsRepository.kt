package com.oneuihomeclone.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.oneuihomeclone.ui.FinderContactResult
import com.oneuihomeclone.ui.MAX_DEVICE_CONTACT_ROWS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class FinderContactSearchState(
    val results: List<FinderContactResult> = emptyList(),
    val indexedCount: Int = 0,
    val enabled: Boolean = false,
    val permissionGranted: Boolean = false,
)

internal class FinderContactsRepository(
    context: Context,
    private val loader: suspend (String, Int) -> List<FinderContactResult> = { query, limit ->
        queryDeviceContacts(context.applicationContext, query, limit)
    },
) {
    private val appContext = context.applicationContext

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    suspend fun search(
        query: String,
        enabled: Boolean,
        permissionGranted: Boolean = hasContactsPermission(),
    ): FinderContactSearchState {
        val trimmedQuery = query.trim()
        if (!enabled || !permissionGranted || trimmedQuery.length < MIN_CONTACT_QUERY_LENGTH) {
            return FinderContactSearchState(
                enabled = enabled,
                permissionGranted = permissionGranted,
            )
        }

        val results = runCatching {
            withContext(Dispatchers.IO) {
                loader(trimmedQuery, MAX_DEVICE_CONTACT_ROWS)
                    .distinctBy(FinderContactResult::id)
                    .take(MAX_DEVICE_CONTACT_ROWS)
            }
        }.getOrDefault(emptyList())

        return FinderContactSearchState(
            results = results,
            indexedCount = results.size,
            enabled = enabled,
            permissionGranted = permissionGranted,
        )
    }

    fun openContact(contact: FinderContactResult): Boolean =
        runCatching {
            appContext.startActivity(
                Intent(Intent.ACTION_VIEW, contact.lookupUri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)

    private companion object {
        private const val MIN_CONTACT_QUERY_LENGTH = 2
    }
}

private fun queryDeviceContacts(
    context: Context,
    query: String,
    limit: Int,
): List<FinderContactResult> {
    val filterUri = Uri.withAppendedPath(
        ContactsContract.Contacts.CONTENT_FILTER_URI,
        Uri.encode(query),
    )
    val projection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
    )
    val contacts = mutableListOf<FinderContactResult>()
    context.contentResolver.query(
        filterUri,
        projection,
        null,
        null,
        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        val lookupColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
        val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        while (cursor.moveToNext() && contacts.size < limit) {
            val id = cursor.getLong(idColumn)
            val lookupKey = cursor.getString(lookupColumn)?.takeIf { it.isNotBlank() } ?: continue
            val displayName = cursor.getString(nameColumn)?.trim()?.takeIf { it.isNotBlank() } ?: continue
            val lookupUri = ContactsContract.Contacts.getLookupUri(id, lookupKey) ?: continue
            contacts += FinderContactResult(
                id = lookupUri.toString(),
                displayName = displayName,
                subtitle = null,
                lookupUri = lookupUri,
            )
        }
    }
    return contacts
}
