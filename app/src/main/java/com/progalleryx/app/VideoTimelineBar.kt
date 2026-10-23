package com.progalleryx.app

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VideoTimelineBar(
    project: VideoStudioProject,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectClip: (String?) -> Unit,
    onSelectOverlay: (String?) -> Unit,
    onSplitClipAtPlayhead: () -> Unit,
    onDuplicateClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onRotateClip: () -> Unit,
    onTrimClip: (clipId: String, newStartMs: Long, newEndMs: Long) -> Unit,
    onAddClipClick: () -> Unit,
    isArabic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalDurationMs = project.totalDurationMs.coerceAtLeast(1L)
    val playheadMs = project.playheadMs.coerceIn(0L, totalDurationMs)
    val selectedClip = project.clips.firstOrNull { it.id == project.selectedClipId }
    val selectedOverlay = project.overlays.firstOrNull { it.id == project.selectedOverlayId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // --- 1. Top Control Bar: Time badge, Play/Pause, Split, Duplicate, Rotate, Delete ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause & Time Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "${VideoTimelineEngine.formatTimeMs(playheadMs)} / ${VideoTimelineEngine.formatTimeMs(totalDurationMs)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Action Quick Buttons for Selected Clip / Global
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Split Button
                IconButton(
                    onClick = onSplitClipAtPlayhead,
                    enabled = selectedClip != null && selectedClip.rawTrimDurationMs >= 1000L,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = if (isArabic) "قص عند المؤشر" else "Split at playhead",
                        tint = if (selectedClip != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Rotate Button
                IconButton(
                    onClick = onRotateClip,
                    enabled = selectedClip != null,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.RotateRight,
                        contentDescription = if (isArabic) "تدوير" else "Rotate",
                        tint = if (selectedClip != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duplicate Button
                IconButton(
                    onClick = onDuplicateClip,
                    enabled = selectedClip != null,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = if (isArabic) "تكرار المقطع" else "Duplicate",
                        tint = if (selectedClip != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClip,
                    enabled = (selectedClip != null && project.clips.size > 1) || selectedOverlay != null,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = if (isArabic) "حذف" else "Delete",
                        tint = if ((selectedClip != null && project.clips.size > 1) || selectedOverlay != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Add Clip Button
                IconButton(
                    onClick = onAddClipClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = if (isArabic) "إضافة مقطع" else "Add Clip",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 2. Interactive Timeline Strip Container ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
        ) {
            val availableWidthPx = constraints.maxWidth.toFloat()
            val availableWidthDp = maxWidth

            // Timeline Tracks Column (Overlays, Clips, Audio)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                // Track 1: Timed Overlays Strip (Height 20dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                ) {
                    for (overlay in project.overlays) {
                        val startFraction = VideoTimelineEngine.timeToFraction(overlay.startMs, totalDurationMs)
                        val endFraction = VideoTimelineEngine.timeToFraction(overlay.endMs, totalDurationMs)
                        val overlayWidthFraction = (endFraction - startFraction).coerceAtLeast(0.04f)
                        val isSelected = overlay.id == project.selectedOverlayId

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(overlayWidthFraction)
                                .offset(x = availableWidthDp * startFraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                                .clickable { onSelectOverlay(overlay.id) }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (overlay.type == VideoOverlayType.TEXT) overlay.text.ifEmpty { "Text" } else overlay.stickerEmoji,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                maxLines = 1,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Track 2: Video Clips Track (Height 56dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for ((index, clip) in project.clips.withIndex()) {
                        val clipWeight = (clip.effectiveDurationMs.toFloat() / totalDurationMs.toFloat()).coerceAtLeast(0.05f)
                        val isSelected = clip.id == project.selectedClipId

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(clipWeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectClip(clip.id) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            // Clip Info Badges
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (clip.rotationDegrees != 0) {
                                        Text(
                                            text = "${clip.rotationDegrees}°",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${"%.1f".format(clip.effectiveDurationMs / 1000f)}s",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (clip.speed != 1f) {
                                        Text(
                                            text = "${clip.speed}x",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Track 3: Audio Soundtrack Track (Height 18dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                ) {
                    val soundtrack = project.soundtrack
                    if (soundtrack != null && !soundtrack.isMuted) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Soundtrack",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = soundtrack.displayName.ifEmpty { if (isArabic) "موسيقى الخلفية" else "Soundtrack" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (project.isMasterOriginalMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Audio State",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (project.isMasterOriginalMuted) (if (isArabic) "صوت الفيديو الأصلي مكتوم" else "Original audio muted") else (if (isArabic) "صوت الفيديو الأصلي" else "Original audio"),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Draggable Red Playhead Line & Cursor
            val playheadFraction = VideoTimelineEngine.timeToFraction(playheadMs, totalDurationMs)
            val playheadOffsetDp = availableWidthDp * playheadFraction

            // Red Playhead Line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset(x = playheadOffsetDp)
                    .background(Color.Red)
            )

            // Red Playhead Drag Knob at Top
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = playheadOffsetDp - 7.dp, y = 0.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )

            // Transparent overlay covering entire timeline strip to capture tactile Scrubbing / Dragging
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalDurationMs, availableWidthPx) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val fraction = (offset.x / availableWidthPx).coerceIn(0f, 1f)
                                onSeek(VideoTimelineEngine.fractionToTime(fraction, totalDurationMs))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / availableWidthPx).coerceIn(0f, 1f)
                                onSeek(VideoTimelineEngine.fractionToTime(fraction, totalDurationMs))
                            }
                        )
                    }
            )
        }

        // --- 3. Trim Handles Panel when a clip is selected ---
        if (selectedClip != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "ضبط بداية المقطع: ${"%.1f".format(selectedClip.startMs / 1000f)}ث" else "Trim Start: ${"%.1f".format(selectedClip.startMs / 1000f)}s",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = if (isArabic) "نهاية المقطع: ${"%.1f".format(selectedClip.endMs / 1000f)}ث" else "Trim End: ${"%.1f".format(selectedClip.endMs / 1000f)}s",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Interactive range sliders for selected clip trim
            RangeSlider(
                value = (selectedClip.startMs.toFloat())..(selectedClip.endMs.toFloat()),
                onValueChange = { range ->
                    val minMs = VideoTimelineEngine.MIN_CLIP_DURATION_MS.toFloat()
                    val newStart = range.start.toLong().coerceIn(0L, (selectedClip.originalDurationMs - minMs.toLong()))
                    val newEnd = range.endInclusive.toLong().coerceIn((newStart + minMs.toLong()), selectedClip.originalDurationMs)
                    onTrimClip(selectedClip.id, newStart, newEnd)
                },
                valueRange = 0f..selectedClip.originalDurationMs.toFloat().coerceAtLeast(1000f),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            )
        }
    }
}
