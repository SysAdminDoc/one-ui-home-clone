package com.oneuihomeclone.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetPersistenceInstrumentedTest {

    @Test
    fun pendingWidgetIds_roundTripAndConsumeOnce() = runBlocking {
        val store = WidgetPersistence(ApplicationProvider.getApplicationContext<Context>())
        store.clear()

        try {
            store.markPending(42)
            store.markPending(7)
            store.markPending(-1)
            store.clearPending(7)

            assertEquals(setOf(42), store.consumePendingWidgetIds())
            assertTrue(store.consumePendingWidgetIds().isEmpty())
        } finally {
            store.clear()
        }
    }
}
