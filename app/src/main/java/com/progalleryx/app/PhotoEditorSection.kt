package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

enum class StudioToolTab {
    TUNE,
    FILTERS,
    TEXT,
    STICKERS,
    DRAW,
    TRANSFORM,
    EXPORT
}

@Composable
fun PhotoEditorSection(
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    initialImageUri: Uri? = null,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editSource by remember(initialImageUri) { mutableStateOf<Uri?>(initialImageUri) }
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
    var selectedPresetId by remember { mutableStateOf(PhotoFilterPresets.ORIGINAL.id) }

    // Interactive Layers State
    var textLayers by remember { mutableStateOf<List<InteractiveTextLayer>>(emptyList()) }
    var stickerLayers by remember { mutableStateOf<List<InteractiveStickerLayer>>(emptyList()) }
    var brushStrokes by remember { mutableStateOf<List<SmoothBrushStroke>>(emptyList()) }
    var activeLayerId by remember { mutableStateOf<String?>(null) }

    // Brush Tooling State
    var brushMode by remember { mutableStateOf(false) }
    var brushColorArgb by remember { mutableLongStateOf(0xFFFF3B30L) }
    var brushWidthNormalized by remember { mutableFloatStateOf(0.015f) }

    // Dialog & Tool Mode State
    var activeToolTab by remember { mutableStateOf(StudioToolTab.TUNE) }
    var showTextDialog by remember { mutableStateOf(false) }
    var editingTextLayer by remember { mutableStateOf<InteractiveTextLayer?>(null) }
    var showStickerDialog by remember { mutableStateOf(false) }

    // Undo / Redo stacks
    var undoStack by remember { mutableStateOf<List<PhotoStudioProject>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<PhotoStudioProject>>(emptyList()) }
    var adjustingSlider by remember { mutableStateOf(false) }

    // Batch Processing State
    var batchSources by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var batchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun currentProject(): PhotoStudioProject = PhotoStudioProject(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        temperature = temperature,
        tint = tint,
        rotation = rotation,
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        cropRatio = cropRatio,
        cropX = cropX,
        cropY = cropY,
        filterId = selectedPresetId,
        textLayers = textLayers,
        stickerLayers = stickerLayers,
        brushStrokes = brushStrokes,
        activeLayerId = activeLayerId
    )

    fun applyProject(proj: PhotoStudioProject) {
        brightness = proj.brightness
        contrast = proj.contrast
        saturation = proj.saturation
        temperature = proj.temperature
        tint = proj.tint
        rotation = proj.rotation
        flipHorizontal = proj.flipHorizontal
        flipVertical = proj.flipVertical
        cropRatio = proj.cropRatio
        cropX = proj.cropX
        cropY = proj.cropY
        selectedPresetId = proj.filterId
        textLayers = proj.textLayers
        stickerLayers = proj.stickerLayers
        brushStrokes = proj.brushStrokes
        activeLayerId = proj.activeLayerId
    }

    fun snapshot() {
        undoStack = (undoStack + currentProject()).takeLast(30)
        redoStack = emptyList()
    }

    val editedOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = editSource
        if (!busy && target != null && source != null) {
            val project = currentProject()
            val adjustments = project.toLegacyAdjustments().copy(
                jpegQuality = jpegQuality,
                maxDimension = maxDimension
            )
            onBusyChange(true)
            scope.launch {
                try {
                    PhotoEditor.export(context, source, target, adjustments)
                    onMessageChange("تم تصدير وحفظ الصورة بنجاح")
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
            textLayers = emptyList(); stickerLayers = emptyList(); brushStrokes = emptyList()
            activeLayerId = null; brushMode = false
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Studio Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                }
            }
            Text(
                "استوديو تحرير الصور",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Undo
                IconButton(
                    onClick = {
                        val previous = undoStack.last()
                        redoStack = (redoStack + currentProject()).takeLast(30)
                        undoStack = undoStack.dropLast(1)
                        applyProject(previous)
                    },
                    enabled = !busy && undoStack.isNotEmpty()
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "تراجع")
                }
                // Redo
                IconButton(
                    onClick = {
                        val next = redoStack.last()
                        redoStack = redoStack.dropLast(1)
                        undoStack = (undoStack + currentProject()).takeLast(30)
                        applyProject(next)
                    },
                    enabled = !busy && redoStack.isNotEmpty()
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "إعادة")
                }
                // Reset
                IconButton(
                    onClick = {
                        snapshot()
                        brightness = 0f; contrast = 1f; saturation = 1f; temperature = 0f; tint = 0f
                        rotation = 0; flipHorizontal = false; flipVertical = false
                        cropRatio = 0f; cropX = 0.5f; cropY = 0.5f
                        selectedPresetId = PhotoFilterPresets.ORIGINAL.id
                        textLayers = emptyList(); stickerLayers = emptyList(); brushStrokes = emptyList()
                        activeLayerId = null
                    },
                    enabled = !busy && editSource != null
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "إعادة الضبط")
                }
            }
        }

        // Selection buttons (Pick photo / Batch)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { editInput.launch("image/*") },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
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

        // Batch processing card
        if (batchSources.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعالجة الجماعية لـ ${batchSources.size} صور", style = MaterialTheme.typography.titleMedium)
                    Text("سيتم تطبيق التعديلات الحالية على كل الصور وحفظها في المعرض.", style = MaterialTheme.typography.bodySmall)

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
                                    val adj = currentProject().toLegacyAdjustments().copy(
                                        jpegQuality = jpegQuality,
                                        maxDimension = maxDimension
                                    )
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
                                        onMessageChange("اكتمل تصدير الدفعة: تم حفظ $exportedCount صورة إلى المعرض بنجاح")
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

        // Active Photo Workspace Canvas
        if (editSource != null) {
            val photoMatrix = remember(brightness, contrast, saturation, temperature, tint) {
                android.graphics.ColorMatrix().apply {
                    setSaturation(saturation)
                    postConcat(android.graphics.ColorMatrix(PhotoColorRules.contrastBrightness(contrast, brightness)))
                    postConcat(android.graphics.ColorMatrix(PhotoColorRules.temperatureTint(temperature, tint)))
                }
            }

            // Visual Frame Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(Color(0xFF14181D)),
                    contentAlignment = Alignment.Center
                ) {
                    val frameAspect = if (cropRatio > 0f) cropRatio else (if (rotation == 90 || rotation == 270) 0.75f else 1.33f)

                    Box(
                        modifier = Modifier
                            .aspectRatio(frameAspect, matchHeightConstraintsFirst = true)
                            .fillMaxHeight(0.94f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background Photo
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

                        // Direct Interactive Layer Canvas Overlay
                        InteractiveLayerCanvas(
                            modifier = Modifier.fillMaxSize(),
                            textLayers = textLayers,
                            stickerLayers = stickerLayers,
                            brushStrokes = brushStrokes,
                            activeLayerId = activeLayerId,
                            brushMode = brushMode,
                            brushColorArgb = brushColorArgb,
                            brushWidthNormalized = brushWidthNormalized,
                            onSelectLayer = { activeLayerId = it },
                            onUpdateTextLayer = { updated ->
                                textLayers = textLayers.map { if (it.id == updated.id) updated else it }
                            },
                            onUpdateStickerLayer = { updated ->
                                stickerLayers = stickerLayers.map { if (it.id == updated.id) updated else it }
                            },
                            onDeleteLayer = { id ->
                                snapshot()
                                textLayers = textLayers.filterNot { it.id == id }
                                stickerLayers = stickerLayers.filterNot { it.id == id }
                                if (activeLayerId == id) activeLayerId = null
                            },
                            onDuplicateLayer = { id ->
                                snapshot()
                                textLayers.find { it.id == id }?.let { original ->
                                    val (nx, ny) = LayerTransformRules.computeDuplicatePosition(original.normalizedX, original.normalizedY)
                                    val cloned = original.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        normalizedX = nx,
                                        normalizedY = ny
                                    )
                                    textLayers = textLayers + cloned
                                    activeLayerId = cloned.id
                                }
                                stickerLayers.find { it.id == id }?.let { original ->
                                    val (nx, ny) = LayerTransformRules.computeDuplicatePosition(original.normalizedX, original.normalizedY)
                                    val cloned = original.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        normalizedX = nx,
                                        normalizedY = ny
                                    )
                                    stickerLayers = stickerLayers + cloned
                                    activeLayerId = cloned.id
                                }
                            },
                            onEditTextLayer = { layer ->
                                editingTextLayer = layer
                                showTextDialog = true
                            },
                            onAddBrushStroke = { stroke ->
                                snapshot()
                                brushStrokes = brushStrokes + stroke
                            },
                            onAppendStrokePoint = { point ->
                                if (brushStrokes.isNotEmpty()) {
                                    val current = brushStrokes.last()
                                    brushStrokes = brushStrokes.dropLast(1) + current.copy(
                                        points = current.points + point
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // 2. Studio Tool Category Tabs (Snapseed / Samsung Gallery Style)
            ScrollableTabRow(
                selectedTabIndex = activeToolTab.ordinal,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeToolTab == StudioToolTab.TUNE,
                    onClick = { activeToolTab = StudioToolTab.TUNE; brushMode = false },
                    text = { Text("الضبط") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                )
                Tab(
                    selected = activeToolTab == StudioToolTab.FILTERS,
                    onClick = { activeToolTab = StudioToolTab.FILTERS; brushMode = false },
                    text = { Text("الفلاتر") },
                    icon = { Icon(Icons.Default.Filter, contentDescription = null) }
                )
                Tab(
                    selected = activeToolTab == StudioToolTab.TEXT,
                    onClick = { activeToolTab = StudioToolTab.TEXT; brushMode = false },
                    text = { Text("النصوص (${textLayers.size})") },
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null) }
                )
                Tab(
                    selected = activeToolTab == StudioToolTab.STICKERS,
                    onClick = { activeToolTab = StudioToolTab.STICKERS; brushMode = false },
                    text = { Text("ملصقات (${stickerLayers.size})") },
                    icon = { Icon(Icons.Default.EmojiEmotions, contentDescription = null) }
                )
                Tab(
                    selected = activeToolTab == StudioToolTab.DRAW,
                    onClick = { activeToolTab = StudioToolTab.DRAW; brushMode = true },
                    text = { Text("الرسم") },
                    icon = { Icon(Icons.Default.Brush, contentDescription = null) }
                )
                Tab(
                    selected = activeToolTab == StudioToolTab.TRANSFORM,
                    onClick = { activeToolTab = StudioToolTab.TRANSFORM; brushMode = false },
                    text = { Text("التحويل") },
                    icon = { Icon(Icons.Default.Crop, contentDescription = null) }
                )
                Tab(
                    selected = activeToolTab == StudioToolTab.EXPORT,
                    onClick = { activeToolTab = StudioToolTab.EXPORT; brushMode = false },
                    text = { Text("التصدير") },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) }
                )
            }

            // 3. Contextual Controls Body
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeToolTab) {
                        StudioToolTab.TUNE -> {
                            Text("تعديلات الإضاءة والألوان", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

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
                        }

                        StudioToolTab.FILTERS -> {
                            Text("الفلاتر الجاهزة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PhotoFilterPresets.all.forEach { preset ->
                                    FilterChip(
                                        selected = selectedPresetId == preset.id,
                                        onClick = {
                                            snapshot()
                                            selectedPresetId = preset.id
                                            val currentAdj = currentProject().toLegacyAdjustments()
                                            val filtered = PhotoFilterPresets.applyTo(currentAdj, preset)
                                            brightness = filtered.brightness
                                            contrast = filtered.contrast
                                            saturation = filtered.saturation
                                            temperature = filtered.temperature
                                            tint = filtered.tint
                                        },
                                        label = { Text(preset.displayName) }
                                    )
                                }
                            }
                        }

                        StudioToolTab.TEXT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("طبقات النصوص", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        editingTextLayer = null
                                        showTextDialog = true
                                    },
                                    enabled = textLayers.size < LayerTransformRules.MAX_LAYERS_COUNT
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("إضافة نص")
                                }
                            }

                            if (textLayers.isEmpty()) {
                                Text("لا توجد طبقات نصوص. اضغط على 'إضافة نص' لكتابة نص جديد على الصورة.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                textLayers.forEachIndexed { index, layer ->
                                    val isSelected = activeLayerId == layer.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { activeLayerId = layer.id },
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        tonalElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    layer.text.ifBlank { "(نص فارغ)" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    "الحجم: ${layer.fontSizeSp.toInt()}sp • التدوير: ${layer.rotationDegrees.toInt()}°",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = {
                                                    editingTextLayer = layer
                                                    showTextDialog = true
                                                }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                                }
                                                IconButton(onClick = {
                                                    snapshot()
                                                    textLayers = textLayers.filterNot { it.id == layer.id }
                                                    if (activeLayerId == layer.id) activeLayerId = null
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        StudioToolTab.STICKERS -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الملصقات والأشكال", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showStickerDialog = true },
                                    enabled = stickerLayers.size < LayerTransformRules.MAX_LAYERS_COUNT
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("إضافة ملصق")
                                }
                            }

                            if (stickerLayers.isEmpty()) {
                                Text("لا توجد ملصقات مضافة. اضغط 'إضافة ملصق' لإضافة رموز تعبيرية أو أشكال.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("يمكنك سحب أي ملصق وتكبيره وتدويره مباشرة على الصورة بإصبعين.", style = MaterialTheme.typography.bodySmall)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stickerLayers.forEach { sticker ->
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (activeLayerId == sticker.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable { activeLayerId = sticker.id },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (sticker.emoji.isNotBlank()) {
                                                Text(sticker.emoji, fontSize = 24.sp)
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(sticker.colorArgb))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        StudioToolTab.DRAW -> {
                            Text("الرسم الحر بالفرشاة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("وضع الرسم فوق الصورة")
                                Switch(
                                    checked = brushMode,
                                    onCheckedChange = { brushMode = it }
                                )
                            }

                            Text("لون الفرشاة", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ColorPaletteRules.STUDIO_COLORS.forEach { colorVal ->
                                    val isSelected = brushColorArgb == colorVal
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorVal))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable { brushColorArgb = colorVal }
                                    )
                                }
                            }

                            Text("سُمك الفرشاة: ${(brushWidthNormalized * 1000).toInt()}")
                            Slider(
                                value = brushWidthNormalized,
                                onValueChange = { brushWidthNormalized = it },
                                valueRange = 0.003f..0.04f
                            )

                            if (brushStrokes.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        snapshot()
                                        brushStrokes = emptyList()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("مسح كل الرسومات (${brushStrokes.size})")
                                }
                            }
                        }

                        StudioToolTab.TRANSFORM -> {
                            Text("التدوير والانعكاس", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { snapshot(); rotation = (rotation + 90) % 360 },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تدوير 90° ($rotation°)")
                                }
                                OutlinedButton(
                                    onClick = { snapshot(); flipHorizontal = !flipHorizontal },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (flipHorizontal) "إلغاء الانعكاس" else "انعكاس أفقي")
                                }
                            }

                            Text("نسبة الاقتصاص (Crop)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                        label = { Text(label) }
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
                                    onValueChangeFinished = { adjustingSlider = false }
                                )
                                Text("بؤرة الاقتصاص رأسيًا: ${(cropY * 100).toInt()}%")
                                Slider(
                                    value = cropY,
                                    onValueChange = {
                                        if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                                        cropY = it
                                    },
                                    onValueChangeFinished = { adjustingSlider = false }
                                )
                            }
                        }

                        StudioToolTab.EXPORT -> {
                            Text("إعدادات الدقة والتصدير", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("أقصى دقة للصورة المصدرة:")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1200 to "1200 (خفيف)", 2400 to "2400 (قياسي)", 3600 to "3600 (فائق)").forEach { (dim, label) ->
                                    FilterChip(
                                        selected = maxDimension == dim,
                                        onClick = { snapshot(); maxDimension = dim },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Text("جودة JPEG: $jpegQuality%")
                            Slider(
                                value = jpegQuality.toFloat(),
                                onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; jpegQuality = it.toInt() },
                                onValueChangeFinished = { adjustingSlider = false },
                                valueRange = 70f..100f
                            )

                            Button(
                                onClick = { editedOutput.launch("ProGalleryX-studio.jpg") },
                                enabled = !busy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("حفظ وتصدير الصورة المعدلة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // Quick Save Button always visible at bottom if not in export tab
            if (activeToolTab != StudioToolTab.EXPORT) {
                Button(
                    onClick = { editedOutput.launch("ProGalleryX-studio.jpg") },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("حفظ وتصدير الصورة", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Modal Text Editor Dialog
    if (showTextDialog) {
        PhotoTextEditDialog(
            initialLayer = editingTextLayer,
            onDismiss = {
                showTextDialog = false
                editingTextLayer = null
            },
            onConfirm = { updatedLayer ->
                snapshot()
                if (editingTextLayer == null) {
                    textLayers = textLayers + updatedLayer
                    activeLayerId = updatedLayer.id
                } else {
                    textLayers = textLayers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                }
                showTextDialog = false
                editingTextLayer = null
            },
            onDelete = editingTextLayer?.let { layer ->
                {
                    snapshot()
                    textLayers = textLayers.filterNot { it.id == layer.id }
                    if (activeLayerId == layer.id) activeLayerId = null
                    showTextDialog = false
                    editingTextLayer = null
                }
            }
        )
    }

    // Modal Sticker Picker Dialog
    if (showStickerDialog) {
        PhotoStickerPicker(
            onDismiss = { showStickerDialog = false },
            onSelectEmoji = { emoji ->
                snapshot()
                val newSticker = InteractiveStickerLayer(
                    emoji = emoji,
                    normalizedX = 0.5f,
                    normalizedY = 0.5f
                )
                stickerLayers = stickerLayers + newSticker
                activeLayerId = newSticker.id
                showStickerDialog = false
            },
            onSelectShape = { shapeType, colorArgb ->
                snapshot()
                val newSticker = InteractiveStickerLayer(
                    shapeType = shapeType,
                    colorArgb = colorArgb,
                    normalizedX = 0.5f,
                    normalizedY = 0.5f
                )
                stickerLayers = stickerLayers + newSticker
                activeLayerId = newSticker.id
                showStickerDialog = false
            }
        )
    }
}
