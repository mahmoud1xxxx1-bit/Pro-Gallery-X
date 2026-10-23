package com.progalleryx.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * First offline export tools. Media originals are read-only and never overwritten.
 * Decode limits deliberately bound memory usage on large camera photos.
 */
enum class PdfLayout { PORTRAIT, LANDSCAPE, AUTO }

object OfflineExports {
    suspend fun rotateAndSave(context: Context, source: Uri, destination: Uri): Unit =
        withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: throw IOException("Cannot read photo")
            val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
            if (maxDimension <= 0) throw IOException("Invalid photo dimensions")
            var sample = 1
            while (maxDimension / sample > 2400) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val original = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: throw IOException("Cannot decode photo")
            try {
                val matrix = Matrix().apply { postRotate(90f) }
                val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                try {
                    context.contentResolver.openOutputStream(destination, "w")?.use {
                        if (!rotated.compress(Bitmap.CompressFormat.JPEG, 90, it)) throw IOException("Image export failed")
                    } ?: throw IOException("Cannot write image")
                } finally { if (rotated !== original) rotated.recycle() }
            } finally { original.recycle() }
        }

    suspend fun imagesToPdf(context: Context, images: List<Uri>, destination: Uri, layout: PdfLayout = PdfLayout.PORTRAIT, pageNumbers: Boolean = false): Unit =
        withContext(Dispatchers.IO) {
            require(images.isNotEmpty() && images.size <= 100) { "Select 1–100 images" }
            val pdf = PdfDocument()
            val temp = File.createTempFile("pgx_pdf_", ".pdf", context.cacheDir)
            try {
                images.forEachIndexed { index, uri ->
                    currentCoroutineContext().ensureActive()
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                        ?: throw IOException("Cannot read image ${index + 1}")
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unsupported image")
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    val bitmap = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    } ?: throw IOException("Cannot decode image ${index + 1}")
                    try {
                        val landscape = layout == PdfLayout.LANDSCAPE || (layout == PdfLayout.AUTO && bitmap.width > bitmap.height)
                        val pageWidth = if (landscape) 842 else 595
                        val pageHeight = if (landscape) 595 else 842
                        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                        val scale = minOf((pageWidth - 40f) / bitmap.width, (pageHeight - 40f) / bitmap.height)
                        val left = (pageWidth - bitmap.width * scale) / 2f
                        val top = (pageHeight - bitmap.height * scale) / 2f
                        page.canvas.drawBitmap(bitmap, null,
                            android.graphics.RectF(left, top, left + bitmap.width * scale, top + bitmap.height * scale), null)
                        if (pageNumbers) {
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.DKGRAY
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 11f
                            }
                            page.canvas.drawText("${index + 1} / ${images.size}", pageWidth / 2f, pageHeight - 10f, paint)
                        }
                        pdf.finishPage(page)
                    } finally { bitmap.recycle() }
                }
                temp.outputStream().use { pdf.writeTo(it) }
                if (temp.length() <= 0L) throw IOException("Empty PDF output")
                currentCoroutineContext().ensureActive()
                context.contentResolver.openOutputStream(destination, "w")?.use { output ->
                    temp.inputStream().use { it.copyTo(output, 256 * 1024) }
                } ?: throw IOException("Cannot save PDF")
            } finally {
                pdf.close()
                temp.delete()
            }
        }
}
