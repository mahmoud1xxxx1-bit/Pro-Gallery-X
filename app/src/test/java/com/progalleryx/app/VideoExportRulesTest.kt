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
    @Test fun timelinePositionsAndTrimBoundaries() {
        val timeline = ClipSequenceTimelineRules.build(listOf(
            Triple("first", 1000L, 3000L),
            Triple("second", 500L, 4000L)
        ))
        org.junit.Assert.assertEquals(0L, timeline[0].timelineStartMs)
        org.junit.Assert.assertEquals(2000L, timeline[1].timelineStartMs)
        org.junit.Assert.assertEquals(5500L, ClipSequenceTimelineRules.totalDurationMs(timeline))
        org.junit.Assert.assertEquals("second", ClipSequenceTimelineRules.segmentAt(timeline, 2000L)?.source)
        org.junit.Assert.assertNull(ClipSequenceTimelineRules.segmentAt(timeline, 5500L))
        org.junit.Assert.assertEquals("01:01:01", ClipSequenceTimelineRules.formatTime(3661000L))
    }

    @Test fun timelineRejectsDuplicatesShortClipsAndOverflow() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.build(listOf(Triple("a", 0L, 499L), Triple("b", 0L, 1000L)))
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.build(listOf(Triple("a", 0L, 1000L), Triple("a", 0L, 1000L)))
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.build(listOf(Triple("a", 0L, Long.MAX_VALUE), Triple("b", 0L, 1000L)))
        }
    }

}
