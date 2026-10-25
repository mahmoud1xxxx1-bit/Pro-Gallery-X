package com.progalleryx.app.studioplus.feature.photoeditor.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progalleryx.app.studioplus.feature.photoeditor.model.StudioPhotoEditState

enum class DoodleToolMode(val label: String) {
    PEN("قلم"),
    HIGHLIGHTER("قلم فسفوري"),
    ERASER("ممحاة")
}

@Composable
fun DoodlePanel(
    state: StudioPhotoEditState,
    currentTool: DoodleToolMode,
    onToolChange: (DoodleToolMode) -> Unit,
    currentColor: Int,
    onColorChange: (Int) -> Unit,
    currentThicknessRelative: Float,
    onThicknessChange: (Float) -> Unit,
    onClearDoodles: () -> Unit,
    onUndoDoodle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(
            AndroidColor.parseColor("#2E6EEF"), // Royal Blue
            AndroidColor.parseColor("#E53935"), // Red
            AndroidColor.parseColor("#FB8C00"), // Orange
            AndroidColor.parseColor("#FDD835"), // Yellow
            AndroidColor.parseColor("#43A047"), // Green
            AndroidColor.parseColor("#00ACC1"), // Cyan
            AndroidColor.parseColor("#8E24AA"), // Purple
            AndroidColor.WHITE,
            AndroidColor.BLACK
        )
    }

    val thicknesses = remember {
        listOf(
            "رفيع" to 0.008f,
            "متوسط" to 0.016f,
            "عريض" to 0.028f
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tool Mode Row (Pen / Highlighter / Eraser / Undo / Clear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DoodleToolMode.values().forEach { mode ->
                    val isSelected = currentTool == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF2E6EEF) else Color(0xFF1E222A))
                            .clickable { onToolChange(mode) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (state.doodlePaths.isNotEmpty()) {
                IconButton(onClick = onClearDoodles) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "مسح الكل",
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Thickness Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "السُمك:",
                color = Color(0xFFB0B0B0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            thicknesses.forEach { (label, thickness) ->
                val isSelected = currentThicknessRelative == thickness
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF2E6EEF) else Color(0xFF1E222A))
                        .clickable { onThicknessChange(thickness) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (currentTool != DoodleToolMode.ERASER) {
            Spacer(modifier = Modifier.height(10.dp))

            // Color Palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { colorInt ->
                    val isSelected = currentColor == colorInt
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2E6EEF) else Color.Gray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { onColorChange(colorInt) }
                    )
                }
            }
        }
    }
}
