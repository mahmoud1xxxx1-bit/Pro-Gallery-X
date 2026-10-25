package com.progalleryx.app.studioplus.feature.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progalleryx.app.studioplus.feature.photoeditor.model.StudioPhotoEditState
import kotlin.math.roundToInt

enum class ToneAdjustType(val label: String, val icon: ImageVector) {
    BRIGHTNESS("الإضاءة", Icons.Default.Brightness6),
    CONTRAST("التباين", Icons.Default.Contrast),
    SATURATION("التشبع", Icons.Default.Palette),
    WARMTH("الدفيء", Icons.Default.WbSunny),
    VIGNETTE("الحواف", Icons.Default.Lens)
}

@Composable
fun AdjustmentsPanel(
    state: StudioPhotoEditState,
    onStateChange: (StudioPhotoEditState) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAdjust by remember { mutableStateOf(ToneAdjustType.BRIGHTNESS) }

    val currentValue = when (selectedAdjust) {
        ToneAdjustType.BRIGHTNESS -> state.brightness
        ToneAdjustType.CONTRAST -> state.contrast
        ToneAdjustType.SATURATION -> state.saturation
        ToneAdjustType.WARMTH -> state.warmth
        ToneAdjustType.VIGNETTE -> state.vignette
    }

    val valueRange = if (selectedAdjust == ToneAdjustType.VIGNETTE) 0f..100f else -100f..100f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Value indicator & Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rounded = currentValue.roundToInt()
            val textFormatted = if (rounded > 0 && selectedAdjust != ToneAdjustType.VIGNETTE) "+$rounded" else "$rounded"
            Text(
                text = textFormatted,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp)
            )

            Slider(
                value = currentValue,
                onValueChange = { newVal ->
                    val updated = when (selectedAdjust) {
                        ToneAdjustType.BRIGHTNESS -> state.copy(brightness = newVal)
                        ToneAdjustType.CONTRAST -> state.copy(contrast = newVal)
                        ToneAdjustType.SATURATION -> state.copy(saturation = newVal)
                        ToneAdjustType.WARMTH -> state.copy(warmth = newVal)
                        ToneAdjustType.VIGNETTE -> state.copy(vignette = newVal)
                    }
                    onStateChange(updated)
                },
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2E6EEF),
                    activeTrackColor = Color(0xFF2E6EEF),
                    inactiveTrackColor = Color(0xFF2B2F33)
                )
            )

            // Reset this specific adjustment
            TextButton(
                onClick = {
                    val updated = when (selectedAdjust) {
                        ToneAdjustType.BRIGHTNESS -> state.copy(brightness = 0f)
                        ToneAdjustType.CONTRAST -> state.copy(contrast = 0f)
                        ToneAdjustType.SATURATION -> state.copy(saturation = 0f)
                        ToneAdjustType.WARMTH -> state.copy(warmth = 0f)
                        ToneAdjustType.VIGNETTE -> state.copy(vignette = 0f)
                    }
                    onStateChange(updated)
                },
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("0", color = Color(0xFF9E9E9E), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Tone Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToneAdjustType.values().forEach { adjustType ->
                val isSelected = selectedAdjust == adjustType
                val hasValue = when (adjustType) {
                    ToneAdjustType.BRIGHTNESS -> state.brightness != 0f
                    ToneAdjustType.CONTRAST -> state.contrast != 0f
                    ToneAdjustType.SATURATION -> state.saturation != 0f
                    ToneAdjustType.WARMTH -> state.warmth != 0f
                    ToneAdjustType.VIGNETTE -> state.vignette != 0f
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFF2E6EEF) else Color(0xFF1E222A))
                        .clickable { selectedAdjust = adjustType }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = adjustType.icon,
                        contentDescription = adjustType.label,
                        tint = if (isSelected || hasValue) Color.White else Color(0xFFB0B0B0),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = adjustType.label,
                        color = if (isSelected || hasValue) Color.White else Color(0xFFB0B0B0),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
