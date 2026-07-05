package com.oneuihomeclone.ui

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinderContactResultsTest {

    @Test
    fun buildFinderContactResults_requiresSettingAndPermission() {
        val contacts = listOf(finderContact("1", "Ada Lovelace"))

        assertTrue(
            buildFinderContactResults(
                query = "ada",
                contacts = contacts,
                enabled = false,
                permissionGranted = true,
            ).isEmpty(),
        )
        assertTrue(
            buildFinderContactResults(
                query = "ada",
                contacts = contacts,
                enabled = true,
                permissionGranted = false,
            ).isEmpty(),
        )
        assertEquals(
            listOf("Ada Lovelace"),
            buildFinderContactResults(
                query = "ada",
                contacts = contacts,
                enabled = true,
                permissionGranted = true,
            ).map(FinderContactResult::displayName),
        )
    }

    @Test
    fun buildFinderContactResults_clearsImmediatelyWhenPermissionRevoked() {
        val contacts = listOf(finderContact("1", "Ada Lovelace"))

        val granted = buildFinderContactResults(
            query = "ada",
            contacts = contacts,
            enabled = true,
            permissionGranted = true,
        )
        val revoked = buildFinderContactResults(
            query = "ada",
            contacts = granted,
            enabled = true,
            permissionGranted = false,
        )

        assertEquals(1, granted.size)
        assertTrue(revoked.isEmpty())
    }

    @Test
    fun buildFinderContactResults_usesSameTypoToleranceAsFinder() {
        val contacts = listOf(finderContact("1", "Lovelace Clock"))

        val results = buildFinderContactResults(
            query = "clok",
            contacts = contacts,
            enabled = true,
            permissionGranted = true,
        )

        assertEquals(listOf("Lovelace Clock"), results.map(FinderContactResult::displayName))
    }

    private fun finderContact(
        id: String,
        displayName: String,
        subtitle: String? = null,
    ): FinderContactResult =
        FinderContactResult(
            id = id,
            displayName = displayName,
            subtitle = subtitle,
            lookupUri = Uri.parse("content://contacts/$id"),
        )
}
