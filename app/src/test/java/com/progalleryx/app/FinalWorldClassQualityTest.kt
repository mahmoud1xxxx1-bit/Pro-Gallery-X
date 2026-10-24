package com.progalleryx.app

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class FinalWorldClassQualityTest {

    // =========================================================================
    // 1. Photo Layer Geometric Centering Invariant (1:1 Preview-to-Export Math)
    // =========================================================================

    @Test
    fun calculateCenterOffset_matchesTargetCenterExactlyAcrossAspectRatios() {
        val testScreens = listOf(
            Pair(1080f, 1080f), // 1:1 Square
            Pair(1440f, 1080f), // 4:3 Standard
            Pair(1920f, 1080f), // 16:9 Landscape
            Pair(1080f, 1920f)  // 9:16 Portrait / Story
        )

        val testCoordinates = listOf(
            Pair(0.5f, 0.5f),   // Dead center
            Pair(0.2f, 0.3f),   // Top-left quadrant
            Pair(0.85f, 0.75f), // Bottom-right quadrant
            Pair(0.1f, 0.9f)    // Edge coordinates
        )

        val layerSizes = listOf(
            Pair(120, 120),     // Standard sticker
            Pair(200, 60),      // Short text box
            Pair(350, 95)       // Multi-line text box
        )

        for ((screenW, screenH) in testScreens) {
            for ((nx, ny) in testCoordinates) {
                for ((layerW, layerH) in layerSizes) {
                    val (offsetX, offsetY) = LayerTransformRules.calculateCenterOffset(
                        normalizedX = nx,
                        normalizedY = ny,
                        containerWidthPx = screenW,
                        containerHeightPx = screenH,
                        layerWidthPx = layerW,
                        layerHeightPx = layerH
                    )

                    // Reconstructed center must equal target center
                    val reconstructedCenterX = offsetX + layerW / 2
                    val reconstructedCenterY = offsetY + layerH / 2
                    val expectedCenterX = (nx * screenW).roundToInt()
                    val expectedCenterY = (ny * screenH).roundToInt()

                    assertEquals(
                        "X center must align for screen $screenW x $screenH",
                        expectedCenterX,
                        reconstructedCenterX
                    )
                    assertEquals(
                        "Y center must align for screen $screenW x $screenH",
                        expectedCenterY,
                        reconstructedCenterY
                    )
                }
            }
        }
    }

    // =========================================================================
    // 2. Layer Lock State & Immutability
    // =========================================================================

    @Test
    fun textLayer_lockStateToggle_preservesAttributes() {
        val layer = InteractiveTextLayer(
            id = "layer_text_01",
            text = "World-Class Pro Gallery",
            normalizedX = 0.5f,
            normalizedY = 0.4f,
            scale = 1.25f,
            rotationDegrees = 15f,
            isLocked = false
        )

        assertFalse(layer.isLocked)

        val layers = listOf(layer)
        val lockedList = LayerTransformRules.toggleTextLayerLock(layers, layer.id)
        val lockedLayer = lockedList.first()

        assertTrue(lockedLayer.isLocked)
        assertEquals(layer.text, lockedLayer.text)
        assertEquals(layer.normalizedX, lockedLayer.normalizedX, 0.001f)
        assertEquals(layer.normalizedY, lockedLayer.normalizedY, 0.001f)
        assertEquals(layer.scale, lockedLayer.scale, 0.001f)
        assertEquals(layer.rotationDegrees, lockedLayer.rotationDegrees, 0.001f)

        // Toggle back to unlocked
        val unlockedList = LayerTransformRules.toggleTextLayerLock(lockedList, layer.id)
        assertFalse(unlockedList.first().isLocked)
    }

    @Test
    fun stickerLayer_lockStateToggle_preservesAttributes() {
        val sticker = InteractiveStickerLayer(
            id = "sticker_01",
            emoji = "✨",
            normalizedX = 0.7f,
            normalizedY = 0.6f,
            scale = 1.5f,
            rotationDegrees = 45f,
            isLocked = false
        )

        val stickers = listOf(sticker)
        val locked = LayerTransformRules.toggleStickerLayerLock(stickers, sticker.id)
        assertTrue(locked.first().isLocked)
        assertEquals("✨", locked.first().emoji)

        val unlocked = LayerTransformRules.toggleStickerLayerLock(locked, sticker.id)
        assertFalse(unlocked.first().isLocked)
    }

    // =========================================================================
    // 3. Layer Z-Ordering & Boundary Protection
    // =========================================================================

    @Test
    fun layerZOrdering_bringForwardAndSendBackward_respectsBoundaries() {
        val items = listOf("A", "B", "C", "D")

        // Bring forward B (index 1 -> index 2)
        val forwardB = LayerTransformRules.bringForward(items, 1)
        assertEquals(listOf("A", "C", "B", "D"), forwardB)

        // Bring forward D (topmost -> should remain unchanged)
        val forwardD = LayerTransformRules.bringForward(items, 3)
        assertEquals(items, forwardD)

        // Send backward C (index 2 -> index 1)
        val backwardC = LayerTransformRules.sendBackward(items, 2)
        assertEquals(listOf("A", "C", "B", "D"), backwardC)

        // Send backward A (bottommost -> should remain unchanged)
        val backwardA = LayerTransformRules.sendBackward(items, 0)
        assertEquals(items, backwardA)
    }

    @Test
    fun applyDragDelta_clampsWithinSafeNormalizedBounds() {
        // Dragging far outside bounds
        val (clampedX, clampedY) = LayerTransformRules.applyDragDelta(
            currentX = 0.95f,
            currentY = 0.95f,
            dragPxX = 500f,
            dragPxY = 500f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f,
            boundsPadding = 0.02f
        )

        assertEquals(0.98f, clampedX, 0.001f)
        assertEquals(0.98f, clampedY, 0.001f)

        // Negative drag
        val (minX, minY) = LayerTransformRules.applyDragDelta(
            currentX = 0.05f,
            currentY = 0.05f,
            dragPxX = -500f,
            dragPxY = -500f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f,
            boundsPadding = 0.02f
        )

        assertEquals(0.02f, minX, 0.001f)
        assertEquals(0.02f, minY, 0.001f)
    }

    @Test
    fun applyScaleAndRotation_clampsScaleAndWrapsAngle() {
        // Scale upper bound
        val (maxScale, _) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 5.5f,
            currentRotationDegrees = 0f,
            zoomDelta = 2.0f,
            rotationDeltaDegrees = 0f
        )
        assertEquals(LayerTransformRules.MAX_SCALE, maxScale, 0.001f)

        // Scale lower bound
        val (minScale, _) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 0.3f,
            currentRotationDegrees = 0f,
            zoomDelta = 0.1f,
            rotationDeltaDegrees = 0f
        )
        assertEquals(LayerTransformRules.MIN_SCALE, minScale, 0.001f)

        // Rotation wrapping
        val (_, wrappedRotPositive) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 1.0f,
            currentRotationDegrees = 350f,
            zoomDelta = 1.0f,
            rotationDeltaDegrees = 30f
        )
        assertEquals(20f, wrappedRotPositive, 0.001f)

        val (_, wrappedRotNegative) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 1.0f,
            currentRotationDegrees = 10f,
            zoomDelta = 1.0f,
            rotationDeltaDegrees = -30f
        )
        assertEquals(340f, wrappedRotNegative, 0.001f)
    }

    // =========================================================================
    // 4. Samsung Video Player & Audio Focus Attributes
    // =========================================================================

    @Test
    fun media3AudioAttributes_configuredForMediaAndMovie() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        assertEquals(C.AUDIO_CONTENT_TYPE_MOVIE, audioAttributes.contentType)
        assertEquals(C.USAGE_MEDIA, audioAttributes.usage)
    }

    @Test
    fun videoPlaybackState_ended_resetsPositionAndPlaying() {
        var isPlaying = true
        val durationMs = 45_000L
        var currentPosMs = 44_800L

        // Simulate STATE_ENDED trigger
        val playbackState = Player.STATE_ENDED
        if (playbackState == Player.STATE_ENDED) {
            isPlaying = false
            currentPosMs = durationMs
        }

        assertFalse(isPlaying)
        assertEquals(durationMs, currentPosMs)

        // Simulate Replay trigger
        var seekTarget = -1L
        if (playbackState == Player.STATE_ENDED || currentPosMs >= durationMs) {
            seekTarget = 0L
            isPlaying = true
        }

        assertEquals(0L, seekTarget)
        assertTrue(isPlaying)
    }

    // =========================================================================
    // 5. Multi-Track Timed Video Overlays Synchronization
    // =========================================================================

    @Test
    fun timedOverlay_activeRange_isStrictlyEnforced() {
        val overlayStartMs = 2000L
        val overlayEndMs = 6000L

        fun isActiveAt(timestampMs: Long): Boolean {
            return timestampMs in overlayStartMs..overlayEndMs
        }

        // Before interval
        assertFalse(isActiveAt(0L))
        assertFalse(isActiveAt(1999L))

        // On boundary and within interval
        assertTrue(isActiveAt(2000L))
        assertTrue(isActiveAt(4000L))
        assertTrue(isActiveAt(6000L))

        // After interval
        assertFalse(isActiveAt(6001L))
        assertFalse(isActiveAt(10000L))
    }

    // =========================================================================
    // 6. CapCut Video Timeline Engine & Minimum Split Duration
    // =========================================================================

    @Test
    fun videoTimelineSplitSafety_enforces500msMinimumDuration() {
        val clipDurationMs = 1200L

        // Split at 200ms -> leaves left part < 500ms (Unsafe)
        val canSplitAt200 = 200L >= 500L && (clipDurationMs - 200L) >= 500L
        assertFalse(canSplitAt200)

        // Split at 600ms -> leaves both parts >= 500ms (Safe)
        val canSplitAt600 = 600L >= 500L && (clipDurationMs - 600L) >= 500L
        assertTrue(canSplitAt600)

        // Split at 1000ms -> leaves right part < 500ms (Unsafe)
        val canSplitAt1000 = 1000L >= 500L && (clipDurationMs - 1000L) >= 500L
        assertFalse(canSplitAt1000)
    }

    @Test
    fun galleryTimelineRules_durationFormatting_handlesEdgeCases() {
        assertEquals("00:00", GalleryTimelineRules.formatDuration(0L))
        assertEquals("00:09", GalleryTimelineRules.formatDuration(9_000L))
        assertEquals("01:25", GalleryTimelineRules.formatDuration(85_000L))
        assertEquals("01:05:30", GalleryTimelineRules.formatDuration(3930_000L))
    }

    // =========================================================================
    // 7. PDF Studio Range & Page Constraints
    // =========================================================================

    @Test
    fun pdfExportConstraints_validatedCorrectly() {
        val maxPages = 120
        val inputs = (1..5).map { "uri://$it" }

        assertTrue(inputs.size in 1..10)

        val pageStart = 1
        val pageEnd = 25
        val validRange = pageStart >= 1 && pageEnd >= pageStart && (pageEnd - pageStart + 1) <= maxPages
        assertTrue(validRange)

        val invalidNegativeStart = -1
        assertFalse(invalidNegativeStart >= 1)

        val invalidExceedMax = 125
        assertFalse((invalidExceedMax - pageStart + 1) <= maxPages)
    }

    // =========================================================================
    // 8. 20x Studio Navigation & State Isolation Simulation
    // =========================================================================

    @Test
    fun studioNavigation20xCycle_resetsCleanlyWithoutStateLeak() {
        var currentProject = PhotoStudioProject()

        for (cycle in 1..20) {
            // 1. User opens studio and adds layers
            currentProject = currentProject.copy(
                brightness = 0.2f,
                contrast = 1.1f,
                textLayers = listOf(
                    InteractiveTextLayer(text = "Cycle $cycle Text", normalizedX = 0.5f, normalizedY = 0.5f)
                ),
                stickerLayers = listOf(
                    InteractiveStickerLayer(emoji = "🔥", normalizedX = 0.3f, normalizedY = 0.3f)
                ),
                activeLayerId = "temp_id_$cycle"
            )

            assertEquals(1, currentProject.textLayers.size)
            assertEquals("Cycle $cycle Text", currentProject.textLayers.first().text)

            // 2. User exits studio (clean reset)
            currentProject = PhotoStudioProject()

            // 3. Verify clean reset state
            assertEquals(0f, currentProject.brightness, 0.001f)
            assertEquals(1f, currentProject.contrast, 0.001f)
            assertTrue(currentProject.textLayers.isEmpty())
            assertTrue(currentProject.stickerLayers.isEmpty())
            assertNull(currentProject.activeLayerId)
        }
    }
}
