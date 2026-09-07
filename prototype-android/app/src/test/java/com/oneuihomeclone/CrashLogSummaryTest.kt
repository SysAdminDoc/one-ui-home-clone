package com.oneuihomeclone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogSummaryTest {

    @Test
    fun summarizeCrashLog_readsHeaderFieldsOnly() {
        val summary = summarizeCrashLog(
            """
            timestamp=2026-07-02 12:00:00.000
            thread=main
            build.versionName=0.2.2
            build.versionCode=4
            exception=java.lang.IllegalStateException
            ---
            java.lang.IllegalStateException: contains user text
                at com.example.SearchResult
            """.trimIndent(),
        )

        assertEquals("2026-07-02 12:00:00.000", summary.timestamp)
        assertEquals("main", summary.thread)
        assertEquals("0.2.2", summary.versionName)
        assertEquals("4", summary.versionCode)
        assertEquals("java.lang.IllegalStateException", summary.exceptionClass)
        assertFalse(summary.toLogLine().contains("contains user text"))
    }

    @Test
    fun buildRecoveryDiagnostics_sanitizesCrashSummary() {
        val diagnostics = buildRecoveryDiagnostics(
            summary = PreviousCrashSummary(
                timestamp = "2026-07-02 12:00:00.000\nsecond line",
                thread = "main",
                versionName = "0.2.2",
                versionCode = "4",
                exceptionClass = "java.lang.IllegalStateException",
            ),
            sdkInt = 35,
            versionName = "0.2.5",
            versionCode = 7,
        )

        assertTrue(diagnostics.contains("versionName=0.2.5"))
        assertTrue(diagnostics.contains("previousCrash.exception=java.lang.IllegalStateException"))
        assertFalse(diagnostics.contains("second line"))
        assertFalse(diagnostics.contains("contains user text"))
    }
}
