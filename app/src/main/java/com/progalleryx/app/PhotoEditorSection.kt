package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun PhotoEditorSection(
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editSource by remember { mutableStateOf<Uri?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var temperature by remember { mutableFloatStateOf(0f) }
    var tint by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var cropRatio by remember { mutableFloatStateOf(0f) }
    var cropX by remember { mutableFloatStateOf(0.5f) }
    var cropY by remember { mutableFloatStateOf(0.5f) }
    var jpegQuality by remember { mutableIntStateOf(92) }
    var maxDimension by remember { mutableIntStateOf(2400) }
    var caption by remember { mutableStateOf("") }
    var captionDark by remember { mutableStateOf(false) }
    var captionSize by remember { mutableIntStateOf(32) }
    var captionPosition by remember { mutableFloatStateOf(0.9f) }
    var textLayers by remember { mutableStateOf<List<PhotoTextLayer>>(emptyList()) }
    var brushStrokes by remember { mutableStateOf<List<BrushStroke>>(emptyList()) }
    var brushMode by remember { mutableStateOf(false) }
    var brushDark by remember { mutableStateOf(false) }
    var brushWidth by remember { mutableFloatStateOf(0.01f) }
    var selectedPresetId by remember { mutableStateOf(PhotoFilterPresets.ORIGINAL.id) }

    var undoStack by remember { mutableStateOf<List<PhotoAdjustments>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<PhotoAdjustments>>(emptyList()) }
    var adjustingSlider by remember { mutableStateOf(false) }

    var batchSources by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var batchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun currentAdjustments(): PhotoAdjustments = PhotoAdjustments(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        rotation = rotation,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        cropRatio = cropRatio,
        jpegQuality = jpegQuality,
        caption = caption,
        captionDark = captionDark,
        captionSize = captionSize,
        captionPosition = captionPosition,
        maxDimension = maxDimension,
        cropX = cropX,
        cropY = cropY,
        temperature = temperature,
        tint = tint,
        textLayers = textLayers,
        brushStrokes = brushStrokes
    )

    fun applyState(adj: PhotoAdjustments) {
        brightness = adj.brightness
        contrast = adj.contrast
        saturation = adj.saturation
        rotation = adj.rotation
        flipHorizontal = adj.flipHorizontal
        flipVertical = adj.flipVertical
        cropRatio = adj.cropRatio
        jpegQuality = adj.jpegQuality
        caption = adj.caption
        captionDark = adj.captionDark
        captionSize = adj.captionSize
        captionPosition = adj.captionPosition
        maxDimension = adj.maxDimension
        cropX = adj.cropX
        cropY = adj.cropY
        temperature = adj.temperature
        tint = adj.tint
        textLayers = adj.textLayers
        brushStrokes = adj.brushStrokes
    }

    fun snapshot() {
        undoStack = (undoStack + currentAdjustments()).takeLast(30)
        redoStack = emptyList()
    }

    val editedOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = editSource
        if (!busy && target != null && source != null) {
            val adjustments = currentAdjustments()
            onBusyChange(true)
            scope.launch {
                try {
                    PhotoEditor.export(context, source, target, adjustments)
                    onMessageChange("تم تصدير الصورة بنجاح")
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    onMessageChange("فشل التصدير: ${error.localizedMessage}")
                } finally {
                    onBusyChange(false)
                }
            }
        }
    }

    val editInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            editSource = uri
            brightness = 0f; contrast = 1f; saturation = 1f; temperature = 0f; tint = 0f
            rotation = 0; flipHorizontal = false; flipVertical = false
            cropRatio = 0f; cropX = 0.5f; cropY = 0.5f
            jpegQuality = 92; maxDimension = 2400
            caption = ""; captionDark = false; captionSize = 32; captionPosition = 0.9f
            textLayers = emptyList(); brushStrokes = emptyList(); brushMode = false
            selectedPresetId = PhotoFilterPresets.ORIGINAL.id
            undoStack = emptyList(); redoStack = emptyList()
        }
    }

    val batchInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            batchSources = uris.take(PhotoBatchRules.MAX_BATCH_SIZE)
            onMessageChange("تم اختيار ${batchSources.size} صورة للمعالجة الجماعية")
        }
    }

    val singleRotateOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = editSource
        if (!busy && target != null && source != null) {
            onBusyChange(true)
            scope.launch {
                try {
                    OfflineExports.rotateAndSave(context, source, target)
                    onMessageChange("تم حفظ النسخة المدوّرة")
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    onMessageChange("تعذر التدوير: ${error.localizedMessage}")
                } finally {
                    onBusyChange(false)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { editInput.launch("image/*") },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (editSource == null) "اختيار صورة للتحرير" else "تغيير الصورة")
            }

            OutlinedButton(
                onClick = { batchInput.launch("image/*") },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                Text("تعديل دفعة صور (2–25)")
            }
        }

        // Batch processing bar
        if (batchSources.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعالجة الجماعية لـ ${batchSources.size} صور", style = MaterialTheme.typography.titleMedium)
                    Text("سيتم تطبيق الفلتر والتعديلات الحالية على كل الصور وحفظها في التخزين.", style = MaterialTheme.typography.bodySmall)

                    if (batchProgress != null) {
                        LinearProgressIndicator(
                            progress = { batchProgress!!.first.toFloat() / batchProgress!!.second.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("جاري معالجة الصورة ${batchProgress!!.first} من ${batchProgress!!.second}")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onBusyChange(true)
                                scope.launch {
                                    val adj = currentAdjustments()
                                    var exportedCount = 0
                                    try {
                                        for (i in batchSources.indices) {
                                            batchProgress = (i + 1) to batchSources.size
                                            val src = batchSources[i]
                                            val fileName = PhotoBatchRules.generateBatchFileName(i, "batch")
                                            val values = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProGalleryX")
                                                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                                                }
                                            }
                                            val destUri = runCatching {
                                                context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                            }.getOrNull() ?: Uri.fromFile(java.io.File(context.cacheDir, fileName))

                                            PhotoEditor.export(context, src, destUri, adj)

                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && destUri.scheme == "content") {
                                                values.clear()
                                                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                                                context.contentResolver.update(destUri, values, null, null)
                                            }
                                            exportedCount++
                                        }
                                        onMessageChange("اكتمل تصدير الدفعة: تم بنجاح $exportedCount صورة إلى المعرض (Pictures/ProGalleryX)")
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                        onMessageChange("أُلغيت معالجة الدفعة")
                                        throw cancelled
                                    } catch (error: Exception) {
                                        onMessageChange("خطأ أثناء معالجة الدفعة: ${error.localizedMessage}")
                                    } finally {
                                        batchProgress = null
                                        onBusyChange(false)
                                    }
                                }
                            },
                            enabled = !busy && batchProgress == null
                        ) {
                            Text("تطبيق وحفظ الدفعة")
                        }

                        OutlinedButton(
                            onClick = { batchSources = emptyList() },
                            enabled = !busy && batchProgress == null
                        ) {
                            Text("إلغاء الدفعة")
                        }
                    }
                }
            }
        }

        // Active Photo Editing Workspace
        if (editSource != null) {
            val photoMatrix = remember(brightness, contrast, saturation, temperature, tint) {
                android.graphics.ColorMatrix().apply {
                    setSaturation(saturation)
                    postConcat(android.graphics.ColorMatrix(PhotoColorRules.contrastBrightness(contrast, brightness)))
                    postConcat(android.graphics.ColorMatrix(PhotoColorRules.temperatureTint(temperature, tint)))
                }
            }

            // Preview frame with unified coordinate system
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF1E242B)),
                    contentAlignment = Alignment.Center
                ) {
                    val frameAspect = if (cropRatio > 0f) cropRatio else (if (rotation == 90 || rotation == 270) 0.75f else 1.33f)

                    Box(
                        modifier = Modifier
                            .aspectRatio(frameAspect, matchHeightConstraintsFirst = true)
                            .fillMaxHeight(0.92f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = editSource,
                            contentDescription = "معاينة الصورة",
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix(photoMatrix.array.copyOf())),
                            contentScale = if (cropRatio == 0f) ContentScale.Fit else ContentScale.Crop,
                            alignment = if (cropRatio == 0f) Alignment.Center else androidx.compose.ui.BiasAlignment(cropX * 2f - 1f, cropY * 2f - 1f),
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(cropRatio, brushMode) {
                                    if (cropRatio != 0f && !brushMode) {
                                        detectDragGestures(
                                            onDragStart = { snapshot() },
                                            onDrag = { change, drag ->
                                                change.consume()
                                                cropX = (cropX - drag.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                cropY = (cropY - drag.y / size.height.toFloat()).coerceIn(0f, 1f)
                                            }
                                        )
                                    }
                                }
                        )

                        // Brush canvas overlay strictly inside image frame
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(brushMode, brushDark, brushWidth, brushStrokes.size) {
                                    if (brushMode) {
                                        detectDragGestures(
                                            onDragStart = { position ->
                                                if (brushStrokes.size < 100) {
                                                    snapshot()
                                                    val nx = (position.x / size.width).coerceIn(0f, 1f)
                                                    val ny = (position.y / size.height).coerceIn(0f, 1f)
                                                    brushStrokes = brushStrokes + BrushStroke(
                                                        listOf(BrushPoint(nx, ny)),
                                                        brushDark,
                                                        brushWidth
                                                    )
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                if (brushStrokes.isNotEmpty()) {
                                                    val current = brushStrokes.last()
                                                    if (current.points.size < 2000) {
                                                        val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                                        val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                                        brushStrokes = brushStrokes.dropLast(1) + current.copy(
                                                            points = current.points + BrushPoint(nx, ny)
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                        ) {
                            brushStrokes.forEach { stroke ->
                                if (stroke.points.isNotEmpty()) {
                                    val path = Path()
                                    stroke.points.forEachIndexed { idx, point ->
                                        val px = point.x * size.width
                                        val py = point.y * size.height
                                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    }
                                    if (stroke.points.size == 1) {
                                        drawCircle(
                                            color = if (stroke.dark) Color.Black else Color.White,
                                            radius = (size.width * stroke.width) / 2f,
                                            center = Offset(stroke.points[0].x * size.width, stroke.points[0].y * size.height)
                                        )
                                    } else {
                                        drawPath(
                                            path = path,
                                            color = if (stroke.dark) Color.Black else Color.White,
                                            style = Stroke(
                                                width = size.width * stroke.width,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Text layers overlay inside frame
                        textLayers.forEach { layer ->
                            if (layer.text.isNotBlank()) {
                                Text(
                                    layer.text,
                                    color = if (layer.dark) Color.Black else Color.White,
                                    fontSize = (layer.size * 0.45f).sp,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = (layer.x * 240f).dp,
                                            y = (layer.y * 200f).dp
                                        ),
                                    maxLines = 1
                                )
                            }
                        }

                        // Caption overlay
                        if (caption.isNotBlank()) {
                            Text(
                                caption,
                                color = if (captionDark) Color.Black else Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = (captionSize * 0.45f).sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = (captionPosition * 220f).dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Filter Presets Section
            Text("الفلاتر الجاهزة", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PhotoFilterPresets.all.forEach { preset ->
                    FilterChip(
                        selected = selectedPresetId == preset.id,
                        onClick = {
                            snapshot()
                            selectedPresetId = preset.id
                            applyState(PhotoFilterPresets.applyTo(currentAdjustments(), preset))
                        },
                        label = { Text(preset.displayName) },
                        enabled = !busy
                    )
                }
            }

            // Undo / Redo / Reset controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val previous = undoStack.last()
                        redoStack = (redoStack + currentAdjustments()).takeLast(30)
                        undoStack = undoStack.dropLast(1)
                        applyState(previous)
                    },
                    enabled = !busy && undoStack.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تراجع (${undoStack.size})")
                }

                OutlinedButton(
                    onClick = {
                        val next = redoStack.last()
                        redoStack = redoStack.dropLast(1)
                        undoStack = (undoStack + currentAdjustments()).takeLast(30)
                        applyState(next)
                    },
                    enabled = !busy && redoStack.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إعادة (${redoStack.size})")
                }

                TextButton(
                    onClick = {
                        snapshot()
                        applyState(PhotoFilterPresets.applyTo(currentAdjustments(), PhotoFilterPresets.ORIGINAL))
                        rotation = 0; flipHorizontal = false; flipVertical = false
                        cropRatio = 0f; cropX = 0.5f; cropY = 0.5f
                        selectedPresetId = PhotoFilterPresets.ORIGINAL.id
                    },
                    enabled = !busy
                ) {
                    Text("إعادة الضبط")
                }
            }

            // Transform Tools (Rotate & Flip)
            Text("التحويل والتدوير", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { snapshot(); rotation = (rotation + 90) % 360 },
                    enabled = !busy
                ) { Text("تدوير 90° ($rotation°)") }

                OutlinedButton(
                    onClick = { snapshot(); flipHorizontal = !flipHorizontal },
                    enabled = !busy
                ) { Text(if (flipHorizontal) "إلغاء الانعكاس الأفقي" else "انعكاس أفقي") }

                OutlinedButton(
                    onClick = { snapshot(); flipVertical = !flipVertical },
                    enabled = !busy
                ) { Text(if (flipVertical) "إلغاء الانعكاس الرأسي" else "انعكاس رأسي") }
            }

            // Crop Ratios
            Text("نسبة الاقتصاص", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "الأصل" to 0f,
                    "1:1" to 1f,
                    "4:3" to (4f / 3f),
                    "16:9" to (16f / 9f),
                    "3:4" to (3f / 4f),
                    "9:16" to (9f / 16f)
                ).forEach { (label, ratio) ->
                    FilterChip(
                        selected = cropRatio == ratio,
                        onClick = { snapshot(); cropRatio = ratio },
                        label = { Text(label) },
                        enabled = !busy
                    )
                }
            }

            if (cropRatio != 0f) {
                Text("بؤرة الاقتصاص أفقيًا: ${(cropX * 100).toInt()}%")
                Slider(
                    value = cropX,
                    onValueChange = {
                        if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                        cropX = it
                    },
                    onValueChangeFinished = { adjustingSlider = false },
                    enabled = !busy
                )
                Text("بؤرة الاقتصاص رأسيًا: ${(cropY * 100).toInt()}%")
                Slider(
                    value = cropY,
                    onValueChange = {
                        if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                        cropY = it
                    },
                    onValueChangeFinished = { adjustingSlider = false },
                    enabled = !busy
                )
            }

            HorizontalDivider()

            // Freehand Brush Mode
            Text("الرسم بالفرشاة", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = brushMode, onCheckedChange = { brushMode = it }, enabled = !busy)
                Text("تفعيل الرسم المباشر فوق الصورة")
            }
            if (brushMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = brushDark, onCheckedChange = { brushDark = it }, enabled = !busy)
                    Text("فرشاة داكنة (بدل الفاتحة)")
                }
                Text("سُمك الفرشاة: ${(brushWidth * 1000).toInt()}")
                Slider(
                    value = brushWidth,
                    onValueChange = { brushWidth = it },
                    valueRange = 0.003f..0.04f,
                    enabled = !busy
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { snapshot(); brushStrokes = emptyList() },
                        enabled = !busy && brushStrokes.isNotEmpty()
                    ) {
                        Text("مسح كل الرسومات (${brushStrokes.size})")
                    }
                }
            }

            HorizontalDivider()

            // Text Layers
            Text("طبقات النصوص (حتى 3)", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    snapshot()
                    textLayers = textLayers + PhotoTextLayer("", 0.5f, 0.3f + 0.2f * textLayers.size)
                },
                enabled = !busy && textLayers.size < 3
            ) {
                Text("إضافة طبقة نص جديدة")
            }

            textLayers.forEachIndexed { index, layer ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("طبقة نص ${index + 1}", style = MaterialTheme.typography.titleSmall)
                            TextButton(
                                onClick = {
                                    snapshot()
                                    textLayers = textLayers.filterIndexed { i, _ -> i != index }
                                },
                                enabled = !busy
                            ) { Text("حذف") }
                        }
                        OutlinedTextField(
                            value = layer.text,
                            onValueChange = { value ->
                                snapshot()
                                textLayers = textLayers.toMutableList().also { it[index] = layer.copy(text = value.take(80)) }
                            },
                            label = { Text("النص") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = layer.dark,
                                onCheckedChange = { v ->
                                    snapshot()
                                    textLayers = textLayers.toMutableList().also { it[index] = layer.copy(dark = v) }
                                },
                                enabled = !busy
                            )
                            Text("لون داكن")
                        }
                    }
                }
            }

            // Sliders for Light, Color & Tone
            Text("تعديلات الإضاءة والألوان", style = MaterialTheme.typography.titleMedium)
            Text("السطوع: ${brightness.toInt()}")
            Slider(
                value = brightness,
                onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; brightness = it },
                onValueChangeFinished = { adjustingSlider = false },
                valueRange = -100f..100f
            )

            Text("التباين: ${(contrast * 100).toInt()}%")
            Slider(
                value = contrast,
                onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; contrast = it },
                onValueChangeFinished = { adjustingSlider = false },
                valueRange = 0.5f..1.5f
            )

            Text("التشبع: ${(saturation * 100).toInt()}%")
            Slider(
                value = saturation,
                onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; saturation = it },
                onValueChangeFinished = { adjustingSlider = false },
                valueRange = 0f..2f
            )

            Text("حرارة اللون: ${temperature.toInt()}")
            Slider(
                value = temperature,
                onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; temperature = it },
                onValueChangeFinished = { adjustingSlider = false },
                valueRange = -100f..100f
            )

            Text("تدرج الصبغة: ${tint.toInt()}")
            Slider(
                value = tint,
                onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; tint = it },
                onValueChangeFinished = { adjustingSlider = false },
                valueRange = -100f..100f
            )

            // Export Resolution & Quality
            Text("إعدادات تصدير الصورة", style = MaterialTheme.typography.titleMedium)
            Text("أقصى دقة (أطول ضلع بالبكسل)")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1200 to "1200 (خفيف)", 2400 to "2400 (قياسي)", 3600 to "3600 (فائق)").forEach { (dim, label) ->
                    FilterChip(
                        selected = maxDimension == dim,
                        onClick = { snapshot(); maxDimension = dim },
                        label = { Text(label) },
                        enabled = !busy
                    )
                }
            }

            Text("جودة JPEG: $jpegQuality%")
            Slider(
                value = jpegQuality.toFloat(),
                onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; jpegQuality = it.toInt() },
                onValueChangeFinished = { adjustingSlider = false },
                valueRange = 70f..100f,
                enabled = !busy
            )

            Button(
                onClick = { editedOutput.launch("ProGalleryX-edited.jpg") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تصدير وحفظ الصورة المعدلة")
            }
        }
    }
}
