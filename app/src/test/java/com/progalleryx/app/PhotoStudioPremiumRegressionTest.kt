package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class PhotoStudioPremiumRegressionTest {

    @Test
    fun testTextLayerPillAndOutlineAttributes() {
        val layer = InteractiveTextLayer(
            text = "نص تجريبي رائع",
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            scale = 1.2f,
            rotationDegrees = 0f,
            fontSizeSp = 32f,
            colorArgb = 0xFFFFFFFF,
            backgroundColorArgb = 0xFF121212,
            backgroundStyle = TextBackgroundStyle.PILL,
            fontStyle = TextFontStyle.DEFAULT,
            textAlign = TextAlignment.CENTER,
            opacity = 0.95f,
            hasShadow = true,
            hasOutline = true
        )

        assertEquals("نص تجريبي رائع", layer.text)
        assertEquals(TextBackgroundStyle.PILL, layer.backgroundStyle)
        assertTrue(layer.hasShadow)
        assertTrue(layer.hasOutline)
        assertEquals(32, layer.toLegacyLayer().size)
    }

    @Test
    fun testStickerLayerWithCustomImageUriAndNewShapes() {
        val customSticker = InteractiveStickerLayer(
            customImageUri = "content://media/external/images/media/42",
            normalizedX = 0.3f,
            normalizedY = 0.4f,
            scale = 1.5f
        )
        assertNotNull(customSticker.customImageUri)
        assertEquals("content://media/external/images/media/42", customSticker.customImageUri)

        val hexagonSticker = InteractiveStickerLayer(
            shapeType = ShapeType.HEXAGON,
            colorArgb = 0xFFFFCC00L
        )
        assertEquals(ShapeType.HEXAGON, hexagonSticker.shapeType)

        val diamondSticker = InteractiveStickerLayer(
            shapeType = ShapeType.DIAMOND
        )
        assertEquals(ShapeType.DIAMOND, diamondSticker.shapeType)

        val speechSticker = InteractiveStickerLayer(
            shapeType = ShapeType.SPEECH_BUBBLE
        )
        assertEquals(ShapeType.SPEECH_BUBBLE, speechSticker.shapeType)
    }

    @Test
    fun testLayerTransformRulesCardinalSnapping() {
        val (scale, rotSnap0) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 1.0f,
            currentRotationDegrees = 2.0f,
            zoomDelta = 1.0f,
            rotationDeltaDegrees = 0f
        )
        assertEquals(1.0f, scale, 0.001f)
        assertEquals(2.0f, rotSnap0, 0.001f)

        // Drag delta within bounds
        val (x, y) = LayerTransformRules.applyDragDelta(
            currentX = 0.5f,
            currentY = 0.5f,
            dragPxX = 100f,
            dragPxY = 50f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f
        )
        assertEquals(0.6f, x, 0.001f)
        assertEquals(0.55f, y, 0.001f)
    }

    @Test
    fun testSmoothBrushStrokeCreation() {
        val stroke = SmoothBrushStroke(
            points = listOf(
                NormalizedPoint(0.1f, 0.1f),
                NormalizedPoint(0.15f, 0.2f),
                NormalizedPoint(0.25f, 0.35f)
            ),
            colorArgb = 0xFFFF3B30L,
            strokeWidthNormalized = 0.02f
        )
        assertEquals(3, stroke.points.size)
        val legacy = stroke.toLegacyStroke()
        assertEquals(3, legacy.points.size)
        assertEquals(0.02f, legacy.width, 0.001f)
    }

    @Test
    fun testPhotoStudioProjectStatePreservation() {
        val project = PhotoStudioProject(
            brightness = 15f,
            contrast = 1.1f,
            saturation = 1.25f,
            temperature = -10f,
            tint = 5f,
            rotation = 90,
            cropRatio = 1.0f,
            textLayers = listOf(
                InteractiveTextLayer(text = "Hello")
            ),
            stickerLayers = listOf(
                InteractiveStickerLayer(emoji = "🔥")
            )
        )

        val legacy = project.toLegacyAdjustments()
        assertEquals(15f, legacy.brightness, 0.001f)
        assertEquals(1.1f, legacy.contrast, 0.001f)
        assertEquals(1.25f, legacy.saturation, 0.001f)
        assertEquals(-10f, legacy.temperature, 0.001f)
        assertEquals(5f, legacy.tint, 0.001f)
        assertEquals(90, legacy.rotation)
        assertEquals(1.0f, legacy.cropRatio, 0.001f)
        assertEquals(1, legacy.interactiveTextLayers.size)
        assertEquals(1, legacy.interactiveStickerLayers.size)
    }
}
