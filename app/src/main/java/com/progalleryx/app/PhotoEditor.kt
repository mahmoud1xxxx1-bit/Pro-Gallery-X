package com.progalleryx.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class PhotoAdjustments(val brightness: Float = 0f, val contrast: Float = 1f, val saturation: Float = 1f, val rotation: Int = 0, val flipHorizontal: Boolean = false, val flipVertical: Boolean = false, val cropRatio: Float = 0f, val jpegQuality: Int = 92)

/** Offline JPEG export. Source URI is read-only; destination must be a newly selected document. */
object PhotoEditor {
    suspend fun export(context: Context, source: Uri, destination: Uri, settings: PhotoAdjustments) =
        withContext(Dispatchers.IO) {
            require(settings.brightness in -100f..100f && settings.contrast in 0.5f..1.5f && settings.saturation in 0f..2f && settings.rotation in listOf(0, 90, 180, 270) && settings.cropRatio in listOf(0f, 1f, 4f / 3f, 16f / 9f) && settings.jpegQuality in 70..100)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: throw IOException("Unable to open image")
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            if (largest <= 0) throw IOException("Invalid image")
            var factor = 1
            while (largest / factor > 2400) factor *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = factor; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: throw IOException("Cannot decode image")
            try {
                val transform = Matrix().apply {
                    if (settings.flipHorizontal || settings.flipVertical) postScale(if (settings.flipHorizontal) -1f else 1f, if (settings.flipVertical) -1f else 1f)
                    postRotate(settings.rotation.toFloat())
                }
                val transformed = if (settings.rotation != 0 || settings.flipHorizontal || settings.flipVertical) Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true) else bitmap
                val ratio = settings.cropRatio
                val cropWidth = if (ratio == 0f) transformed.width else minOf(transformed.width, (transformed.height * ratio).toInt().coerceAtLeast(1))
                val cropHeight = if (ratio == 0f) transformed.height else minOf(transformed.height, (transformed.width / ratio).toInt().coerceAtLeast(1))
                val output = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
                try {
                    val colors = ColorMatrix().apply { setSaturation(settings.saturation) }
                    val contrast = settings.contrast
                    val offset = 128f * (1f - contrast) + settings.brightness * 2.55f
                    colors.postConcat(ColorMatrix(floatArrayOf(
                        contrast, 0f, 0f, 0f, offset,
                        0f, contrast, 0f, 0f, offset,
                        0f, 0f, contrast, 0f, offset,
                        0f, 0f, 0f, 1f, 0f
                    )))
                    Canvas(output).drawBitmap(transformed, -(transformed.width - cropWidth) / 2f, -(transformed.height - cropHeight) / 2f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
                        colorFilter = ColorMatrixColorFilter(colors)
                    })
                    context.contentResolver.openOutputStream(destination, "w")?.use { stream ->
                        if (!output.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, stream)) throw IOException("Export failed")
                    } ?: throw IOException("Output permission denied")
                } finally { output.recycle(); if (transformed !== bitmap) transformed.recycle() }
            } finally { bitmap.recycle() }
        }
}
