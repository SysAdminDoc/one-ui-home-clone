package com.oneuihomeclone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinderUsageStatsTest {

    @Test
    fun codec_roundTripsBoundedUsageStats() {
        val stats = FinderUsageStatsCodec.record(
            stats = FinderUsageStats(),
            targetKey = "app:clock",
            nowMillis = 100L,
        ).let { first ->
            FinderUsageStatsCodec.record(first, targetKey = "app:clock", nowMillis = 200L)
        }.let { second ->
            FinderUsageStatsCodec.record(second, targetKey = "setting:HOME", nowMillis = 150L)
        }

        val decoded = FinderUsageStatsCodec.decode(FinderUsageStatsCodec.encode(stats))

        assertEquals(2, decoded.targetCount)
        assertEquals(3, decoded.totalLaunchCount)
        assertEquals(2, decoded.entryFor("app:clock")?.count)
        assertEquals(200L, decoded.entryFor("app:clock")?.lastUsedAtMillis)
        assertEquals(1, decoded.entryFor("setting:HOME")?.count)
    }

    @Test
    fun codec_rejectsMalformedOrUnknownSchema() {
        assertTrue(FinderUsageStatsCodec.decode("{").entriesByTargetKey.isEmpty())
        assertTrue(FinderUsageStatsCodec.decode("""{"schemaVersion":99}""").entriesByTargetKey.isEmpty())
    }
}
