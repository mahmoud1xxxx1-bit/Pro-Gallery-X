package com.progalleryx.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoExportRulesTest {
    @Test fun acceptsValidTrim() {
        assertTrue(VideoExportRules.validTrim(0, 500, 500))
        assertTrue(VideoExportRules.validTrim(2500, 5000, 9000))
    }
    @Test fun rejectsOutOfBoundsTrims() {
        assertFalse(VideoExportRules.validTrim(-1, 1000, 5000))
        assertFalse(VideoExportRules.validTrim(0, 499, 5000))
        assertFalse(VideoExportRules.validTrim(1000, 1000, 5000))
        assertFalse(VideoExportRules.validTrim(1000, 6000, 5000))
        assertFalse(VideoExportRules.validTrim(0, 500, 0))
    }
    @Test fun splitRequiresTwoUsableSegments() {
        assertTrue(VideoExportRules.validSplit(500, 1000))
        assertTrue(VideoExportRules.validSplit(2500, 5000))
        assertFalse(VideoExportRules.validSplit(499, 5000))
        assertFalse(VideoExportRules.validSplit(4501, 5000))
        assertFalse(VideoExportRules.validSplit(500, 999))
    }
    @Test fun validatesFramePositions() {
        assertTrue(VideoExportRules.validFrame(0, 1000))
        assertTrue(VideoExportRules.validFrame(1000, 1000))
        assertFalse(VideoExportRules.validFrame(-1, 1000))
        assertFalse(VideoExportRules.validFrame(1001, 1000))
        assertFalse(VideoExportRules.validFrame(0, 0))
    }
}
