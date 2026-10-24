package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class StudioDestination {
    PHOTO,
    VIDEO,
    PDF,
    DUPLICATES
}

@Composable
fun StudioHubScreen(
    onClose: () -> Unit,
    isArabic: Boolean = false,
    initialDestination: StudioDestination? = null,
    initialMediaUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    var activeDestination by remember { mutableStateOf<StudioDestination?>(initialDestination) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(initialMediaUri) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(initialMediaUri) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            activeDestination = StudioDestination.PHOTO
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            activeDestination = StudioDestination.VIDEO
        }
    }

    // Direct Screen Navigation
    when (activeDestination) {
        StudioDestination.PHOTO -> {
            var photoBusy by remember { mutableStateOf(false) }
            var photoMessage by remember { mutableStateOf("") }
            PhotoEditorSection(
                busy = photoBusy,
                onBusyChange = { photoBusy = it },
                onMessageChange = { photoMessage = it },
                initialImageUri = selectedPhotoUri,
                onClose = {
                    activeDestination = null
                    selectedPhotoUri = null
                }
            )
        }

        StudioDestination.VIDEO -> {
            var videoBusy by remember { mutableStateOf(false) }
            var videoMessage by remember { mutableStateOf("") }
            AdvancedVideoStudio(
                initialVideoUri = selectedVideoUri,
                busy = videoBusy,
                onBusyChange = { videoBusy = it },
                onMessageChange = { videoMessage = it },
                onClose = {
                    activeDestination = null
                    selectedVideoUri = null
                },
                isArabic = isArabic
            )
        }

        StudioDestination.PDF -> {
            PdfStudioSection(
                onClose = { activeDestination = null },
                isArabic = isArabic
            )
        }

        StudioDestination.DUPLICATES -> {
            DuplicateScannerSection(
                onClose = { activeDestination = null },
                isArabic = isArabic
            )
        }

        null -> {
            // Main Studio Creative Hub Dashboard
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "استوديو Pro Gallery X" else "Pro Gallery X Studio",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isArabic) "محررات احترافية ومعالجة محلية 100% دون إنترنت" else "Professional editors • 100% local on-device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Card 1: Photo Studio
                StudioHubCard(
                    icon = Icons.Outlined.AutoFixHigh,
                    title = if (isArabic) "استوديو الصور الاحترافي" else "Photo Studio",
                    description = if (isArabic) "فلاتر، نصوص تفاعلية، ملصقات، رسم بالفرشاة، وقص وتعديل ألوان دقيق" else "Interactive text, stickers, brush, filters & precision color tuning",
                    accentColor = Color(0xFF007AFF),
                    actionLabel = if (isArabic) "بدء تعديل صورة" else "Edit Photo",
                    onClick = { photoPicker.launch("image/*") }
                )

                // Card 2: Video Studio
                StudioHubCard(
                    icon = Icons.Outlined.MovieFilter,
                    title = if (isArabic) "استوديو مونتاج الفيديو" else "Video Studio 2.0",
                    description = if (isArabic) "مسار زمني متعدد، قص، تقسيم، نصوص وملصقات متزامنة، وموسيقى مع تصدير Media3" else "Multi-track timeline, trim, split, timed overlays, music & Media3 export",
                    accentColor = Color(0xFFFF9500),
                    actionLabel = if (isArabic) "بدء مونتاج فيديو" else "Edit Video",
                    onClick = { videoPicker.launch("video/*") }
                )

                // Card 3: PDF Studio
                StudioHubCard(
                    icon = Icons.Outlined.PictureAsPdf,
                    title = if (isArabic) "استوديو مستندات PDF" else "PDF Studio",
                    description = if (isArabic) "تحويل الصور إلى PDF، دمج المستندات، واستخراج الصفحات بدقة وسرعة" else "Convert photos to PDF, merge documents & extract pages",
                    accentColor = Color(0xFFFF2D55),
                    actionLabel = if (isArabic) "فتح أدوات PDF" else "Open PDF Tools",
                    onClick = { activeDestination = StudioDestination.PDF }
                )

                // Card 4: Duplicate Media Scanner
                StudioHubCard(
                    icon = Icons.Outlined.CleaningServices,
                    title = if (isArabic) "كاشف الصور المكررة" else "Duplicate Photo Scanner",
                    description = if (isArabic) "فحص آمن بالبصمة الرقمية (SHA-256) لتوفير التخزين دون حذف تلقائي" else "Safe cryptographic hash scan to reclaim phone storage",
                    accentColor = Color(0xFF34C759),
                    actionLabel = if (isArabic) "بدء فحص التخزين" else "Scan Storage",
                    onClick = { activeDestination = StudioDestination.DUPLICATES }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "العودة إلى المعرض" else "Back to Gallery")
                }
            }
        }
    }
}

@Composable
private fun StudioHubCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                        }
                    }
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
