package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun PdfStudioSection(
    onClose: () -> Unit,
    isArabic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf(if (isArabic) "جميع معالجة PDF تتم محليًا على جهازك." else "All PDF processing happens locally on device.") }

    // Images to PDF state
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfLayout by remember { mutableStateOf(PdfLayout.AUTO) }
    var pdfPaper by remember { mutableStateOf(PdfPaper.A4) }
    var pdfPageNumbers by remember { mutableStateOf(false) }

    // Merge PDF state
    var pdfMergeInputs by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Extract PDF state
    var pdfExtractSource by remember { mutableStateOf<Uri?>(null) }
    var pdfPageStart by remember { mutableStateOf("1") }
    var pdfPageEnd by remember { mutableStateOf("1") }

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
                    message = if (isArabic) "تم تصدير ملف PDF بنجاح" else "PDF exported successfully"
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    message = if (isArabic) "تعذر تصدير PDF: ${error.localizedMessage}" else "Failed to export PDF: ${error.localizedMessage}"
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
                    message = if (isArabic) "تم دمج ملفات PDF بنجاح" else "PDF files merged successfully"
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    message = if (isArabic) "تعذر الدمج: ${error.localizedMessage}" else "Failed to merge: ${error.localizedMessage}"
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
                    message = if (isArabic) "تم استخراج الصفحات بنجاح" else "Pages extracted successfully"
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    message = if (isArabic) "تعذر استخراج الصفحات: ${error.localizedMessage}" else "Failed to extract pages: ${error.localizedMessage}"
                } finally {
                    busy = false
                }
            }
        }
    }

    val pdfInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        images = selected.take(100)
        message = if (images.isNotEmpty()) {
            if (isArabic) "تم اختيار ${images.size} صورة. راجع الترتيب ثم صدّر" else "${images.size} images selected. Review order and export"
        } else {
            if (isArabic) "اختر صورة واحدة على الأقل" else "Select at least one photo"
        }
    }

    val mergeInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { chosen ->
        pdfMergeInputs = chosen.take(10)
        message = if (pdfMergeInputs.size >= 2) {
            if (isArabic) "ملفات PDF: ${pdfMergeInputs.size}. راجع الترتيب ثم ادمج" else "${pdfMergeInputs.size} PDF files selected. Review order and merge"
        } else {
            if (isArabic) "اختر ملفي PDF على الأقل" else "Select at least 2 PDF files"
        }
    }

    val extractInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { chosen ->
        pdfExtractSource = chosen
        pdfPageStart = "1"
        pdfPageEnd = "1"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isArabic) "استوديو مستندات PDF" else "PDF Studio",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (message.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Section 1: Images to PDF
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "إنشاء PDF من الصور" else "Create PDF from Photos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = { pdfInput.launch("image/*") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isArabic) "اختيار صور المستند (حتى 100 صورة)" else "Select Photos (Up to 100)")
                }

                if (images.isNotEmpty()) {
                    Text(
                        text = if (isArabic) "حجم الورق" else "Paper Size",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = pdfPaper == PdfPaper.A4,
                            onClick = { pdfPaper = PdfPaper.A4 },
                            label = { Text("A4") },
                            enabled = !busy
                        )
                        FilterChip(
                            selected = pdfPaper == PdfPaper.LETTER,
                            onClick = { pdfPaper = PdfPaper.LETTER },
                            label = { Text("Letter") },
                            enabled = !busy
                        )
                    }

                    Text(
                        text = if (isArabic) "اتجاه الصفحة" else "Page Orientation",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            PdfLayout.AUTO to (if (isArabic) "تلقائي" else "Auto"),
                            PdfLayout.PORTRAIT to (if (isArabic) "عمودي" else "Portrait"),
                            PdfLayout.LANDSCAPE to (if (isArabic) "أفقي" else "Landscape")
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = pdfLayout == mode,
                                onClick = { pdfLayout = mode },
                                label = { Text(label) },
                                enabled = !busy
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = pdfPageNumbers,
                            onCheckedChange = { pdfPageNumbers = it },
                            enabled = !busy
                        )
                        Text(if (isArabic) "إضافة ترقيم للصفحات" else "Include page numbers")
                    }

                    Text(
                        text = if (isArabic) "الصفحات المحددة (${images.size})" else "Selected Pages (${images.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    images.take(100).forEachIndexed { index, uri ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp, 56.dp)
                            )
                            Text(
                                text = if (isArabic) "الصفحة ${index + 1}" else "Page ${index + 1}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(
                                onClick = {
                                    val copy = images.toMutableList()
                                    val item = copy.removeAt(index)
                                    copy.add(index - 1, item)
                                    images = copy
                                },
                                enabled = !busy && index > 0
                            ) { Text("↑") }
                            TextButton(
                                onClick = {
                                    val copy = images.toMutableList()
                                    val item = copy.removeAt(index)
                                    copy.add(index + 1, item)
                                    images = copy
                                },
                                enabled = !busy && index < images.lastIndex
                            ) { Text("↓") }
                            TextButton(
                                onClick = { images = images.filterIndexed { pos, _ -> pos != index } },
                                enabled = !busy
                            ) {
                                Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Button(
                        onClick = { pdfOutput.launch("ProGalleryX_${System.currentTimeMillis()}.pdf") },
                        enabled = !busy && images.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "تصدير ملف PDF النهائي" else "Export PDF Document")
                    }
                }
            }
        }

        // Section 2: Merge PDFs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "دمج مستندات PDF" else "Merge PDF Documents",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = { mergeInput.launch("application/pdf") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabic) "اختيار ملفات PDF للدمج (2–10)" else "Choose PDF Files to Merge (2–10)")
                }

                if (pdfMergeInputs.isNotEmpty()) {
                    pdfMergeInputs.forEachIndexed { index, _ ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isArabic) "مستند ${index + 1}" else "Document ${index + 1}")
                            Row {
                                TextButton(
                                    onClick = {
                                        if (index > 0) {
                                            val list = pdfMergeInputs.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index - 1, item)
                                            pdfMergeInputs = list
                                        }
                                    },
                                    enabled = !busy && index > 0
                                ) { Text("↑") }
                                TextButton(
                                    onClick = {
                                        if (index < pdfMergeInputs.lastIndex) {
                                            val list = pdfMergeInputs.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index + 1, item)
                                            pdfMergeInputs = list
                                        }
                                    },
                                    enabled = !busy && index < pdfMergeInputs.lastIndex
                                ) { Text("↓") }
                            }
                        }
                    }

                    Button(
                        onClick = { mergeOutput.launch("ProGalleryX_Merged_${System.currentTimeMillis()}.pdf") },
                        enabled = !busy && pdfMergeInputs.size in 2..10,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "تصدير الملف المدمج" else "Export Merged PDF")
                    }
                }
            }
        }

        // Section 3: Extract Pages
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "استخراج صفحات من PDF" else "Extract PDF Pages",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = { extractInput.launch("application/pdf") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabic) "اختيار ملف PDF للاستخراج" else "Select PDF to Extract From")
                }

                if (pdfExtractSource != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pdfPageStart,
                            onValueChange = { pdfPageStart = it.filter(Char::isDigit).take(3) },
                            label = { Text(if (isArabic) "من الصفحة" else "From Page") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pdfPageEnd,
                            onValueChange = { pdfPageEnd = it.filter(Char::isDigit).take(3) },
                            label = { Text(if (isArabic) "إلى الصفحة" else "To Page") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { extractOutput.launch("ProGalleryX_Extracted_${System.currentTimeMillis()}.pdf") },
                        enabled = !busy && pdfPageStart.toIntOrNull()?.let { start ->
                            pdfPageEnd.toIntOrNull()?.let { end -> start in 1..120 && end in start..120 }
                        } == true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "تصدير الصفحات المحددة" else "Export Extracted Pages")
                    }
                }
            }
        }

        if (busy) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
