package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoProjectModelsTest {

    @Test
    fun videoClipItem_effectiveDurationCalculatedAccurately() {
        val clipNormal = VideoClipItem(
            originalDurationMs = 10_000L,
            startMs = 2_000L,
            endMs = 8_000L,
            speed = 1.0f
        )
        assertEquals(6_000L, clipNormal.rawTrimDurationMs)
        assertEquals(6_000L, clipNormal.effectiveDurationMs)

        val clipFast = clipNormal.copy(speed = 2.0f)
        assertEquals(6_000L, clipFast.rawTrimDurationMs)
        assertEquals(3_000L, clipFast.effectiveDurationMs)

        val clipSlow = clipNormal.copy(speed = 0.5f)
        assertEquals(6_000L, clipSlow.rawTrimDurationMs)
        assertEquals(12_000L, clipSlow.effectiveDurationMs)
    }

    @Test
    fun videoTimedOverlay_visibilityCheck() {
        val overlay = VideoTimedOverlay(
            id = "ov1",
            type = VideoOverlayType.TEXT,
            text = "Hello World",
            startMs = 2_000L,
            endMs = 5_000L
        )

        assertFalse(overlay.isVisibleAt(1_999L))
        assertTrue(overlay.isVisibleAt(2_000L))
        assertTrue(overlay.isVisibleAt(3_500L))
        assertTrue(overlay.isVisibleAt(5_000L))
        assertFalse(overlay.isVisibleAt(5_001L))
        assertEquals(3_000L, overlay.durationMs())
    }

    @Test
    fun videoStudioProject_totalDurationSum() {
        val c1 = VideoClipItem(originalDurationMs = 5_000L, startMs = 0L, endMs = 4_000L, speed = 1.0f)
        val c2 = VideoClipItem(originalDurationMs = 6_000L, startMs = 1_000L, endMs = 5_000L, speed = 2.0f)
        val c3 = VideoClipItem(originalDurationMs = 3_000L, startMs = 0L, endMs = 3_000L, speed = 0.5f)

        val project = VideoStudioProject(clips = listOf(c1, c2, c3))
        assertEquals(12_000L, project.totalDurationMs)
    }

    @Test
    fun videoAspectRatio_valuesAndRatios() {
        assertEquals(0f, VideoAspectRatio.ORIGINAL.ratio)
        assertEquals(16f / 9f, VideoAspectRatio.RATIO_16_9.ratio, 0.001f)
        assertEquals(9f / 16f, VideoAspectRatio.RATIO_9_16.ratio, 0.001f)
        assertEquals(1f, VideoAspectRatio.RATIO_1_1.ratio, 0.001f)
        assertEquals(4f / 3f, VideoAspectRatio.RATIO_4_3.ratio, 0.001f)
    }
}
