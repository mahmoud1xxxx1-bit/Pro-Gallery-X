package com.progalleryx.app

/** Pure geometry calculations for photo editing, coordinate normalization, and preview aspect ratios. */
object PhotoGeometryRules {

    /** Determines effective aspect ratio for the editor canvas given base dimensions, rotation, and crop ratio. */
    fun previewAspectRatio(originalWidth: Int, originalHeight: Int, rotationDegrees: Int, cropRatio: Float): Float {
        require(originalWidth > 0 && originalHeight > 0)
        require(rotationDegrees in listOf(0, 90, 180, 270))
        require(cropRatio == 0f || (cropRatio.isFinite() && cropRatio > 0f))

        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270
        return if (cropRatio > 0f) {
            cropRatio
        } else {
            val w = if (isSwapped) originalHeight else originalWidth
            val h = if (isSwapped) originalWidth else originalHeight
            (w.toFloat() / h.toFloat()).coerceIn(0.2f, 5f)
        }
    }

    /** Clamps normalized coordinate into safe [0..1] range. */
    fun clampCoordinate(value: Float): Float = value.coerceIn(0f, 1f)

    /** Clamps text layer coordinate within safe boundaries [0.05..0.95]. */
    fun clampTextCoordinate(value: Float): Float = value.coerceIn(0.05f, 0.95f)

    /** Checks whether a point is within normalized canvas bounds. */
    fun isValidPoint(x: Float, y: Float): Boolean = x in 0f..1f && y in 0f..1f
}
