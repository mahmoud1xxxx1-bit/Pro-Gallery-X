package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundtrackRulesTest {
    @Test fun validOffsetsFitExactly() {
        assertTrue(SoundtrackRules.validWindow(0, 5000, 5000))
        assertTrue(SoundtrackRules.validWindow(5000, 5000, 10000))
        assertEquals(10000L, SoundtrackRules.endMs(5000, 5000, 10000))
    }
    @Test fun rejectOutOfBoundsOffsets() {
        assertFalse(SoundtrackRules.validWindow(-1, 5000, 10000))
        assertFalse(SoundtrackRules.validWindow(5001, 5000, 10000))
        assertFalse(SoundtrackRules.validWindow(0, 499, 10000))
        assertFalse(SoundtrackRules.validWindow(Long.MAX_VALUE, 5000, Long.MAX_VALUE))
        assertThrows(IllegalArgumentException::class.java) { SoundtrackRules.endMs(7000, 5000, 10000) }
    }
}
