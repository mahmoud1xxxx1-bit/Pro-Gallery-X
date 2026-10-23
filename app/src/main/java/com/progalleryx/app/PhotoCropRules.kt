package com.progalleryx.app

/** Pure calculations shared by JPEG crop exports; no Android dependencies. */
data class CropArea(val left: Int, val top: Int, val width: Int, val height: Int)

object PhotoCropRules {
    fun area(imageWidth: Int, imageHeight: Int, ratio: Float, focusX: Float, focusY: Float): CropArea {
        require(imageWidth > 0 && imageHeight > 0)
        require(ratio == 0f || (ratio.isFinite() && ratio > 0f))
        require(focusX in 0f..1f && focusY in 0f..1f)
        val width = if (ratio == 0f) imageWidth
            else minOf(imageWidth, (imageHeight * ratio).toInt().coerceAtLeast(1))
        val height = if (ratio == 0f) imageHeight
            else minOf(imageHeight, (imageWidth / ratio).toInt().coerceAtLeast(1))
        return CropArea(
            ((imageWidth - width) * focusX).toInt().coerceIn(0, imageWidth - width),
            ((imageHeight - height) * focusY).toInt().coerceIn(0, imageHeight - height),
            width, height
        )
    }
}
