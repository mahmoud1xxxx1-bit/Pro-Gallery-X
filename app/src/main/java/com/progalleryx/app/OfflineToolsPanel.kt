package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun OfflineToolsPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageToRotate by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("كل المعالجة محلية. لن تُعدّل الملفات الأصلية.") }
    var busy by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var clipsToJoin by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val joinOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { dest ->
        if (!busy && dest != null && clipsToJoin.size in 2..12) {
            val orderedClips = clipsToJoin.toList()
            busy = true
            scope.launch {
                try {
                    MultiClipEditor.export(context, orderedClips, dest)
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
        clipsToJoin = selected.take(12)
        message = if (clipsToJoin.size in 2..12) "رتّب المقاطع ثم صدّر الفيديو" else "اختر مقطعين على الأقل"
    }
    var muteVideoAudio by remember { mutableStateOf(false) }
    var startSecond by remember { mutableFloatStateOf(0f) }
    var endSecond by remember { mutableFloatStateOf(10f) }
    var videoDuration by remember { mutableFloatStateOf(10f) }
    val videoOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val source = selectedVideo
        if (!busy && source != null && target != null) {
            val startMs = (startSecond * 1000).toLong()
            val endMs = (endSecond * 1000).toLong()
            val removeAudio = muteVideoAudio
            busy = true
            scope.launch {
                try {
                    VideoEditor.trimToDocument(context, source, target, startMs, endMs, removeAudio)
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
    val videoInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideo = uri
        if (uri != null) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val seconds = (retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000L) / 1000f
                videoDuration = seconds.coerceAtLeast(1f)
                startSecond = 0f
                endSecond = videoDuration
            } catch (ignored: Exception) {
                videoDuration = 10f
                startSecond = 0f
                endSecond = 10f
            } finally { retriever.release() }
        }
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
    var undoStack by remember { mutableStateOf<List<PhotoAdjustments>>(emptyList()) }
    fun snapshot() {
        undoStack = (undoStack + PhotoAdjustments(brightness, contrast, saturation, rotation, flipHorizontal, flipVertical, cropRatio, jpegQuality)).takeLast(30)
    }
    val editedOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = editSource
        if (!busy && target != null && source != null) {
            val adjustments = PhotoAdjustments(brightness, contrast, saturation, rotation, flipHorizontal, flipVertical, cropRatio, jpegQuality)
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
        if (uri != null) { editSource = uri; showAdjustments = true; brightness = 0f; contrast = 1f; saturation = 1f; rotation = 0; flipHorizontal = false; flipVertical = false; cropRatio = 0f; jpegQuality = 92; undoStack = emptyList() }
    }
    val pdfOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        if (!busy && target != null && images.isNotEmpty()) {
            val selectedImages = images.toList()
            busy = true
            scope.launch {
                try { OfflineExports.imagesToPdf(context, selectedImages, target); message = "تم تصدير ملف PDF بنجاح" }
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
            listOf("الصور", "الفيديو", "PDF").forEachIndexed { index, title ->
                SegmentedButton(
                    selected = selectedSection == index,
                    onClick = { if (!busy) selectedSection = index },
                    enabled = !busy,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                ) { Text(title) }
            }
        }
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (selectedSection == 0) {
        Button(onClick = { editInput.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("تحرير الإضاءة والتباين والألوان") }
        if (showAdjustments && editSource != null) {
            val photoMatrix = remember(brightness, contrast, saturation) {
                android.graphics.ColorMatrix().apply {
                    setSaturation(saturation)
                    val offset = 128f * (1f - contrast) + brightness * 2.55f
                    postConcat(android.graphics.ColorMatrix(floatArrayOf(
                        contrast, 0f, 0f, 0f, offset,
                        0f, contrast, 0f, 0f, offset,
                        0f, 0f, contrast, 0f, offset,
                        0f, 0f, 0f, 1f, 0f
                    )))
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = editSource,
                    contentDescription = "معاينة التعديلات",
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix(photoMatrix.array.copyOf())),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(260.dp).graphicsLayer { rotationZ = rotation.toFloat(); scaleX = if (flipHorizontal) -1f else 1f; scaleY = if (flipVertical) -1f else 1f }
                )
            }
            Text("معاينة قبل الحفظ", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { snapshot(); rotation = (rotation + 90) % 360 }, enabled = !busy) { Text("تدوير 90°") }
                OutlinedButton(onClick = { snapshot(); flipHorizontal = !flipHorizontal }, enabled = !busy) { Text("أفقي") }
                OutlinedButton(onClick = { snapshot(); flipVertical = !flipVertical }, enabled = !busy) { Text("عمودي") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    val previous = undoStack.last()
                    undoStack = undoStack.dropLast(1)
                    brightness = previous.brightness
                    contrast = previous.contrast
                    saturation = previous.saturation
                    rotation = previous.rotation
                    flipHorizontal = previous.flipHorizontal
                    flipVertical = previous.flipVertical
                    cropRatio = previous.cropRatio
                    jpegQuality = previous.jpegQuality
                }, enabled = !busy && undoStack.isNotEmpty()) { Text("تراجع") }
                TextButton(onClick = { snapshot(); brightness = 18f; contrast = 1.12f; saturation = 1.18f }, enabled = !busy) { Text("ألوان نابضة") }
                TextButton(onClick = { snapshot(); brightness = 0f; contrast = 1.08f; saturation = 0f }, enabled = !busy) { Text("أبيض وأسود") }
            }
            Text("نسبة القص", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("الأصل" to 0f, "1:1" to 1f, "4:3" to (4f / 3f), "16:9" to (16f / 9f)).forEach { (label, ratio) ->
                    FilterChip(selected = cropRatio == ratio, onClick = { snapshot(); cropRatio = ratio },
                        label = { Text(label) }, enabled = !busy)
                }
            }
            Text("جودة JPEG: $jpegQuality%")
            Slider(value = jpegQuality.toFloat(), onValueChange = { jpegQuality = it.toInt() }, valueRange = 70f..100f, enabled = !busy)
            Text("الإضاءة: ${brightness.toInt()}")
            Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -100f..100f)
            Text("التباين: ${(contrast * 100).toInt()}%")
            Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.5f..1.5f)
            Text("التشبع: ${(saturation * 100).toInt()}%")
            Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f)
            TextButton(onClick = { snapshot(); brightness = 0f; contrast = 1f; saturation = 1f; rotation = 0; flipHorizontal = false; flipVertical = false; cropRatio = 0f; jpegQuality = 92 }, enabled = !busy) { Text("إعادة ضبط التعديلات") }
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
                Text("صفحات المستند: ${images.size}", style = MaterialTheme.typography.titleMedium)
                images.take(100).forEachIndexed { index, uri ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        if (selectedSection == 1) {
            Text("محرر الفيديو", style = MaterialTheme.typography.titleLarge)
            Text("قص ودمج المقاطع وتحديد الصوت")
        Button(onClick = { videoInput.launch("video/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("اختيار فيديو لقصه") }
        if (selectedVideo != null) {
            Text("بداية المقطع: ${startSecond.toInt()} ثانية")
            Slider(value = startSecond, onValueChange = { startSecond = it.coerceAtMost(endSecond - 0.5f) }, valueRange = 0f..(videoDuration - 0.5f).coerceAtLeast(0.5f))
            Text("نهاية المقطع: ${endSecond.toInt()} ثانية")
            Slider(value = endSecond, onValueChange = { endSecond = it.coerceAtLeast(startSecond + 0.5f) }, valueRange = 0.5f..videoDuration.coerceAtLeast(1f))
            Row { Checkbox(checked = muteVideoAudio, onCheckedChange = { muteVideoAudio = it }); Text("تصدير بدون صوت") }
            Text("تأكد أن النهاية لا تتجاوز مدة الفيديو. يدعم التصدير وفق إمكانات الجهاز.")
            Button(onClick = { videoOutput.launch("ProGalleryX-trimmed.mp4") }, enabled = !busy) { Text("تصدير مقطع MP4") }
        }
        Button(onClick = { joinInput.launch("video/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("دمج مقاطع فيديو (2–12)") }
        if (clipsToJoin.isNotEmpty()) {
            Text("ترتيب المقاطع", style = MaterialTheme.typography.titleMedium)
            clipsToJoin.forEachIndexed { index, uri ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المقطع ${index + 1}", modifier = Modifier.weight(1f))
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
            Button(onClick = { joinOutput.launch("ProGalleryX-joined.mp4") },
                enabled = !busy && clipsToJoin.size in 2..12, modifier = Modifier.fillMaxWidth()) {
                Text("تصدير الفيديو المدمج")
            }
        }
        }
        if (busy) CircularProgressIndicator()
        OutlinedButton(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("العودة إلى المعرض") }
    }
}
