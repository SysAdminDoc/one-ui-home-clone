package com.oneuihomeclone.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPreviewSizeTest {
    @Test
    fun providerIconUsesSquareBitmap() {
        assertEquals(144 to 144, widgetPreviewBitmapSize(true, 512, 256))
    }

    @Test
    fun previewImageKeepsItsAspectRatio() {
        assertEquals(640 to 320, widgetPreviewBitmapSize(false, 1200, 600))
    }

    @Test
    fun previewImageUsesSafeFallbackForMissingIntrinsicSize() {
        assertEquals(320 to 180, widgetPreviewBitmapSize(false, -1, -1))
    }
}
