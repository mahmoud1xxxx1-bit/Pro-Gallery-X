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
import com.progalleryx.app.studioplus.feature.photoeditor.model.StudioCropRect
import com.progalleryx.app.studioplus.feature.photoeditor.model.StudioPhotoEditState
import kotlin.math.roundToInt

data class AspectRatioPreset(val label: String, val ratio: Float?)

@Composable
fun CropTransformPanel(
    state: StudioPhotoEditState,
    onStateChange: (StudioPhotoEditState) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember {
        listOf(
            AspectRatioPreset("حر", null),
            AspectRatioPreset("1:1", 1.0f),
            AspectRatioPreset("4:3", 4f / 3f),
            AspectRatioPreset("3:4", 3f / 4f),
            AspectRatioPreset("16:9", 16f / 9f),
            AspectRatioPreset("9:16", 9f / 16f)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Aspect Ratio Presets Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = state.targetAspectRatio == preset.ratio
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFF2E6EEF) else Color(0xFF1E222A))
                        .clickable {
                            val newCrop = if (preset.ratio == null) {
                                StudioCropRect()
                            } else {
                                // Calculate centered crop for ratio
                                val r = preset.ratio
                                if (r >= 1.0f) {
                                    val h = (1.0f / r).coerceIn(0.2f, 1.0f)
                                    val t = (1.0f - h) / 2f
                                    StudioCropRect(0f, t, 1f, t + h)
                                } else {
                                    val w = r.coerceIn(0.2f, 1.0f)
                                    val l = (1.0f - w) / 2f
                                    StudioCropRect(l, 0f, l + w, 1f)
                                }
                            }
                            onStateChange(
                                state.copy(
                                    targetAspectRatio = preset.ratio,
                                    cropRect = newCrop
                                )
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Action buttons: Rotate 90, Flip H, Flip V, Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransformActionButton(
                icon = Icons.Default.RotateRight,
                label = "تدوير 90°",
                onClick = {
                    onStateChange(state.copy(rotationSteps90 = (state.rotationSteps90 + 1) % 4))
                }
            )

            TransformActionButton(
                icon = Icons.Default.Flip,
                label = "عكس أفقي",
                isActive = state.flipHorizontal,
                onClick = {
                    onStateChange(state.copy(flipHorizontal = !state.flipHorizontal))
                }
            )

            TransformActionButton(
                icon = Icons.Default.SwapVert,
                label = "عكس رأسي",
                isActive = state.flipVertical,
                onClick = {
                    onStateChange(state.copy(flipVertical = !state.flipVertical))
                }
            )

            TransformActionButton(
                icon = Icons.Default.RestartAlt,
                label = "إعادة ضبط",
                onClick = {
                    onStateChange(
                        state.copy(
                            rotationSteps90 = 0,
                            fineRotationAngle = 0f,
                            flipHorizontal = false,
                            flipVertical = false,
                            cropRect = StudioCropRect(),
                            targetAspectRatio = null
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Fine Angle Slider (-45°..+45°)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${state.fineRotationAngle.roundToInt()}°",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(42.dp)
            )

            Slider(
                value = state.fineRotationAngle,
                onValueChange = { angle ->
                    onStateChange(state.copy(fineRotationAngle = angle))
                },
                valueRange = -45f..45f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2E6EEF),
                    activeTrackColor = Color(0xFF2E6EEF),
                    inactiveTrackColor = Color(0xFF2B2F33)
                )
            )

            TextButton(
                onClick = { onStateChange(state.copy(fineRotationAngle = 0f)) },
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("0°", color = Color(0xFF9E9E9E), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TransformActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isActive) Color(0xFF2E6EEF) else Color(0xFF1E222A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) Color(0xFF2E6EEF) else Color(0xFFB0B0B0),
            fontSize = 11.sp
        )
    }
}
