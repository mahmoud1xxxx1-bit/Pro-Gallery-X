package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoQualityRulesTest {
    @Test fun bitratePresetsAreBounded() {
        assertTrue(VideoQualityRules.valid(VideoQualityRules.AUTO))
        assertTrue(VideoQualityRules.valid(VideoQualityRules.BALANCED))
        assertFalse(VideoQualityRules.valid(1000))
        assertFalse(VideoQualityRules.valid(-1))
    }

    @Test fun resolutionPresetsAreBounded() {
        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_AUTO))
        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_480P))
        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_720P))
        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_1080P))
        assertFalse(VideoQualityRules.validHeight(360))
        assertFalse(VideoQualityRules.validHeight(4000))
        assertFalse(VideoQualityRules.validHeight(-1))
    }

    @Test fun bitrateSizeEstimateIsExplicitlyVideoOnly() {
        assertEquals(6.25, VideoQualityRules.expectedVideoMegabytes(10_000L, VideoQualityRules.BALANCED), 0.01)
    }

    @Test fun estimationRejectsAutoAndInvalidTimes() {
        assertThrows(IllegalArgumentException::class.java) { VideoQualityRules.expectedVideoMegabytes(1000L, 0) }
        assertThrows(IllegalArgumentException::class.java) { VideoQualityRules.expectedVideoMegabytes(-1L, VideoQualityRules.HIGH) }
    }
}
