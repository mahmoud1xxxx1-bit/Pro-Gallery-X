package com.progalleryx.app

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun VideoTextOverlayDialog(
    initialOverlay: VideoTimedOverlay?,
    totalDurationMs: Long,
    currentPlayheadMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (VideoTimedOverlay) -> Unit,
    isArabic: Boolean = false
) {
    var text by remember { mutableStateOf(initialOverlay?.text ?: "") }
    var fontIndex by remember { mutableIntStateOf(initialOverlay?.fontIndex ?: 0) }
    var isBold by remember { mutableStateOf(initialOverlay?.isBold ?: false) }
    var isItalic by remember { mutableStateOf(initialOverlay?.isItalic ?: false) }
    var textColorArgb by remember { mutableLongStateOf(initialOverlay?.textColorArgb ?: 0xFFFFFFFFL) }
    var backgroundColorArgb by remember { mutableLongStateOf(initialOverlay?.backgroundColorArgb ?: 0x88000000L) }

    val defaultStart = initialOverlay?.startMs ?: currentPlayheadMs.coerceAtMost((totalDurationMs - 1500L).coerceAtLeast(0L))
    val defaultEnd = initialOverlay?.endMs ?: (defaultStart + 3000L).coerceAtMost(totalDurationMs)

    var startMs by remember { mutableLongStateOf(defaultStart) }
    var endMs by remember { mutableLongStateOf(defaultEnd) }

    val fontNames = if (isArabic) listOf("افتراضي", "مزخرف", "أحادي", "حديث") else listOf("Default", "Serif", "Mono", "Sans")
    val colorPalette = listOf(
        0xFFFFFFFFL to "White",
        0xFF000000L to "Black",
        0xFFFF3B30L to "Red",
        0xFFFF9500L to "Orange",
        0xFFFFCC00L to "Yellow",
        0xFF34C759L to "Green",
        0xFF007AFFL to "Blue",
        0xFFAF52DEL to "Purple"
    )

    val backgroundPalette = listOf(
        0x00000000L to "None",
        0x88000000L to "Translucent Dark",
        0x88FFFFFFL to "Translucent Light",
        0xFF000000L to "Solid Black",
        0xFF007AFFL to "Solid Blue",
        0xFFFF3B30L to "Solid Red"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialOverlay != null) (if (isArabic) "تعديل النص المتحرك" else "Edit Timed Text") else (if (isArabic) "إضافة نص متحرك" else "Add Timed Text"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isArabic) "اكتب النص هنا..." else "Enter text here...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                // Font Style Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    fontNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = fontIndex == index,
                            onClick = { fontIndex = index },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                // Bold & Italic Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isBold,
                        onClick = { isBold = !isBold },
                        label = { Text("B", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = isItalic,
                        onClick = { isItalic = !isItalic },
                        label = { Text("I", fontWeight = FontWeight.Normal) }
                    )
                }

                // Text Color Palette
                Text(if (isArabic) "لون الخط:" else "Text Color:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((colorArgb, _) in colorPalette) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb))
                                .border(
                                    width = if (textColorArgb == colorArgb) 2.5.dp else 1.dp,
                                    color = if (textColorArgb == colorArgb) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { textColorArgb = colorArgb }
                        )
                    }
                }

                // Background Box Palette
                Text(if (isArabic) "خلفية النص:" else "Box Background:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((bgArgb, _) in backgroundPalette) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (bgArgb == 0L) Color.Transparent else Color(bgArgb))
                                .border(
                                    width = if (backgroundColorArgb == bgArgb) 2.5.dp else 1.dp,
                                    color = if (backgroundColorArgb == bgArgb) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { backgroundColorArgb = bgArgb },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bgArgb == 0L) {
                                Text("/", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Time Duration Range Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) "من: ${VideoTimelineEngine.formatTimeMs(startMs)}" else "From: ${VideoTimelineEngine.formatTimeMs(startMs)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = if (isArabic) "إلى: ${VideoTimelineEngine.formatTimeMs(endMs)}" else "To: ${VideoTimelineEngine.formatTimeMs(endMs)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    RangeSlider(
                        value = startMs.toFloat()..endMs.toFloat(),
                        onValueChange = { range ->
                            val s = range.start.toLong().coerceIn(0L, totalDurationMs - 200L)
                            val e = range.endInclusive.toLong().coerceIn(s + 200L, totalDurationMs)
                            startMs = s
                            endMs = e
                        },
                        valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1000f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val overlay = (initialOverlay ?: VideoTimedOverlay(type = VideoOverlayType.TEXT)).copy(
                                text = text,
                                fontIndex = fontIndex,
                                isBold = isBold,
                                isItalic = isItalic,
                                textColorArgb = textColorArgb,
                                backgroundColorArgb = backgroundColorArgb,
                                startMs = startMs,
                                endMs = endMs
                            )
                            onConfirm(overlay)
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Text(if (isArabic) "حفظ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoStickerSheet(
    onDismiss: () -> Unit,
    onSelectSticker: (String) -> Unit,
    isArabic: Boolean = false
) {
    val stickers = listOf(
        "🔥", "❤️", "😂", "👍", "👏", "🎉",
        "✨", "🌟", "💡", "⚡", "🚀", "💯",
        "😍", "🥳", "🙌", "💪", "🎯", "👑",
        "🔴", "🟢", "⚠️", "❌", "✔️", "🕒"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "اختر ملصق أو إيموجي" else "Choose Sticker / Emoji",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(stickers) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onSelectSticker(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isArabic) "إغلاق" else "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoAudioSheet(
    originalVolume: Float,
    isOriginalMuted: Boolean,
    soundtrack: VideoAudioTrack?,
    onUpdateOriginalAudio: (volume: Float, isMuted: Boolean) -> Unit,
    onPickSoundtrack: () -> Unit,
    onUpdateSoundtrackVolume: (Float) -> Unit,
    onRemoveSoundtrack: () -> Unit,
    onDismiss: () -> Unit,
    isArabic: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isArabic) "التحكم بالصوت والموسيقى" else "Audio & Soundtrack Controls",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Original Audio Controls
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "صوت الفيديو الأصلي: ${(originalVolume * 100).toInt()}%" else "Original Audio: ${(originalVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isArabic) "كتم" else "Mute", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = isOriginalMuted,
                                onCheckedChange = { onUpdateOriginalAudio(originalVolume, it) }
                            )
                        }
                    }
                    Slider(
                        value = originalVolume,
                        onValueChange = { onUpdateOriginalAudio(it, isOriginalMuted) },
                        valueRange = 0f..1f,
                        enabled = !isOriginalMuted
                    )
                }

                HorizontalDivider()

                // Background Soundtrack Controls
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "الموسيقى التصويرية / الخلفية" else "Background Soundtrack",
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (soundtrack != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = soundtrack.displayName.ifEmpty { "Audio Track" },
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = onRemoveSoundtrack, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(
                            text = if (isArabic) "مستوى صوت الموسيقى: ${(soundtrack.volume * 100).toInt()}%" else "Soundtrack Volume: ${(soundtrack.volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = soundtrack.volume,
                            onValueChange = onUpdateSoundtrackVolume,
                            valueRange = 0f..1f
                        )
                    } else {
                        Button(
                            onClick = onPickSoundtrack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "إضافة مقطع صوتي من الهاتف" else "Add Audio from Device")
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) {
                        Text(if (isArabic) "تم" else "Done")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoSpeedSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit,
    isArabic: Boolean = false
) {
    val speedOptions = VideoSpeedRules.allowed

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "سرعة تشغيل المقطع" else "Clip Playback Speed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (speed in speedOptions) {
                        val isSelected = currentSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectSpeed(speed)
                                onDismiss()
                            },
                            label = { Text("${speed}x", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoFiltersSheet(
    currentFilterId: String,
    onSelectFilter: (String) -> Unit,
    onDismiss: () -> Unit,
    isArabic: Boolean = false
) {
    val presets = PhotoFilterPresets.all

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "فلاتر وتأثيرات الألوان" else "Color Filter Presets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(presets) { preset ->
                        val isSelected = currentFilterId == preset.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFilter(preset.id) }
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) {
                        Text(if (isArabic) "تم" else "Done")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoTuneSheet(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onUpdateTune: (b: Float, c: Float, s: Float) -> Unit,
    onDismiss: () -> Unit,
    isArabic: Boolean = false
) {
    var b by remember { mutableFloatStateOf(brightness) }
    var c by remember { mutableFloatStateOf(contrast) }
    var s by remember { mutableFloatStateOf(saturation) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "ضبط الإضاءة والألوان" else "Visual Tuning",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Brightness
                Text(
                    text = if (isArabic) "السطوع: ${(b * 100).toInt()}%" else "Brightness: ${(b * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = b,
                    onValueChange = {
                        b = it
                        onUpdateTune(b, c, s)
                    },
                    valueRange = -0.5f..0.5f
                )

                // Contrast
                Text(
                    text = if (isArabic) "التباين: ${"%.2f".format(c)}" else "Contrast: ${"%.2f".format(c)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = c,
                    onValueChange = {
                        c = it
                        onUpdateTune(b, c, s)
                    },
                    valueRange = 0.5f..2.0f
                )

                // Saturation
                Text(
                    text = if (isArabic) "التشبع: ${"%.2f".format(s)}" else "Saturation: ${"%.2f".format(s)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = s,
                    onValueChange = {
                        s = it
                        onUpdateTune(b, c, s)
                    },
                    valueRange = 0.0f..2.0f
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) {
                        Text(if (isArabic) "تم" else "Done")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoExportDialog(
    project: VideoStudioProject,
    busy: Boolean,
    exportProgress: Float?,
    onExportToGallery: (targetHeight: Int, bitrate: Int, aspectRatio: VideoAspectRatio) -> Unit,
    onExportToDocument: (targetHeight: Int, bitrate: Int, aspectRatio: VideoAspectRatio) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit,
    isArabic: Boolean = false
) {
    var targetHeight by remember { mutableIntStateOf(project.targetHeight) }
    var bitrate by remember { mutableIntStateOf(project.bitrate) }
    var aspectRatio by remember { mutableStateOf(project.aspectRatio) }

    val resolutions = listOf(
        VideoQualityRules.HEIGHT_AUTO to (if (isArabic) "تلقائي (أصلي)" else "Auto (Original)"),
        VideoQualityRules.HEIGHT_1080P to "1080p Full HD",
        VideoQualityRules.HEIGHT_720P to "720p HD",
        VideoQualityRules.HEIGHT_480P to "480p SD"
    )

    val bitrates = listOf(
        VideoQualityRules.AUTO to (if (isArabic) "تلقائي" else "Auto"),
        VideoQualityRules.HIGH to (if (isArabic) "عالي (10Mbps)" else "High (10Mbps)"),
        VideoQualityRules.BALANCED to (if (isArabic) "متوازن (5Mbps)" else "Balanced (5Mbps)"),
        VideoQualityRules.COMPACT to (if (isArabic) "مضغوط (2Mbps)" else "Compact (2Mbps)")
    )

    val ratios = listOf(
        VideoAspectRatio.ORIGINAL,
        VideoAspectRatio.RATIO_16_9,
        VideoAspectRatio.RATIO_9_16,
        VideoAspectRatio.RATIO_1_1,
        VideoAspectRatio.RATIO_4_3
    )

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isArabic) "خيارات تصدير الفيديو" else "Export Video",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (busy) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { exportProgress ?: 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (exportProgress != null) "${(exportProgress * 100).toInt()}%" else (if (isArabic) "جارٍ المعالجة والتصدير..." else "Processing export..."),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onCancelExport,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (isArabic) "إلغاء التصدير" else "Cancel Export")
                        }
                    }
                } else {
                    // Resolution selector
                    Text(if (isArabic) "دقة التصدير:" else "Resolution:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((height, label) in resolutions) {
                            FilterChip(
                                selected = targetHeight == height,
                                onClick = { targetHeight = height },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Aspect ratio selector
                    Text(if (isArabic) "نسبة العرض للارتفاع:" else "Aspect Ratio:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (r in ratios) {
                            FilterChip(
                                selected = aspectRatio == r,
                                onClick = { aspectRatio = r },
                                label = { Text(if (isArabic) r.labelAr else r.labelEn, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Quality / Bitrate selector
                    Text(if (isArabic) "معدل البت والجودة:" else "Bitrate / Quality:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((br, label) in bitrates) {
                            FilterChip(
                                selected = bitrate == br,
                                onClick = { bitrate = br },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExportToGallery(targetHeight, bitrate, aspectRatio) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "حفظ في معرض الصور (MediaStore)" else "Save to Gallery (MediaStore)")
                        }

                        OutlinedButton(
                            onClick = { onExportToDocument(targetHeight, bitrate, aspectRatio) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "حفظ كملف في مجلد محدد..." else "Save As File...")
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isArabic) "إلغاء" else "Cancel")
                        }
                    }
                }
            }
        }
    }
}
