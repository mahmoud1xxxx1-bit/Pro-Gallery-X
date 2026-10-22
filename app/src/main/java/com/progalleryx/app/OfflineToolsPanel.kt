package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun OfflineToolsPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageToRotate by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("كل المعالجة محلية. لن تُعدّل الملفات الأصلية.") }
    var busy by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var clipsToJoin by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val joinOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { dest ->
        if (!busy && dest != null && clipsToJoin.size in 2..12) {
            busy = true
            scope.launch {
                message = runCatching { MultiClipEditor.export(context, clipsToJoin, dest); "تم دمج المقاطع" }.getOrElse { "فشل دمج المقاطع: ${it.localizedMessage}" }
                busy = false
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
        if (source != null && target != null) {
            busy = true
            scope.launch {
                message = runCatching { VideoEditor.trimToDocument(context, source, target, (startSecond * 1000).toLong(), (endSecond * 1000).toLong(), muteVideoAudio); "تم تصدير الفيديو" }.getOrElse { "فشل الفيديو: ${it.localizedMessage}" }
                busy = false
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
    val editedOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = editSource
        if (target != null && source != null) {
            busy = true
            scope.launch {
                message = runCatching { PhotoEditor.export(context, source, target, PhotoAdjustments(brightness, contrast, saturation)); "تم تصدير الصورة بنجاح" }.getOrElse { "فشل التصدير: ${it.localizedMessage}" }
                busy = false
            }
        }
    }
    val editInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { editSource = uri; showAdjustments = true }
    }
    val pdfOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        if (target != null && images.isNotEmpty()) {
            busy = true
            scope.launch {
                message = runCatching { OfflineExports.imagesToPdf(context, images, target); "تم تصدير ملف PDF بنجاح" }
                    .getOrElse { "تعذر تصدير PDF: ${it.localizedMessage}" }
                busy = false
            }
        }
    }
    val rotateOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = imageToRotate
        if (target != null && source != null) {
            busy = true
            scope.launch {
                message = runCatching { OfflineExports.rotateAndSave(context, source, target); "تم حفظ الصورة المعدلة" }
                    .getOrElse { "تعذر تعديل الصورة: ${it.localizedMessage}" }
                busy = false
            }
        }
    }
    val pdfInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        images = selected.take(100)
        if (images.isNotEmpty()) pdfOutput.launch("ProGalleryX.pdf")
    }
    val rotateInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selected ->
        if (selected != null) { imageToRotate = selected; rotateOutput.launch("ProGalleryX-rotated.jpg") }
    }
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("أدوات Pro Gallery X", style = MaterialTheme.typography.headlineSmall)
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { editInput.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("تحرير الإضاءة والتباين والألوان") }
        if (showAdjustments) {
            Text("الإضاءة: ${brightness.toInt()}")
            Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -100f..100f)
            Text("التباين: ${(contrast * 100).toInt()}%")
            Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.5f..1.5f)
            Text("التشبع: ${(saturation * 100).toInt()}%")
            Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f)
            Button(onClick = { editedOutput.launch("ProGalleryX-edited.jpg") }, enabled = !busy) { Text("تصدير صورة جديدة") }
        }
        Button(onClick = { rotateInput.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("تدوير صورة 90° وتصدير نسخة JPEG")
        }
        Button(onClick = { pdfInput.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("تحويل صور إلى PDF (حتى 100 صورة)")
        }
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
        if (busy) CircularProgressIndicator()
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("العودة إلى المعرض") }
    }
}
