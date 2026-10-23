package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun OfflineToolsPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) }
    var duplicateScan by remember { mutableStateOf<DuplicateScan?>(null) }
    var selectedDuplicateGroup by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfLayout by remember { mutableStateOf(PdfLayout.AUTO) }
    var pdfPaper by remember { mutableStateOf(PdfPaper.A4) }
    var pdfPageNumbers by remember { mutableStateOf(false) }
    var pdfMergeInputs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfExtractSource by remember { mutableStateOf<Uri?>(null) }
    var pdfPageStart by remember { mutableStateOf("1") }
    var pdfPageEnd by remember { mutableStateOf("1") }
    var imageToRotate by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("كل المعالجة محلية. لن تُعدّل الملفات الأصلية.") }
    var busy by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var clipsToJoin by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var joinRanges by remember { mutableStateOf<Map<Uri, Pair<Float, Float>>>(emptyMap()) }
    var joinDurations by remember { mutableStateOf<Map<Uri, Float>>(emptyMap()) }
    var mutedJoinClips by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var timelineCursorMs by remember { mutableFloatStateOf(0f) }
    val joinOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { dest ->
        if (!busy && dest != null && clipsToJoin.size in 2..12) {
            val orderedClips = clipsToJoin.mapNotNull { uri -> joinRanges[uri]?.let { VideoJoinPart(uri, (it.first * 1000f).toLong(), (it.second * 1000f).toLong(), uri in mutedJoinClips) } }
            if (orderedClips.size != clipsToJoin.size) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                try {
                    MultiClipEditor.exportParts(context, orderedClips, dest)
                    message = "تم دمج المقاطع"
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    message = "أُلغي تصدير الفيديو"
                    throw cancelled
                } catch (error: Exception) {
                    message = "فشل دمج المقاطع: ${error.localizedMessage}"
                } finally { busy = false }
            }
        }
    }
    val joinInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        clipsToJoin = selected.distinct().take(12)
        joinRanges = emptyMap()
        joinDurations = emptyMap()
        mutedJoinClips = emptySet()
        val captured = clipsToJoin.toList()
        scope.launch {
            val durations = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                captured.mapNotNull { uri ->
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val ms = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        if (ms >= 500L) uri to (ms / 1000f) else null
                    } catch (_: Exception) { null }
                    finally { retriever.release() }
                }.toMap()
            }
            if (captured == clipsToJoin) {
                joinDurations = durations
                joinRanges = durations.mapValues { 0f to it.value }
                message = if (durations.size == captured.size) "حدد قص كل مقطع ثم صدّر" else "تعذر قراءة بعض المقاطع؛ احذف غير الصالح"
            }
        }
        message = if (clipsToJoin.size in 2..12) "رتّب المقاطع ثم صدّر الفيديو" else "اختر مقطعين على الأقل"
    }
    var muteVideoAudio by remember { mutableStateOf(false) }
    var videoSpeed by remember { mutableFloatStateOf(1f) }
    var videoBitrate by remember { mutableIntStateOf(VideoQualityRules.AUTO) }
    var startSecond by remember { mutableFloatStateOf(0f) }
    var endSecond by remember { mutableFloatStateOf(10f) }
    var videoDuration by remember { mutableFloatStateOf(0f) }
    var videoInfoValid by remember { mutableStateOf(false) }
    var soundtrackUri by remember { mutableStateOf<Uri?>(null) }
    var soundtrackOffset by remember { mutableFloatStateOf(0f) }
    val soundtrackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { soundtrackUri = it; soundtrackOffset = 0f }
    val soundtrackOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val video = selectedVideo
        val audio = soundtrackUri
        if (!busy && video != null && audio != null && target != null && videoInfoValid) {
            val start = (startSecond * 1000f).toLong()
            val end = (endSecond * 1000f).toLong()
            busy = true
            scope.launch {
                try {
                    VideoSoundtrackEditor.export(context, video, audio, target, start, end, (soundtrackOffset * 1000).toLong())
                    message = "تم تصدير الفيديو مع المقطع الصوتي المختار"
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { message = "تعذر تصدير الصوت: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }

    var storyboardFrames by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var storyboardBusy by remember { mutableStateOf(false) }
    var requestedSeekMs by remember { mutableLongStateOf(-1L) }
    val videoOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val source = selectedVideo
        if (!busy && source != null && target != null) {
            val startMs = (startSecond * 1000).toLong()
            val endMs = (endSecond * 1000).toLong()
            val removeAudio = muteVideoAudio
            val exportSpeed = videoSpeed
            val exportBitrate = videoBitrate
            busy = true
            scope.launch {
                try {
                    VideoEditor.trimToDocument(context, source, target, startMs, endMs, removeAudio, exportSpeed, exportBitrate)
                    message = "تم تصدير الفيديو"
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    message = "أُلغي تصدير الفيديو"
                    throw cancelled
                } catch (error: Exception) {
                    message = "فشل الفيديو: ${error.localizedMessage}"
                } finally { busy = false }
            }
        }
    }
    val frameOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = selectedVideo
        if (!busy && videoInfoValid && source != null && target != null) {
            val frameTime = (startSecond * 1000).toLong()
            busy = true
            scope.launch {
                try {
                    VideoFrameExporter.export(context, source, target, frameTime)
                    message = "تم استخراج صورة ثابتة من الفيديو"
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { message = "تعذر استخراج الإطار: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }
    var splitSecondHalf by remember { mutableStateOf(false) }
    var splitSecond by remember { mutableFloatStateOf(0.5f) }
    val splitOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val source = selectedVideo
        if (!busy && source != null && target != null && videoInfoValid) {
            val positionMs = (splitSecond * 1000f).toLong()
            val half = splitSecondHalf
            val mute = muteVideoAudio
            busy = true
            scope.launch {
                try {
                    VideoEditor.splitPart(context, source, target, positionMs, half, mute)
                    message = "تم حفظ جزء الفيديو المحدد"
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { message = "تعذر تقسيم الفيديو: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }
    val videoInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideo = uri
        videoInfoValid = false
        if (uri != null) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val seconds = (retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000f
                if (seconds < 0.5f || !seconds.isFinite()) throw IllegalArgumentException("Video duration unavailable")
                videoDuration = seconds
                splitSecond = (seconds / 2f).coerceIn(0.5f, (seconds - 0.5f).coerceAtLeast(0.5f))
                videoInfoValid = true
                startSecond = 0f
                endSecond = videoDuration
            } catch (ignored: Exception) {
                videoDuration = 0f
                startSecond = 0f
                endSecond = 0f
                message = "لم أتمكن من قراءة مدة الفيديو. اختر مقطعًا صالحًا."
            } finally { retriever.release() }
        }
    }
    LaunchedEffect(selectedVideo) {
        storyboardFrames = emptyList()
        val uri = selectedVideo ?: return@LaunchedEffect
        storyboardBusy = true
        try {
            storyboardFrames = VideoStoryboard.generate(context, uri)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { storyboardFrames = emptyList() }
        finally { storyboardBusy = false }
    }
    var editSource by remember { mutableStateOf<Uri?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var showAdjustments by remember { mutableStateOf(false) }
    var rotation by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var cropRatio by remember { mutableFloatStateOf(0f) }
    var jpegQuality by remember { mutableIntStateOf(92) }
    var caption by remember { mutableStateOf("") }
    var captionDark by remember { mutableStateOf(false) }
    var captionSize by remember { mutableIntStateOf(32) }
    var captionPosition by remember { mutableFloatStateOf(0.9f) }
    var maxDimension by remember { mutableIntStateOf(2400) }
    var cropX by remember { mutableFloatStateOf(0.5f) }
    var cropY by remember { mutableFloatStateOf(0.5f) }
    var temperature by remember { mutableFloatStateOf(0f) }
    var tint by remember { mutableFloatStateOf(0f) }
    var textLayers by remember { mutableStateOf<List<PhotoTextLayer>>(emptyList()) }
    var brushStrokes by remember { mutableStateOf<List<BrushStroke>>(emptyList()) }
    var brushMode by remember { mutableStateOf(false) }
    var brushDark by remember { mutableStateOf(false) }
    var brushWidth by remember { mutableFloatStateOf(0.01f) }
    var undoStack by remember { mutableStateOf<List<PhotoAdjustments>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<PhotoAdjustments>>(emptyList()) }
    var adjustingSlider by remember { mutableStateOf(false) }
    fun snapshot() {
        undoStack = (undoStack + PhotoAdjustments(brightness, contrast, saturation, rotation, flipHorizontal, flipVertical, cropRatio, jpegQuality, caption, captionDark, captionSize, captionPosition, maxDimension, cropX, cropY, temperature, tint, textLayers, brushStrokes)).takeLast(30)
        redoStack = emptyList()
    }
    val editedOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = editSource
        if (!busy && target != null && source != null) {
            val adjustments = PhotoAdjustments(brightness, contrast, saturation, rotation, flipHorizontal, flipVertical, cropRatio, jpegQuality, caption, captionDark, captionSize, captionPosition, maxDimension, cropX, cropY, temperature, tint, textLayers, brushStrokes)
            busy = true
            scope.launch {
                try { PhotoEditor.export(context, source, target, adjustments); message = "تم تصدير الصورة بنجاح" }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { message = "فشل التصدير: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }
    val editInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { editSource = uri; showAdjustments = true; brightness = 0f; contrast = 1f; saturation = 1f; rotation = 0; flipHorizontal = false; flipVertical = false; cropRatio = 0f; jpegQuality = 92; undoStack = emptyList(); redoStack = emptyList(); caption = ""; captionDark = false; captionSize = 32; captionPosition = 0.9f; maxDimension = 2400; cropX = 0.5f; cropY = 0.5f; temperature = 0f; tint = 0f; textLayers = emptyList(); brushStrokes = emptyList() }
    }
    val pdfOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        if (!busy && target != null && images.isNotEmpty()) {
            val selectedImages = images.toList()
            val selectedLayout = pdfLayout
            val selectedPaper = pdfPaper
            val includeNumbers = pdfPageNumbers
            busy = true
            scope.launch {
                try { OfflineExports.imagesToPdf(context, selectedImages, target, selectedLayout, includeNumbers, selectedPaper); message = "تم تصدير ملف PDF بنجاح" }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { message = "تعذر تصدير PDF: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }
    val rotateOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = imageToRotate
        if (!busy && target != null && source != null) {
            busy = true
            scope.launch {
                try { OfflineExports.rotateAndSave(context, source, target); message = "تم حفظ الصورة المعدلة" }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { message = "تعذر تعديل الصورة: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }
    val mergeOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        if (!busy && target != null && pdfMergeInputs.size in 2..10) {
            val inputList = pdfMergeInputs.toList()
            busy = true
            scope.launch {
                try { PdfImportTools.merge(context, inputList, target); message = "تم دمج ملفات PDF بنجاح" }
                catch (error: kotlinx.coroutines.CancellationException) { throw error }
                catch (error: Exception) { message = "تعذر الدمج: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }
    val mergeInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { chosen ->
        pdfMergeInputs = chosen.take(10)
        message = if (pdfMergeInputs.size >= 2) "ملفات PDF: ${pdfMergeInputs.size}، راجع الترتيب ثم صدر" else "اختر ملفي PDF على الأقل"
    }
    val extractOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        val source = pdfExtractSource
        val start = pdfPageStart.toIntOrNull()
        val end = pdfPageEnd.toIntOrNull()
        if (!busy && target != null && source != null && start != null && end != null && start in 1..120 && end in start..120) {
            busy = true
            scope.launch {
                try { PdfImportTools.extract(context, source, target, start..end); message = "تم استخراج الصفحات" }
                catch (error: kotlinx.coroutines.CancellationException) { throw error }
                catch (error: Exception) { message = "تعذر استخراج الصفحات: ${error.localizedMessage}" }
                finally { busy = false }
            }
        }
    }
    val extractInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { chosen ->
        pdfExtractSource = chosen
        pdfPageStart = "1"; pdfPageEnd = "1"
    }
    val pdfInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        images = selected.take(100)
        message = if (images.isNotEmpty()) "رتّب صور المستند ثم اختر تصدير PDF" else "اختر صورة واحدة على الأقل"
    }
    val rotateInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selected ->
        if (selected != null) { imageToRotate = selected; rotateOutput.launch("ProGalleryX-rotated.jpg") }
    }
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("استوديو Pro Gallery X", style = MaterialTheme.typography.headlineSmall)
        Text("تحرير محلي • الملفات الأصلية محفوظة", style = MaterialTheme.typography.bodySmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("الصور", "الفيديو", "PDF", "المكررات").forEachIndexed { index, title ->
                SegmentedButton(
                    selected = selectedSection == index,
                    onClick = { if (!busy) selectedSection = index },
                    enabled = !busy,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                ) { Text(title) }
            }
        }
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (selectedSection == 0) {
        Button(onClick = { editInput.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("تحرير الإضاءة والتباين والألوان") }
        if (showAdjustments && editSource != null) {
            val photoMatrix = remember(brightness, contrast, saturation, temperature, tint) {
                android.graphics.ColorMatrix().apply {
                    setSaturation(saturation)
                    postConcat(android.graphics.ColorMatrix(PhotoColorRules.contrastBrightness(contrast, brightness)))
                    postConcat(android.graphics.ColorMatrix(PhotoColorRules.temperatureTint(temperature, tint)))
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = editSource,
                        contentDescription = "معاينة التعديلات",
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix(photoMatrix.array.copyOf())),
                        contentScale = if (cropRatio == 0f) ContentScale.Fit else ContentScale.Crop,
                        alignment = if (cropRatio == 0f) Alignment.Center else androidx.compose.ui.BiasAlignment(cropX * 2f - 1f, cropY * 2f - 1f),
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(cropRatio) {
                                if (cropRatio != 0f && !brushMode) detectDragGestures(
                                    onDragStart = { snapshot() },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        cropX = (cropX - drag.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        cropY = (cropY - drag.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    }
                                )
                            }
                            .graphicsLayer { rotationZ = rotation.toFloat(); scaleX = if (flipHorizontal) -1f else 1f; scaleY = if (flipVertical) -1f else 1f }
                    )
                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(brushMode, brushDark, brushWidth, brushStrokes.size) {
                        if (brushMode) detectDragGestures(
                            onDragStart = { position ->
                                if (brushStrokes.size < 100) {
                                    snapshot()
                                    brushStrokes = brushStrokes + BrushStroke(listOf(
                                        BrushPoint((position.x / size.width).coerceIn(0f, 1f),
                                            (position.y / size.height).coerceIn(0f, 1f))), brushDark, brushWidth)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (brushStrokes.isNotEmpty()) {
                                    val current = brushStrokes.last()
                                    if (current.points.size < 2000) {
                                        brushStrokes = brushStrokes.dropLast(1) + current.copy(points = current.points +
                                            BrushPoint((change.position.x / size.width).coerceIn(0f, 1f),
                                                (change.position.y / size.height).coerceIn(0f, 1f)))
                                    }
                                }
                            }
                        )
                    }) {
                        brushStrokes.forEach { stroke ->
                            if (stroke.points.isNotEmpty()) {
                                val path = Path()
                                stroke.points.forEachIndexed { idx, point ->
                                    val x = point.x * size.width
                                    val y = point.y * size.height
                                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, color = if (stroke.dark) Color.Black else Color.White,
                                    style = Stroke(width = size.width * stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                            }
                        }
                    }
                    textLayers.forEach { layer ->
                        if (layer.text.isNotBlank()) Text(
                            layer.text,
                            color = if (layer.dark) Color.Black else Color.White,
                            fontSize = (layer.size * 0.5f).sp,
                            modifier = Modifier.offset(x = ((layer.x - 0.5f) * 290f).dp, y = ((layer.y - 0.5f) * 210f).dp),
                            maxLines = 1
                        )
                    }
                    if (caption.isNotBlank()) Text(
                        caption,
                        color = if (captionDark) Color.Black else Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = (captionSize * 0.5f).sp,
                        modifier = Modifier.fillMaxWidth().padding(8.dp).offset(y = ((captionPosition - 0.5f) * 210f).dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
            Text("المعاينة تستخدم موضع القص المحدد؛ قد تختلف النتيجة النهائية بعد تصحيح اتجاه الكاميرا.", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { snapshot(); rotation = (rotation + 90) % 360 }, enabled = !busy) { Text("تدوير 90°") }
                OutlinedButton(onClick = { snapshot(); flipHorizontal = !flipHorizontal }, enabled = !busy) { Text("أفقي") }
                OutlinedButton(onClick = { snapshot(); flipVertical = !flipVertical }, enabled = !busy) { Text("عمودي") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    val previous = undoStack.last()
                    redoStack = (redoStack + PhotoAdjustments(brightness, contrast, saturation, rotation, flipHorizontal, flipVertical, cropRatio, jpegQuality, caption, captionDark, captionSize, captionPosition, maxDimension, cropX, cropY, temperature, tint, textLayers, brushStrokes)).takeLast(30)
                    undoStack = undoStack.dropLast(1)
                    brightness = previous.brightness
                    contrast = previous.contrast
                    saturation = previous.saturation
                    rotation = previous.rotation
                    flipHorizontal = previous.flipHorizontal
                    flipVertical = previous.flipVertical
                    cropRatio = previous.cropRatio
                    jpegQuality = previous.jpegQuality
                    caption = previous.caption
                    captionDark = previous.captionDark
                    captionSize = previous.captionSize
                    captionPosition = previous.captionPosition
                    maxDimension = previous.maxDimension
                    cropX = previous.cropX; cropY = previous.cropY
                    temperature = previous.temperature; tint = previous.tint; textLayers = previous.textLayers; brushStrokes = previous.brushStrokes
                }, enabled = !busy && undoStack.isNotEmpty()) { Text("تراجع") }
                TextButton(onClick = {
                    val next = redoStack.last()
                    redoStack = redoStack.dropLast(1)
                    undoStack = (undoStack + PhotoAdjustments(brightness, contrast, saturation, rotation, flipHorizontal, flipVertical, cropRatio, jpegQuality, caption, captionDark, captionSize, captionPosition, maxDimension, cropX, cropY, temperature, tint, textLayers, brushStrokes)).takeLast(30)
                    brightness = next.brightness; contrast = next.contrast; saturation = next.saturation
                    rotation = next.rotation; flipHorizontal = next.flipHorizontal; flipVertical = next.flipVertical
                    cropRatio = next.cropRatio; jpegQuality = next.jpegQuality
                    caption = next.caption; captionDark = next.captionDark; captionSize = next.captionSize; captionPosition = next.captionPosition; maxDimension = next.maxDimension; cropX = next.cropX; cropY = next.cropY; temperature = next.temperature; tint = next.tint; textLayers = next.textLayers; brushStrokes = next.brushStrokes
                }, enabled = !busy && redoStack.isNotEmpty()) { Text("إعادة") }
                TextButton(onClick = { snapshot(); brightness = 18f; contrast = 1.12f; saturation = 1.18f }, enabled = !busy) { Text("ألوان نابضة") }
                TextButton(onClick = { snapshot(); brightness = 0f; contrast = 1.08f; saturation = 0f }, enabled = !busy) { Text("أبيض وأسود") }
            }
            Text("نسبة القص", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                listOf("الأصل" to 0f, "1:1" to 1f, "4:3" to (4f / 3f), "16:9" to (16f / 9f), "3:4" to (3f / 4f), "9:16" to (9f / 16f)).forEach { (label, ratio) ->
                    FilterChip(selected = cropRatio == ratio, onClick = { snapshot(); cropRatio = ratio },
                        label = { Text(label) }, enabled = !busy)
                }
            }
            if (cropRatio != 0f) {
                Text("موضع القص أفقيًا: ${(cropX * 100).toInt()}%")
                Slider(value = cropX, onValueChange = {
                    if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                    cropX = it
                }, onValueChangeFinished = { adjustingSlider = false }, enabled = !busy)
                Text("موضع القص عموديًا: ${(cropY * 100).toInt()}%")
                Slider(value = cropY, onValueChange = {
                    if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                    cropY = it
                }, onValueChangeFinished = { adjustingSlider = false }, enabled = !busy)
                Text("الإزاحة تطبق على الملف الناتج؛ المعاينة الحالية لا تعرض موضع القص بدقة.")
            }
            HorizontalDivider()
            Text("الرسم الحر فوق الصورة", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = brushMode, onCheckedChange = { brushMode = it }, enabled = !busy)
                Text("تفعيل فرشاة الرسم داخل المعاينة")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = brushDark, onCheckedChange = { brushDark = it }, enabled = !busy)
                Text("فرشاة سوداء (بدل البيضاء)")
            }
            Text("سُمك الفرشاة: ${(brushWidth * 1000).toInt()}")
            Slider(value = brushWidth, onValueChange = { brushWidth = it },
                valueRange = 0.003f..0.04f, enabled = !busy)
            Text("عدد الخطوط: ${brushStrokes.size} / 100")
            OutlinedButton(onClick = { snapshot(); brushStrokes = emptyList() },
                enabled = !busy && brushStrokes.isNotEmpty()) { Text("مسح كل خطوط الرسم") }
            Text("الرسم فوق المعاينة تقريبي عند تدوير الصورة؛ الخطوط تُحفظ في نسخة JPEG الناتجة.")
            Text("طبقات نص مستقلة (حتى 3)", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { snapshot(); textLayers = textLayers + PhotoTextLayer("", 0.5f, 0.3f + 0.2f * textLayers.size) },
                enabled = !busy && textLayers.size < 3) { Text("إضافة طبقة نص") }
            textLayers.forEachIndexed { index, layer ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("طبقة ${index + 1}")
                            TextButton(onClick = { snapshot(); textLayers = textLayers.filterIndexed { i, _ -> i != index } },
                                enabled = !busy) { Text("حذف الطبقة") }
                        }
                        OutlinedTextField(value = layer.text, onValueChange = { value ->
                            snapshot()
                            textLayers = textLayers.toMutableList().also { it[index] = layer.copy(text = value.take(80)) }
                        }, label = { Text("نص الطبقة") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        Text("الموقع أفقيًا")
                        Slider(value = layer.x, onValueChange = { value ->
                            if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                            textLayers = textLayers.toMutableList().also { it[index] = layer.copy(x = value) }
                        }, onValueChangeFinished = { adjustingSlider = false }, valueRange = 0.05f..0.95f, enabled = !busy)
                        Text("الموقع عموديًا")
                        Slider(value = layer.y, onValueChange = { value ->
                            if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                            textLayers = textLayers.toMutableList().also { it[index] = layer.copy(y = value) }
                        }, onValueChangeFinished = { adjustingSlider = false }, valueRange = 0.05f..0.95f, enabled = !busy)
                        Text("حجم الطبقة: ${layer.size}")
                        Slider(value = layer.size.toFloat(), onValueChange = { value ->
                            if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                            textLayers = textLayers.toMutableList().also { it[index] = layer.copy(size = value.toInt()) }
                        }, onValueChangeFinished = { adjustingSlider = false }, valueRange = 16f..72f, enabled = !busy)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = layer.dark, onCheckedChange = { value ->
                                snapshot()
                                textLayers = textLayers.toMutableList().also { it[index] = layer.copy(dark = value) }
                            }, enabled = !busy)
                            Text("نص داكن")
                        }
                    }
                }
            }
            Text("نص على الصورة", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(value = caption, onValueChange = { snapshot(); caption = it.take(80) },
                label = { Text("اكتب نصًا يظهر أسفل الصورة") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = captionDark, onCheckedChange = { snapshot(); captionDark = it }, enabled = !busy)
                Text("نص داكن")
            }
            Text("موضع النص: ${(captionPosition * 100).toInt()}% من الأعلى")
            Slider(value = captionPosition, onValueChange = {
                if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                captionPosition = it
            }, onValueChangeFinished = { adjustingSlider = false },
                valueRange = 0.1f..0.9f, enabled = !busy)
            Text("حجم النص: $captionSize")
            Slider(value = captionSize.toFloat(), onValueChange = {
                if (!adjustingSlider) { snapshot(); adjustingSlider = true }
                captionSize = it.toInt()
            }, onValueChangeFinished = { adjustingSlider = false }, valueRange = 16f..72f, enabled = !busy)
            Text("دقة الصورة المصدّرة (أطول ضلع)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1200 to "1200", 2400 to "2400", 3600 to "3600").forEach { (size, label) ->
                    FilterChip(selected = maxDimension == size,
                        onClick = { snapshot(); maxDimension = size },
                        enabled = !busy, label = { Text(label) })
                }
            }
            Text("الدقة الأعلى قد تستهلك ذاكرة أكبر؛ الجودة الأصلية لا يمكن استعادتها إذا كانت الصورة صغيرة.")
            Text("جودة JPEG: $jpegQuality%")
            Slider(value = jpegQuality.toFloat(), onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; jpegQuality = it.toInt() }, onValueChangeFinished = { adjustingSlider = false }, valueRange = 70f..100f, enabled = !busy)
            Text("الإضاءة: ${brightness.toInt()}")
            Slider(value = brightness, onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; brightness = it }, onValueChangeFinished = { adjustingSlider = false }, valueRange = -100f..100f)
            Text("التباين: ${(contrast * 100).toInt()}%")
            Slider(value = contrast, onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; contrast = it }, onValueChangeFinished = { adjustingSlider = false }, valueRange = 0.5f..1.5f)
            Text("حرارة اللون: ${temperature.toInt()}")
            Slider(value = temperature, onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; temperature = it }, onValueChangeFinished = { adjustingSlider = false }, valueRange = -100f..100f)
            Text("تدرج أخضر / أرجواني: ${tint.toInt()}")
            Slider(value = tint, onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; tint = it }, onValueChangeFinished = { adjustingSlider = false }, valueRange = -100f..100f)
            Text("التشبع: ${(saturation * 100).toInt()}%")
            Slider(value = saturation, onValueChange = { if (!adjustingSlider) { snapshot(); adjustingSlider = true }; saturation = it }, onValueChangeFinished = { adjustingSlider = false }, valueRange = 0f..2f)
            TextButton(onClick = { snapshot(); brightness = 0f; contrast = 1f; saturation = 1f; rotation = 0; flipHorizontal = false; flipVertical = false; cropRatio = 0f; jpegQuality = 92; caption = ""; captionDark = false; captionSize = 32; captionPosition = 0.9f; maxDimension = 2400; cropX = 0.5f; cropY = 0.5f; temperature = 0f; tint = 0f; textLayers = emptyList() }, enabled = !busy) { Text("إعادة ضبط التعديلات") }
            Button(onClick = { editedOutput.launch("ProGalleryX-edited.jpg") }, enabled = !busy) { Text("تصدير صورة جديدة") }
        }
        Button(onClick = { rotateInput.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("تدوير صورة 90° وتصدير نسخة JPEG")
        }
        }
        if (selectedSection == 2) {
            Text("أدوات المستندات", style = MaterialTheme.typography.titleLarge)
            Text("إنشاء PDF من الصور دون إنترنت")
            Button(onClick = { pdfInput.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("اختيار صفحات PDF (حتى 100 صورة)")
            }
            if (images.isNotEmpty()) {
                Text("حجم الورق", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = pdfPaper == PdfPaper.A4, onClick = { pdfPaper = PdfPaper.A4 }, label = { Text("A4") }, enabled = !busy)
                    FilterChip(selected = pdfPaper == PdfPaper.LETTER, onClick = { pdfPaper = PdfPaper.LETTER }, label = { Text("Letter") }, enabled = !busy)
                }
                Text("اتجاه الصفحات", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(PdfLayout.AUTO to "تلقائي", PdfLayout.PORTRAIT to "عمودي", PdfLayout.LANDSCAPE to "أفقي").forEach { (mode, label) ->
                        FilterChip(selected = pdfLayout == mode, onClick = { pdfLayout = mode }, label = { Text(label) }, enabled = !busy)
                    }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = pdfPageNumbers, onCheckedChange = { pdfPageNumbers = it }, enabled = !busy)
                    Text("ترقيم صفحات المستند")
                }
                Text("صفحات المستند: ${images.size}", style = MaterialTheme.typography.titleMedium)
                images.take(100).forEachIndexed { index, uri ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(model = uri, contentDescription = "معاينة الصفحة ${index + 1}", contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp, 60.dp))
                        Text("الصفحة ${index + 1}", Modifier.weight(1f))
                        TextButton(onClick = {
                            val copy = images.toMutableList()
                            val selected = copy.removeAt(index)
                            copy.add(index - 1, selected)
                            images = copy
                        }, enabled = !busy && index > 0) { Text("↑") }
                        TextButton(onClick = {
                            val copy = images.toMutableList()
                            val selected = copy.removeAt(index)
                            copy.add(index + 1, selected)
                            images = copy
                        }, enabled = !busy && index < images.lastIndex) { Text("↓") }
                        TextButton(onClick = { images = images.filterIndexed { position, _ -> position != index } }, enabled = !busy) { Text("حذف") }
                    }
                }
                Button(onClick = { pdfOutput.launch("ProGalleryX.pdf") },
                    enabled = !busy && images.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("تصدير PDF بالترتيب المحدد") }
            }
        }
        if (selectedSection == 2) {
            HorizontalDivider()
            Text("دمج مستندات PDF", style = MaterialTheme.typography.titleLarge)
            Text("دمج 2–10 ملفات، بحد 120 صفحة. تتم إعادة رسم الصفحات؛ قد لا يبقى النص قابلًا للتحديد.")
            Button(onClick = { mergeInput.launch("application/pdf") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("اختيار ملفات PDF للدمج")
            }
            pdfMergeInputs.forEachIndexed { index, _ ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("مستند ${index + 1}")
                    TextButton(onClick = {
                        if (index > 0) {
                            val list = pdfMergeInputs.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index - 1, item)
                            pdfMergeInputs = list
                        }
                    }, enabled = !busy && index > 0) { Text("↑") }
                    TextButton(onClick = {
                        if (index < pdfMergeInputs.lastIndex) {
                            val list = pdfMergeInputs.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index + 1, item)
                            pdfMergeInputs = list
                        }
                    }, enabled = !busy && index < pdfMergeInputs.lastIndex) { Text("↓") }
                }
            }
            Button(onClick = { mergeOutput.launch("ProGalleryX-merged.pdf") },
                enabled = !busy && pdfMergeInputs.size in 2..10, modifier = Modifier.fillMaxWidth()) { Text("تصدير PDF مدمج") }
            HorizontalDivider()
            Text("استخراج صفحات PDF", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { extractInput.launch("application/pdf") }, enabled = !busy) { Text("اختيار ملف PDF") }
            if (pdfExtractSource != null) {
                OutlinedTextField(value = pdfPageStart, onValueChange = { pdfPageStart = it.filter(Char::isDigit).take(3) },
                    label = { Text("من الصفحة") }, singleLine = true)
                OutlinedTextField(value = pdfPageEnd, onValueChange = { pdfPageEnd = it.filter(Char::isDigit).take(3) },
                    label = { Text("إلى الصفحة") }, singleLine = true)
                Button(onClick = { extractOutput.launch("ProGalleryX-pages.pdf") },
                    enabled = !busy && pdfPageStart.toIntOrNull()?.let { start ->
                        pdfPageEnd.toIntOrNull()?.let { end -> start in 1..120 && end in start..120 }
                    } == true) { Text("تصدير الصفحات المحددة") }
            }
        }
        if (selectedSection == 1) {
            Text("محرر الفيديو", style = MaterialTheme.typography.titleLarge)
            Text("قص ودمج المقاطع وتحديد الصوت")
        Button(onClick = { videoInput.launch("video/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("اختيار فيديو لقصه") }
        if (selectedVideo != null) {
            Text("معاينة الفيديو", style = MaterialTheme.typography.titleMedium)
            key(selectedVideo) {
                AndroidView(
                    factory = { ctx -> android.widget.VideoView(ctx).apply {
                        setVideoURI(selectedVideo)
                        setOnPreparedListener { player -> player.isLooping = false }
                        setOnClickListener { if (isPlaying) pause() else start() }
                    } },
                    update = { view ->
                        if (view.tag != selectedVideo) { view.setVideoURI(selectedVideo); view.tag = selectedVideo }
                        if (requestedSeekMs >= 0) { view.seekTo(requestedSeekMs.toInt()); requestedSeekMs = -1L }
                    },
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }
            Text("يمكن تشغيل المعاينة بالضغط على الفيديو؛ حدد الإطار في الشريط أدناه للانتقال إليه.")
            if (storyboardBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (storyboardFrames.isNotEmpty() && videoInfoValid) {
                Text("شريط لقطات الفيديو", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(storyboardFrames) { index, bitmap ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(model = bitmap, contentDescription = "إطار فيديو ${index + 1}",
                                modifier = Modifier.size(94.dp, 64.dp).clickable {
                                    val seek = videoDuration * 1000f * index / (storyboardFrames.size - 1)
                                    requestedSeekMs = seek.toLong()
                                    startSecond = (seek / 1000f).coerceAtMost((endSecond - 0.5f).coerceAtLeast(0f))
                                })
                            Text("${(videoDuration * index / (storyboardFrames.size - 1)).toInt()}ث",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (videoInfoValid) {
            Text("بداية المقطع: ${startSecond.toInt()} ثانية")
            Slider(value = startSecond, onValueChange = { startSecond = it.coerceAtMost(endSecond - 0.5f) }, valueRange = 0f..(videoDuration - 0.5f).coerceAtLeast(0.5f))
            Text("نهاية المقطع: ${endSecond.toInt()} ثانية")
            Slider(value = endSecond, onValueChange = { endSecond = it.coerceAtLeast(startSecond + 0.5f) }, valueRange = 0.5f..videoDuration.coerceAtLeast(1f))
            Row { Checkbox(checked = muteVideoAudio, onCheckedChange = { muteVideoAudio = it }); Text("تصدير بدون صوت") }
            Text("سرعة الفيديو عند التصدير", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VideoSpeedRules.allowed.forEach { speed ->
                    FilterChip(selected = videoSpeed == speed, onClick = { videoSpeed = speed },
                        label = { Text("${speed}x") }, enabled = !busy)
                }
            }
            Text("جودة الفيديو المصدّر", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    VideoQualityRules.AUTO to "تلقائي",
                    VideoQualityRules.COMPACT to "مضغوط",
                    VideoQualityRules.BALANCED to "متوازن",
                    VideoQualityRules.HIGH to "عالٍ"
                ).forEach { (target, label) ->
                    FilterChip(selected = videoBitrate == target, onClick = { videoBitrate = target },
                        label = { Text(label) }, enabled = !busy)
                }
            }
            Text("المعدل المختار تقديري ويعتمد على دعم الجهاز وترميز الفيديو. حجم الملف يشمل الصوت أيضًا.")
            Text("المدة المتوقعة بعد تعديل السرعة: ${VideoSpeedRules.expectedDurationMs(((endSecond - startSecond) * 1000f).toLong().coerceAtLeast(500L), videoSpeed) / 1000f} ثانية")

            Text("تأكد أن النهاية لا تتجاوز مدة الفيديو. يدعم التصدير وفق إمكانات الجهاز.")
            HorizontalDivider()
            Text("إضافة موسيقى للفيديو", style = MaterialTheme.typography.titleMedium)
            Text("يستبدل هذا التصدير الصوت الأصلي بمقطع صوتي مستقل. يجب أن يكون الصوت بطول الجزء المختار أو أطول.")
            OutlinedButton(onClick = { soundtrackPicker.launch("audio/*") }, enabled = !busy) {
                Text(if (soundtrackUri == null) "اختيار ملف صوتي" else "تغيير الملف الصوتي")
            }
            if (soundtrackUri != null) {
                Text("بداية الموسيقى (بالثواني): ${"%.1f".format(soundtrackOffset)}")
                Slider(value = soundtrackOffset, onValueChange = { soundtrackOffset = it },
                    valueRange = 0f..120f, enabled = !busy)
                Text("يجب ألا تتجاوز مدة الموسيقى المتبقية طول الفيديو؛ سيُتحقق من ذلك عند التصدير.")
                Text("تم اختيار ملف صوتي؛ سيتم استخدام ${(endSecond - startSecond).toInt()} ثانية منه.")
                OutlinedButton(onClick = { soundtrackUri = null }, enabled = !busy) { Text("إزالة الملف الصوتي") }
                Button(onClick = { soundtrackOutput.launch("ProGalleryX-with-music.mp4") },
                    enabled = !busy && videoInfoValid && VideoExportRules.validTrim(
                        (startSecond * 1000f).toLong(), (endSecond * 1000f).toLong(), (videoDuration * 1000f).toLong()),
                    modifier = Modifier.fillMaxWidth()) { Text("تصدير فيديو مع الموسيقى") }
            }
            Text("تقسيم الفيديو إلى جزأين", style = MaterialTheme.typography.titleMedium)
            if (videoDuration >= 1f) {
                Text("موضع التقسيم: ${"%.1f".format(splitSecond)} ثانية")
                Slider(value = splitSecond.coerceIn(0.5f, videoDuration - 0.5f),
                    onValueChange = { splitSecond = it },
                    valueRange = 0.5f..(videoDuration - 0.5f), enabled = !busy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = !splitSecondHalf, onClick = { splitSecondHalf = false },
                        label = { Text("الجزء الأول") }, enabled = !busy)
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = splitSecondHalf, onClick = { splitSecondHalf = true },
                        label = { Text("الجزء الثاني") }, enabled = !busy)
                }
                OutlinedButton(onClick = { splitOutput.launch(if (splitSecondHalf) "ProGalleryX-part2.mp4" else "ProGalleryX-part1.mp4") },
                    enabled = !busy && videoInfoValid && VideoExportRules.validSplit((splitSecond * 1000).toLong(), (videoDuration * 1000).toLong()),
                    modifier = Modifier.fillMaxWidth()) { Text("تصدير الجزء المحدد") }
            }
            OutlinedButton(onClick = { frameOutput.launch("ProGalleryX-frame.jpg") },
                enabled = !busy && videoInfoValid, modifier = Modifier.fillMaxWidth()) {
                Text("حفظ صورة JPEG عند وقت البداية المحدد")
            }
            Button(onClick = { videoOutput.launch("ProGalleryX-trimmed.mp4") }, enabled = !busy && videoInfoValid && endSecond - startSecond >= 0.5f) { Text("تصدير مقطع MP4") }
            }
        }
        Button(onClick = { joinInput.launch("video/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("دمج مقاطع فيديو (2–12)") }
        if (clipsToJoin.isNotEmpty()) {
            val timelineSegments = remember(clipsToJoin, joinRanges) {
                if (clipsToJoin.size in 2..12 && clipsToJoin.all { joinRanges[it] != null }) {
                    runCatching {
                        ClipSequenceTimelineRules.build(clipsToJoin.map { uri ->
                            val range = requireNotNull(joinRanges[uri])
                            Triple(uri, (range.first * 1000f).toLong(), (range.second * 1000f).toLong())
                        })
                    }.getOrNull()
                } else null
            }
            if (timelineSegments != null) {
                Text("الخط الزمني للمقاطع", style = MaterialTheme.typography.titleMedium)
                Text("المدة الإجمالية: ${ClipSequenceTimelineRules.formatTime(ClipSequenceTimelineRules.totalDurationMs(timelineSegments))}")
                val timelineDurationMs = ClipSequenceTimelineRules.totalDurationMs(timelineSegments)
                val cursor = timelineCursorMs.coerceIn(0f, timelineDurationMs.toFloat())
                val selectedClipIndex = ClipSequenceTimelineRules.clipIndexAt(timelineSegments, cursor.toLong())
                val sourcePosition = ClipSequenceTimelineRules.sourcePositionAt(timelineSegments, cursor.toLong())
                Text("موضع المؤشر: ${ClipSequenceTimelineRules.formatTime(cursor.toLong())} / ${ClipSequenceTimelineRules.formatTime(timelineDurationMs)}")
                Slider(
                    value = cursor,
                    onValueChange = { timelineCursorMs = it },
                    valueRange = 0f..timelineDurationMs.toFloat().coerceAtLeast(1f),
                    enabled = !busy && timelineDurationMs > 0L
                )
                if (selectedClipIndex >= 0 && sourcePosition != null) {
                    Text("المقطع الحالي: ${selectedClipIndex + 1} — الوقت داخل الملف الأصلي: ${ClipSequenceTimelineRules.formatTime(sourcePosition.second)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { timelineCursorMs = 0f },
                        enabled = !busy && timelineDurationMs > 0L
                    ) { Text("البداية") }
                    OutlinedButton(
                        onClick = {
                            val current = ClipSequenceTimelineRules.clipIndexAt(timelineSegments, cursor.toLong())
                            val previous = (current - 1).coerceAtLeast(0)
                            timelineCursorMs = timelineSegments[previous].timelineStartMs.toFloat()
                        },
                        enabled = !busy && timelineDurationMs > 0L
                    ) { Text("المقطع السابق") }
                    OutlinedButton(
                        onClick = {
                            val next = timelineSegments.firstOrNull { it.timelineStartMs > cursor.toLong() }
                            timelineCursorMs = (next?.timelineStartMs ?: timelineDurationMs).toFloat()
                        },
                        enabled = !busy && timelineDurationMs > 0L
                    ) { Text("المقطع التالي") }
                }
                Text("هذا المؤشر يحدد المقطع وتوقيته؛ لا يشغّل الفيديو داخل هذه المعاينة.",
                    style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    timelineSegments.forEachIndexed { index, segment ->
                        ElevatedCard(modifier = Modifier.width(124.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("مقطع ${index + 1}")
                                TextButton(onClick = {
                                    timelineCursorMs = segment.timelineStartMs.toFloat()
                                }, enabled = !busy) { Text("تحديد المقطع") }
                                Text("${ClipSequenceTimelineRules.formatTime(segment.timelineStartMs)} → ${ClipSequenceTimelineRules.formatTime(segment.timelineEndMs)}",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("الطول: ${ClipSequenceTimelineRules.formatTime(segment.durationMs)}",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                Text("الخط الزمني غير جاهز: تأكد من صلاحية جميع المقاطع ونقاط القص.")
            }
            Text("ترتيب المقاطع", style = MaterialTheme.typography.titleMedium)
            clipsToJoin.forEachIndexed { index, uri ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المقطع ${index + 1}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { clipsToJoin = clipsToJoin.filterIndexed { i, _ -> i != index } }, enabled = !busy) { Text("حذف") }
                    TextButton(onClick = {
                        val clips = clipsToJoin.toMutableList()
                        val item = clips.removeAt(index)
                        clips.add(index - 1, item)
                        clipsToJoin = clips
                    }, enabled = !busy && index > 0) { Text("↑") }
                    TextButton(onClick = {
                        val clips = clipsToJoin.toMutableList()
                        val item = clips.removeAt(index)
                        clips.add(index + 1, item)
                        clipsToJoin = clips
                    }, enabled = !busy && index < clipsToJoin.lastIndex) { Text("↓") }
                }
            }
            clipsToJoin.forEachIndexed { index, uri ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uri in mutedJoinClips, onCheckedChange = { mute ->
                        mutedJoinClips = if (mute) mutedJoinClips + uri else mutedJoinClips - uri
                    }, enabled = !busy)
                    Text("كتم الصوت الأصلي للمقطع ${index + 1}")
                }
                joinRanges[uri]?.let { (start, end) ->
                    val duration = joinDurations[uri] ?: end
                    Text("المقطع ${index + 1}: من ${"%.1f".format(start)} إلى ${"%.1f".format(end)} ثانية")
                    Text("نقطة البداية")
                    Slider(value = start, onValueChange = { value ->
                        joinRanges = joinRanges + (uri to (value.coerceAtMost(end - 0.5f) to end))
                    }, valueRange = 0f..(duration - 0.5f).coerceAtLeast(0f), enabled = !busy)
                    Text("نقطة النهاية")
                    Slider(value = end, onValueChange = { value ->
                        joinRanges = joinRanges + (uri to (start to value.coerceAtLeast(start + 0.5f)))
                    }, valueRange = 0.5f..duration, enabled = !busy)
                }
            }
            Button(onClick = { joinOutput.launch("ProGalleryX-joined.mp4") },
                enabled = !busy && clipsToJoin.size in 2..12 && clipsToJoin.all { joinRanges.containsKey(it) }, modifier = Modifier.fillMaxWidth()) {
                Text("تصدير الفيديو المدمج")
            }
        }
        }
        if (selectedSection == 3) {
            Text("كشف الصور المتطابقة", style = MaterialTheme.typography.titleLarge)
            Text("فحص محلي للصور المتطابقة تمامًا باستخدام SHA-256. لا تُحذف أي ملفات تلقائيًا.")
            Text("الفحص محدود بأحدث 3000 صورة مرئية للتطبيق؛ الصور المتشابهة بصريًا ليست بالضرورة مكررة.")
            Button(onClick = {
                busy = true
                duplicateScan = null
                scope.launch {
                    try {
                        duplicateScan = DuplicatePhotoScanner.scan(context)
                        message = "اكتمل فحص الصور"
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (error: Exception) { message = "تعذر فحص الصور: ${error.localizedMessage}" }
                    finally { busy = false }
                }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("فحص المكررات") }
            duplicateScan?.let { result ->
                Text("الصور المفحوصة: ${result.examined} • مجموعات المكررات: ${result.groups.size}")
                Text("المساحة القابلة للتوفير نظريًا: ${result.reclaimableBytes / (1024 * 1024)} MB")
                if (result.truncated) Text("النتائج جزئية: تم بلوغ حد 3000 صورة.")
                if (result.skipped > 0) Text("تعذر قراءة ${result.skipped} صورة؛ قد تكون النتائج غير مكتملة.")
                result.groups.forEachIndexed { index, group ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مجموعة ${index + 1} — ${group.photos.size} نسخ", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = {
                                    selectedDuplicateGroup = if (selectedDuplicateGroup == index) -1 else index
                                }) { Text(if (selectedDuplicateGroup == index) "إخفاء" else "مراجعة") }
                            }
                            Text("يمكن توفير نحو ${group.reclaimableBytes / 1024} KB مع الاحتفاظ بنسخة واحدة.")
                            if (selectedDuplicateGroup == index) {
                                group.photos.forEachIndexed { position, photo ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AsyncImage(model = photo.uri, contentDescription = "صورة مكررة", modifier = Modifier.size(64.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(photo.name.ifBlank { "صورة بدون اسم" })
                                            Text("${photo.bytes / 1024} KB", style = MaterialTheme.typography.labelSmall)
                                            if (position == 0) Text("النسخة المرجعية — يُنصح بمراجعتها قبل أي حذف", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(onClick = {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(photo.uri, "image/*")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try { context.startActivity(intent) }
                                            catch (_: android.content.ActivityNotFoundException) { message = "لا يوجد تطبيق لعرض الصورة" }
                                            catch (error: SecurityException) { message = "تعذر فتح الصورة بسبب الصلاحيات" }
                                        }) { Text("فتح") }
                                    }
                                }
                            }
                        }
                    }
                }
                Text("راجع النسخ من المعرض أو تطبيق الملفات قبل الحذف. هذا الفحص للعرض فقط حفاظًا على ملفاتك.")
            }
        }
        if (busy) CircularProgressIndicator()
        OutlinedButton(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("العودة إلى المعرض") }
    }
}
