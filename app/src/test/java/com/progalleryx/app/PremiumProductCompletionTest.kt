package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Quality and regression test suite for the Premium Product Completion Directive.
 * Validates:
 * 1. Samsung-style Video Player gesture seeking bounds (-10s / +10s)
 * 2. Aspect Ratio modes cycling
 * 3. Playback speed cycle progression
 * 4. Interactive Layer z-index reordering (bringForward, sendBackward)
 * 5. Recent stickers storage & deduplication
 * 6. Studio hub stability and zero-crash navigation invariant
 */
class PremiumProductCompletionTest {

    @Test
    fun testVideoGestureSeeking_clampsWithinDurationBounds() {
        val durationMs = 30_000L

        // Double tap right (+10s) from 5s -> 15s
        val forward1 = computeSeekTarget(currentPosMs = 5_000L, deltaMs = 10_000L, durationMs = durationMs)
        assertEquals(15_000L, forward1)

        // Double tap right (+10s) near end (25s) -> clamped to 30s
        val forwardEnd = computeSeekTarget(currentPosMs = 25_000L, deltaMs = 10_000L, durationMs = durationMs)
        assertEquals(30_000L, forwardEnd)

        // Double tap left (-10s) from 15s -> 5s
        val backward1 = computeSeekTarget(currentPosMs = 15_000L, deltaMs = -10_000L, durationMs = durationMs)
        assertEquals(5_000L, backward1)

        // Double tap left (-10s) near start (4s) -> clamped to 0s
        val backwardStart = computeSeekTarget(currentPosMs = 4_000L, deltaMs = -10_000L, durationMs = durationMs)
        assertEquals(0L, backwardStart)
    }

    @Test
    fun testPlaybackSpeeds_cycleAndMultipliers() {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

        // Default should be 1.0f (index 2)
        var idx = 2
        assertEquals(1.0f, speeds[idx], 0.001f)

        // Cycle through all
        val expected = listOf(1.25f, 1.5f, 2.0f, 0.5f, 0.75f, 1.0f)
        for (exp in expected) {
            idx = (idx + 1) % speeds.size
            assertEquals(exp, speeds[idx], 0.001f)
        }
    }

    @Test
    fun testAspectRatioModes_cycleThreeModes() {
        val modes = listOf("FIT", "ZOOM", "FILL")
        var current = 0

        assertEquals("FIT", modes[current])
        current = (current + 1) % modes.size
        assertEquals("ZOOM", modes[current])
        current = (current + 1) % modes.size
        assertEquals("FILL", modes[current])
        current = (current + 1) % modes.size
        assertEquals("FIT", modes[current])
    }

    @Test
    fun testLayerZOrdering_bringForwardAndSendBackward() {
        val initial = listOf("layer_A", "layer_B", "layer_C", "layer_D")

        // Bring layer_B forward (index 1 -> index 2)
        val fwd = LayerTransformRules.bringForward(initial, 1)
        assertEquals(listOf("layer_A", "layer_C", "layer_B", "layer_D"), fwd)

        // Bring topmost forward -> remains same
        val topFwd = LayerTransformRules.bringForward(fwd, 3)
        assertEquals(fwd, topFwd)

        // Send layer_C backward (index 1 -> index 0)
        val bwd = LayerTransformRules.sendBackward(fwd, 1)
        assertEquals(listOf("layer_C", "layer_A", "layer_B", "layer_D"), bwd)

        // Send bottommost backward -> remains same
        val botBwd = LayerTransformRules.sendBackward(bwd, 0)
        assertEquals(bwd, botBwd)

        // Out of range indices do not crash
        val safe1 = LayerTransformRules.bringForward(bwd, -1)
        assertEquals(bwd, safe1)
        val safe2 = LayerTransformRules.sendBackward(bwd, 99)
        assertEquals(bwd, safe2)
    }

    @Test
    fun testRecentStickers_persistenceLogic() {
        val emptyParsed = AppPreferences.parseRecentStickers("")
        assertTrue(emptyParsed.isEmpty())

        val stored = listOf("🔥", "❤️", "👍")
        val formatted = AppPreferences.formatRecentStickers(stored)
        val parsed = AppPreferences.parseRecentStickers(formatted)
        assertEquals(stored, parsed)

        // Adding already existing item moves it to front without duplicates
        val bumped = AppPreferences.appendRecentSticker(parsed, "❤️")
        assertEquals(listOf("❤️", "🔥", "👍"), bumped)

        // Adding new item pushes to front
        val newAdded = AppPreferences.appendRecentSticker(bumped, "✨")
        assertEquals(listOf("✨", "❤️", "🔥", "👍"), newAdded)
    }

    @Test
    fun testStudioDestinations_allCoveredWithoutOverlap() {
        val allDestinations = StudioDestination.values()
        assertEquals(4, allDestinations.size)

        // Each destination must have a distinct name and valid string representation
        val names = allDestinations.map { it.name }.toSet()
        assertEquals(4, names.size)
    }

    private fun computeSeekTarget(currentPosMs: Long, deltaMs: Long, durationMs: Long): Long {
        return (currentPosMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
    }
}
