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

    @Test fun timelinePlayheadMapsAcrossClipsAndClampsEnd() {
        val clips = ClipSequenceTimelineRules.build(listOf(
            Triple("first", 1000L, 3000L), Triple("second", 500L, 4000L)
        ))
        org.junit.Assert.assertEquals("first" to 1000L, ClipSequenceTimelineRules.sourcePositionAt(clips, 0L))
        org.junit.Assert.assertEquals("first" to 2999L, ClipSequenceTimelineRules.sourcePositionAt(clips, 1999L))
        org.junit.Assert.assertEquals("second" to 500L, ClipSequenceTimelineRules.sourcePositionAt(clips, 2000L))
        org.junit.Assert.assertEquals("second" to 3999L, ClipSequenceTimelineRules.sourcePositionAt(clips, 5500L))
        org.junit.Assert.assertEquals(1, ClipSequenceTimelineRules.clipIndexAt(clips, 5500L))
        org.junit.Assert.assertNull(ClipSequenceTimelineRules.sourcePositionAt(clips, -1L))
        org.junit.Assert.assertNull(ClipSequenceTimelineRules.sourcePositionAt(emptyList<TimelineSegment<String>>(), 0L))
    }

    @Test fun bulkTrimPresetsPreserveValidWindows() {
        val durations = mapOf("a" to 12000L, "b" to 3000L)
        val rules = ClipSequenceTimelineRules
        org.junit.Assert.assertEquals(0L to 5000L, rules.batchRanges(durations, ClipSequenceTimelineRules.TrimPreset.FIRST_FIVE_SECONDS)["a"])
        org.junit.Assert.assertEquals(0L to 3000L, rules.batchRanges(durations, ClipSequenceTimelineRules.TrimPreset.FIRST_FIVE_SECONDS)["b"])
        org.junit.Assert.assertEquals(7000L to 12000L, rules.batchRanges(durations, ClipSequenceTimelineRules.TrimPreset.LAST_FIVE_SECONDS)["a"])
        org.junit.Assert.assertEquals(0L to 3000L, rules.batchRanges(durations, ClipSequenceTimelineRules.TrimPreset.LAST_FIVE_SECONDS)["b"])
        org.junit.Assert.assertEquals(12000L, rules.totalSelectedDurationMs(listOf(0L to 5000L, 0L to 7000L)))
    }

    @Test fun bulkTrimsRejectBadDurationsAndWindows() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.batchRanges(mapOf("a" to 499L), ClipSequenceTimelineRules.TrimPreset.FULL)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.totalSelectedDurationMs(listOf(0L to 499L, 0L to 2000L))
        }
    }

    @Test fun timelineEditsPreserveClipIdentityAndOrdering() {
        val original = listOf("a", "b", "c")
        org.junit.Assert.assertEquals(listOf("b", "c", "a"), ClipSequenceTimelineRules.moveClip(original, 0, 2))
        org.junit.Assert.assertEquals(listOf("a", "c"), ClipSequenceTimelineRules.removeClip(original, 1))
        org.junit.Assert.assertEquals(original, ClipSequenceTimelineRules.moveClip(original, 1, 1))
        org.junit.Assert.assertEquals(listOf("a", "b", "c"), original)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.moveClip(original, -1, 0)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.removeClip(original, 3)
        }
    }

    @Test fun selectedTimelineDurationAndCursorRemainBounded() {
        val clips = listOf(Triple("a", 1000L, 3000L), Triple("b", 0L, 900L))
        org.junit.Assert.assertEquals(2900L, ClipSequenceTimelineRules.selectedDurationMs(clips))
        org.junit.Assert.assertEquals(899L, ClipSequenceTimelineRules.recommendedCursorAfterRemoval(5000L, 900L))
        org.junit.Assert.assertEquals(0L, ClipSequenceTimelineRules.recommendedCursorAfterRemoval(-1L, 900L))
        org.junit.Assert.assertEquals(0L, ClipSequenceTimelineRules.recommendedCursorAfterRemoval(900L, 0L))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ClipSequenceTimelineRules.selectedDurationMs(listOf(Triple("a", 0L, 1000L)))
        }
    }

}
