package com.progalleryx.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PhotoTextEditDialog(
    initialLayer: InteractiveTextLayer? = null,
    onDismiss: () -> Unit,
    onConfirm: (InteractiveTextLayer) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialLayer?.text ?: "") }
    var fontStyle by remember { mutableStateOf(initialLayer?.fontStyle ?: TextFontStyle.DEFAULT) }
    var backgroundStyle by remember { mutableStateOf(initialLayer?.backgroundStyle ?: TextBackgroundStyle.TRANSPARENT) }
    var textAlign by remember { mutableStateOf(initialLayer?.textAlign ?: TextAlignment.CENTER) }
    var colorArgb by remember { mutableLongStateOf(initialLayer?.colorArgb ?: 0xFFFFFFFFL) }
    var backgroundColorArgb by remember { mutableLongStateOf(initialLayer?.backgroundColorArgb ?: 0xCC121212L) }
    var fontSizeSp by remember { mutableFloatStateOf(initialLayer?.fontSizeSp ?: 28f) }
    var opacity by remember { mutableFloatStateOf(initialLayer?.opacity ?: 1.0f) }
    var customHex by remember { mutableStateOf(ColorPaletteRules.formatHexColor(colorArgb)) }

    val resolvedFontFamily = when (fontStyle) {
        TextFontStyle.DEFAULT -> FontFamily.SansSerif
        TextFontStyle.SERIF -> FontFamily.Serif
        TextFontStyle.MONOSPACE -> FontFamily.Monospace
        TextFontStyle.BOLD_HEADING -> FontFamily.SansSerif
        TextFontStyle.CASUAL_HANDWRITING -> FontFamily.Cursive
    }
    val resolvedFontWeight = when (fontStyle) {
        TextFontStyle.BOLD_HEADING -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                    }
                    Text(
                        if (initialLayer == null) "إضافة نص جديد" else "تعديل النص",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                val target = (initialLayer ?: InteractiveTextLayer(text = text)).copy(
                                    text = text,
                                    fontStyle = fontStyle,
                                    backgroundStyle = backgroundStyle,
                                    textAlign = textAlign,
                                    colorArgb = colorArgb,
                                    backgroundColorArgb = backgroundColorArgb,
                                    fontSizeSp = fontSizeSp,
                                    opacity = opacity
                                )
                                onConfirm(target)
                            }
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "تأكيد", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Live Preview Canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E242B))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val previewTextColor = Color(colorArgb).copy(alpha = opacity)
                    val previewBgColor = when (backgroundStyle) {
                        TextBackgroundStyle.TRANSPARENT -> Color.Transparent
                        TextBackgroundStyle.FILLED_BOX -> Color(backgroundColorArgb).copy(alpha = opacity)
                        TextBackgroundStyle.OUTLINE -> Color.Transparent
                        TextBackgroundStyle.SEMI_TRANSPARENT -> Color.Black.copy(alpha = 0.5f * opacity)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewBgColor)
                            .then(
                                if (backgroundStyle == TextBackgroundStyle.OUTLINE) {
                                    Modifier.border(2.dp, previewTextColor, RoundedCornerShape(8.dp))
                                } else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (text.isBlank()) "معاينة النص هنا" else text,
                            color = if (text.isBlank()) Color.Gray else previewTextColor,
                            fontSize = (fontSizeSp * 0.75f).sp,
                            fontFamily = resolvedFontFamily,
                            fontWeight = resolvedFontWeight,
                            textAlign = when (textAlign) {
                                TextAlignment.START -> TextAlign.Start
                                TextAlignment.CENTER -> TextAlign.Center
                                TextAlignment.END -> TextAlign.End
                            }
                        )
                    }
                }

                // Input Field
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("اكتب النص...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                // Font Style Chips
                Text("نمط الخط", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TextFontStyle.DEFAULT to "عادي (Roboto)",
                        TextFontStyle.SERIF to "كلاسيكي (Serif)",
                        TextFontStyle.MONOSPACE to "ثابت (Mono)",
                        TextFontStyle.BOLD_HEADING to "عريض (Bold)",
                        TextFontStyle.CASUAL_HANDWRITING to "يدوي (Cursive)"
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = fontStyle == style,
                            onClick = { fontStyle = style },
                            label = { Text(label) }
                        )
                    }
                }

                // Background Style Chips
                Text("خلفية النص", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TextBackgroundStyle.TRANSPARENT to "بدون خلفية",
                        TextBackgroundStyle.FILLED_BOX to "مربع ممتلئ",
                        TextBackgroundStyle.OUTLINE to "إطار محيط",
                        TextBackgroundStyle.SEMI_TRANSPARENT to "نصف شفاف"
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = backgroundStyle == style,
                            onClick = {
                                backgroundStyle = style
                                if (style == TextBackgroundStyle.FILLED_BOX && backgroundColorArgb == 0x00000000L) {
                                    backgroundColorArgb = 0xFF121212L
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                // Alignment Selector
                Text("المحاذاة", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { textAlign = TextAlignment.START },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (textAlign == TextAlignment.START) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(Icons.Default.FormatAlignRight, contentDescription = "يمين")
                    }
                    IconButton(
                        onClick = { textAlign = TextAlignment.CENTER },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (textAlign == TextAlignment.CENTER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(Icons.Default.FormatAlignCenter, contentDescription = "وسط")
                    }
                    IconButton(
                        onClick = { textAlign = TextAlignment.END },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (textAlign == TextAlignment.END) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(Icons.Default.FormatAlignLeft, contentDescription = "يسار")
                    }
                }

                // Color Swatches Palette
                Text("لون النص", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorPaletteRules.STUDIO_COLORS.forEach { colorVal ->
                        val isSelected = colorArgb == colorVal
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorArgb = colorVal
                                    customHex = ColorPaletteRules.formatHexColor(colorVal)
                                }
                        )
                    }
                }

                // If filled box background, also show background box color swatches
                if (backgroundStyle == TextBackgroundStyle.FILLED_BOX) {
                    Text("لون خلفية المربع", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorPaletteRules.STUDIO_COLORS.forEach { colorVal ->
                            val isSelected = backgroundColorArgb == colorVal
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        backgroundColorArgb = colorVal
                                        colorArgb = ColorPaletteRules.getContrastingTextColor(colorVal)
                                    }
                            )
                        }
                    }
                }

                // Custom HEX Color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customHex,
                        onValueChange = {
                            customHex = it
                            ColorPaletteRules.parseHexColor(it)?.let { parsed ->
                                colorArgb = parsed
                            }
                        },
                        label = { Text("رمز HEX مخصص (#RRGGBB)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(colorArgb))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                }

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حجم الخط", style = MaterialTheme.typography.bodyMedium)
                    Text("${fontSizeSp.toInt()} sp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = fontSizeSp,
                    onValueChange = { fontSizeSp = it },
                    valueRange = 16f..72f
                )

                // Opacity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الشفافية", style = MaterialTheme.typography.bodyMedium)
                    Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.2f..1.0f
                )

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDelete != null && initialLayer != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                            Spacer(Modifier.width(4.dp))
                            Text("حذف")
                        }
                    }
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                val target = (initialLayer ?: InteractiveTextLayer(text = text)).copy(
                                    text = text,
                                    fontStyle = fontStyle,
                                    backgroundStyle = backgroundStyle,
                                    textAlign = textAlign,
                                    colorArgb = colorArgb,
                                    backgroundColorArgb = backgroundColorArgb,
                                    fontSizeSp = fontSizeSp,
                                    opacity = opacity
                                )
                                onConfirm(target)
                            }
                        },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (initialLayer == null) "إضافة النص" else "حفظ التعديلات")
                    }
                }
            }
        }
    }
}
