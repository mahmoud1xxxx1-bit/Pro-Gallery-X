package com.progalleryx.app.studioplus.feature.photoeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.progalleryx.app.studioplus.feature.photoeditor.engine.StudioBitmapProcessor
import com.progalleryx.app.studioplus.feature.photoeditor.model.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun EditorCanvasView(
    previewBitmap: Bitmap?,
    state: StudioPhotoEditState,
    currentDoodleTool: DoodleToolMode,
    currentDoodleColor: Int,
    currentDoodleThickness: Float,
    onAddDoodlePath: (StudioDoodlePath) -> Unit,
    onUpdateTextPosition: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (previewBitmap == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    // Active in-progress doodle stroke
    var activeStrokePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    // Recomputed transformed bitmap whenever tone/filter/transform changes
    val processedBitmap by remember(previewBitmap, state.brightness, state.contrast, state.saturation, state.warmth, state.vignette, state.filterType, state.filterIntensity, state.rotationSteps90, state.fineRotationAngle, state.flipHorizontal, state.flipVertical, state.cropRect) {
        derivedStateOf {
            StudioBitmapProcessor.processBitmap(
                source = previewBitmap,
                state = state.copy(doodlePaths = emptyList(), textLayers = emptyList())
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.activeTab, currentDoodleTool, state.selectedTextLayerId) {
                when (state.activeTab) {
                    StudioEditorTab.DOODLE -> {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                activeStrokePoints = listOf(normX to normY)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                                val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                                activeStrokePoints = activeStrokePoints + (normX to normY)
                            },
                            onDragEnd = {
                                if (activeStrokePoints.size >= 2) {
                                    val newDoodle = StudioDoodlePath(
                                        points = activeStrokePoints,
                                        color = currentDoodleColor,
                                        strokeWidthRelative = currentDoodleThickness,
                                        isHighlighter = currentDoodleTool == DoodleToolMode.HIGHLIGHTER,
                                        isEraser = currentDoodleTool == DoodleToolMode.ERASER
                                    )
                                    onAddDoodlePath(newDoodle)
                                }
                                activeStrokePoints = emptyList()
                            },
                            onDragCancel = {
                                activeStrokePoints = emptyList()
                            }
                        )
                    }
                    StudioEditorTab.TEXT -> {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val selectedId = state.selectedTextLayerId
                                ?: state.textLayers.lastOrNull()?.id
                            if (selectedId != null) {
                                val dx = dragAmount.x / size.width
                                val dy = dragAmount.y / size.height
                                onUpdateTextPosition(selectedId, dx, dy)
                            }
                        }
                    }
                    else -> {}
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate aspect fit coordinates for the processed bitmap
        val bmpW = processedBitmap.width.toFloat()
        val bmpH = processedBitmap.height.toFloat()
        val scale = min(canvasWidth / bmpW, canvasHeight / bmpH)
        val dstW = bmpW * scale
        val dstH = bmpH * scale
        val dstX = (canvasWidth - dstW) / 2f
        val dstY = (canvasHeight - dstH) / 2f

        // 1. Draw base processed bitmap
        drawImage(
            image = processedBitmap.asImageBitmap(),
            dstOffset = androidx.compose.ui.unit.IntOffset(dstX.toInt(), dstY.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(dstW.toInt(), dstH.toInt())
        )

        // 2. Draw existing and in-progress doodles
        drawIntoCanvas { nativeCanvas ->
            val allPaths = if (activeStrokePoints.size >= 2) {
                state.doodlePaths + StudioDoodlePath(
                    points = activeStrokePoints,
                    color = currentDoodleColor,
                    strokeWidthRelative = currentDoodleThickness,
                    isHighlighter = currentDoodleTool == DoodleToolMode.HIGHLIGHTER,
                    isEraser = currentDoodleTool == DoodleToolMode.ERASER
                )
            } else {
                state.doodlePaths
            }

            // Draw doodles within dst bounds
            if (allPaths.isNotEmpty()) {
                nativeCanvas.save()
                nativeCanvas.translate(dstX, dstY)
                val androidCanvas = nativeCanvas.nativeCanvas
                for (doodle in allPaths) {
                    if (doodle.points.size < 2) continue
                    val strokeWidthPx = max(4f, doodle.strokeWidthRelative * dstW)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        strokeWidth = strokeWidthPx
                        if (doodle.isEraser) {
                            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                        } else if (doodle.isHighlighter) {
                            color = doodle.color
                            alpha = 100
                        } else {
                            color = doodle.color
                        }
                    }

                    val path = android.graphics.Path()
                    val p0 = doodle.points[0]
                    path.moveTo(p0.first * dstW, p0.second * dstH)
                    for (i in 1 until doodle.points.size) {
                        val prev = doodle.points[i - 1]
                        val curr = doodle.points[i]
                        val midX = (prev.first + curr.first) * dstW / 2f
                        val midY = (prev.second + curr.second) * dstH / 2f
                        path.quadTo(prev.first * dstW, prev.second * dstH, midX, midY)
                    }
                    path.lineTo(doodle.points.last().first * dstW, doodle.points.last().second * dstH)
                    androidCanvas.drawPath(path, paint)
                }
                nativeCanvas.restore()
            }

            // 3. Draw Arabic/multi-language text layers
            if (state.textLayers.isNotEmpty()) {
                nativeCanvas.save()
                nativeCanvas.translate(dstX, dstY)
                val androidCanvas = nativeCanvas.nativeCanvas
                for (layer in state.textLayers) {
                    if (layer.text.isBlank()) continue
                    val baseSizePx = (layer.fontSizeSp / 360f) * dstW * layer.scale
                    val textPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.textColor
                        textSize = baseSizePx
                        typeface = layer.style.getTypeface()
                    }
                    val maxTextW = (dstW * 0.85f).toInt().coerceAtLeast(100)
                    val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.text.StaticLayout.Builder.obtain(layer.text, 0, layer.text.length, textPaint, maxTextW)
                            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                            .setIncludePad(true)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        android.text.StaticLayout(
                            layer.text, textPaint, maxTextW,
                            android.text.Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true
                        )
                    }

                    val lWidth = staticLayout.width.toFloat()
                    val lHeight = staticLayout.height.toFloat()
                    val posX = layer.normalizedX * dstW
                    val posY = layer.normalizedY * dstH

                    androidCanvas.save()
                    androidCanvas.translate(posX, posY)
                    if (layer.rotationDegrees != 0f) {
                        androidCanvas.rotate(layer.rotationDegrees)
                    }
                    val left = -lWidth / 2f
                    val top = -lHeight / 2f

                    // Draw background
                    if (layer.backgroundMode != StudioTextBackgroundMode.NONE) {
                        val padH = baseSizePx * 0.45f
                        val padV = baseSizePx * 0.25f
                        val bgRect = android.graphics.RectF(left - padH, top - padV, left + lWidth + padH, top + lHeight + padV)
                        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            when (layer.backgroundMode) {
                                StudioTextBackgroundMode.SOLID -> {
                                    style = android.graphics.Paint.Style.FILL
                                    color = layer.backgroundColor
                                }
                                StudioTextBackgroundMode.TRANSLUCENT -> {
                                    style = android.graphics.Paint.Style.FILL
                                    color = layer.backgroundColor
                                    alpha = 180
                                }
                                StudioTextBackgroundMode.OUTLINE -> {
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = max(3f, baseSizePx * 0.08f)
                                    color = layer.textColor
                                }
                                else -> {}
                            }
                        }
                        val corner = baseSizePx * 0.4f
                        androidCanvas.drawRoundRect(bgRect, corner, corner, bgPaint)
                    }

                    androidCanvas.translate(left, top)
                    staticLayout.draw(androidCanvas)
                    androidCanvas.restore()
                }
                nativeCanvas.restore()
            }
        }
    }
}
