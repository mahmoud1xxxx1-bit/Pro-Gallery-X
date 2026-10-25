package com.progalleryx.app.studioplus.feature.photoeditor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progalleryx.app.studioplus.feature.photoeditor.model.StudioFilterType
import com.progalleryx.app.studioplus.feature.photoeditor.model.StudioPhotoEditState
import kotlin.math.roundToInt

@Composable
fun FiltersPanel(
    state: StudioPhotoEditState,
    onStateChange: (StudioPhotoEditState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Intensity Slider (visible when a filter is chosen)
        AnimatedVisibility(
            visible = state.filterType != StudioFilterType.ORIGINAL,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(state.filterIntensity * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(42.dp)
                )

                Slider(
                    value = state.filterIntensity,
                    onValueChange = { intensity ->
                        onStateChange(state.copy(filterIntensity = intensity))
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2E6EEF),
                        activeTrackColor = Color(0xFF2E6EEF),
                        inactiveTrackColor = Color(0xFF2B2F33)
                    )
                )
            }
        }

        // Horizontal filter presets strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StudioFilterType.values().forEach { filter ->
                val isSelected = state.filterType == filter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onStateChange(state.copy(filterType = filter))
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 68.dp, height = 68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(getFilterPreviewBgColor(filter))
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, Color(0xFF2E6EEF), RoundedCornerShape(16.dp))
                                } else {
                                    Modifier.border(1.dp, Color(0xFF2B2F33), RoundedCornerShape(16.dp))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.labelAr.take(2),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = filter.labelAr,
                        color = if (isSelected) Color(0xFF2E6EEF) else Color(0xFFB0B0B0),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun getFilterPreviewBgColor(filter: StudioFilterType): Color {
    return when (filter) {
        StudioFilterType.ORIGINAL -> Color(0xFF2B2F33)
        StudioFilterType.VIVID -> Color(0xFF1E3A8A)
        StudioFilterType.WARM -> Color(0xFF9A3412)
        StudioFilterType.COOL -> Color(0xFF0E7490)
        StudioFilterType.NOIR -> Color(0xFF374151)
        StudioFilterType.SEPIA -> Color(0xFF78350F)
        StudioFilterType.VINTAGE -> Color(0xFF713F12)
        StudioFilterType.CINEMATIC -> Color(0xFF134E4A)
        StudioFilterType.CRISP -> Color(0xFF1E293B)
        StudioFilterType.SUNSET -> Color(0xFF831843)
    }
}
