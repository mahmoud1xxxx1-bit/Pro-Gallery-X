package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTimelineRulesTest {
    @Test fun samplesStartAndEndWithoutOverflow() {
        val points = VideoTimelineRules.sampleTimes(12000, 9)
        assertEquals(9, points.size)
        assertEquals(0L, points.first())
        assertEquals(11999L, points.last())
        assertTrue(points.zipWithNext().all { (a, b) -> a <= b })
    }
    @Test fun shortVideoHasSafePositions() {
        val points = VideoTimelineRules.sampleTimes(500, 8)
        assertTrue(points.all { it in 0..499 })
    }
    @Test fun invalidRequestsFail() {
        assertThrows(IllegalArgumentException::class.java) { VideoTimelineRules.sampleTimes(0, 8) }
        assertThrows(IllegalArgumentException::class.java) { VideoTimelineRules.sampleTimes(1000, 1) }
        assertThrows(IllegalArgumentException::class.java) { VideoTimelineRules.sampleTimes(1000, 13) }
    }
}
