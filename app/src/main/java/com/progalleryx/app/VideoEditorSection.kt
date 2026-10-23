package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.text.font.FontWeight

@Composable
fun VideoEditorSection(
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    initialVideoUri: Uri? = null,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideo by remember(initialVideoUri) { mutableStateOf<Uri?>(initialVideoUri) }
    var videoDuration by remember { mutableFloatStateOf(0f) }
    var videoInfoValid by remember { mutableStateOf(false) }
    var startSecond by remember { mutableFloatStateOf(0f) }
    var endSecond by remember { mutableFloatStateOf(10f) }
    var muteVideoAudio by remember { mutableStateOf(false) }
    var videoSpeed by remember { mutableFloatStateOf(1f) }
    var videoBitrate by remember { mutableIntStateOf(VideoQualityRules.AUTO) }
    var videoHeight by remember { mutableIntStateOf(VideoQualityRules.HEIGHT_AUTO) }

    var soundtrackUri by remember { mutableStateOf<Uri?>(null) }
    var soundtrackOffset by remember { mutableFloatStateOf(0f) }
    var muteOriginalForSoundtrack by remember { mutableStateOf(true) }

    var splitSecond by remember { mutableFloatStateOf(0.5f) }
    var splitSecondHalf by remember { mutableStateOf(false) }

    var storyboardFrames by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var storyboardBusy by remember { mutableStateOf(false) }

    // Multi-clip state
    var clipsToJoin by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var joinRanges by remember { mutableStateOf<Map<Uri, Pair<Float, Float>>>(emptyMap()) }
    var joinDurations by remember { mutableStateOf<Map<Uri, Float>>(emptyMap()) }
    var mutedJoinClips by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var previousJoinRanges by remember { mutableStateOf<Map<Uri, Pair<Float, Float>>?>(null) }
    var lastRemovedClip by remember { mutableStateOf<Pair<Uri, Int>?>(null) }
    var timelineCursorMs by remember { mutableFloatStateOf(0f) }
    var showSequentialPreview by remember { mutableStateOf(false) }
    var showSingleTrimPreview by remember { mutableStateOf(false) }
    var multiClipBitrate by remember { mutableIntStateOf(VideoQualityRules.AUTO) }
    var multiClipHeight by remember { mutableIntStateOf(VideoQualityRules.HEIGHT_AUTO) }
    var timelinePreviewFrame by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Launchers
    val videoInput = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideo = uri
        videoInfoValid = false
        if (uri != null) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val seconds = (retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000f
                if (seconds >= 0.5f && seconds.isFinite()) {
                    videoDuration = seconds
                    splitSecond = (seconds / 2f).coerceIn(0.5f, (seconds - 0.5f).coerceAtLeast(0.5f))
                    videoInfoValid = true
                    startSecond = 0f
                    endSecond = videoDuration
                } else {
                    onMessageChange("مدة الفيديو قصيرة جداً")
                }
            } catch (error: Exception) {
                onMessageChange("تعذر قراءة بيانات الفيديو: ${error.localizedMessage}")
            } finally {
                retriever.release()
            }
        }
    }

    LaunchedEffect(initialVideoUri) {
        val uri = initialVideoUri ?: return@LaunchedEffect
        selectedVideo = uri
        videoInfoValid = false
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val seconds = (retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000f
            if (seconds >= 0.5f && seconds.isFinite()) {
                videoDuration = seconds
                splitSecond = (seconds / 2f).coerceIn(0.5f, (seconds - 0.5f).coerceAtLeast(0.5f))
                videoInfoValid = true
                startSecond = 0f
                endSecond = videoDuration
            }
        } catch (_: Exception) {}
        finally {
            retriever.release()
        }
    }

    LaunchedEffect(selectedVideo) {
        storyboardFrames = emptyList()
        val uri = selectedVideo ?: return@LaunchedEffect
        storyboardBusy = true
        try {
            storyboardFrames = VideoStoryboard.generate(context, uri)
        } catch (_: Exception) {
            storyboardFrames = emptyList()
        } finally {
            storyboardBusy = false
        }
    }

    val videoOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val source = selectedVideo
        if (!busy && source != null && target != null && videoInfoValid) {
            onBusyChange(true)
            scope.launch {
                try {
                    VideoEditor.trimToDocument(
                        context, source, target,
                        (startSecond * 1000).toLong(),
                        (endSecond * 1000).toLong(),
                        muteVideoAudio,
                        videoSpeed,
                        videoBitrate,
                        videoHeight
                    )
                    onMessageChange("تم تصدير الفيديو بنجاح")
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    onMessageChange("أُلغي تصدير الفيديو")
                    throw cancelled
                } catch (error: Exception) {
                    onMessageChange("فشل تصدير الفيديو: ${error.localizedMessage}")
                } finally {
                    onBusyChange(false)
                }
            }
        }
    }

    val frameOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { target ->
        val source = selectedVideo
        if (!busy && videoInfoValid && source != null && target != null) {
            onBusyChange(true)
            scope.launch {
                try {
                    VideoFrameExporter.export(context, source, target, (startSecond * 1000).toLong())
                    onMessageChange("تم استخراج الإطار الثابت بنجاح")
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    onMessageChange("تعذر استخراج الإطار: ${error.localizedMessage}")
                } finally {
                    onBusyChange(false)
                }
            }
        }
    }

    val soundtrackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        soundtrackUri = uri
        soundtrackOffset = 0f
    }

    val soundtrackOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val video = selectedVideo
        val audio = soundtrackUri
        if (!busy && video != null && audio != null && target != null && videoInfoValid) {
            onBusyChange(true)
            scope.launch {
                try {
                    VideoSoundtrackEditor.export(
                        context = context,
                        video = video,
                        soundtrack = audio,
                        output = target,
                        startMs = (startSecond * 1000f).toLong(),
                        endMs = (endSecond * 1000f).toLong(),
                        audioOffsetMs = (soundtrackOffset * 1000).toLong(),
                        muteOriginalAudio = muteOriginalForSoundtrack,
                        bitrate = videoBitrate,
                        targetHeight = videoHeight
                    )
                    onMessageChange("تم تصدير الفيديو مع المقطع الصوتي")
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    onMessageChange("تعذر دمج الصوت: ${error.localizedMessage}")
                } finally {
                    onBusyChange(false)
                }
            }
        }
    }

    val splitOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { target ->
        val source = selectedVideo
        if (!busy && source != null && target != null && videoInfoValid) {
            onBusyChange(true)
            scope.launch {
                try {
                    VideoEditor.splitPart(context, source, target, (splitSecond * 1000f).toLong(), splitSecondHalf, muteVideoAudio)
                    onMessageChange("تم حفظ المقطع المجزأ")
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    onMessageChange("تعذر تقسيم الفيديو: ${error.localizedMessage}")
                } finally {
                    onBusyChange(false)
                }
            }
        }
    }

    // Join launchers
    val joinInput = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        clipsToJoin = selected.distinct().take(12)
        joinRanges = emptyMap()
        joinDurations = emptyMap()
        mutedJoinClips = emptySet()
        previousJoinRanges = null
        lastRemovedClip = null
        timelineCursorMs = 0f
        showSequentialPreview = false
        val captured = clipsToJoin.toList()
        scope.launch {
            val durations = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                captured.mapNotNull { uri ->
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val ms = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        if (ms >= 500L) uri to (ms / 1000f) else null
                    } catch (_: Exception) { null }
                    finally { retriever.release() }
                }.toMap()
            }
            if (captured == clipsToJoin) {
                joinDurations = durations
                joinRanges = durations.mapValues { 0f to it.value }
                onMessageChange(if (durations.size == captured.size) "تم تحميل المقاطع بنجاح" else "تعذر قراءة بعض المقاطع")
            }
        }
    }

    val joinOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { dest ->
        if (!busy && dest != null && clipsToJoin.size in 2..12) {
            val orderedClips = clipsToJoin.mapNotNull { uri ->
                joinRanges[uri]?.let { VideoJoinPart(uri, (it.first * 1000f).toLong(), (it.second * 1000f).toLong(), uri in mutedJoinClips) }
            }
            if (orderedClips.size != clipsToJoin.size) return@rememberLauncherForActivityResult
            onBusyChange(true)
            scope.launch {
                try {
                    MultiClipEditor.exportParts(
                        context = context,
                        parts = orderedClips,
                        destination = dest,
                        bitrate = multiClipBitrate,
                        targetHeight = multiClipHeight
                    )
                    onMessageChange("تم دمج المقاطع بنجاح")
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    onMessageChange("أُلغي دمج الفيديو")
                    throw cancelled
                } catch (error: Exception) {
                    onMessageChange("فشل دمج المقاطع: ${error.localizedMessage}")
                } finally {
                    onBusyChange(false)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (onClose != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                }
                Text("محرر الفيديو", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { videoInput.launch("video/*") }, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text(if (selectedVideo == null) "اختيار فيديو للقص" else "تغيير الفيديو")
            }
            OutlinedButton(onClick = { joinInput.launch("video/*") }, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text("دمج مقاطع (2–12)")
            }
        }

        // Single Video Editing Section
        if (selectedVideo != null && videoInfoValid) {
            Text("معاينة الفيديو (Media3 ExoPlayer)", style = MaterialTheme.typography.titleMedium)
            key(selectedVideo) {
                Media3SingleClipPlayer(
                    source = selectedVideo!!,
                    trimStartMs = (startSecond * 1000f).toLong(),
                    trimEndMs = (endSecond * 1000f).toLong(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (storyboardBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (storyboardFrames.isNotEmpty()) {
                Text("شريط لقطات الفيديو", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(storyboardFrames) { index, bitmap ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = bitmap,
                                contentDescription = "لقطة ${index + 1}",
                                modifier = Modifier
                                    .size(94.dp, 64.dp)
                                    .clickable {
                                        val seek = videoDuration * 1000f * index / (storyboardFrames.size - 1)
                                        startSecond = (seek / 1000f).coerceAtMost((endSecond - 0.5f).coerceAtLeast(0f))
                                    }
                            )
                            Text("${(videoDuration * index / (storyboardFrames.size - 1)).toInt()}ث", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Text("بداية المقطع: ${"%.1f".format(startSecond)} ثانية")
            Slider(
                value = startSecond,
                onValueChange = { startSecond = it.coerceAtMost(endSecond - 0.5f) },
                valueRange = 0f..(videoDuration - 0.5f).coerceAtLeast(0.5f)
            )

            Text("نهاية المقطع: ${"%.1f".format(endSecond)} ثانية")
            Slider(
                value = endSecond,
                onValueChange = { endSecond = it.coerceAtLeast(startSecond + 0.5f) },
                valueRange = 0.5f..videoDuration.coerceAtLeast(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = muteVideoAudio, onCheckedChange = { muteVideoAudio = it })
                Text("تصدير بدون صوت الفيديو الأصلي")
            }

            // Quality & Resolution Controls
            Text("دقة الفيديو المصدّر", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    VideoQualityRules.HEIGHT_AUTO to "الأصل",
                    VideoQualityRules.HEIGHT_480P to "480p",
                    VideoQualityRules.HEIGHT_720P to "720p HD",
                    VideoQualityRules.HEIGHT_1080P to "1080p FHD"
                ).forEach { (res, label) ->
                    FilterChip(
                        selected = videoHeight == res,
                        onClick = { videoHeight = res },
                        label = { Text(label) },
                        enabled = !busy
                    )
                }
            }

            Text("سرعة التشغيل والتصدير", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoSpeedRules.allowed.forEach { speed ->
                    FilterChip(
                        selected = videoSpeed == speed,
                        onClick = { videoSpeed = speed },
                        label = { Text("${speed}x") },
                        enabled = !busy
                    )
                }
            }

            Text("معدل جودة الفيديو (Bitrate)", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    VideoQualityRules.AUTO to "تلقائي",
                    VideoQualityRules.COMPACT to "مضغوط",
                    VideoQualityRules.BALANCED to "متوازن",
                    VideoQualityRules.HIGH to "عالٍ"
                ).forEach { (target, label) ->
                    FilterChip(
                        selected = videoBitrate == target,
                        onClick = { videoBitrate = target },
                        label = { Text(label) },
                        enabled = !busy
                    )
                }
            }

            // Soundtrack & Audio Replacement / Mixing
            HorizontalDivider()
            Text("إضافة مقطع صوتي (Soundtrack)", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { soundtrackPicker.launch("audio/*") }, enabled = !busy) {
                Text(if (soundtrackUri == null) "اختيار ملف صوتي خارجي" else "تغيير الملف الصوتي")
            }

            if (soundtrackUri != null) {
                Text("بداية الصوت: ${"%.1f".format(soundtrackOffset)} ثانية")
                Slider(
                    value = soundtrackOffset,
                    onValueChange = { soundtrackOffset = it },
                    valueRange = 0f..120f,
                    enabled = !busy
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = !muteOriginalForSoundtrack,
                        onCheckedChange = { muteOriginalForSoundtrack = !it },
                        enabled = !busy
                    )
                    Text("دمج الموسيقى مع صوت الفيديو (بدل الاستبدال)")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { soundtrackOutput.launch("ProGalleryX-music.mp4") },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تصدير مع الموسيقى")
                    }
                    OutlinedButton(onClick = { soundtrackUri = null }, enabled = !busy) {
                        Text("إزالة الصوت")
                    }
                }
            }

            // Split Video
            HorizontalDivider()
            Text("تقسيم الفيديو إلى جزأين", style = MaterialTheme.typography.titleMedium)
            if (videoDuration >= 1f) {
                Text("نقطة التقسيم: ${"%.1f".format(splitSecond)} ثانية")
                Slider(
                    value = splitSecond.coerceIn(0.5f, videoDuration - 0.5f),
                    onValueChange = { splitSecond = it },
                    valueRange = 0.5f..(videoDuration - 0.5f),
                    enabled = !busy
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !splitSecondHalf,
                        onClick = { splitSecondHalf = false },
                        label = { Text("حفظ الجزء الأول") },
                        enabled = !busy
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = splitSecondHalf,
                        onClick = { splitSecondHalf = true },
                        label = { Text("حفظ الجزء الثاني") },
                        enabled = !busy
                    )
                }
                OutlinedButton(
                    onClick = { splitOutput.launch(if (splitSecondHalf) "ProGalleryX-part2.mp4" else "ProGalleryX-part1.mp4") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تصدير الجزء المختار")
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { frameOutput.launch("ProGalleryX-frame.jpg") },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("حفظ صورة ثابتة")
                }
                Button(
                    onClick = { videoOutput.launch("ProGalleryX-trimmed.mp4") },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تصدير الفيديو المقصوص")
                }
            }
        }

        // Multi-Clip Joiner Section
        if (clipsToJoin.isNotEmpty()) {
            HorizontalDivider()
            Text("دمج وترتيب المقاطع (${clipsToJoin.size} مقاطع)", style = MaterialTheme.typography.titleLarge)

            val timelineSegments = remember(clipsToJoin, joinRanges) {
                if (clipsToJoin.size in 2..12 && clipsToJoin.all { joinRanges[it] != null }) {
                    runCatching {
                        ClipSequenceTimelineRules.build(clipsToJoin.map { uri ->
                            val range = requireNotNull(joinRanges[uri])
                            Triple(uri, (range.first * 1000f).toLong(), (range.second * 1000f).toLong())
                        })
                    }.getOrNull()
                } else null
            }

            if (timelineSegments != null) {
                val totalMs = ClipSequenceTimelineRules.totalDurationMs(timelineSegments)
                Text("المدة الإجمالية للخط الزمني: ${ClipSequenceTimelineRules.formatTime(totalMs)}")

                // Gapless Media3 sequential player toggle
                OutlinedButton(
                    onClick = { showSequentialPreview = !showSequentialPreview },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) {
                    Text(if (showSequentialPreview) "إغلاق مشغل التايملاين" else "تشغيل المقاطع بالترتيب (Media3 ExoPlayer)")
                }

                if (showSequentialPreview) {
                    key(timelineSegments) {
                        Media3SequentialTimelinePlayer(
                            segments = timelineSegments,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Batch presets
                Text("قص جماعي سريع للمقاطع", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val usableDurations = clipsToJoin.mapNotNull { uri ->
                        joinDurations[uri]?.let { d -> if (d >= 0.5f) uri.toString() to (d * 1000f).toLong() else null }
                    }.toMap()

                    listOf(
                        "المدة الكاملة" to ClipSequenceTimelineRules.TrimPreset.FULL,
                        "أول 5 ثوانٍ" to ClipSequenceTimelineRules.TrimPreset.FIRST_FIVE_SECONDS,
                        "آخر 5 ثوانٍ" to ClipSequenceTimelineRules.TrimPreset.LAST_FIVE_SECONDS
                    ).forEach { (label, preset) ->
                        OutlinedButton(
                            onClick = {
                                val res = runCatching { ClipSequenceTimelineRules.batchRanges(usableDurations, preset) }.getOrNull()
                                if (res != null) {
                                    previousJoinRanges = joinRanges
                                    joinRanges = clipsToJoin.associateWith { uri ->
                                        val r = requireNotNull(res[uri.toString()])
                                        r.first / 1000f to r.second / 1000f
                                    }
                                }
                            },
                            enabled = !busy && usableDurations.size == clipsToJoin.size
                        ) {
                            Text(label)
                        }
                    }

                    if (previousJoinRanges != null) {
                        OutlinedButton(
                            onClick = {
                                joinRanges = previousJoinRanges ?: joinRanges
                                previousJoinRanges = null
                            },
                            enabled = !busy
                        ) {
                            Text("تراجع عن القص الجماعي")
                        }
                    }
                }

                // Global mute controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { mutedJoinClips = clipsToJoin.toSet() },
                        enabled = !busy
                    ) { Text("كتم صوت الكل") }

                    OutlinedButton(
                        onClick = { mutedJoinClips = emptySet() },
                        enabled = !busy
                    ) { Text("تشغيل صوت الكل") }
                }

                // Joined Video Resolution & Quality
                Text("إعدادات تصدير الفيديو المدمج", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        VideoQualityRules.HEIGHT_AUTO to "الأصل",
                        VideoQualityRules.HEIGHT_480P to "480p",
                        VideoQualityRules.HEIGHT_720P to "720p HD",
                        VideoQualityRules.HEIGHT_1080P to "1080p FHD"
                    ).forEach { (res, label) ->
                        FilterChip(
                            selected = multiClipHeight == res,
                            onClick = { multiClipHeight = res },
                            label = { Text(label) },
                            enabled = !busy
                        )
                    }
                }

                // Individual clip list with reordering, trimming and mute
                Text("قائمة المقاطع والقص الفردي", style = MaterialTheme.typography.titleMedium)
                clipsToJoin.forEachIndexed { index, uri ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المقطع ${index + 1}", style = MaterialTheme.typography.titleSmall)
                                Row {
                                    TextButton(
                                        onClick = {
                                            clipsToJoin = ClipSequenceTimelineRules.moveClip(clipsToJoin, index, index - 1)
                                        },
                                        enabled = !busy && index > 0
                                    ) { Text("↑") }
                                    TextButton(
                                        onClick = {
                                            clipsToJoin = ClipSequenceTimelineRules.moveClip(clipsToJoin, index, index + 1)
                                        },
                                        enabled = !busy && index < clipsToJoin.lastIndex
                                    ) { Text("↓") }
                                    TextButton(
                                        onClick = {
                                            lastRemovedClip = uri to index
                                            clipsToJoin = ClipSequenceTimelineRules.removeClip(clipsToJoin, index)
                                        },
                                        enabled = !busy
                                    ) { Text("حذف") }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = uri in mutedJoinClips,
                                    onCheckedChange = { mute ->
                                        mutedJoinClips = if (mute) mutedJoinClips + uri else mutedJoinClips - uri
                                    },
                                    enabled = !busy
                                )
                                Text("كتم صوت هذا المقطع")
                            }

                            joinRanges[uri]?.let { (start, end) ->
                                val dur = joinDurations[uri] ?: end
                                Text("القص: من ${"%.1f".format(start)}ث إلى ${"%.1f".format(end)}ث (الطول: ${"%.1f".format(end - start)}ث)")
                                Slider(
                                    value = start,
                                    onValueChange = { v ->
                                        joinRanges = joinRanges + (uri to (v.coerceAtMost(end - 0.5f) to end))
                                    },
                                    valueRange = 0f..(dur - 0.5f).coerceAtLeast(0f),
                                    enabled = !busy
                                )
                                Slider(
                                    value = end,
                                    onValueChange = { v ->
                                        joinRanges = joinRanges + (uri to (start to v.coerceAtLeast(start + 0.5f)))
                                    },
                                    valueRange = 0.5f..dur,
                                    enabled = !busy
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { joinOutput.launch("ProGalleryX-joined.mp4") },
                    enabled = !busy && clipsToJoin.size in 2..12 && clipsToJoin.all { joinRanges.containsKey(it) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تصدير الفيديو المدمج بالكامل")
                }
            }
        }
    }
}
