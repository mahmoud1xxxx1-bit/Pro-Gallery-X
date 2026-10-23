package com.progalleryx.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface

object VideoOverlayRenderer {

    fun renderOverlaysToBitmap(
        overlays: List<VideoTimedOverlay>,
        timeMs: Long,
        width: Int,
        height: Int
    ): Bitmap? {
        val activeOverlays = VideoTimelineEngine.activeOverlaysAt(overlays, timeMs)
        if (activeOverlays.isEmpty() || width <= 0 || height <= 0) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        for (overlay in activeOverlays) {
            val cx = overlay.normalizedX * width
            val cy = overlay.normalizedY * height

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(overlay.rotationDegrees)
            canvas.scale(overlay.scale, overlay.scale)

            when (overlay.type) {
                VideoOverlayType.TEXT -> {
                    if (overlay.text.isNotEmpty()) {
                        val baseTypeface = when (overlay.fontIndex) {
                            1 -> Typeface.SERIF
                            2 -> Typeface.MONOSPACE
                            3 -> Typeface.SANS_SERIF
                            else -> Typeface.DEFAULT
                        }
                        val style = when {
                            overlay.isBold && overlay.isItalic -> Typeface.BOLD_ITALIC
                            overlay.isBold -> Typeface.BOLD
                            overlay.isItalic -> Typeface.ITALIC
                            else -> Typeface.NORMAL
                        }
                        val finalTypeface = Typeface.create(baseTypeface, style)

                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = overlay.textColorArgb.toInt()
                            textSize = width * 0.055f
                            typeface = finalTypeface
                            textAlign = Paint.Align.CENTER
                        }

                        val textBounds = Rect()
                        textPaint.getTextBounds(overlay.text, 0, overlay.text.length, textBounds)
                        val textWidth = textPaint.measureText(overlay.text)
                        val textHeight = textBounds.height().toFloat()

                        if (overlay.backgroundColorArgb != 0L) {
                            val padX = width * 0.025f
                            val padY = height * 0.015f
                            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = overlay.backgroundColorArgb.toInt()
                                this.style = Paint.Style.FILL
                            }
                            val bgRect = RectF(
                                -textWidth / 2f - padX,
                                -textHeight / 2f - padY,
                                textWidth / 2f + padX,
                                textHeight / 2f + padY
                            )
                            val cornerRadius = width * 0.02f
                            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)
                        }

                        val yOffset = textHeight / 2f - textBounds.bottom
                        canvas.drawText(overlay.text, 0f, yOffset, textPaint)
                    }
                }
                VideoOverlayType.STICKER -> {
                    if (overlay.stickerEmoji.isNotEmpty()) {
                        val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = width * 0.12f
                            textAlign = Paint.Align.CENTER
                        }
                        val bounds = Rect()
                        stickerPaint.getTextBounds(overlay.stickerEmoji, 0, overlay.stickerEmoji.length, bounds)
                        val yOffset = bounds.height() / 2f - bounds.bottom
                        canvas.drawText(overlay.stickerEmoji, 0f, yOffset, stickerPaint)
                    }
                }
            }

            canvas.restore()
        }

        return bitmap
    }
}
