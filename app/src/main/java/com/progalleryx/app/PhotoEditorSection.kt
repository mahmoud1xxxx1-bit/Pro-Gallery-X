package com.progalleryx.app

import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var isComparing by remember { mutableStateOf(false) }

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

    // Determine intrinsic aspect ratio of the image
    var intrinsicAspect by remember(editSource) { mutableFloatStateOf(1f) }
    LaunchedEffect(editSource) {
        val source = editSource ?: return@LaunchedEffect
        intrinsicAspect = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(source)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                val orientation = context.contentResolver.openInputStream(source)?.use {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
                val rotated = orientation in listOf(
                    ExifInterface.ORIENTATION_ROTATE_90,
                    ExifInterface.ORIENTATION_ROTATE_270,
                    ExifInterface.ORIENTATION_TRANSPOSE,
                    ExifInterface.ORIENTATION_TRANSVERSE
                )
                val w = if (rotated) bounds.outHeight else bounds.outWidth
                val h = if (rotated) bounds.outWidth else bounds.outHeight
                if (w > 0 && h > 0) w.toFloat() / h else 1f
            }.getOrDefault(1f)
        }
    }

    if (editSource == null) {
        // Welcoming Empty State Studio
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                    "استوديو الصور الاحترافي",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(48.dp))
            }

            // Hero Pick Button Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !busy) { editInput.launch("image/*") },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "اختر صورة من المعرض للبدء",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "تحرير احترافي، فلاتر سينمائية، ملصقات ونصوص تفاعلية مع تحكم كامل باللمس.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { editInput.launch("image/*") },
                        enabled = !busy
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("فتح المعرض")
                    }
                }
            }

            // Batch Processing Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تعديل دفعة صور (Batch)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("تطبيق التعديلات والفلاتر على عدة صور بنقرة واحدة", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = { batchInput.launch("image/*") },
                            enabled = !busy
                        ) {
                            Text("اختيار (2–25)")
                        }
                    }

                    if (batchSources.isNotEmpty()) {
                        HorizontalDivider()
                        Text("تم اختيار ${batchSources.size} صور للمعالجة الجماعية")
                        if (batchProgress != null) {
                            LinearProgressIndicator(
                                progress = { batchProgress!!.first.toFloat() / batchProgress!!.second.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("معالجة الصورة ${batchProgress!!.first} من ${batchProgress!!.second}")
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
                                        var count = 0
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
                                                val dest = runCatching {
                                                    context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                                }.getOrNull() ?: Uri.fromFile(java.io.File(context.cacheDir, fileName))

                                                PhotoEditor.export(context, src, dest, adj)

                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && dest.scheme == "content") {
                                                    values.clear()
                                                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                                                    context.contentResolver.update(dest, values, null, null)
                                                }
                                                count++
                                            }
                                            onMessageChange("تم تصدير $count صور بنجاح إلى المعرض")
                                        } catch (e: Exception) {
                                            onMessageChange("خطأ أثناء معالجة الدفعة: ${e.localizedMessage}")
                                        } finally {
                                            batchProgress = null
                                            onBusyChange(false)
                                        }
                                    }
                                },
                                enabled = !busy && batchProgress == null
                            ) {
                                Text("تطبيق وتصدير الكل")
                            }
                            OutlinedButton(
                                onClick = { batchSources = emptyList() },
                                enabled = !busy && batchProgress == null
                            ) {
                                Text("إلغاء")
                            }
                        }
                    }
                }
            }

            // Feature Guide Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مميزات استوديو الصور الاحترافي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("• تحريك وتكبير وتدوير النصوص والملصقات مباشرة بإصبعين على الصورة.")
                    Text("• خطوط إرشادية تلقائية (Magnetic Snap) عند المحاذاة مع المنتصف أو الزوايا.")
                    Text("• استيراد ملصقات خاصة من جهازك بصيغة PNG شفافة.")
                    Text("• رسم بالفرشاة الناعمة (Bézier Smoothing) مع خيارات الشفافية والممحاة.")
                    Text("• زر 'قبل / بعد' بالضغط المطول لمقارنة الصورة مع الأصل في أي لحظة.")
                }
            }
        }
    } else {
        // Active Editing Full-Screen Workspace (Non-scrolling root column)
        val photoMatrix = remember(brightness, contrast, saturation, temperature, tint) {
            android.graphics.ColorMatrix().apply {
                setSaturation(saturation)
                postConcat(android.graphics.ColorMatrix(PhotoColorRules.contrastBrightness(contrast, brightness)))
                postConcat(android.graphics.ColorMatrix(PhotoColorRules.temperatureTint(temperature, tint)))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                    Text(
                        "استوديو الصور",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hold to Compare Button
                    FilledTonalButton(
                        onClick = { },
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isComparing = true
                                    tryAwaitRelease()
                                    isComparing = false
                                }
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isComparing) "الأصل" else "قبل / بعد",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

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
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "تراجع")
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
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "إعادة")
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
                        enabled = !busy
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "إعادة الضبط")
                    }

                    // Change Photo
                    IconButton(
                        onClick = { editInput.launch("image/*") },
                        enabled = !busy
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "تغيير الصورة")
                    }
                }
            }

            // 2. Full-Viewport Canvas Workspace (fills remaining vertical space)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0C0F12)),
                contentAlignment = Alignment.Center
            ) {
                val rotatedAspect = if (rotation == 90 || rotation == 270) 1f / intrinsicAspect.coerceAtLeast(0.01f) else intrinsicAspect
                val frameAspect = (if (cropRatio > 0f) cropRatio else rotatedAspect).coerceIn(0.1f, 10f)

                // Fit entire photo inside constraints while maximizing area
                val availableWidth = maxWidth * 0.98f
                val availableHeight = maxHeight * 0.98f
                val imageWidth = minOf(availableWidth, availableHeight * frameAspect)
                val imageHeight = imageWidth / frameAspect

                Box(
                    modifier = Modifier
                        .width(imageWidth)
                        .height(imageHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Base Photo
                    AsyncImage(
                        model = editSource,
                        contentDescription = "معاينة الصورة",
                        colorFilter = if (isComparing) null else ColorFilter.colorMatrix(ColorMatrix(photoMatrix.array.copyOf())),
                        contentScale = if (isComparing || cropRatio == 0f) ContentScale.Fit else ContentScale.Crop,
                        alignment = if (isComparing || cropRatio == 0f) Alignment.Center else androidx.compose.ui.BiasAlignment(cropX * 2f - 1f, cropY * 2f - 1f),
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

                    // Interactive Touch Layer Overlay (Hidden while holding compare)
                    if (!isComparing) {
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
                            },
                            onBringForward = { id ->
                                snapshot()
                                val tIdx = textLayers.indexOfFirst { it.id == id }
                                if (tIdx >= 0) {
                                    textLayers = LayerTransformRules.bringForward(textLayers, tIdx)
                                }
                                val sIdx = stickerLayers.indexOfFirst { it.id == id }
                                if (sIdx >= 0) {
                                    stickerLayers = LayerTransformRules.bringForward(stickerLayers, sIdx)
                                }
                            },
                            onSendBackward = { id ->
                                snapshot()
                                val tIdx = textLayers.indexOfFirst { it.id == id }
                                if (tIdx >= 0) {
                                    textLayers = LayerTransformRules.sendBackward(textLayers, tIdx)
                                }
                                val sIdx = stickerLayers.indexOfFirst { it.id == id }
                                if (sIdx >= 0) {
                                    stickerLayers = LayerTransformRules.sendBackward(stickerLayers, sIdx)
                                }
                            }
                        )
                    } else {
                        // "Comparing Original" Floating Pill Badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(percent = 50),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Text(
                                "المعاينة الأصلية (قبل التعديل)",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. Category Tabs Bar
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

            // 4. Contextual Bottom Toolbar (Fixed bounded height with internal vertical scroll)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 220.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
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
                            Text("الفلاتر السينمائية الجاهزة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                Text("اضغط 'إضافة نص' لكتابة نص عربي أو إنجليزي مع خطوط وخلفيات وظلال.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                textLayers.forEach { layer ->
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
                                                val layerIdx = textLayers.indexOfFirst { it.id == layer.id }
                                                IconButton(
                                                    onClick = {
                                                        if (layerIdx < textLayers.size - 1) {
                                                            snapshot()
                                                            textLayers = LayerTransformRules.bringForward(textLayers, layerIdx)
                                                        }
                                                    },
                                                    enabled = layerIdx < textLayers.size - 1
                                                ) {
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = "للأمام", modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        if (layerIdx > 0) {
                                                            snapshot()
                                                            textLayers = LayerTransformRules.sendBackward(textLayers, layerIdx)
                                                        }
                                                    },
                                                    enabled = layerIdx > 0
                                                ) {
                                                    Icon(Icons.Default.ArrowDownward, contentDescription = "للخلف", modifier = Modifier.size(18.dp))
                                                }
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
                                    Text("مكتبة الملصقات")
                                }
                            }

                            if (stickerLayers.isEmpty()) {
                                Text("يمكنك إضافة رموز تعبيرية، قلوب، أسهم، أشكال هندسية، أو صور PNG خاصة من جهازك.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stickerLayers.forEach { sticker ->
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (activeLayerId == sticker.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable { activeLayerId = sticker.id },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!sticker.customImageUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = sticker.customImageUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                                                )
                                            } else if (sticker.emoji.isNotBlank()) {
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

                                if (activeLayerId != null && stickerLayers.any { it.id == activeLayerId }) {
                                    val stickerIdx = stickerLayers.indexOfFirst { it.id == activeLayerId }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("التحكم بالملصق المحدد", style = MaterialTheme.typography.labelMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    if (stickerIdx < stickerLayers.size - 1) {
                                                        snapshot()
                                                        stickerLayers = LayerTransformRules.bringForward(stickerLayers, stickerIdx)
                                                    }
                                                },
                                                enabled = stickerIdx < stickerLayers.size - 1
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "للأمام", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (stickerIdx > 0) {
                                                        snapshot()
                                                        stickerLayers = LayerTransformRules.sendBackward(stickerLayers, stickerIdx)
                                                    }
                                                },
                                                enabled = stickerIdx > 0
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "للخلف", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    snapshot()
                                                    stickerLayers = stickerLayers.filterNot { it.id == activeLayerId }
                                                    activeLayerId = null
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
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
                                Text("تفعيل وضع الرسم باللمس")
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
                                Text("حفظ وتصدير الصورة في المعرض", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
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
            },
            onSelectCustomImage = { uri ->
                snapshot()
                val newSticker = InteractiveStickerLayer(
                    customImageUri = uri.toString(),
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
