package com.oneuihomeclone.contacts

import android.net.Uri
import com.oneuihomeclone.ui.FinderContactResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FinderContactsRepositoryTest {

    @Test
    fun searchQueriesOnlyWhenSettingAndPermissionAreGranted() = runBlocking {
        var loadCount = 0
        val repository = FinderContactsRepository(RuntimeEnvironment.getApplication()) { _, _ ->
            loadCount += 1
            listOf(finderContact("1", "Ada Lovelace"))
        }

        assertTrue(repository.search("ada", enabled = false, permissionGranted = true).results.isEmpty())
        assertTrue(repository.search("ada", enabled = true, permissionGranted = false).results.isEmpty())
        assertEquals(0, loadCount)

        val granted = repository.search("ada", enabled = true, permissionGranted = true)
        assertEquals(listOf("Ada Lovelace"), granted.results.map(FinderContactResult::displayName))
        assertEquals(1, loadCount)

        val revoked = repository.search("ada", enabled = true, permissionGranted = false)
        assertTrue(revoked.results.isEmpty())
        assertEquals(1, loadCount)
    }

    private fun finderContact(
        id: String,
        displayName: String,
    ): FinderContactResult =
        FinderContactResult(
            id = id,
            displayName = displayName,
            subtitle = null,
            lookupUri = Uri.parse("content://contacts/$id"),
        )
}
