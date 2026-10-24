package com.progalleryx.app

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun DuplicateScannerSection(
    onClose: () -> Unit,
    isArabic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf(
            if (isArabic) "فحص محلي للصور المتطابقة تمامًا باستخدام SHA-256. لا تُحذف أي ملفات تلقائيًا."
            else "Local exact photo scan using SHA-256 hashes. No files are ever deleted automatically."
        )
    }

    var duplicateScan by remember { mutableStateOf<DuplicateScan?>(null) }
    var selectedDuplicateGroup by remember { mutableIntStateOf(-1) }

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
                    text = if (isArabic) "كاشف الصور المكررة" else "Duplicate Photo Scanner",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }

        Button(
            onClick = {
                busy = true
                duplicateScan = null
                scope.launch {
                    try {
                        duplicateScan = DuplicatePhotoScanner.scan(context)
                        message = if (isArabic) "اكتمل فحص الصور بنجاح" else "Photo scan completed successfully"
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        message = if (isArabic) "تعذر فحص الصور: ${error.localizedMessage}" else "Failed to scan: ${error.localizedMessage}"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isArabic) "بدء فحص الصور المكررة" else "Start Duplicate Scan")
        }

        if (busy) {
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        duplicateScan?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "نتائج الفحص:" else "Scan Results:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isArabic) {
                            "الصور المفحوصة: ${result.examined} • مجموعات المكررات: ${result.groups.size}"
                        } else {
                            "Examined photos: ${result.examined} • Duplicate groups: ${result.groups.size}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (isArabic) {
                            "المساحة القابلة للتوفير: ${result.reclaimableBytes / (1024 * 1024)} ميجابايت"
                        } else {
                            "Reclaimable storage: ${result.reclaimableBytes / (1024 * 1024)} MB"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            result.groups.forEachIndexed { index, group ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isArabic) "مجموعة ${index + 1} — ${group.photos.size} نسخ" else "Group ${index + 1} — ${group.photos.size} copies",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(onClick = {
                                selectedDuplicateGroup = if (selectedDuplicateGroup == index) -1 else index
                            }) {
                                Text(
                                    if (selectedDuplicateGroup == index) (if (isArabic) "إخفاء" else "Hide")
                                    else (if (isArabic) "مراجعة" else "Review")
                                )
                            }
                        }

                        if (selectedDuplicateGroup == index) {
                            group.photos.forEachIndexed { position, photo ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AsyncImage(
                                        model = photo.uri,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            photo.name.ifBlank { if (isArabic) "صورة بدون اسم" else "Untitled" },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "${photo.bytes / 1024} KB" + if (position == 0) (if (isArabic) " (النسخة الأصلية)" else " (Original copy)") else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (position == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(photo.uri, "image/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            message = if (isArabic) "تعذر فتح الصورة" else "Unable to open photo"
                                        }
                                    }) {
                                        Text(if (isArabic) "فتح" else "Open")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
