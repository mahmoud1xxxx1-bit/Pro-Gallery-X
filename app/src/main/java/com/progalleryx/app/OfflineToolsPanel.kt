package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
        if (busy) CircularProgressIndicator()
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("العودة إلى المعرض") }
    }
}
