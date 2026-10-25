package com.progalleryx.app.studioplus.feature.photoeditor.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progalleryx.app.studioplus.feature.photoeditor.engine.StudioBitmapProcessor
import com.progalleryx.app.studioplus.feature.photoeditor.engine.StudioImageLoader
import com.progalleryx.app.studioplus.feature.photoeditor.model.*
import com.progalleryx.app.studioplus.feature.photoeditor.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun StudioPhotoEditorScreen(
    imageUri: Uri,
    onClose: () -> Unit,
    onSaved: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Undo / Redo History Stack
    val historyStack = remember { mutableStateListOf(StudioPhotoEditState()) }
    var historyIndex by remember { mutableIntStateOf(0) }

    val currentState = historyStack.getOrElse(historyIndex) { StudioPhotoEditState() }

    fun pushState(newState: StudioPhotoEditState) {
        // Discard forward history if we are in the middle of the stack
        while (historyStack.size > historyIndex + 1) {
            historyStack.removeAt(historyStack.lastIndex)
        }
        historyStack.add(newState)
        if (historyStack.size > 30) {
            historyStack.removeAt(0)
        }
        historyIndex = historyStack.lastIndex
    }

    val canUndo = historyIndex > 0
    val canRedo = historyIndex < historyStack.lastIndex

    // Doodle tool states
    var currentDoodleTool by remember { mutableStateOf(DoodleToolMode.PEN) }
    var currentDoodleColor by remember { mutableIntStateOf(android.graphics.Color.parseColor("#2E6EEF")) }
    var currentDoodleThickness by remember { mutableFloatStateOf(0.016f) }

    // Load preview bitmap on initial entry
    LaunchedEffect(imageUri) {
        isLoadingImage = true
        previewBitmap = StudioImageLoader.loadPreviewBitmap(context, imageUri, 2048)
        isLoadingImage = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black,
        topBar = {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cancel Button
                IconButton(
                    onClick = {
                        if (currentState.hasModifications) {
                            showDiscardDialog = true
                        } else {
                            onClose()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إلغاء",
                        tint = Color.White
                    )
                }

                // Undo / Redo Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            if (canUndo) historyIndex--
                        },
                        enabled = canUndo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "تراجع",
                            tint = if (canUndo) Color.White else Color(0xFF555555)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (canRedo) historyIndex++
                        },
                        enabled = canRedo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "إعادة",
                            tint = if (canRedo) Color.White else Color(0xFF555555)
                        )
                    }
                }

                // Save Action Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isExporting = true
                            val result = StudioBitmapProcessor.exportImage(
                                context = context,
                                sourceUri = imageUri,
                                state = currentState,
                                saveAsCopy = true
                            )
                            isExporting = false
                            result.fold(
                                onSuccess = { savedUri ->
                                    Toast.makeText(context, "تم حفظ الصورة بنجاح في الاستديو", Toast.LENGTH_SHORT).show()
                                    onSaved(savedUri)
                                },
                                onFailure = {
                                    Toast.makeText(context, "فشل حفظ الصورة: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    enabled = !isExporting && !isLoadingImage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6EEF)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "حفظ",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color(0xFF14171C))
            ) {
                // Active Tool Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                ) {
                    when (currentState.activeTab) {
                        StudioEditorTab.TRANSFORM -> {
                            CropTransformPanel(
                                state = currentState,
                                onStateChange = { pushState(it) }
                            )
                        }
                        StudioEditorTab.FILTERS -> {
                            FiltersPanel(
                                state = currentState,
                                onStateChange = { pushState(it) }
                            )
                        }
                        StudioEditorTab.ADJUST -> {
                            AdjustmentsPanel(
                                state = currentState,
                                onStateChange = { pushState(it) }
                            )
                        }
                        StudioEditorTab.TEXT -> {
                            TypographyPanel(
                                state = currentState,
                                onStateChange = { pushState(it) }
                            )
                        }
                        StudioEditorTab.DOODLE -> {
                            DoodlePanel(
                                state = currentState,
                                currentTool = currentDoodleTool,
                                onToolChange = { currentDoodleTool = it },
                                currentColor = currentDoodleColor,
                                onColorChange = { currentDoodleColor = it },
                                currentThicknessRelative = currentDoodleThickness,
                                onThicknessChange = { currentDoodleThickness = it },
                                onClearDoodles = {
                                    pushState(currentState.copy(doodlePaths = emptyList()))
                                },
                                onUndoDoodle = {
                                    if (currentState.doodlePaths.isNotEmpty()) {
                                        pushState(currentState.copy(doodlePaths = currentState.doodlePaths.dropLast(1)))
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF252932), thickness = 1.dp)

                // Tool Switcher Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioEditorTab.values().forEach { tab ->
                        val isSelected = currentState.activeTab == tab
                        val icon = when (tab) {
                            StudioEditorTab.TRANSFORM -> Icons.Default.Crop
                            StudioEditorTab.FILTERS -> Icons.Default.AutoAwesome
                            StudioEditorTab.ADJUST -> Icons.Default.Tune
                            StudioEditorTab.TEXT -> Icons.Default.TextFields
                            StudioEditorTab.DOODLE -> Icons.Default.Brush
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    pushState(currentState.copy(activeTab = tab))
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.labelAr,
                                tint = if (isSelected) Color(0xFF2E6EEF) else Color(0xFF8E929B),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tab.labelAr,
                                color = if (isSelected) Color(0xFF2E6EEF) else Color(0xFF8E929B),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingImage) {
                CircularProgressIndicator(color = Color(0xFF2E6EEF))
            } else {
                EditorCanvasView(
                    previewBitmap = previewBitmap,
                    state = currentState,
                    currentDoodleTool = currentDoodleTool,
                    currentDoodleColor = currentDoodleColor,
                    currentDoodleThickness = currentDoodleThickness,
                    onAddDoodlePath = { newPath ->
                        pushState(currentState.copy(doodlePaths = currentState.doodlePaths + newPath))
                    },
                    onUpdateTextPosition = { layerId, dx, dy ->
                        val updated = currentState.textLayers.map { layer ->
                            if (layer.id == layerId) {
                                layer.copy(
                                    normalizedX = (layer.normalizedX + dx).coerceIn(0.1f, 0.9f),
                                    normalizedY = (layer.normalizedY + dy).coerceIn(0.1f, 0.9f)
                                )
                            } else layer
                        }
                        pushState(currentState.copy(textLayers = updated))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF2E6EEF))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("جارٍ حفظ الصورة بأعلى دقة...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Discard Confirmation Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("تجاهل التعديلات؟", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("لديك تعديلات غير محفوظة، هل تريد الخروج بدون حفظ؟", color = Color(0xFFCCCCCC)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("تجاهل وخروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("البقاء", color = Color(0xFF9E9E9E))
                }
            },
            containerColor = Color(0xFF1E222A),
            shape = RoundedCornerShape(24.dp)
        )
    }
}
