package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoOverlayTimelineIntegrationTest {

    @Test
    fun testTimedText2to5AndSticker4to8_frameByFrameTransitions() {
        // Requirement 2:
        // Text overlay visible from 2s (2000ms) to 5s (5000ms)
        // Sticker overlay visible from 4s (4000ms) to 8s (8000ms)
        val textOverlay = VideoTimedOverlay(
            id = "text_2_to_5",
            type = VideoOverlayType.TEXT,
            text = "Breaking News",
            startMs = 2000L,
            endMs = 5000L,
            normalizedX = 0.5f,
            normalizedY = 0.2f,
            scale = 1.0f,
            rotationDegrees = 0f,
            textColorArgb = 0xFFFF0000L, // Red
            backgroundColorArgb = 0x88000000L,
            fontIndex = 0,
            isBold = true,
            isItalic = false
        )

        val stickerOverlay = VideoTimedOverlay(
            id = "sticker_4_to_8",
            type = VideoOverlayType.STICKER,
            stickerEmoji = "⭐",
            startMs = 4000L,
            endMs = 8000L,
            normalizedX = 0.8f,
            normalizedY = 0.8f,
            scale = 1.5f,
            rotationDegrees = 15f
        )

        val overlays = listOf(textOverlay, stickerOverlay)

        // 1. Before 2s (0ms..1999ms): Neither overlay should be active
        val state0 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 0L)
        assertEquals(0, state0.activeCount)
        assertFalse(state0.hasText)
        assertFalse(state0.hasSticker)

        val state1000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 1000L)
        assertEquals(0, state1000.activeCount)
        assertFalse(state1000.hasText)
        assertFalse(state1000.hasSticker)

        val state1999 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 1999L)
        assertEquals(0, state1999.activeCount)
        assertFalse(state1999.hasText)
        assertFalse(state1999.hasSticker)

        // 2. Exactly at 2s and during [2000ms..3999ms]: ONLY text should be active
        val state2000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 2000L)
        assertEquals(1, state2000.activeCount)
        assertTrue(state2000.hasText)
        assertEquals("Breaking News", state2000.activeTexts[0])
        assertFalse(state2000.hasSticker)

        val state3000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 3000L)
        assertEquals(1, state3000.activeCount)
        assertTrue(state3000.hasText)
        assertEquals(listOf("text_2_to_5"), state3000.overlayIds)
        assertFalse(state3000.hasSticker)

        val state3999 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 3999L)
        assertEquals(1, state3999.activeCount)
        assertTrue(state3999.hasText)
        assertFalse(state3999.hasSticker)

        // 3. During overlap [4000ms..5000ms]: BOTH text and sticker MUST be active
        val state4000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 4000L)
        assertEquals(2, state4000.activeCount)
        assertTrue(state4000.hasText)
        assertTrue(state4000.hasSticker)
        assertEquals("Breaking News", state4000.activeTexts[0])
        assertEquals("⭐", state4000.activeStickers[0])

        val state4500 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 4500L)
        assertEquals(2, state4500.activeCount)
        assertTrue(state4500.hasText)
        assertTrue(state4500.hasSticker)

        val state5000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 5000L)
        assertEquals(2, state5000.activeCount)
        assertTrue(state5000.hasText)
        assertTrue(state5000.hasSticker)

        // 4. After 5s [5001ms..8000ms]: ONLY sticker should be active (text expired)
        val state5001 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 5001L)
        assertEquals(1, state5001.activeCount)
        assertFalse(state5001.hasText)
        assertTrue(state5001.hasSticker)
        assertEquals("⭐", state5001.activeStickers[0])
        assertEquals(listOf("sticker_4_to_8"), state5001.overlayIds)

        val state6000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 6000L)
        assertEquals(1, state6000.activeCount)
        assertFalse(state6000.hasText)
        assertTrue(state6000.hasSticker)

        val state8000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 8000L)
        assertEquals(1, state8000.activeCount)
        assertFalse(state8000.hasText)
        assertTrue(state8000.hasSticker)

        // 5. After 8s (8001ms..): BOTH expired, no active overlays
        val state8001 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 8001L)
        assertEquals(0, state8001.activeCount)
        assertFalse(state8001.hasText)
        assertFalse(state8001.hasSticker)

        val state10000 = VideoOverlayRenderer.evaluateOverlayFrame(overlays, 10000L)
        assertEquals(0, state10000.activeCount)
        assertFalse(state10000.hasText)
        assertFalse(state10000.hasSticker)
    }

    @Test
    fun testSimulatedFrameInspectionSequence_30fps() {
        val text = VideoTimedOverlay(
            id = "t1",
            type = VideoOverlayType.TEXT,
            text = "Video 30fps Title",
            startMs = 2000L,
            endMs = 5000L
        )
        val sticker = VideoTimedOverlay(
            id = "s1",
            type = VideoOverlayType.STICKER,
            stickerEmoji = "🔥",
            startMs = 4000L,
            endMs = 8000L
        )
        val overlays = listOf(text, sticker)

        // Simulate 10 seconds of video at 30 fps (step ~33.33ms)
        var textFramesCount = 0
        var stickerFramesCount = 0
        var overlapFramesCount = 0
        var emptyFramesCount = 0

        val totalFrames = 300 // 10 seconds * 30 fps
        for (frameIndex in 0 until totalFrames) {
            val frameTimeMs = (frameIndex * 1000L) / 30L
            val state = VideoOverlayRenderer.evaluateOverlayFrame(overlays, frameTimeMs)

            if (state.hasText && state.hasSticker) {
                overlapFramesCount++
            } else if (state.hasText) {
                textFramesCount++
            } else if (state.hasSticker) {
                stickerFramesCount++
            } else {
                emptyFramesCount++
            }
        }

        // 0..2s (approx 60 frames): empty
        // 2..4s (approx 60 frames): text only
        // 4..5s (approx 31 frames): overlap (text + sticker)
        // 5..8s (approx 90 frames): sticker only
        // 8..10s (approx 60 frames): empty
        assertTrue("Expected text only frames around 60", textFramesCount in 55..65)
        assertTrue("Expected overlap frames around 31", overlapFramesCount in 28..35)
        assertTrue("Expected sticker only frames around 90", stickerFramesCount in 85..95)
        assertTrue("Expected empty frames around 120", emptyFramesCount in 115..125)
    }
}
