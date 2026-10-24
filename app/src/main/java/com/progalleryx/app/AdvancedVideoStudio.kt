package com.progalleryx.app

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VideoStudioTab {
    NONE,
    TEXT,
    STICKERS,
    AUDIO,
    SPEED,
    FILTERS,
    TUNE,
    EXPORT
}

@Composable
fun AdvancedVideoStudio(
    initialVideoUri: Uri?,
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    onClose: () -> Unit,
    isArabic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Master Project State ---
    var project by remember { mutableStateOf(VideoStudioProject()) }
    var undoStack by remember { mutableStateOf<List<VideoStudioProject>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<VideoStudioProject>>(emptyList()) }

    // --- Active Bottom Sheet / Dialog State ---
    var activeTab by remember { mutableStateOf(VideoStudioTab.NONE) }
    var editingOverlay by remember { mutableStateOf<VideoTimedOverlay?>(null) }
    var exportProgress by remember { mutableStateOf<Float?>(null) }
    var showSavedProjectsDialog by remember { mutableStateOf(false) }
    var savedProjectsList by remember { mutableStateOf<List<VideoStudioProject>>(emptyList()) }

    // --- Playback State ---
    var isPlaying by remember { mutableStateOf(false) }

    fun pushUndo() {
        undoStack = undoStack + project
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.last()
            undoStack = undoStack.dropLast(1)
            redoStack = redoStack + project
            project = previous
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.last()
            redoStack = redoStack.dropLast(1)
            undoStack = undoStack + project
            project = next
        }
    }

    // --- ExoPlayer Instance Management ---
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Synchronize ExoPlayer Playlist when project clips change
    LaunchedEffect(project.clips) {
        if (project.clips.isEmpty()) {
            exoPlayer.clearMediaItems()
            return@LaunchedEffect
        }
        val mediaItems = project.clips.map { clip ->
            MediaItem.Builder()
                .setUri(clip.sourceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                )
                .build()
        }
        val currentPlayhead = project.playheadMs
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()

        // Seek to current playhead position
        val mapping = VideoTimelineEngine.clipAtProjectTime(project.clips, currentPlayhead)
        if (mapping != null) {
            exoPlayer.seekTo(mapping.clipIndex, mapping.localOffsetInClipMs)
        }
    }

    // Smooth playback progress sync loop (approx 60fps)
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val itemIndex = exoPlayer.currentMediaItemIndex
            val posInClip = exoPlayer.currentPosition.coerceAtLeast(0L)

            if (itemIndex in project.clips.indices) {
                var accumulated = 0L
                for (i in 0 until itemIndex) {
                    accumulated += project.clips[i].effectiveDurationMs
                }
                val globalTime = accumulated + posInClip
                project = project.copy(playheadMs = globalTime.coerceIn(0L, project.totalDurationMs))
            }
            delay(16)
        }
    }

    // Audio & Speed Sync with ExoPlayer
    LaunchedEffect(project.isMasterOriginalMuted, project.masterOriginalVolume, project.selectedClipId) {
        val selected = project.clips.firstOrNull { it.id == project.selectedClipId }
        val speed = selected?.speed ?: 1.0f
        exoPlayer.setPlaybackSpeed(speed)
        val vol = if (project.isMasterOriginalMuted) 0f else project.masterOriginalVolume
        exoPlayer.volume = vol
    }

    // Initial Clip Loader
    LaunchedEffect(initialVideoUri) {
        val uri = initialVideoUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                if (durationMs >= 500L) {
                    val initialClip = VideoClipItem(
                        sourceUri = uri,
                        displayName = "Video 1",
                        originalDurationMs = durationMs,
                        startMs = 0L,
                        endMs = durationMs
                    )
                    withContext(Dispatchers.Main) {
                        project = VideoStudioProject(
                            clips = listOf(initialClip),
                            selectedClipId = initialClip.id
                        )
                    }
                } else {
                    onMessageChange(if (isArabic) "مدة الفيديو قصيرة جداً (أقل من 0.5 ثانية)" else "Video is too short (< 0.5s)")
                }
            } catch (e: Exception) {
                onMessageChange(if (isArabic) "تعذر فتح الفيديو: ${e.localizedMessage}" else "Failed to open video: ${e.localizedMessage}")
            } finally {
                retriever.release()
            }
        }
    }

    // --- Launchers for Adding Clips & Audio ---
    val clipPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            pushUndo()
            scope.launch {
                val newClips = mutableListOf<VideoClipItem>()
                withContext(Dispatchers.IO) {
                    for ((idx, uri) in uris.withIndex()) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            if (durationMs >= 500L) {
                                newClips.add(
                                    VideoClipItem(
                                        sourceUri = uri,
                                        displayName = "Clip ${project.clips.size + idx + 1}",
                                        originalDurationMs = durationMs,
                                        startMs = 0L,
                                        endMs = durationMs
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                        finally { retriever.release() }
                    }
                }
                if (newClips.isNotEmpty()) {
                    project = project.copy(clips = project.clips + newClips)
                    onMessageChange(if (isArabic) "تمت إضافة ${newClips.size} مقطع بنجاح" else "Added ${newClips.size} clips")
                }
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pushUndo()
            scope.launch {
                withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        withContext(Dispatchers.Main) {
                            project = project.copy(
                                soundtrack = VideoAudioTrack(
                                    uri = uri,
                                    displayName = uri.lastPathSegment ?: "Audio",
                                    audioDurationMs = durationMs
                                )
                            )
                            onMessageChange(if (isArabic) "تم اختيار المقطع الصوتي" else "Soundtrack attached")
                        }
                    } catch (e: Exception) {
                        onMessageChange(if (isArabic) "تعذر قراءة ملف الصوت: ${e.localizedMessage}" else "Cannot read audio: ${e.localizedMessage}")
                    } finally {
                        retriever.release()
                    }
                }
            }
        }
    }

    val documentExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { targetUri ->
        if (targetUri != null) {
            onBusyChange(true)
            scope.launch {
                try {
                    VideoExportManager.exportProjectToUri(
                        context = context,
                        project = project,
                        targetUri = targetUri,
                        onProgress = { exportProgress = it }
                    )
                    onMessageChange(if (isArabic) "تم تصدير الفيديو بنجاح" else "Video exported successfully")
                    activeTab = VideoStudioTab.NONE
                } catch (e: Exception) {
                    onMessageChange(if (isArabic) "فشل التصدير: ${e.localizedMessage}" else "Export failed: ${e.localizedMessage}")
                } finally {
                    onBusyChange(false)
                    exportProgress = null
                }
            }
        }
    }

    // Seek handler
    val handleSeek: (Long) -> Unit = { targetTimeMs ->
        val safeTime = targetTimeMs.coerceIn(0L, project.totalDurationMs)
        project = project.copy(playheadMs = safeTime)
        val mapping = VideoTimelineEngine.clipAtProjectTime(project.clips, safeTime)
        if (mapping != null) {
            exoPlayer.seekTo(mapping.clipIndex, mapping.localOffsetInClipMs)
        }
    }

    // Toggle Play / Pause handler
    val handleTogglePlay: () -> Unit = {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED || project.playheadMs >= project.totalDurationMs) {
                handleSeek(0L)
            }
            exoPlayer.play()
        }
    }

    // Master UI Column
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Top Bar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                    Text(
                        text = if (isArabic) "استوديو الفيديو" else "Video Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Undo Button
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty() && !busy) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Redo Button
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty() && !busy) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Save Project Draft Button
                    IconButton(
                        onClick = {
                            try {
                                VideoProjectStorage.saveProject(context, project)
                                onMessageChange(if (isArabic) "تم حفظ مسودة المشروع بنجاح" else "Project draft saved successfully")
                            } catch (e: Exception) {
                                onMessageChange(if (isArabic) "فشل حفظ المشروع: ${e.localizedMessage}" else "Failed to save project: ${e.localizedMessage}")
                            }
                        },
                        enabled = project.clips.isNotEmpty() && !busy
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = if (isArabic) "حفظ المشروع" else "Save Project",
                            tint = if (project.clips.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Open Saved Projects Button
                    IconButton(
                        onClick = {
                            savedProjectsList = VideoProjectStorage.listSavedProjects(context)
                            showSavedProjectsDialog = true
                        },
                        enabled = !busy
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = if (isArabic) "المشاريع المحفوظة" else "Saved Projects"
                        )
                    }

                    // Export Button Pill
                    Button(
                        onClick = { activeTab = VideoStudioTab.EXPORT },
                        enabled = project.clips.isNotEmpty() && !busy,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "تصدير" else "Export", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 2. Center Video Preview & Gesture Canvas ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            VideoPreviewCanvas(
                exoPlayer = exoPlayer,
                project = project,
                onTogglePlay = handleTogglePlay,
                onSelectOverlay = { id -> project = project.copy(selectedOverlayId = id) },
                onUpdateOverlayPosition = { id, x, y ->
                    project = project.copy(
                        overlays = project.overlays.map {
                            if (it.id == id) it.copy(normalizedX = x, normalizedY = y) else it
                        }
                    )
                },
                onUpdateOverlayScaleRotation = { id, scale, rot ->
                    project = project.copy(
                        overlays = project.overlays.map {
                            if (it.id == id) it.copy(scale = scale, rotationDegrees = rot) else it
                        }
                    )
                },
                onEditOverlay = { overlay ->
                    editingOverlay = overlay
                    activeTab = VideoStudioTab.TEXT
                },
                onDeleteOverlay = { id ->
                    pushUndo()
                    project = project.copy(
                        overlays = project.overlays.filterNot { it.id == id },
                        selectedOverlayId = null
                    )
                }
            )
        }

        // --- 3. Interactive Multi-Track Timeline Bar ---
        VideoTimelineBar(
            project = project,
            isPlaying = isPlaying,
            onTogglePlay = handleTogglePlay,
            onSeek = handleSeek,
            onSelectClip = { id -> project = project.copy(selectedClipId = id) },
            onSelectOverlay = { id -> project = project.copy(selectedOverlayId = id) },
            onSplitClipAtPlayhead = {
                val selectedId = project.selectedClipId
                if (selectedId != null) {
                    pushUndo()
                    val (updatedClips, newPartId) = VideoTimelineEngine.splitClip(
                        clips = project.clips,
                        clipId = selectedId,
                        projectSplitMs = project.playheadMs
                    )
                    if (newPartId != null) {
                        project = project.copy(clips = updatedClips, selectedClipId = newPartId)
                        onMessageChange(if (isArabic) "تم تقسيم المقطع بنجاح" else "Clip split at playhead")
                    } else {
                        onMessageChange(if (isArabic) "موضع التقسيم قريب جداً من البداية أو النهاية (أقل من 500 ملي ثانية)" else "Split point too close to boundary (<500ms)")
                    }
                }
            },
            onDuplicateClip = {
                val selectedId = project.selectedClipId
                if (selectedId != null) {
                    pushUndo()
                    val (updatedClips, dupId) = VideoTimelineEngine.duplicateClip(project.clips, selectedId)
                    project = project.copy(clips = updatedClips, selectedClipId = dupId)
                }
            },
            onDeleteClip = {
                if (project.selectedOverlayId != null) {
                    pushUndo()
                    project = project.copy(
                        overlays = project.overlays.filterNot { it.id == project.selectedOverlayId },
                        selectedOverlayId = null
                    )
                } else if (project.selectedClipId != null && project.clips.size > 1) {
                    pushUndo()
                    val updated = VideoTimelineEngine.removeClip(project.clips, project.selectedClipId!!)
                    project = project.copy(clips = updated, selectedClipId = updated.firstOrNull()?.id)
                }
            },
            onRotateClip = {
                val selectedId = project.selectedClipId
                if (selectedId != null) {
                    pushUndo()
                    val updated = VideoTimelineEngine.updateClipRotation(project.clips, selectedId, 90)
                    project = project.copy(clips = updated)
                }
            },
            onTrimClip = { clipId, newStart, newEnd ->
                val updated = VideoTimelineEngine.updateClipTrim(project.clips, clipId, newStart, newEnd)
                project = project.copy(clips = updated)
            },
            onAddClipClick = {
                clipPickerLauncher.launch("video/*")
            },
            isArabic = isArabic
        )

        // --- 4. Bottom Dock: Studio Tool Tabs (CapCut & Samsung style) ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text Tool
                StudioToolChip(
                    icon = Icons.Default.TextFields,
                    label = if (isArabic) "نص" else "Text",
                    onClick = {
                        editingOverlay = null
                        activeTab = VideoStudioTab.TEXT
                    }
                )

                // Stickers Tool
                StudioToolChip(
                    icon = Icons.Default.Mood,
                    label = if (isArabic) "ملصقات" else "Stickers",
                    onClick = { activeTab = VideoStudioTab.STICKERS }
                )

                // Audio & Music Tool
                StudioToolChip(
                    icon = Icons.Default.MusicNote,
                    label = if (isArabic) "صوتيات" else "Audio",
                    onClick = { activeTab = VideoStudioTab.AUDIO }
                )

                // Speed Tool
                StudioToolChip(
                    icon = Icons.Default.Speed,
                    label = if (isArabic) "السرعة" else "Speed",
                    onClick = { activeTab = VideoStudioTab.SPEED }
                )

                // Filters Tool
                StudioToolChip(
                    icon = Icons.Default.AutoAwesome,
                    label = if (isArabic) "فلاتر" else "Filters",
                    onClick = { activeTab = VideoStudioTab.FILTERS }
                )

                // Visual Tune Tool
                StudioToolChip(
                    icon = Icons.Default.Tune,
                    label = if (isArabic) "تعديل" else "Tune",
                    onClick = { activeTab = VideoStudioTab.TUNE }
                )

                // Aspect Ratio Selector
                StudioToolChip(
                    icon = Icons.Default.AspectRatio,
                    label = if (isArabic) project.aspectRatio.labelAr else project.aspectRatio.labelEn,
                    onClick = {
                        pushUndo()
                        val ratios = VideoAspectRatio.values()
                        val nextIdx = (project.aspectRatio.ordinal + 1) % ratios.size
                        project = project.copy(aspectRatio = ratios[nextIdx])
                    }
                )
            }
        }
    }

    // --- 5. Contextual Dialogs & Bottom Sheets ---
    when (activeTab) {
        VideoStudioTab.TEXT -> {
            VideoTextOverlayDialog(
                initialOverlay = editingOverlay,
                totalDurationMs = project.totalDurationMs,
                currentPlayheadMs = project.playheadMs,
                onDismiss = {
                    activeTab = VideoStudioTab.NONE
                    editingOverlay = null
                },
                onConfirm = { overlay ->
                    pushUndo()
                    if (editingOverlay != null) {
                        project = project.copy(
                            overlays = project.overlays.map { if (it.id == overlay.id) overlay else it }
                        )
                    } else {
                        project = project.copy(
                            overlays = project.overlays + overlay,
                            selectedOverlayId = overlay.id
                        )
                    }
                    activeTab = VideoStudioTab.NONE
                    editingOverlay = null
                },
                isArabic = isArabic
            )
        }
        VideoStudioTab.STICKERS -> {
            VideoStickerSheet(
                onDismiss = { activeTab = VideoStudioTab.NONE },
                onSelectSticker = { emoji ->
                    pushUndo()
                    val start = project.playheadMs.coerceAtMost((project.totalDurationMs - 1500L).coerceAtLeast(0L))
                    val end = (start + 3000L).coerceAtMost(project.totalDurationMs)
                    val sticker = VideoTimedOverlay(
                        type = VideoOverlayType.STICKER,
                        stickerEmoji = emoji,
                        startMs = start,
                        endMs = end
                    )
                    project = project.copy(
                        overlays = project.overlays + sticker,
                        selectedOverlayId = sticker.id
                    )
                    activeTab = VideoStudioTab.NONE
                },
                isArabic = isArabic
            )
        }
        VideoStudioTab.AUDIO -> {
            VideoAudioSheet(
                originalVolume = project.masterOriginalVolume,
                isOriginalMuted = project.isMasterOriginalMuted,
                soundtrack = project.soundtrack,
                onUpdateOriginalAudio = { vol, muted ->
                    pushUndo()
                    project = project.copy(masterOriginalVolume = vol, isMasterOriginalMuted = muted)
                },
                onPickSoundtrack = { audioPickerLauncher.launch("audio/*") },
                onUpdateSoundtrackVolume = { vol ->
                    project.soundtrack?.let { track ->
                        project = project.copy(soundtrack = track.copy(volume = vol))
                    }
                },
                onRemoveSoundtrack = {
                    pushUndo()
                    project = project.copy(soundtrack = null)
                },
                onDismiss = { activeTab = VideoStudioTab.NONE },
                isArabic = isArabic
            )
        }
        VideoStudioTab.SPEED -> {
            val selected = project.clips.firstOrNull { it.id == project.selectedClipId }
            VideoSpeedSheet(
                currentSpeed = selected?.speed ?: 1.0f,
                onSelectSpeed = { speed ->
                    val selectedId = project.selectedClipId
                    if (selectedId != null) {
                        pushUndo()
                        val updated = VideoTimelineEngine.updateClipSpeed(project.clips, selectedId, speed)
                        project = project.copy(clips = updated)
                    }
                },
                onDismiss = { activeTab = VideoStudioTab.NONE },
                isArabic = isArabic
            )
        }
        VideoStudioTab.FILTERS -> {
            val selected = project.clips.firstOrNull { it.id == project.selectedClipId }
            VideoFiltersSheet(
                currentFilterId = selected?.filterId ?: "",
                onSelectFilter = { filterId ->
                    val selectedId = project.selectedClipId
                    if (selectedId != null) {
                        pushUndo()
                        val updated = VideoTimelineEngine.updateClipFilter(project.clips, selectedId, filterId)
                        project = project.copy(clips = updated)
                    }
                },
                onDismiss = { activeTab = VideoStudioTab.NONE },
                isArabic = isArabic
            )
        }
        VideoStudioTab.TUNE -> {
            val selected = project.clips.firstOrNull { it.id == project.selectedClipId }
            VideoTuneSheet(
                brightness = selected?.brightness ?: 0f,
                contrast = selected?.contrast ?: 1f,
                saturation = selected?.saturation ?: 1f,
                onUpdateTune = { b, c, s ->
                    val selectedId = project.selectedClipId
                    if (selectedId != null) {
                        val updated = VideoTimelineEngine.updateClipTuning(project.clips, selectedId, b, c, s)
                        project = project.copy(clips = updated)
                    }
                },
                onDismiss = { activeTab = VideoStudioTab.NONE },
                isArabic = isArabic
            )
        }
        VideoStudioTab.EXPORT -> {
            VideoExportDialog(
                project = project,
                busy = busy,
                exportProgress = exportProgress,
                onExportToGallery = { height, bitrate, ratio ->
                    onBusyChange(true)
                    val exportProject = project.copy(targetHeight = height, bitrate = bitrate, aspectRatio = ratio)
                    scope.launch {
                        try {
                            val savedUri = VideoExportManager.exportProjectToGallery(
                                context = context,
                                project = exportProject,
                                onProgress = { p -> exportProgress = p }
                            )
                            onMessageChange(if (isArabic) "تم حفظ الفيديو بنجاح في المعرض" else "Video saved to Gallery successfully")
                            activeTab = VideoStudioTab.NONE
                        } catch (e: Exception) {
                            onMessageChange(if (isArabic) "فشل حفظ الفيديو: ${e.localizedMessage}" else "Export failed: ${e.localizedMessage}")
                        } finally {
                            onBusyChange(false)
                            exportProgress = null
                        }
                    }
                },
                onExportToDocument = { height, bitrate, ratio ->
                    project = project.copy(targetHeight = height, bitrate = bitrate, aspectRatio = ratio)
                    documentExportLauncher.launch("ProGalleryX_Video_${System.currentTimeMillis()}.mp4")
                },
                onCancelExport = {
                    onBusyChange(false)
                    exportProgress = null
                    activeTab = VideoStudioTab.NONE
                },
                onDismiss = { activeTab = VideoStudioTab.NONE },
                isArabic = isArabic
            )
        }
        VideoStudioTab.NONE -> {}
    }

    if (showSavedProjectsDialog) {
        SavedProjectsDialog(
            projects = savedProjectsList,
            onSelectProject = { restoredProject ->
                pushUndo()
                project = restoredProject
                handleSeek(0L)
                showSavedProjectsDialog = false
                onMessageChange(if (isArabic) "تم استعادة المشروع بنجاح" else "Project restored successfully")
            },
            onDeleteProject = { idToDelete ->
                VideoProjectStorage.deleteProject(context, idToDelete)
                savedProjectsList = VideoProjectStorage.listSavedProjects(context)
            },
            onDismiss = { showSavedProjectsDialog = false },
            isArabic = isArabic
        )
    }
}

@Composable
private fun StudioToolChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
