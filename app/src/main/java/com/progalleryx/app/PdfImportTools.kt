package com.progalleryx.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Offline raster-based PDF merging or extracting. Does not retain selectable text or vector content. */
object PdfImportTools {
    suspend fun merge(context: Context, inputs: List<Uri>, destination: Uri, maxPages: Int = 120) =
        render(context, inputs, destination, null, maxPages)

    suspend fun extract(context: Context, source: Uri, destination: Uri, pages: IntRange) {
        require(pages.first >= 1 && pages.last >= pages.first) { "Invalid page range" }
        render(context, listOf(source), destination, pages, 120)
    }

    private suspend fun render(
        context: Context, inputs: List<Uri>, destination: Uri,
        range: IntRange?, maxPages: Int
    ) = withContext(Dispatchers.IO) {
        require(inputs.isNotEmpty() && inputs.size <= 10) { "Choose up to ten PDFs" }
        require(maxPages in 1..120)
        require(destination !in inputs) { "Output must not replace any original" }
        val temp = File.createTempFile("pgx_merge_", ".pdf", context.cacheDir)
        val document = PdfDocument()
        var written = 0
        try {
            inputs.forEach { source ->
                currentCoroutineContext().ensureActive()
                val local = File.createTempFile("pgx_input_", ".pdf", context.cacheDir)
                try {
                    context.contentResolver.openInputStream(source)?.use { input ->
                        local.outputStream().use { input.copyTo(it, 256 * 1024) }
                    } ?: throw IOException("Cannot open PDF")
                    ParcelFileDescriptor.open(local, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                        PdfRenderer(fd).use { renderer ->
                            val begin = range?.first?.minus(1) ?: 0
                            val finish = range?.last?.minus(1) ?: (renderer.pageCount - 1)
                            require(begin >= 0 && finish < renderer.pageCount) { "Selected pages exceed document length" }
                            require(written + finish - begin + 1 <= maxPages) { "Maximum 120 pages per export" }
                            for (i in begin..finish) {
                                currentCoroutineContext().ensureActive()
                                renderer.openPage(i).use { page ->
                                    val width = page.width.coerceAtLeast(1)
                                    val height = page.height.coerceAtLeast(1)
                                    val scale = minOf(1.5f, 1600f / maxOf(width, height))
                                    val bitmap = Bitmap.createBitmap(
                                        (width * scale).toInt().coerceAtLeast(1),
                                        (height * scale).toInt().coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    try {
                                        bitmap.eraseColor(Color.WHITE)
                                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                        val outputPage = document.startPage(
                                            PdfDocument.PageInfo.Builder(width, height, written + 1).create()
                                        )
                                        outputPage.canvas.drawColor(Color.WHITE)
                                        outputPage.canvas.drawBitmap(bitmap, null,
                                            android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
                                        document.finishPage(outputPage)
                                        written++
                                    } finally { bitmap.recycle() }
                                }
                            }
                        }
                    }
                } finally { local.delete() }
            }
            require(written > 0)
            temp.outputStream().use { document.writeTo(it) }
            if (temp.length() <= 0) throw IOException("Empty PDF output")
            currentCoroutineContext().ensureActive()
            context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                temp.inputStream().use { it.copyTo(out, 256 * 1024) }
            } ?: throw IOException("Cannot save PDF")
        } finally {
            document.close()
            temp.delete()
        }
    }
}
