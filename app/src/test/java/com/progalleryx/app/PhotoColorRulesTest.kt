package com.progalleryx.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoColorRulesTest {
    @Test fun neutralAdjustmentsAreIdentity() {
        val expected = floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)
        assertArrayEquals(expected, PhotoColorRules.contrastBrightness(1f,0f), 0.001f)
        assertArrayEquals(expected, PhotoColorRules.temperatureTint(0f,0f), 0.001f)
    }
    @Test fun warmthAndTintHaveExpectedChannels() {
        val warm = PhotoColorRules.temperatureTint(80f,0f)
        assertEquals(48f,warm[4],0.001f)
        assertEquals(-48f,warm[14],0.001f)
        val tint = PhotoColorRules.temperatureTint(0f,100f)
        assertEquals(22f,tint[4],0.001f)
        assertEquals(-45f,tint[9],0.001f)
        assertEquals(22f,tint[14],0.001f)
    }
    @Test fun rejectsOutOfBoundsInputs() {
        assertThrows(IllegalArgumentException::class.java) { PhotoColorRules.temperatureTint(101f,0f) }
        assertThrows(IllegalArgumentException::class.java) { PhotoColorRules.contrastBrightness(1f,Float.NaN) }
    }
}
