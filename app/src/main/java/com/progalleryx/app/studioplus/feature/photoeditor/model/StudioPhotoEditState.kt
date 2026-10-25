package com.progalleryx.app.studioplus.feature.photoeditor.model

import android.graphics.ColorMatrix

enum class StudioEditorTab(val labelEn: String, val labelAr: String) {
    TRANSFORM("Transform", "القص والتدوير"),
    FILTERS("Filters", "الفلاتر"),
    ADJUST("Adjust", "الإضاءة والألوان"),
    TEXT("Text", "النصوص"),
    DOODLE("Draw", "الرسم")
}

data class StudioCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceAtLeast(0.01f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.01f)
}

/**
 * Immutable state container for Studio Plus + Photo Editor.
 * Every edit produces a new immutable copy, enabling 100% bug-free Undo / Redo.
 */
data class StudioPhotoEditState(
    // Tone adjustments (-100..+100 or 0..100)
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,

    // Filter
    val filterType: StudioFilterType = StudioFilterType.ORIGINAL,
    val filterIntensity: Float = 1.0f,

    // Transform
    val rotationSteps90: Int = 0,
    val fineRotationAngle: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRect: StudioCropRect = StudioCropRect(),
    val targetAspectRatio: Float? = null, // null = free

    // Overlays
    val textLayers: List<StudioTextLayer> = emptyList(),
    val doodlePaths: List<StudioDoodlePath> = emptyList(),

    // UI state
    val activeTab: StudioEditorTab = StudioEditorTab.TRANSFORM,
    val selectedTextLayerId: String? = null
) {
    val totalRotationDegrees: Float
        get() = (rotationSteps90 * 90f + fineRotationAngle) % 360f

    val hasModifications: Boolean
        get() = brightness != 0f ||
                contrast != 0f ||
                saturation != 0f ||
                warmth != 0f ||
                vignette != 0f ||
                filterType != StudioFilterType.ORIGINAL ||
                rotationSteps90 != 0 ||
                fineRotationAngle != 0f ||
                flipHorizontal ||
                flipVertical ||
                cropRect.left > 0.001f ||
                cropRect.top > 0.001f ||
                cropRect.right < 0.999f ||
                cropRect.bottom < 0.999f ||
                textLayers.isNotEmpty() ||
                doodlePaths.isNotEmpty()

    /**
     * Builds the unified ColorMatrix combining the preset filter with custom tone adjustments.
     */
    fun buildUnifiedColorMatrix(): ColorMatrix {
        val result = filterType.getColorMatrix(filterIntensity)

        // 1. Brightness (-100..100 -> offset -128..128)
        if (brightness != 0f) {
            val offset = (brightness / 100f) * 128f
            val brightMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, offset,
                    0f, 1f, 0f, 0f, offset,
                    0f, 0f, 1f, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(brightMatrix)
        }

        // 2. Contrast (-100..100 -> scale 0.5..1.8)
        if (contrast != 0f) {
            val scale = if (contrast > 0) 1f + (contrast / 100f) * 0.9f else 1f + (contrast / 100f) * 0.5f
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(contrastMatrix)
        }

        // 3. Saturation (-100..100 -> scale 0..2.0)
        if (saturation != 0f) {
            val satScale = (1f + saturation / 100f).coerceIn(0f, 2.5f)
            val satMatrix = ColorMatrix().apply { setSaturation(satScale) }
            result.postConcat(satMatrix)
        }

        // 4. Warmth (-100..100 -> shift red/blue)
        if (warmth != 0f) {
            val warmShift = (warmth / 100f) * 25f
            val warmthMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, warmShift,
                    0f, 1f, 0f, 0f, warmShift * 0.4f,
                    0f, 0f, 1f, 0f, -warmShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(warmthMatrix)
        }

        return result
    }
}
