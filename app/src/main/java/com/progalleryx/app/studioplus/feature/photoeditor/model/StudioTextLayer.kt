package com.progalleryx.app.studioplus.feature.photoeditor.model

import android.graphics.Color
import android.graphics.Typeface
import java.util.UUID

enum class StudioTextStyle(val labelEn: String, val labelAr: String) {
    BOLD("Bold", "عريض"),
    REGULAR("Regular", "عادي"),
    SERIF("Serif", "كلاسيكي"),
    MONO("Monospace", "آلي");

    fun getTypeface(): Typeface {
        return when (this) {
            BOLD -> Typeface.DEFAULT_BOLD
            REGULAR -> Typeface.DEFAULT
            SERIF -> Typeface.SERIF
            MONO -> Typeface.MONOSPACE
        }
    }
}

enum class StudioTextBackgroundMode(val labelEn: String, val labelAr: String) {
    NONE("None", "بدون"),
    TRANSLUCENT("Glass", "زجاجي"),
    SOLID("Solid", "ممتلئ"),
    OUTLINE("Outline", "محدد")
}

/**
 * Immutable model for an interactive Arabic/multi-language text layer on the canvas.
 */
data class StudioTextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val normalizedX: Float = 0.5f, // Center of the canvas (0..1)
    val normalizedY: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val textColor: Int = Color.WHITE,
    val backgroundMode: StudioTextBackgroundMode = StudioTextBackgroundMode.NONE,
    val backgroundColor: Int = Color.argb(160, 0, 0, 0),
    val fontSizeSp: Float = 28f,
    val style: StudioTextStyle = StudioTextStyle.BOLD
)
