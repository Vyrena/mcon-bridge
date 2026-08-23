package com.vyrena.mconbridge

import com.vyrena.mconbridge.ui.imageAspectRatioLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkRatioTest {
    @Test
    fun `shows familiar ratios in reduced form`() {
        assertEquals("2:3", imageAspectRatioLabel(600, 900))
        assertEquals("16:9", imageAspectRatioLabel(1920, 1080))
        assertEquals("10:9", imageAspectRatioLabel(160, 144))
    }

    @Test
    fun `keeps uncommon ratios readable`() {
        assertEquals("1.13:1", imageAspectRatioLabel(768, 680))
        assertEquals("1:1.41", imageAspectRatioLabel(342, 482))
        assertNull(imageAspectRatioLabel(0, 900))
    }
}
