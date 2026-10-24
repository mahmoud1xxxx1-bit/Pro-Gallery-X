package com.progalleryx.app

import kotlin.math.*

enum class TextBackgroundStyle {
    TRANSPARENT,
    FILLED_BOX,      // Snapchat-style filled rounded box with contrast text
    PILL,            // Rounded capsule pill background
    OUTLINE,         // Bold outline around text
    SEMI_TRANSPARENT // Subtle black/white tinted translucent background
}

enum class TextFontStyle {
    DEFAULT,
    SERIF,
    MONOSPACE,
    BOLD_HEADING,
    CASUAL_HANDWRITING
}

enum class TextAlignment {
    START,
    CENTER,
    END
}

enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    STAR,
    ARROW,
    HEART,
    HEXAGON,
    DIAMOND,
    SPEECH_BUBBLE
}

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }
}

object LayerTransformRules {

    const val MIN_SCALE = 0.25f
    const val MAX_SCALE = 6.0f
    const val MAX_LAYERS_COUNT = 30

    /** Normalizes a drag delta in screen pixels into normalized 0.0..1.0 coordinate delta. */
    fun applyDragDelta(
        currentX: Float,
        currentY: Float,
        dragPxX: Float,
        dragPxY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        boundsPadding: Float = 0.02f
    ): Pair<Float, Float> {
        val w = containerWidthPx.coerceAtLeast(1f)
        val h = containerHeightPx.coerceAtLeast(1f)
        val newX = (currentX + dragPxX / w).coerceIn(boundsPadding, 1f - boundsPadding)
        val newY = (currentY + dragPxY / h).coerceIn(boundsPadding, 1f - boundsPadding)
        return Pair(newX, newY)
    }

    /** Applies scale and rotation gestures within bounded safe ranges. */
    fun applyScaleAndRotation(
        currentScale: Float,
        currentRotationDegrees: Float,
        zoomDelta: Float,
        rotationDeltaDegrees: Float
    ): Pair<Float, Float> {
        val newScale = (currentScale * zoomDelta).coerceIn(MIN_SCALE, MAX_SCALE)
        var newRot = (currentRotationDegrees + rotationDeltaDegrees) % 360f
        if (newRot < 0f) newRot += 360f
        return Pair(newScale, newRot)
    }

    /** Computes an approximate normalized hit box for a text layer. */
    fun computeTextBoundingBox(
        normalizedX: Float,
        normalizedY: Float,
        scale: Float,
        textLength: Int,
        fontSizeSp: Float,
        containerWidthPx: Float,
        containerHeightPx: Float
    ): NormalizedRect {
        val w = containerWidthPx.coerceAtLeast(1f)
        val h = containerHeightPx.coerceAtLeast(1f)

        // Estimated bounds based on character count and font scale
        val approxCharWidthPx = fontSizeSp * 0.55f * scale
        val approxHeightPx = fontSizeSp * 1.3f * scale
        val totalWidthPx = (approxCharWidthPx * textLength.coerceAtLeast(1)).coerceAtLeast(40f * scale)

        val halfWNorm = (totalWidthPx / 2f) / w
        val halfHNorm = (approxHeightPx / 2f) / h

        return NormalizedRect(
            left = (normalizedX - halfWNorm).coerceIn(0f, 1f),
            top = (normalizedY - halfHNorm).coerceIn(0f, 1f),
            right = (normalizedX + halfWNorm).coerceIn(0f, 1f),
            bottom = (normalizedY + halfHNorm).coerceIn(0f, 1f)
        )
    }

    /** Hit tests whether a touch at normalized coordinates hits the layer. */
    fun hitTest(
        touchXNorm: Float,
        touchYNorm: Float,
        layerBox: NormalizedRect,
        slopNorm: Float = 0.04f
    ): Boolean {
        val expanded = NormalizedRect(
            left = layerBox.left - slopNorm,
            top = layerBox.top - slopNorm,
            right = layerBox.right + slopNorm,
            bottom = layerBox.bottom + slopNorm
        )
        return expanded.contains(touchXNorm, touchYNorm)
    }

    /** Calculates mapped font size in pixels for export bitmap. */
    fun calculateExportFontSize(
        fontSizeSp: Float,
        scale: Float,
        exportBitmapWidth: Int
    ): Float {
        val baseFactor = exportBitmapWidth.toFloat() / 720f
        return (fontSizeSp * scale * baseFactor).coerceAtLeast(12f)
    }

    /** Calculates mapped brush stroke width in pixels for export bitmap. */
    fun calculateExportStrokeWidth(
        normalizedStrokeWidth: Float,
        exportBitmapWidth: Int
    ): Float {
        return (normalizedStrokeWidth * exportBitmapWidth.toFloat()).coerceAtLeast(1.5f)
    }

    /** Clones a layer with slight offset so it is visible beside original. */
    fun computeDuplicatePosition(
        x: Float,
        y: Float,
        offset: Float = 0.04f
    ): Pair<Float, Float> {
        val nextX = if (x + offset <= 0.92f) x + offset else x - offset
        val nextY = if (y + offset <= 0.92f) y + offset else y - offset
        return Pair(nextX, nextY)
    }

    /** Reorders layers: moves target layer one step forward towards top. */
    fun <T> bringForward(list: List<T>, index: Int): List<T> {
        if (index !in list.indices || index >= list.size - 1) return list
        val result = list.toMutableList()
        val item = result.removeAt(index)
        result.add(index + 1, item)
        return result
    }

    /** Reorders layers: moves target layer one step backward towards bottom. */
    fun <T> sendBackward(list: List<T>, index: Int): List<T> {
        if (index !in list.indices || index <= 0) return list
        val result = list.toMutableList()
        val item = result.removeAt(index)
        result.add(index - 1, item)
        return result
    }

    /** Calculates exact center offset for placing a layer on canvas. */
    fun calculateCenterOffset(
        normalizedX: Float,
        normalizedY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        layerWidthPx: Int,
        layerHeightPx: Int
    ): Pair<Int, Int> {
        val targetCenterX = (normalizedX * containerWidthPx.coerceAtLeast(1f)).roundToInt()
        val targetCenterY = (normalizedY * containerHeightPx.coerceAtLeast(1f)).roundToInt()
        return Pair(targetCenterX - layerWidthPx / 2, targetCenterY - layerHeightPx / 2)
    }

    /** Toggles lock state for a text layer in a list. */
    fun toggleTextLayerLock(layers: List<InteractiveTextLayer>, id: String): List<InteractiveTextLayer> {
        return layers.map { if (it.id == id) it.copy(isLocked = !it.isLocked) else it }
    }

    /** Toggles lock state for a sticker layer in a list. */
    fun toggleStickerLayerLock(layers: List<InteractiveStickerLayer>, id: String): List<InteractiveStickerLayer> {
        return layers.map { if (it.id == id) it.copy(isLocked = !it.isLocked) else it }
    }
}
