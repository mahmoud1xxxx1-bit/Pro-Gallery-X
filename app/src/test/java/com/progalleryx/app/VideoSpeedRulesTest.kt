package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSpeedRulesTest {
    @Test fun supportedPresets() {
        assertTrue(VideoSpeedRules.valid(0.5f))
        assertTrue(VideoSpeedRules.valid(1f))
        assertTrue(VideoSpeedRules.valid(2f))
        assertFalse(VideoSpeedRules.valid(0f))
        assertFalse(VideoSpeedRules.valid(Float.NaN))
        assertFalse(VideoSpeedRules.valid(4f))
    }
    @Test fun outputDurationCalculations() {
        assertEquals(20000L, VideoSpeedRules.expectedDurationMs(10000L, 0.5f))
        assertEquals(10000L, VideoSpeedRules.expectedDurationMs(10000L, 1f))
        assertEquals(5000L, VideoSpeedRules.expectedDurationMs(10000L, 2f))
    }
    @Test fun inputIsValidated() {
        assertThrows(IllegalArgumentException::class.java) { VideoSpeedRules.expectedDurationMs(499L, 1f) }
        assertThrows(IllegalArgumentException::class.java) { VideoSpeedRules.expectedDurationMs(1000L, -1f) }
    }
}
