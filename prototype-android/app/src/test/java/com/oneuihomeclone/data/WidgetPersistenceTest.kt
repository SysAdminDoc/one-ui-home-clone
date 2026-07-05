package com.oneuihomeclone.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetPersistenceTest {

    @Test
    fun pendingWidgetIdCodec_dropsInvalidValuesAndSortsOutput() {
        assertEquals(setOf("2", "10"), WidgetPersistence.encodePendingIds(setOf(10, -4, 2, 0)))
        assertEquals(setOf(2, 10), WidgetPersistence.decodePendingIds(setOf("x", "10", "-1", "2")))
    }
}
