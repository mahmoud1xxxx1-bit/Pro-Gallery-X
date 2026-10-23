package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class LayerTransformRulesTest {

    @Test
    fun dragDeltaWithinBounds() {
        val (nx, ny) = LayerTransformRules.applyDragDelta(
            currentX = 0.5f,
            currentY = 0.5f,
            dragPxX = 100f,
            dragPxY = 50f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f
        )
        assertEquals(0.6f, nx, 0.001f)
        assertEquals(0.55f, ny, 0.001f)
    }

    @Test
    fun dragDeltaClampsToBoundsPadding() {
        val (nx, ny) = LayerTransformRules.applyDragDelta(
            currentX = 0.95f,
            currentY = 0.95f,
            dragPxX = 500f,
            dragPxY = 500f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f,
            boundsPadding = 0.02f
        )
        assertEquals(0.98f, nx, 0.001f)
        assertEquals(0.98f, ny, 0.001f)

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
    fun scaleAndRotationBounded() {
        // Test scale clamping
        val (minScale, _) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 0.5f,
            currentRotationDegrees = 0f,
            zoomDelta = 0.1f,
            rotationDeltaDegrees = 0f
        )
        assertEquals(LayerTransformRules.MIN_SCALE, minScale, 0.001f)

        val (maxScale, _) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 5.0f,
            currentRotationDegrees = 0f,
            zoomDelta = 2.0f,
            rotationDeltaDegrees = 0f
        )
        assertEquals(LayerTransformRules.MAX_SCALE, maxScale, 0.001f)

        // Test rotation wrapping
        val (_, wrappedRot) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 1.0f,
            currentRotationDegrees = 350f,
            zoomDelta = 1.0f,
            rotationDeltaDegrees = 20f
        )
        assertEquals(10f, wrappedRot, 0.001f)

        val (_, negativeWrappedRot) = LayerTransformRules.applyScaleAndRotation(
            currentScale = 1.0f,
            currentRotationDegrees = 10f,
            zoomDelta = 1.0f,
            rotationDeltaDegrees = -30f
        )
        assertEquals(340f, negativeWrappedRot, 0.001f)
    }

    @Test
    fun textBoundingBoxAndHitTest() {
        val box = LayerTransformRules.computeTextBoundingBox(
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            scale = 1.0f,
            textLength = 10,
            fontSizeSp = 28f,
            containerWidthPx = 1000f,
            containerHeightPx = 1000f
        )
        assertTrue(box.left < 0.5f)
        assertTrue(box.right > 0.5f)
        assertTrue(box.top < 0.5f)
        assertTrue(box.bottom > 0.5f)

        // Hit test inside
        assertTrue(LayerTransformRules.hitTest(0.5f, 0.5f, box))
        // Hit test with slop
        assertTrue(LayerTransformRules.hitTest(box.right + 0.02f, box.centerY, box))
        // Hit test far outside
        assertFalse(LayerTransformRules.hitTest(0.05f, 0.05f, box))
    }

    @Test
    fun exportFontSizeAndStrokeScaling() {
        val fontSize = LayerTransformRules.calculateExportFontSize(
            fontSizeSp = 24f,
            scale = 1.0f,
            exportBitmapWidth = 1440
        )
        assertEquals(48f, fontSize, 0.01f)

        val strokeWidth = LayerTransformRules.calculateExportStrokeWidth(
            normalizedStrokeWidth = 0.01f,
            exportBitmapWidth = 1000
        )
        assertEquals(10f, strokeWidth, 0.01f)
    }

    @Test
    fun duplicatePositionOffset() {
        val (x1, y1) = LayerTransformRules.computeDuplicatePosition(0.5f, 0.5f)
        assertEquals(0.54f, x1, 0.001f)
        assertEquals(0.54f, y1, 0.001f)

        // Near edge should offset backward
        val (x2, y2) = LayerTransformRules.computeDuplicatePosition(0.95f, 0.95f)
        assertEquals(0.91f, x2, 0.001f)
        assertEquals(0.91f, y2, 0.001f)
    }

    @Test
    fun layerReorderingBringForwardAndSendBackward() {
        val list = listOf("A", "B", "C")
        val forward = LayerTransformRules.bringForward(list, 0)
        assertEquals(listOf("B", "A", "C"), forward)

        val backward = LayerTransformRules.sendBackward(list, 2)
        assertEquals(listOf("A", "C", "B"), backward)

        // Out of bounds or edge boundary
        assertEquals(list, LayerTransformRules.bringForward(list, 2))
        assertEquals(list, LayerTransformRules.sendBackward(list, 0))
    }
}
