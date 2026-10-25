package com.progalleryx.app.studioplus.feature.photoeditor.model

import android.graphics.Color
import java.util.UUID

/**
 * Immutable model for a freehand drawing stroke on the image.
 * Points are stored as normalized coordinates (0f..1f) to ensure 100% precision
 * when rendering on both preview screen and full-sensor resolution export.
 */
data class StudioDoodlePath(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Pair<Float, Float>>,
    val color: Int = Color.parseColor("#2E6EEF"),
    val strokeWidthRelative: Float = 0.012f, // Fraction of image width
    val isHighlighter: Boolean = false,
    val isEraser: Boolean = false
)
