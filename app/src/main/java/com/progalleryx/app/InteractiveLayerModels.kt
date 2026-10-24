package com.progalleryx.app

import java.util.UUID

data class InteractiveTextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val fontSizeSp: Float = 28f,
    val colorArgb: Long = 0xFFFFFFFF,
    val backgroundColorArgb: Long = 0x00000000,
    val backgroundStyle: TextBackgroundStyle = TextBackgroundStyle.TRANSPARENT,
    val fontStyle: TextFontStyle = TextFontStyle.DEFAULT,
    val textAlign: TextAlignment = TextAlignment.CENTER,
    val opacity: Float = 1.0f,
    val hasShadow: Boolean = false,
    val hasOutline: Boolean = false,
    val isLocked: Boolean = false
) {
    fun toLegacyLayer(): PhotoTextLayer {
        val isDark = ColorPaletteRules.getContrastingTextColor(colorArgb) == 0xFFFFFFFFL
        return PhotoTextLayer(
            text = text.take(80),
            x = normalizedX.coerceIn(0.05f, 0.95f),
            y = normalizedY.coerceIn(0.05f, 0.95f),
            size = fontSizeSp.toInt().coerceIn(16, 72),
            dark = isDark
        )
    }

    companion object {
        fun fromLegacy(legacy: PhotoTextLayer): InteractiveTextLayer {
            return InteractiveTextLayer(
                text = legacy.text,
                normalizedX = legacy.x,
                normalizedY = legacy.y,
                fontSizeSp = legacy.size.toFloat(),
                colorArgb = if (legacy.dark) 0xFF121212L else 0xFFFFFFFFL
            )
        }
    }
}

data class InteractiveStickerLayer(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String = "",
    val shapeType: ShapeType? = null,
    val customImageUri: String? = null,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val colorArgb: Long = 0xFFFFFFFF,
    val opacity: Float = 1.0f,
    val isLocked: Boolean = false
)

data class NormalizedPoint(val x: Float, val y: Float)

data class SmoothBrushStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NormalizedPoint>,
    val colorArgb: Long = 0xFFFF3B30,
    val strokeWidthNormalized: Float = 0.015f,
    val isEraser: Boolean = false
) {
    fun toLegacyStroke(): BrushStroke {
        return BrushStroke(
            points = points.map { BrushPoint(it.x, it.y) },
            dark = (colorArgb and 0x00FFFFFFL) < 0x888888L,
            width = strokeWidthNormalized
        )
    }
}

data class PhotoStudioProject(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val vignette: Float = 0f,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRatio: Float = 0f,
    val cropX: Float = 0.5f,
    val cropY: Float = 0.5f,
    val filterId: String = PhotoFilterPresets.ORIGINAL.id,
    val filterIntensity: Float = 1.0f,
    val textLayers: List<InteractiveTextLayer> = emptyList(),
    val stickerLayers: List<InteractiveStickerLayer> = emptyList(),
    val brushStrokes: List<SmoothBrushStroke> = emptyList(),
    val activeLayerId: String? = null
) {
    fun toLegacyAdjustments(): PhotoAdjustments {
        return PhotoAdjustments(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            rotation = rotation,
            flipHorizontal = flipHorizontal,
            flipVertical = flipVertical,
            cropRatio = cropRatio,
            cropX = cropX,
            cropY = cropY,
            temperature = temperature,
            tint = tint,
            textLayers = textLayers.take(3).map { it.toLegacyLayer() },
            brushStrokes = brushStrokes.map { it.toLegacyStroke() },
            interactiveTextLayers = textLayers,
            interactiveStickerLayers = stickerLayers,
            smoothBrushStrokes = brushStrokes
        )
    }

    companion object {
        fun fromLegacy(adj: PhotoAdjustments): PhotoStudioProject {
            return PhotoStudioProject(
                brightness = adj.brightness,
                contrast = adj.contrast,
                saturation = adj.saturation,
                rotation = adj.rotation,
                flipHorizontal = adj.flipHorizontal,
                flipVertical = adj.flipVertical,
                cropRatio = adj.cropRatio,
                cropX = adj.cropX,
                cropY = adj.cropY,
                temperature = adj.temperature,
                tint = adj.tint,
                textLayers = adj.textLayers.map { InteractiveTextLayer.fromLegacy(it) },
                brushStrokes = adj.brushStrokes.map { stroke ->
                    SmoothBrushStroke(
                        points = stroke.points.map { NormalizedPoint(it.x, it.y) },
                        colorArgb = if (stroke.dark) 0xFF121212L else 0xFFFFFFFFL,
                        strokeWidthNormalized = stroke.width
                    )
                }
            )
        }
    }
}
