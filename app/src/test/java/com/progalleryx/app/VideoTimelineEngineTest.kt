package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTimelineEngineTest {

    @Test
    fun totalDurationMs_calculatesCorrectSumWithSpeeds() {
        val clip1 = VideoClipItem(
            id = "c1",
            originalDurationMs = 10_000L,
            startMs = 0L,
            endMs = 10_000L,
            speed = 1.0f
        )
        val clip2 = VideoClipItem(
            id = "c2",
            originalDurationMs = 8_000L,
            startMs = 0L,
            endMs = 8_000L,
            speed = 2.0f
        )

        val total = VideoTimelineEngine.totalDurationMs(listOf(clip1, clip2))
        assertEquals(14_000L, total)
    }

    @Test
    fun clipAtProjectTime_findsCorrectClipAndSourceTime() {
        val clip1 = VideoClipItem(
            id = "c1",
            originalDurationMs = 10_000L,
            startMs = 1_000L,
            endMs = 5_000L,
            speed = 1.0f
        )
        val clip2 = VideoClipItem(
            id = "c2",
            originalDurationMs = 12_000L,
            startMs = 2_000L,
            endMs = 8_000L,
            speed = 2.0f
        )
        val clips = listOf(clip1, clip2)

        val mapping1 = VideoTimelineEngine.clipAtProjectTime(clips, 2_000L)
        assertNotNull(mapping1)
        assertEquals("c1", mapping1!!.clip.id)
        assertEquals(0, mapping1.clipIndex)
        assertEquals(0L, mapping1.clipStartInProjectMs)
        assertEquals(2_000L, mapping1.localOffsetInClipMs)
        assertEquals(3_000L, mapping1.sourceMediaTimeMs)

        val mapping2 = VideoTimelineEngine.clipAtProjectTime(clips, 5_500L)
        assertNotNull(mapping2)
        assertEquals("c2", mapping2!!.clip.id)
        assertEquals(1, mapping2.clipIndex)
        assertEquals(4_000L, mapping2.clipStartInProjectMs)
        assertEquals(1_500L, mapping2.localOffsetInClipMs)
        assertEquals(5_000L, mapping2.sourceMediaTimeMs)
    }

    @Test
    fun splitClip_validSplitDividesClipAccurately() {
        val clip = VideoClipItem(
            id = "clip_to_split",
            originalDurationMs = 10_000L,
            startMs = 0L,
            endMs = 10_000L,
            speed = 1.0f
        )
        val clips = listOf(clip)

        val (newClips, newId) = VideoTimelineEngine.splitClip(clips, "clip_to_split", 4_000L)
        assertEquals(2, newClips.size)
        assertNotNull(newId)

        val part1 = newClips[0]
        val part2 = newClips[1]

        assertEquals("clip_to_split", part1.id)
        assertEquals(0L, part1.startMs)
        assertEquals(4_000L, part1.endMs)

        assertEquals(newId, part2.id)
        assertEquals(4_000L, part2.startMs)
        assertEquals(10_000L, part2.endMs)
    }

    @Test
    fun splitClip_tooCloseToBoundaryReturnsOriginal() {
        val clip = VideoClipItem(
            id = "clip_too_short",
            originalDurationMs = 10_000L,
            startMs = 0L,
            endMs = 10_000L,
            speed = 1.0f
        )
        val clips = listOf(clip)

        val (result1, id1) = VideoTimelineEngine.splitClip(clips, "clip_too_short", 200L)
        assertEquals(1, result1.size)
        assertNull(id1)

        val (result2, id2) = VideoTimelineEngine.splitClip(clips, "clip_too_short", 9_800L)
        assertEquals(1, result2.size)
        assertNull(id2)
    }

    @Test
    fun updateClipTrim_clampsWithinValidBounds() {
        val clip = VideoClipItem(
            id = "c1",
            originalDurationMs = 5_000L,
            startMs = 0L,
            endMs = 5_000L
        )
        val clips = listOf(clip)

        val updated = VideoTimelineEngine.updateClipTrim(clips, "c1", 1_000L, 4_000L)
        assertEquals(1_000L, updated[0].startMs)
        assertEquals(4_000L, updated[0].endMs)

        val invalid = VideoTimelineEngine.updateClipTrim(clips, "c1", 1_000L, 1_200L)
        assertTrue((invalid[0].endMs - invalid[0].startMs) >= 500L)
    }

    @Test
    fun reorderClips_movesItemProperly() {
        val c1 = VideoClipItem(id = "1", originalDurationMs = 1000L)
        val c2 = VideoClipItem(id = "2", originalDurationMs = 1000L)
        val c3 = VideoClipItem(id = "3", originalDurationMs = 1000L)

        val reordered = VideoTimelineEngine.reorderClips(listOf(c1, c2, c3), 0, 2)
        assertEquals(listOf("2", "3", "1"), reordered.map { it.id })
    }

    @Test
    fun updateClipRotation_cyclesModulo360() {
        val clip = VideoClipItem(id = "c1", originalDurationMs = 1000L, rotationDegrees = 0)
        val clips = listOf(clip)

        val r1 = VideoTimelineEngine.updateClipRotation(clips, "c1", 90)
        assertEquals(90, r1[0].rotationDegrees)

        val r2 = VideoTimelineEngine.updateClipRotation(r1, "c1", 90)
        assertEquals(180, r2[0].rotationDegrees)

        val r4 = VideoTimelineEngine.updateClipRotation(r2, "c1", 180)
        assertEquals(0, r4[0].rotationDegrees)
    }

    @Test
    fun activeOverlaysAt_filtersBasedOnWindow() {
        val o1 = VideoTimedOverlay(id = "o1", startMs = 0L, endMs = 2000L, text = "Intro")
        val o2 = VideoTimedOverlay(id = "o2", startMs = 1500L, endMs = 4000L, text = "Mid")
        val overlays = listOf(o1, o2)

        val at1000 = VideoTimelineEngine.activeOverlaysAt(overlays, 1000L)
        assertEquals(1, at1000.size)
        assertEquals("o1", at1000[0].id)

        val at1800 = VideoTimelineEngine.activeOverlaysAt(overlays, 1800L)
        assertEquals(2, at1800.size)

        val at3500 = VideoTimelineEngine.activeOverlaysAt(overlays, 3500L)
        assertEquals(1, at3500.size)
        assertEquals("o2", at3500[0].id)

        val at5000 = VideoTimelineEngine.activeOverlaysAt(overlays, 5000L)
        assertTrue(at5000.isEmpty())
    }

    @Test
    fun formatTimeMs_formatsCorrectly() {
        assertEquals("00:00.00", VideoTimelineEngine.formatTimeMs(0L))
        assertEquals("01:05.43", VideoTimelineEngine.formatTimeMs(65_430L))
        assertEquals("00:09.05", VideoTimelineEngine.formatTimeMs(9_050L))
    }

    @Test
    fun fractionConversions_workBothWays() {
        val total = 10_000L
        val frac = VideoTimelineEngine.timeToFraction(5_000L, total)
        assertEquals(0.5f, frac, 0.001f)
        val time = VideoTimelineEngine.fractionToTime(0.5f, total)
        assertEquals(5_000L, time)
    }
}
