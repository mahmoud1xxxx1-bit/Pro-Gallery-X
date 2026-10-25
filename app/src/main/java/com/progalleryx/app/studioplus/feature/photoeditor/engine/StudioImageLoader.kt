package com.progalleryx.app.studioplus.feature.photoeditor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

/**
 * Memory-safe Bitmap loader for Studio Plus + Photo Editor.
 * Decodes preview bitmaps with proportional downsampling to ensure smooth 60fps editing
 * without OutOfMemoryError, while also supporting full-resolution decoding for export.
 */
object StudioImageLoader {

    suspend fun loadPreviewBitmap(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 1. Read dimensions only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // 2. Compute sample size
            var sampleSize = 1
            val maxSide = max(origWidth, origHeight)
            while ((maxSide / sampleSize) > maxDimensionPx) {
                sampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // 4. Adjust orientation from EXIF
            val exifOrientation = getExifOrientation(context, uri)
            applyExifOrientation(decoded, exifOrientation)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadFullResolutionBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val exifOrientation = getExifOrientation(context, uri)
            applyExifOrientation(decoded, exifOrientation)
        } catch (e: Exception) {
            null
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            val transformed = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (transformed != bitmap) {
                bitmap.recycle()
            }
            transformed
        } catch (_: Exception) {
            bitmap
        }
    }
}
