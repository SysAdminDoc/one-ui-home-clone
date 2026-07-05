package com.oneuihomeclone.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WidgetPersistenceTest {

    @Test
    fun pendingWidgetIds_roundTripAndConsumeOnce() = runBlocking {
        val store = WidgetPersistence(RuntimeEnvironment.getApplication())
        store.clear()

        store.markPending(42)
        store.markPending(7)
        store.markPending(-1)
        store.clearPending(7)

        assertEquals(setOf(42), store.consumePendingWidgetIds())
        assertTrue(store.consumePendingWidgetIds().isEmpty())
        store.clear()
    }

    @Test
    fun pendingWidgetIdCodec_dropsInvalidValuesAndSortsOutput() {
        assertEquals(setOf("2", "10"), WidgetPersistence.encodePendingIds(setOf(10, -4, 2, 0)))
        assertEquals(setOf(2, 10), WidgetPersistence.decodePendingIds(setOf("x", "10", "-1", "2")))
    }
}
