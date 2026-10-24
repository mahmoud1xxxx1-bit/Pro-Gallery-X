package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun OfflineToolsPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("كل المعالجة محلية. لن تُعدّل الملفات الأصلية.") }

    // PDF state
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfLayout by remember { mutableStateOf(PdfLayout.AUTO) }
    var pdfPaper by remember { mutableStateOf(PdfPaper.A4) }
    var pdfPageNumbers by remember { mutableStateOf(false) }
    var pdfMergeInputs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfExtractSource by remember { mutableStateOf<Uri?>(null) }
    var pdfPageStart by remember { mutableStateOf("1") }
    var pdfPageEnd by remember { mutableStateOf("1") }

    // Duplicate scanner state
    var duplicateScan by remember { mutableStateOf<DuplicateScan?>(null) }
    var selectedDuplicateGroup by remember { mutableIntStateOf(-1) }

    // Launchers for PDF
    val pdfOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        if (!busy && target != null && images.isNotEmpty()) {
            val selectedImages = images.toList()
            val selectedLayout = pdfLayout
            val selectedPaper = pdfPaper
            val includeNumbers = pdfPageNumbers
            busy = true
            scope.launch {
                try {
                    OfflineExports.imagesToPdf(context, selectedImages, target, selectedLayout, includeNumbers, selectedPaper)
                    message = "تم تصدير ملف PDF بنجاح"
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    message = "تعذر تصدير PDF: ${error.localizedMessage}"
                } finally {
                    busy = false
                }
            }
        }
    }

    val mergeOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        if (!busy && target != null && pdfMergeInputs.size in 2..10) {
            val inputList = pdfMergeInputs.toList()
            busy = true
            scope.launch {
                try {
                    PdfImportTools.merge(context, inputList, target)
                    message = "تم دمج ملفات PDF بنجاح"
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    message = "تعذر الدمج: ${error.localizedMessage}"
                } finally {
                    busy = false
                }
            }
        }
    }

    val extractOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        val source = pdfExtractSource
        val start = pdfPageStart.toIntOrNull()
        val end = pdfPageEnd.toIntOrNull()
        if (!busy && target != null && source != null && start != null && end != null && start in 1..120 && end in start..120) {
            busy = true
            scope.launch {
                try {
                    PdfImportTools.extract(context, source, target, start..end)
                    message = "تم استخراج الصفحات بنجاح"
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    message = "تعذر استخراج الصفحات: ${error.localizedMessage}"
                } finally {
                    busy = false
                }
            }
        }
    }

    val pdfInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        images = selected.take(100)
        message = if (images.isNotEmpty()) "رتّب صور المستند ثم اختر تصدير PDF" else "اختر صورة واحدة على الأقل"
    }

    val mergeInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { chosen ->
        pdfMergeInputs = chosen.take(10)
        message = if (pdfMergeInputs.size >= 2) "ملفات PDF: ${pdfMergeInputs.size}، راجع الترتيب ثم صدر" else "اختر ملفي PDF على الأقل"
    }

    val extractInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { chosen ->
        pdfExtractSource = chosen
        pdfPageStart = "1"
        pdfPageEnd = "1"
    }

    Column(
        Modifier
            .fillMaxSize()
            // Image/Video editors own their scrolling content. Nesting them in
            // an unbounded vertical scroll causes Compose to crash at measure.
            // PDF and duplicate tools still need a scrollable container.
            .then(if (selectedSection >= 2) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("استوديو Pro Gallery X", style = MaterialTheme.typography.headlineSmall)
        Text("تحرير محلي • الملفات الأصلية محفوظة دائمًا", style = MaterialTheme.typography.bodySmall)

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

        when (selectedSection) {
            0 -> PhotoEditorSection(
                busy = busy,
                onBusyChange = { busy = it },
                onMessageChange = { message = it }
            )
            1 -> VideoEditorSection(
                busy = busy,
                onBusyChange = { busy = it },
                onMessageChange = { message = it }
            )
            2 -> {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
            3 -> {
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
        }

        if (busy) CircularProgressIndicator()
        OutlinedButton(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("العودة إلى المعرض")
        }
    }
}
