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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progalleryx.app.studioplus.feature.photoeditor.model.*

@Composable
fun TypographyPanel(
    state: StudioPhotoEditState,
    onStateChange: (StudioPhotoEditState) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddTextDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    val selectedLayer = state.textLayers.find { it.id == state.selectedTextLayerId }
        ?: state.textLayers.lastOrNull()

    val colors = remember {
        listOf(
            AndroidColor.WHITE,
            AndroidColor.BLACK,
            AndroidColor.parseColor("#E53935"), // Red
            AndroidColor.parseColor("#FB8C00"), // Orange
            AndroidColor.parseColor("#FDD835"), // Gold
            AndroidColor.parseColor("#43A047"), // Green
            AndroidColor.parseColor("#00ACC1"), // Cyan
            AndroidColor.parseColor("#2E6EEF"), // Royal Blue
            AndroidColor.parseColor("#8E24AA"), // Purple
            AndroidColor.parseColor("#D81B60")  // Magenta
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Add Text button & Delete button (if layer exists)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    inputText = ""
                    showAddTextDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6EEF)),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة نص",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة نص جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (selectedLayer != null) {
                IconButton(
                    onClick = {
                        val remaining = state.textLayers.filter { it.id != selectedLayer.id }
                        onStateChange(
                            state.copy(
                                textLayers = remaining,
                                selectedTextLayerId = remaining.lastOrNull()?.id
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف النص",
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        }

        if (selectedLayer != null) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Font Style Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudioTextStyle.values().forEach { fontStyle ->
                    val isSelected = selectedLayer.style == fontStyle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF2E6EEF) else Color(0xFF1E222A))
                            .clickable {
                                val updated = state.textLayers.map {
                                    if (it.id == selectedLayer.id) it.copy(style = fontStyle) else it
                                }
                                onStateChange(state.copy(textLayers = updated))
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = fontStyle.labelAr,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Background Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudioTextBackgroundMode.values().forEach { bgMode ->
                    val isSelected = selectedLayer.backgroundMode == bgMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF2E6EEF) else Color(0xFF1E222A))
                            .clickable {
                                val updated = state.textLayers.map {
                                    if (it.id == selectedLayer.id) it.copy(backgroundMode = bgMode) else it
                                }
                                onStateChange(state.copy(textLayers = updated))
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = bgMode.labelAr,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Color Palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { colorInt ->
                    val isSelected = selectedLayer.textColor == colorInt
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
                            .clickable {
                                val updated = state.textLayers.map {
                                    if (it.id == selectedLayer.id) it.copy(textColor = colorInt) else it
                                }
                                onStateChange(state.copy(textLayers = updated))
                            }
                    )
                }
            }
        }
    }

    // Add/Edit Text Dialog with Arabic RTL support
    if (showAddTextDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = {
                Text(
                    text = "كتابة نص جديد",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("اكتب نصاً هنا...", color = Color(0xFF888888)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2E6EEF),
                        unfocusedBorderColor = Color(0xFF2B2F33)
                    ),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val newLayer = StudioTextLayer(text = inputText.trim())
                            onStateChange(
                                state.copy(
                                    textLayers = state.textLayers + newLayer,
                                    selectedTextLayerId = newLayer.id
                                )
                            )
                        }
                        showAddTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6EEF))
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) {
                    Text("إلغاء", color = Color(0xFF9E9E9E))
                }
            },
            containerColor = Color(0xFF1E222A),
            shape = RoundedCornerShape(24.dp)
        )
    }
}
