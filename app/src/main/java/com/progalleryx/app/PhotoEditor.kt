package com.progalleryx.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class PhotoTextLayer(val text: String, val x: Float = 0.5f, val y: Float = 0.5f, val size: Int = 32, val dark: Boolean = false) {
    fun valid() = text.length <= 80 && x in 0.05f..0.95f && y in 0.05f..0.95f && size in 16..72
}

data class PhotoAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRatio: Float = 0f,
    val jpegQuality: Int = 92,
    val caption: String = "",
    val captionDark: Boolean = false,
    val captionSize: Int = 32,
    val captionPosition: Float = 0.9f,
    val maxDimension: Int = 2400,
    val cropX: Float = 0.5f,
    val cropY: Float = 0.5f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val textLayers: List<PhotoTextLayer> = emptyList(),
    val brushStrokes: List<BrushStroke> = emptyList(),
    val interactiveTextLayers: List<InteractiveTextLayer> = emptyList(),
    val interactiveStickerLayers: List<InteractiveStickerLayer> = emptyList(),
    val smoothBrushStrokes: List<SmoothBrushStroke> = emptyList()
)

/** Offline JPEG export. Source URI is read-only; destination must be a newly selected document. */
object PhotoEditor {
    suspend fun exportBatch(
        context: Context,
        targets: List<Pair<Uri, Uri>>,
        settings: PhotoAdjustments,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        require(PhotoBatchRules.validBatchSize(targets.size)) { "Batch size must be between 1 and 25" }
        targets.forEachIndexed { index, (source, dest) ->
            currentCoroutineContext().ensureActive()
            export(context, source, dest, settings)
            onProgress?.invoke(index + 1, targets.size)
        }
    }

    suspend fun export(context: Context, source: Uri, destination: Uri, settings: PhotoAdjustments) =
        withContext(Dispatchers.IO) {
            require(source != destination) { "Original and output must be different" }
            require(
                settings.brightness in -100f..100f &&
                settings.contrast in 0.5f..1.5f &&
                settings.saturation in 0f..2f &&
                settings.rotation in listOf(0, 90, 180, 270) &&
                settings.cropRatio in listOf(0f, 1f, 4f / 3f, 16f / 9f, 3f / 4f, 9f / 16f) &&
                settings.jpegQuality in 70..100 &&
                settings.caption.length <= 80 &&
                settings.captionSize in 16..72 &&
                settings.captionPosition in 0.1f..0.9f &&
                settings.maxDimension in listOf(1200, 2400, 3600) &&
                settings.cropX in 0f..1f &&
                settings.cropY in 0f..1f &&
                settings.temperature in -100f..100f &&
                settings.tint in -100f..100f &&
                settings.textLayers.size <= 3 &&
                settings.textLayers.all { it.valid() } &&
                settings.brushStrokes.size <= 100 &&
                settings.brushStrokes.all { it.valid() } &&
                settings.interactiveTextLayers.size <= LayerTransformRules.MAX_LAYERS_COUNT &&
                settings.interactiveStickerLayers.size <= LayerTransformRules.MAX_LAYERS_COUNT &&
                settings.smoothBrushStrokes.size <= 200
            )
            val orientation = runCatching {
                context.contentResolver.openInputStream(source)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: throw IOException("Unable to open image")
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            if (largest <= 0) throw IOException("Invalid image")
            var factor = 1
            while (largest / factor > settings.maxDimension) factor *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = factor; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: throw IOException("Cannot decode image")
            try {
                val exifMatrix = Matrix().apply {
                    when (orientation) {
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                        ExifInterface.ORIENTATION_TRANSPOSE -> { postScale(-1f, 1f); postRotate(270f) }
                        ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                        ExifInterface.ORIENTATION_TRANSVERSE -> { postScale(-1f, 1f); postRotate(90f) }
                        ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    }
                }
                val upright = if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation in 2..8)
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, exifMatrix, true)
                else bitmap
                try {
                val transform = Matrix().apply {
                    if (settings.flipHorizontal || settings.flipVertical) postScale(if (settings.flipHorizontal) -1f else 1f, if (settings.flipVertical) -1f else 1f)
                    postRotate(settings.rotation.toFloat())
                }
                val transformed = if (settings.rotation != 0 || settings.flipHorizontal || settings.flipVertical) Bitmap.createBitmap(upright, 0, 0, upright.width, upright.height, transform, true) else upright
                val crop = PhotoCropRules.area(transformed.width, transformed.height, settings.cropRatio, settings.cropX, settings.cropY)
                val cropWidth = crop.width
                val cropHeight = crop.height
                val output = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
                try {
                    val colors = ColorMatrix().apply { setSaturation(settings.saturation) }
                    colors.postConcat(ColorMatrix(PhotoColorRules.contrastBrightness(settings.contrast, settings.brightness)))
                    colors.postConcat(ColorMatrix(PhotoColorRules.temperatureTint(settings.temperature, settings.tint)))
                    Canvas(output).drawBitmap(transformed, -crop.left.toFloat(), -crop.top.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG).apply {
                        colorFilter = ColorMatrixColorFilter(colors)
                    })

                    val canvas = Canvas(output)

                    // 1. Draw Smooth Brush Strokes (rich ARGB)
                    if (settings.smoothBrushStrokes.isNotEmpty()) {
                        settings.smoothBrushStrokes.forEach { stroke ->
                            drawSmoothBrushStroke(canvas, stroke, output.width, output.height)
                        }
                    }

                    // 2. Draw Legacy Brush Strokes (backward compatibility)
                    if (settings.brushStrokes.isNotEmpty()) {
                        settings.brushStrokes.forEach { stroke ->
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = if (stroke.dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                style = Paint.Style.STROKE
                                strokeWidth = (output.width * stroke.width).coerceAtLeast(1f)
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                            }
                            val path = android.graphics.Path()
                            stroke.points.forEachIndexed { index, point ->
                                val x = point.x * output.width
                                val y = point.y * output.height
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (stroke.points.size == 1) {
                                canvas.drawCircle(stroke.points[0].x * output.width, stroke.points[0].y * output.height, paint.strokeWidth / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paint.color })
                            } else canvas.drawPath(path, paint)
                        }
                    }

                    // 3. Draw Interactive Stickers & Shapes
                    if (settings.interactiveStickerLayers.isNotEmpty()) {
                        settings.interactiveStickerLayers.forEach { sticker ->
                            drawStickerOnCanvas(context, canvas, sticker, output.width, output.height)
                        }
                    }

                    // 4. Draw Legacy Captions (backward compatibility)
                    if (settings.caption.isNotBlank()) {
                        val scale = output.width / 1000f
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (settings.captionDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                            textAlign = Paint.Align.CENTER
                            textSize = settings.captionSize * scale.coerceAtLeast(0.5f)
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                            setShadowLayer(2.5f * scale.coerceAtLeast(0.5f), 1f, 1f, if (settings.captionDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                        }
                        val baseline = (output.height * settings.captionPosition).coerceIn(paint.textSize, output.height - paint.textSize * 0.2f)
                        canvas.drawText(settings.caption, output.width / 2f, baseline, paint)
                    }

                    // 5. Draw Legacy Text Layers (backward compatibility)
                    for (layer in settings.textLayers) {
                        if (layer.text.isBlank()) continue
                        val scale = (output.width / 1000f).coerceAtLeast(0.5f)
                        val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (layer.dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                            textAlign = Paint.Align.CENTER
                            textSize = layer.size * scale
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                            setShadowLayer(2.5f * scale, 1f, 1f, if (layer.dark) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                        }
                        val x = output.width * layer.x
                        val y = (output.height * layer.y).coerceIn(layerPaint.textSize, output.height - layerPaint.textSize * 0.2f)
                        canvas.drawText(layer.text, x, y, layerPaint)
                    }

                    // 6. Draw Interactive Text Layers (rich typography, background boxes, rotation)
                    if (settings.interactiveTextLayers.isNotEmpty()) {
                        settings.interactiveTextLayers.forEach { layer ->
                            drawInteractiveTextOnCanvas(canvas, layer, output.width, output.height)
                        }
                    }

                    val temp = File.createTempFile("pgx_photo_", ".jpg", context.cacheDir)
                    try {
                        temp.outputStream().use { stream ->
                            if (!output.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, stream)) throw IOException("JPEG encoding failed")
                        }
                        if (temp.length() <= 0L) throw IOException("Empty JPEG export")
                        currentCoroutineContext().ensureActive()
                        val outputStream = if (destination.scheme == "file" && destination.path != null) {
                            java.io.FileOutputStream(java.io.File(destination.path!!))
                        } else {
                            context.contentResolver.openOutputStream(destination, "w")
                        }
                        outputStream?.use { stream ->
                            temp.inputStream().use { it.copyTo(stream, 256 * 1024) }
                        } ?: throw IOException("Output permission denied")
                    } finally { temp.delete() }
                } finally { output.recycle(); if (transformed !== upright) transformed.recycle() }
                } finally { if (upright !== bitmap) upright.recycle() }
            } finally { bitmap.recycle() }
        }

    private fun drawSmoothBrushStroke(canvas: Canvas, stroke: SmoothBrushStroke, bitmapWidth: Int, bitmapHeight: Int) {
        if (stroke.points.isEmpty()) return
        val strokeColor = stroke.colorArgb.toInt()
        val strokeWidthPx = LayerTransformRules.calculateExportStrokeWidth(stroke.strokeWidthNormalized, bitmapWidth)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        if (stroke.points.size == 1) {
            val pt = stroke.points[0]
            canvas.drawCircle(pt.x * bitmapWidth, pt.y * bitmapHeight, strokeWidthPx / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.FILL
            })
        } else {
            val path = android.graphics.Path()
            val pts = stroke.points
            path.moveTo(pts[0].x * bitmapWidth, pts[0].y * bitmapHeight)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val curr = pts[i]
                val midX = (prev.x + curr.x) * bitmapWidth / 2f
                val midY = (prev.y + curr.y) * bitmapHeight / 2f
                path.quadTo(prev.x * bitmapWidth, prev.y * bitmapHeight, midX, midY)
            }
            path.lineTo(pts.last().x * bitmapWidth, pts.last().y * bitmapHeight)
            canvas.drawPath(path, paint)
        }
    }

    private fun drawStickerOnCanvas(context: Context, canvas: Canvas, sticker: InteractiveStickerLayer, bitmapWidth: Int, bitmapHeight: Int) {
        val cx = sticker.normalizedX * bitmapWidth
        val cy = sticker.normalizedY * bitmapHeight
        val baseSize = bitmapWidth * 0.12f * sticker.scale

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(sticker.rotationDegrees)

        if (!sticker.customImageUri.isNullOrBlank()) {
            runCatching {
                val uri = android.net.Uri.parse(sticker.customImageUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { customBmp ->
                val aspect = customBmp.width.toFloat() / customBmp.height.toFloat().coerceAtLeast(1f)
                val dstW = if (aspect >= 1f) baseSize else baseSize * aspect
                val dstH = if (aspect >= 1f) baseSize / aspect else baseSize
                val dstRect = android.graphics.RectF(-dstW / 2f, -dstH / 2f, dstW / 2f, dstH / 2f)
                val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = (sticker.opacity * 255).toInt().coerceIn(0, 255)
                }
                canvas.drawBitmap(customBmp, null, dstRect, bmpPaint)
            }
        } else if (sticker.emoji.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = baseSize
                textAlign = Paint.Align.CENTER
            }
            val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(sticker.emoji, 0f, -yOffset, textPaint)
        } else if (sticker.shapeType != null) {
            val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = ((sticker.opacity * 255).toInt().coerceIn(0, 255) shl 24) or (sticker.colorArgb.toInt() and 0x00FFFFFF)
            }
            val half = baseSize / 2f
            when (sticker.shapeType) {
                ShapeType.CIRCLE -> {
                    canvas.drawCircle(0f, 0f, half, shapePaint)
                }
                ShapeType.RECTANGLE -> {
                    canvas.drawRoundRect(android.graphics.RectF(-half, -half, half, half), baseSize * 0.15f, baseSize * 0.15f, shapePaint)
                }
                ShapeType.HEART -> {
                    val path = android.graphics.Path().apply {
                        val w = baseSize
                        moveTo(0f, half * 0.7f)
                        cubicTo(-w * 0.4f, half * 0.1f, -half, -half * 0.5f, -half * 0.5f, -half * 0.8f)
                        cubicTo(-w * 0.05f, -half * 1.0f, 0f, -half * 0.6f, 0f, -half * 0.5f)
                        cubicTo(0f, -half * 0.6f, w * 0.05f, -half * 1.0f, half * 0.5f, -half * 0.8f)
                        cubicTo(half, -half * 0.5f, w * 0.4f, half * 0.1f, 0f, half * 0.7f)
                        close()
                    }
                    canvas.drawPath(path, shapePaint)
                }
                ShapeType.STAR -> {
                    val path = android.graphics.Path()
                    val outerR = half
                    val innerR = outerR * 0.4f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outerR else innerR
                        val angle = Math.toRadians((i * 36 - 90).toDouble())
                        val x = (r * Math.cos(angle)).toFloat()
                        val y = (r * Math.sin(angle)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, shapePaint)
                }
                ShapeType.ARROW -> {
                    val path = android.graphics.Path().apply {
                        moveTo(-half * 0.6f, -half * 0.2f)
                        lineTo(0f, -half * 0.2f)
                        lineTo(0f, -half * 0.6f)
                        lineTo(half * 0.8f, 0f)
                        lineTo(0f, half * 0.6f)
                        lineTo(0f, half * 0.2f)
                        lineTo(-half * 0.6f, half * 0.2f)
                        close()
                    }
                    canvas.drawPath(path, shapePaint)
                }
                ShapeType.HEXAGON -> {
                    val path = android.graphics.Path()
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((i * 60 - 30).toDouble())
                        val x = (half * Math.cos(angle)).toFloat()
                        val y = (half * Math.sin(angle)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    canvas.drawPath(path, shapePaint)
                }
                ShapeType.DIAMOND -> {
                    val path = android.graphics.Path().apply {
                        moveTo(0f, -half)
                        lineTo(half, 0f)
                        lineTo(0f, half)
                        lineTo(-half, 0f)
                        close()
                    }
                    canvas.drawPath(path, shapePaint)
                }
                ShapeType.SPEECH_BUBBLE -> {
                    val path = android.graphics.Path().apply {
                        addRoundRect(android.graphics.RectF(-half, -half, half, half * 0.65f), baseSize * 0.15f, baseSize * 0.15f, android.graphics.Path.Direction.CW)
                        moveTo(-half * 0.4f, half * 0.65f)
                        lineTo(-half * 0.6f, half)
                        lineTo(-half * 0.15f, half * 0.65f)
                        close()
                    }
                    canvas.drawPath(path, shapePaint)
                }
            }
        }
        canvas.restore()
    }

    private fun drawInteractiveTextOnCanvas(canvas: Canvas, layer: InteractiveTextLayer, bitmapWidth: Int, bitmapHeight: Int) {
        if (layer.text.isBlank()) return
        val fontSizePx = LayerTransformRules.calculateExportFontSize(layer.fontSizeSp, layer.scale, bitmapWidth)
        val typeface = when (layer.fontStyle) {
            TextFontStyle.DEFAULT -> android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            TextFontStyle.SERIF -> android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
            TextFontStyle.MONOSPACE -> android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
            TextFontStyle.BOLD_HEADING -> android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            TextFontStyle.CASUAL_HANDWRITING -> android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ((layer.opacity * 255).toInt().coerceIn(0, 255) shl 24) or (layer.colorArgb.toInt() and 0x00FFFFFF)
            this.textSize = fontSizePx
            this.typeface = typeface
            textAlign = when (layer.textAlign) {
                TextAlignment.START -> Paint.Align.LEFT
                TextAlignment.CENTER -> Paint.Align.CENTER
                TextAlignment.END -> Paint.Align.RIGHT
            }
            if (layer.hasShadow) {
                setShadowLayer(fontSizePx * 0.12f, 3f, 3f, android.graphics.Color.BLACK)
            }
        }

        val lines = layer.text.split("\n")
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading
        val totalHeight = lines.size * lineHeight
        var maxLineWidth = 0f
        lines.forEach { line ->
            val w = textPaint.measureText(line)
            if (w > maxLineWidth) maxLineWidth = w
        }

        val cx = layer.normalizedX * bitmapWidth
        val cy = layer.normalizedY * bitmapHeight

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(layer.rotationDegrees)

        val padX = fontSizePx * 0.5f
        val padY = fontSizePx * 0.3f
        val halfW = (maxLineWidth / 2f) + padX
        val halfH = (totalHeight / 2f) + padY
        val bgRect = android.graphics.RectF(-halfW, -halfH, halfW, halfH)

        when (layer.backgroundStyle) {
            TextBackgroundStyle.FILLED_BOX -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = ((layer.opacity * 255).toInt().coerceIn(0, 255) shl 24) or (layer.backgroundColorArgb.toInt() and 0x00FFFFFF)
                }
                canvas.drawRoundRect(bgRect, fontSizePx * 0.35f, fontSizePx * 0.35f, bgPaint)
            }
            TextBackgroundStyle.PILL -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = ((layer.opacity * 255).toInt().coerceIn(0, 255) shl 24) or (layer.backgroundColorArgb.toInt() and 0x00FFFFFF)
                }
                val pillRadius = halfH.coerceAtLeast(fontSizePx * 0.5f)
                canvas.drawRoundRect(bgRect, pillRadius, pillRadius, bgPaint)
            }
            TextBackgroundStyle.SEMI_TRANSPARENT -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = ((0.55f * layer.opacity * 255).toInt().coerceIn(0, 255) shl 24) or 0x00121212
                }
                canvas.drawRoundRect(bgRect, fontSizePx * 0.35f, fontSizePx * 0.35f, bgPaint)
            }
            TextBackgroundStyle.OUTLINE -> {
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (fontSizePx * 0.08f).coerceAtLeast(2.5f)
                    color = textPaint.color
                }
                canvas.drawRoundRect(bgRect, fontSizePx * 0.35f, fontSizePx * 0.35f, outlinePaint)
            }
            TextBackgroundStyle.TRANSPARENT -> { /* No background box */ }
        }

        if (layer.hasOutline && layer.backgroundStyle != TextBackgroundStyle.OUTLINE) {
            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (fontSizePx * 0.08f).coerceAtLeast(2.5f)
                color = textPaint.color
            }
            val radius = if (layer.backgroundStyle == TextBackgroundStyle.PILL) halfH else fontSizePx * 0.35f
            canvas.drawRoundRect(bgRect, radius, radius, outlinePaint)
        }

        var lineY = -halfH + padY - fontMetrics.ascent
        lines.forEach { line ->
            val lineX = when (layer.textAlign) {
                TextAlignment.START -> -halfW + padX
                TextAlignment.CENTER -> 0f
                TextAlignment.END -> halfW - padX
            }
            canvas.drawText(line, lineX, lineY, textPaint)
            lineY += lineHeight
        }

        canvas.restore()
    }
}
