package com.progalleryx.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun InteractiveLayerCanvas(
    modifier: Modifier = Modifier,
    textLayers: List<InteractiveTextLayer>,
    stickerLayers: List<InteractiveStickerLayer>,
    brushStrokes: List<SmoothBrushStroke>,
    activeLayerId: String?,
    brushMode: Boolean,
    brushColorArgb: Long,
    brushWidthNormalized: Float,
    onSelectLayer: (String?) -> Unit,
    onUpdateTextLayer: (InteractiveTextLayer) -> Unit,
    onUpdateStickerLayer: (InteractiveStickerLayer) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onEditTextLayer: (InteractiveTextLayer) -> Unit,
    onAddBrushStroke: (SmoothBrushStroke) -> Unit,
    onAppendStrokePoint: (NormalizedPoint) -> Unit,
    onBringForward: (String) -> Unit = {},
    onSendBackward: (String) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(brushMode, textLayers.size, stickerLayers.size) {
                if (!brushMode) {
                    detectTapGestures(
                        onTap = {
                            // Tapping blank canvas deselects active layer
                            onSelectLayer(null)
                        }
                    )
                }
            }
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // 1. Draw Brush Strokes Layer with Smooth Bézier Curves
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(brushMode, brushColorArgb, brushWidthNormalized, brushStrokes.size) {
                    if (brushMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val nx = (offset.x / size.width).coerceIn(0f, 1f)
                                val ny = (offset.y / size.height).coerceIn(0f, 1f)
                                val stroke = SmoothBrushStroke(
                                    points = listOf(NormalizedPoint(nx, ny)),
                                    colorArgb = brushColorArgb,
                                    strokeWidthNormalized = brushWidthNormalized
                                )
                                onAddBrushStroke(stroke)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                onAppendStrokePoint(NormalizedPoint(nx, ny))
                            }
                        )
                    }
                }
        ) {
            brushStrokes.forEach { stroke ->
                if (stroke.points.isNotEmpty()) {
                    val strokeColor = Color(stroke.colorArgb)
                    val strokeWidthPx = (stroke.strokeWidthNormalized * size.width).coerceAtLeast(2f)

                    if (stroke.points.size == 1) {
                        val pt = stroke.points[0]
                        drawCircle(
                            color = strokeColor,
                            radius = strokeWidthPx / 2f,
                            center = Offset(pt.x * size.width, pt.y * size.height)
                        )
                    } else {
                        val path = Path()
                        val pts = stroke.points
                        path.moveTo(pts[0].x * size.width, pts[0].y * size.height)

                        for (i in 1 until pts.size) {
                            val prev = pts[i - 1]
                            val curr = pts[i]
                            val midX = (prev.x + curr.x) * size.width / 2f
                            val midY = (prev.y + curr.y) * size.height / 2f
                            path.quadraticTo(prev.x * size.width, prev.y * size.height, midX, midY)
                        }
                        // Connect to last point
                        path.lineTo(pts.last().x * size.width, pts.last().y * size.height)

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        // Active layer coordinates for visual guidelines
        val activeText = textLayers.find { it.id == activeLayerId }
        val activeSticker = stickerLayers.find { it.id == activeLayerId }
        val activeX = activeText?.normalizedX ?: activeSticker?.normalizedX
        val activeY = activeText?.normalizedY ?: activeSticker?.normalizedY
        val showCenterXGuide = activeX != null && abs(activeX - 0.5f) < 0.015f
        val showCenterYGuide = activeY != null && abs(activeY - 0.5f) < 0.015f

        // Center Guidelines Overlay
        if (!brushMode && (showCenterXGuide || showCenterYGuide)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dashed = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                val guideColor = Color(0xFF007AFF).copy(alpha = 0.85f)

                if (showCenterXGuide) {
                    val cx = size.width * 0.5f
                    drawLine(
                        color = guideColor,
                        start = Offset(cx, 0f),
                        end = Offset(cx, size.height),
                        strokeWidth = 2f,
                        pathEffect = dashed
                    )
                }
                if (showCenterYGuide) {
                    val cy = size.height * 0.5f
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, cy),
                        end = Offset(size.width, cy),
                        strokeWidth = 2f,
                        pathEffect = dashed
                    )
                }
            }
        }

        // 2. Render Sticker Layers
        stickerLayers.forEach { sticker ->
            val isSelected = activeLayerId == sticker.id

            Box(
                modifier = Modifier
                    .offset {
                        val stickerSizePx = (120.dp.toPx()).roundToInt()
                        val (ox, oy) = LayerTransformRules.calculateCenterOffset(
                            normalizedX = sticker.normalizedX,
                            normalizedY = sticker.normalizedY,
                            containerWidthPx = containerWidthPx,
                            containerHeightPx = containerHeightPx,
                            layerWidthPx = stickerSizePx,
                            layerHeightPx = stickerSizePx
                        )
                        IntOffset(ox, oy)
                    }
                    .graphicsLayer {
                        scaleX = sticker.scale
                        scaleY = sticker.scale
                        rotationZ = sticker.rotationDegrees
                        transformOrigin = TransformOrigin.Center
                    }
                    .size(120.dp)
                    .pointerInput(sticker.id, isSelected, brushMode, sticker.isLocked) {
                        if (!brushMode && !sticker.isLocked) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                var (newX, newY) = LayerTransformRules.applyDragDelta(
                                    currentX = sticker.normalizedX,
                                    currentY = sticker.normalizedY,
                                    dragPxX = pan.x,
                                    dragPxY = pan.y,
                                    containerWidthPx = containerWidthPx,
                                    containerHeightPx = containerHeightPx
                                )
                                // Snap position to center
                                if (abs(newX - 0.5f) < 0.012f) newX = 0.5f
                                if (abs(newY - 0.5f) < 0.012f) newY = 0.5f

                                var (newScale, newRot) = LayerTransformRules.applyScaleAndRotation(
                                    currentScale = sticker.scale,
                                    currentRotationDegrees = sticker.rotationDegrees,
                                    zoomDelta = zoom,
                                    rotationDeltaDegrees = rotation
                                )
                                // Snap rotation to cardinal angles
                                for (angle in listOf(0f, 90f, 180f, 270f, 360f)) {
                                    if (abs(newRot - angle) < 3.5f) {
                                        newRot = if (angle == 360f) 0f else angle
                                        break
                                    }
                                }

                                onSelectLayer(sticker.id)
                                onUpdateStickerLayer(
                                    sticker.copy(
                                        normalizedX = newX,
                                        normalizedY = newY,
                                        scale = newScale,
                                        rotationDegrees = newRot
                                    )
                                )
                            }
                        }
                    }
                    .clickable(enabled = !brushMode) { onSelectLayer(sticker.id) }
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                1.5.dp,
                                if (sticker.isLocked) Color(0xFFFF9500) else MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Sticker content: Custom Image / Emoji / Shape
                if (!sticker.customImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = sticker.customImageUri,
                        contentDescription = "ملصق خاص",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else if (sticker.emoji.isNotBlank()) {
                    Text(
                        text = sticker.emoji,
                        fontSize = 54.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (sticker.shapeType != null) {
                    val shapeColor = Color(sticker.colorArgb).copy(alpha = sticker.opacity)
                    Canvas(modifier = Modifier.size(76.dp)) {
                        when (sticker.shapeType) {
                            ShapeType.CIRCLE -> {
                                drawCircle(color = shapeColor)
                            }
                            ShapeType.RECTANGLE -> {
                                drawRect(color = shapeColor)
                            }
                            ShapeType.HEART -> {
                                val path = Path().apply {
                                    val w = size.width
                                    val h = size.height
                                    moveTo(w / 2f, h * 0.85f)
                                    cubicTo(w * 0.1f, h * 0.55f, 0f, h * 0.25f, w * 0.25f, h * 0.1f)
                                    cubicTo(w * 0.45f, 0f, w / 2f, h * 0.2f, w / 2f, h * 0.25f)
                                    cubicTo(w / 2f, h * 0.2f, w * 0.55f, 0f, w * 0.75f, h * 0.1f)
                                    cubicTo(w, h * 0.25f, w * 0.9f, h * 0.55f, w / 2f, h * 0.85f)
                                    close()
                                }
                                drawPath(path, color = shapeColor)
                            }
                            ShapeType.STAR -> {
                                val path = Path()
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val outerR = size.width / 2f
                                val innerR = outerR * 0.4f
                                for (i in 0 until 10) {
                                    val r = if (i % 2 == 0) outerR else innerR
                                    val angle = Math.toRadians((i * 36 - 90).toDouble())
                                    val x = cx + (r * cos(angle)).toFloat()
                                    val y = cy + (r * sin(angle)).toFloat()
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                path.close()
                                drawPath(path, color = shapeColor)
                            }
                            ShapeType.ARROW -> {
                                val path = Path().apply {
                                    val w = size.width
                                    val h = size.height
                                    moveTo(w * 0.2f, h * 0.4f)
                                    lineTo(w * 0.6f, h * 0.4f)
                                    lineTo(w * 0.6f, h * 0.2f)
                                    lineTo(w * 0.9f, h * 0.5f)
                                    lineTo(w * 0.6f, h * 0.8f)
                                    lineTo(w * 0.6f, h * 0.6f)
                                    lineTo(w * 0.2f, h * 0.6f)
                                    close()
                                }
                                drawPath(path, color = shapeColor)
                            }
                            ShapeType.HEXAGON -> {
                                val path = Path()
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val r = size.width / 2f
                                for (i in 0 until 6) {
                                    val angle = Math.toRadians((i * 60 - 30).toDouble())
                                    val x = cx + (r * cos(angle)).toFloat()
                                    val y = cy + (r * sin(angle)).toFloat()
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                path.close()
                                drawPath(path, color = shapeColor)
                            }
                            ShapeType.DIAMOND -> {
                                val path = Path().apply {
                                    val w = size.width
                                    val h = size.height
                                    moveTo(w / 2f, 0f)
                                    lineTo(w, h / 2f)
                                    lineTo(w / 2f, h)
                                    lineTo(0f, h / 2f)
                                    close()
                                }
                                drawPath(path, color = shapeColor)
                            }
                            ShapeType.SPEECH_BUBBLE -> {
                                val path = Path().apply {
                                    val w = size.width
                                    val h = size.height * 0.8f
                                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, 16f, 16f))
                                    moveTo(w * 0.25f, h)
                                    lineTo(w * 0.15f, size.height)
                                    lineTo(w * 0.4f, h)
                                    close()
                                }
                                drawPath(path, color = shapeColor)
                            }
                        }
                    }
                }

                // If selected, show action handles
                if (isSelected) {
                    // Lock/Unlock handle (Top-Center)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-14).dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (sticker.isLocked) Color(0xFFFF9500) else Color(0xFF64D2FF))
                            .clickable { onUpdateStickerLayer(sticker.copy(isLocked = !sticker.isLocked)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (sticker.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (sticker.isLocked) "إلغاء القفل" else "قفل",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Bring forward handle (Top-Start)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-10).dp, y = (-10).dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5856D6))
                            .clickable { onBringForward(sticker.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "للأمام",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete handle (Top-Right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-10).dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .clickable { onDeleteLayer(sticker.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "حذف",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Duplicate handle (Bottom-Start)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-10).dp, y = 10.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                            .clickable { onDuplicateLayer(sticker.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Rotate/Scale indicator handle (Bottom-Right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 10.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.OpenWith,
                            contentDescription = "تحريك وتحجيم",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Render Text Layers
        textLayers.forEach { layer ->
            val isSelected = activeLayerId == layer.id

            val resolvedFontFamily = when (layer.fontStyle) {
                TextFontStyle.DEFAULT -> FontFamily.SansSerif
                TextFontStyle.SERIF -> FontFamily.Serif
                TextFontStyle.MONOSPACE -> FontFamily.Monospace
                TextFontStyle.BOLD_HEADING -> FontFamily.SansSerif
                TextFontStyle.CASUAL_HANDWRITING -> FontFamily.Cursive
            }
            val resolvedFontWeight = when (layer.fontStyle) {
                TextFontStyle.BOLD_HEADING -> FontWeight.Bold
                else -> FontWeight.Normal
            }

            val textColor = Color(layer.colorArgb).copy(alpha = layer.opacity)
            val bgColor = when (layer.backgroundStyle) {
                TextBackgroundStyle.TRANSPARENT -> Color.Transparent
                TextBackgroundStyle.FILLED_BOX -> Color(layer.backgroundColorArgb).copy(alpha = layer.opacity)
                TextBackgroundStyle.PILL -> Color(layer.backgroundColorArgb).copy(alpha = layer.opacity)
                TextBackgroundStyle.OUTLINE -> Color.Transparent
                TextBackgroundStyle.SEMI_TRANSPARENT -> Color.Black.copy(alpha = 0.5f * layer.opacity)
            }
            val textShape = if (layer.backgroundStyle == TextBackgroundStyle.PILL) RoundedCornerShape(percent = 50) else RoundedCornerShape(8.dp)
            var layerSize by remember(layer.id, layer.text, layer.fontSizeSp) { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .onSizeChanged { size ->
                        if (size.width > 0 && size.height > 0) {
                            layerSize = size
                        }
                    }
                    .offset {
                        val w = if (layerSize.width > 0) layerSize.width else (containerWidthPx * 0.35f).roundToInt()
                        val h = if (layerSize.height > 0) layerSize.height else (containerHeightPx * 0.08f).roundToInt()
                        val (ox, oy) = LayerTransformRules.calculateCenterOffset(
                            normalizedX = layer.normalizedX,
                            normalizedY = layer.normalizedY,
                            containerWidthPx = containerWidthPx,
                            containerHeightPx = containerHeightPx,
                            layerWidthPx = w,
                            layerHeightPx = h
                        )
                        IntOffset(ox, oy)
                    }
                    .graphicsLayer {
                        scaleX = layer.scale
                        scaleY = layer.scale
                        rotationZ = layer.rotationDegrees
                        transformOrigin = TransformOrigin.Center
                    }
                    .widthIn(min = 120.dp, max = 280.dp)
                    .pointerInput(layer.id, isSelected, brushMode, layer.isLocked) {
                        if (!brushMode && !layer.isLocked) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                var (newX, newY) = LayerTransformRules.applyDragDelta(
                                    currentX = layer.normalizedX,
                                    currentY = layer.normalizedY,
                                    dragPxX = pan.x,
                                    dragPxY = pan.y,
                                    containerWidthPx = containerWidthPx,
                                    containerHeightPx = containerHeightPx
                                )
                                // Snap position to center
                                if (abs(newX - 0.5f) < 0.012f) newX = 0.5f
                                if (abs(newY - 0.5f) < 0.012f) newY = 0.5f

                                var (newScale, newRot) = LayerTransformRules.applyScaleAndRotation(
                                    currentScale = layer.scale,
                                    currentRotationDegrees = layer.rotationDegrees,
                                    zoomDelta = zoom,
                                    rotationDeltaDegrees = rotation
                                )
                                // Snap rotation to cardinal angles
                                for (angle in listOf(0f, 90f, 180f, 270f, 360f)) {
                                    if (abs(newRot - angle) < 3.5f) {
                                        newRot = if (angle == 360f) 0f else angle
                                        break
                                    }
                                }

                                onSelectLayer(layer.id)
                                onUpdateTextLayer(
                                    layer.copy(
                                        normalizedX = newX,
                                        normalizedY = newY,
                                        scale = newScale,
                                        rotationDegrees = newRot
                                    )
                                )
                            }
                        }
                    }
                    .pointerInput(layer.id, isSelected, brushMode) {
                        if (!brushMode) {
                            detectTapGestures(
                                onTap = { onSelectLayer(layer.id) },
                                onDoubleTap = { onEditTextLayer(layer) }
                            )
                        }
                    }
                    .clip(textShape)
                    .background(bgColor)
                    .then(
                        if (layer.backgroundStyle == TextBackgroundStyle.OUTLINE || layer.hasOutline) {
                            Modifier.border(2.dp, textColor, textShape)
                        } else if (isSelected) {
                            Modifier.border(
                                1.5.dp,
                                if (layer.isLocked) Color(0xFFFF9500) else MaterialTheme.colorScheme.primary,
                                textShape
                            )
                        } else Modifier
                    )
                    .padding(
                        horizontal = if (layer.backgroundStyle == TextBackgroundStyle.PILL) 18.dp else 12.dp,
                        vertical = 6.dp
                    )
            ) {
                Text(
                    text = layer.text,
                    color = textColor,
                    fontSize = (layer.fontSizeSp * 0.7f).sp,
                    fontFamily = resolvedFontFamily,
                    fontWeight = resolvedFontWeight,
                    textAlign = when (layer.textAlign) {
                        TextAlignment.START -> TextAlign.Start
                        TextAlignment.CENTER -> TextAlign.Center
                        TextAlignment.END -> TextAlign.End
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // If selected, show action handles around the text box
                if (isSelected) {
                    // Bring forward handle (Top-Center)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-10).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5856D6))
                            .clickable { onBringForward(layer.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "للأمام",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Edit handle (Top-Start)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-8).dp, y = (-8).dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .clickable { onEditTextLayer(layer) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete handle (Top-End)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .clickable { onDeleteLayer(layer.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "حذف",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Lock/Unlock handle (Bottom-Center)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (layer.isLocked) Color(0xFFFF9500) else Color(0xFF64D2FF))
                            .clickable { onUpdateTextLayer(layer.copy(isLocked = !layer.isLocked)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (layer.isLocked) "إلغاء القفل" else "قفل",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Duplicate handle (Bottom-Start)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-8).dp, y = 8.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                            .clickable { onDuplicateLayer(layer.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Rotate/Scale handle (Bottom-End)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.OpenWith,
                            contentDescription = "تحريك وتحجيم",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
