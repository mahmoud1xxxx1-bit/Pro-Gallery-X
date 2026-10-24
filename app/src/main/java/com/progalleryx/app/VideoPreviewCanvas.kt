package com.progalleryx.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPreviewCanvas(
    exoPlayer: ExoPlayer?,
    project: VideoStudioProject,
    onTogglePlay: () -> Unit,
    onSelectOverlay: (String?) -> Unit,
    onUpdateOverlayPosition: (overlayId: String, normX: Float, normY: Float) -> Unit,
    onUpdateOverlayScaleRotation: (overlayId: String, scale: Float, rotation: Float) -> Unit,
    onEditOverlay: (VideoTimedOverlay) -> Unit,
    onDeleteOverlay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOverlays = VideoTimelineEngine.activeOverlaysAt(project.overlays, project.playheadMs)
    val currentMapping = VideoTimelineEngine.clipAtProjectTime(project.clips, project.playheadMs)
    val activeClip = currentMapping?.clip ?: project.clips.firstOrNull()

    // Determine aspect ratio modifier
    val ratioModifier = when (project.aspectRatio) {
        VideoAspectRatio.RATIO_16_9 -> Modifier.aspectRatio(16f / 9f)
        VideoAspectRatio.RATIO_9_16 -> Modifier.aspectRatio(9f / 16f)
        VideoAspectRatio.RATIO_1_1 -> Modifier.aspectRatio(1f)
        VideoAspectRatio.RATIO_4_3 -> Modifier.aspectRatio(4f / 3f)
        VideoAspectRatio.ORIGINAL -> Modifier.fillMaxSize()
    }

    // Color tuning calculations based on active clip settings
    val brightness = activeClip?.brightness ?: 0f
    val contrast = activeClip?.contrast ?: 1f
    val saturation = activeClip?.saturation ?: 1f
    val filterPreset = PhotoFilterPresets.find(activeClip?.filterId ?: "") ?: PhotoFilterPresets.ORIGINAL

    val combinedBrightness = brightness * 100f + filterPreset.brightness
    val combinedContrast = (contrast * filterPreset.contrast).coerceIn(0.5f, 1.5f)
    val combinedSaturation = (saturation * filterPreset.saturation).coerceIn(0f, 2f)

    val colorMatrix = remember(combinedBrightness, combinedContrast, combinedSaturation, filterPreset) {
        val matrix = ColorMatrix()
        // Saturation
        matrix.setToSaturation(combinedSaturation)
        // Contrast & Brightness
        val cbArray = PhotoColorRules.contrastBrightness(combinedContrast, combinedBrightness)
        matrix.timesAssign(ColorMatrix(cbArray))
        // Temperature & Tint
        val ttArray = PhotoColorRules.temperatureTint(filterPreset.temperature, filterPreset.tint)
        matrix.timesAssign(ColorMatrix(ttArray))
        matrix
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Aspect ratio container
        BoxWithConstraints(
            modifier = Modifier
                .then(ratioModifier)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onSelectOverlay(null)
                        },
                        onDoubleTap = {
                            onTogglePlay()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            // Video Player Surface
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate((activeClip?.rotationDegrees ?: 0).toFloat())
                )
            }

            // Interactive Timed Overlays Layer
            for (overlay in activeOverlays) {
                val isSelected = overlay.id == project.selectedOverlayId
                val offsetX = (canvasWidthPx * overlay.normalizedX) - (canvasWidthPx / 2f)
                val offsetY = (canvasHeightPx * overlay.normalizedY) - (canvasHeightPx / 2f)

                Box(
                    modifier = Modifier
                        .offset(
                            x = (maxWidth * overlay.normalizedX) - (maxWidth / 2f),
                            y = (maxHeight * overlay.normalizedY) - (maxHeight / 2f)
                        )
                        .scale(overlay.scale)
                        .rotate(overlay.rotationDegrees)
                        .pointerInput(overlay.id) {
                            detectDragGestures(
                                onDragStart = { onSelectOverlay(overlay.id) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newNormX = (overlay.normalizedX + (dragAmount.x / canvasWidthPx)).coerceIn(0.05f, 0.95f)
                                    val newNormY = (overlay.normalizedY + (dragAmount.y / canvasHeightPx)).coerceIn(0.05f, 0.95f)
                                    onUpdateOverlayPosition(overlay.id, newNormX, newNormY)
                                }
                            )
                        }
                        .pointerInput(overlay.id) {
                            detectTapGestures(
                                onTap = { onSelectOverlay(overlay.id) }
                            )
                        }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(if (isSelected) 6.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (overlay.type) {
                        VideoOverlayType.TEXT -> {
                            val fontFamily = when (overlay.fontIndex) {
                                1 -> FontFamily.Serif
                                2 -> FontFamily.Monospace
                                3 -> FontFamily.SansSerif
                                else -> FontFamily.Default
                            }
                            val fontWeight = if (overlay.isBold) FontWeight.Bold else FontWeight.Normal
                            val fontStyle = if (overlay.isItalic) FontStyle.Italic else FontStyle.Normal

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (overlay.backgroundColorArgb != 0L) Color(overlay.backgroundColorArgb) else Color.Transparent,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = overlay.text.ifEmpty { "Text" },
                                    color = Color(overlay.textColorArgb),
                                    fontSize = 18.sp,
                                    fontFamily = fontFamily,
                                    fontWeight = fontWeight,
                                    fontStyle = fontStyle,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        VideoOverlayType.STICKER -> {
                            Text(
                                text = overlay.stickerEmoji,
                                fontSize = 42.sp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    // Selection Corner Handles (Delete & Edit)
                    if (isSelected) {
                        // Delete Button (top end)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 10.dp, y = (-10).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable { onDeleteOverlay(overlay.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Overlay",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Edit Button (top start)
                        if (overlay.type == VideoOverlayType.TEXT) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-10).dp, y = (-10).dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onEditOverlay(overlay) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
