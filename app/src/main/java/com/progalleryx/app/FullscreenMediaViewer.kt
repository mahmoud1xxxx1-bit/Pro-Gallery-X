package com.progalleryx.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMediaViewer(
    mediaList: List<GalleryMedia>,
    initialIndex: Int,
    isArabic: Boolean,
    onClose: () -> Unit,
    onEditPhoto: (GalleryMedia) -> Unit,
    onEditVideo: (GalleryMedia) -> Unit,
    onToggleFavorite: (GalleryMedia) -> Unit,
    onTrashItem: (GalleryMedia) -> Unit,
    isFavorite: (GalleryMedia) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safeInitial = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { mediaList.size })

    var showControls by remember { mutableStateOf(true) }
    var showDetailsSheet by remember { mutableStateOf(false) }

    val currentItem = mediaList.getOrNull(pagerState.currentPage)

    // Auto-hide controls after 3.5 seconds of user inactivity
    LaunchedEffect(showControls, pagerState.currentPage) {
        if (showControls) {
            delay(3500L)
            showControls = false
        }
    }

    // Preload next and previous images
    LaunchedEffect(pagerState.currentPage) {
        val nextIdx = pagerState.currentPage + 1
        val prevIdx = pagerState.currentPage - 1
        if (nextIdx < mediaList.size && !mediaList[nextIdx].video) {
            val req = ImageRequest.Builder(context).data(mediaList[nextIdx].uri).build()
            coil.Coil.imageLoader(context).enqueue(req)
        }
        if (prevIdx >= 0 && !mediaList[prevIdx].video) {
            val req = ImageRequest.Builder(context).data(mediaList[prevIdx].uri).build()
            coil.Coil.imageLoader(context).enqueue(req)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal Pager for swiping between media
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> mediaList.getOrNull(index)?.id ?: index }
        ) { page ->
            val item = mediaList.getOrNull(page)
            if (item != null) {
                if (item.video) {
                    FullscreenVideoPlayerItem(
                        item = item,
                        isCurrentPage = page == pagerState.currentPage,
                        showControls = showControls,
                        isArabic = isArabic,
                        onToggleControls = { showControls = !showControls },
                        onEditVideo = { onEditVideo(item) }
                    )
                } else {
                    ZoomablePhotoItem(
                        item = item,
                        onToggleControls = { showControls = !showControls }
                    )
                }
            }
        }

        // Top App Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = Localization.back(isArabic),
                        tint = Color.White
                    )
                }

                if (currentItem != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(currentItem.date * 1000L)),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                IconButton(onClick = { showDetailsSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = Localization.details(isArabic),
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Action Bar Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (currentItem != null) {
                val fav = isFavorite(currentItem)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Button
                    IconButton(onClick = { onToggleFavorite(currentItem) }) {
                        Icon(
                            imageVector = if (fav) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = Localization.favorite(isArabic),
                            tint = if (fav) Color(0xFFFFD700) else Color.White
                        )
                    }

                    // Edit Button (Direct Launch)
                    IconButton(onClick = {
                        if (currentItem.video) {
                            onEditVideo(currentItem)
                        } else {
                            onEditPhoto(currentItem)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = Localization.edit(isArabic),
                            tint = Color.White
                        )
                    }

                    // Share Button
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (currentItem.video) "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, Localization.share(isArabic)))
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = Localization.share(isArabic),
                            tint = Color.White
                        )
                    }

                    // Details Button
                    IconButton(onClick = { showDetailsSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = Localization.details(isArabic),
                            tint = Color.White
                        )
                    }

                    // Trash Button
                    IconButton(onClick = { onTrashItem(currentItem) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = Localization.delete(isArabic),
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }
        }

        // Details Bottom Sheet
        if (showDetailsSheet && currentItem != null) {
            ModalBottomSheet(
                onDismissRequest = { showDetailsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = Localization.details(isArabic),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    DetailRow(
                        label = Localization.detailsType(isArabic),
                        value = if (currentItem.video) Localization.typeVideo(isArabic) else Localization.typePhoto(isArabic)
                    )

                    if (currentItem.displayName.isNotBlank()) {
                        DetailRow(label = "الاسم / Name", value = currentItem.displayName)
                    }

                    if (currentItem.sizeBytes > 0L) {
                        DetailRow(
                            label = Localization.detailsSize(isArabic),
                            value = GalleryTimelineRules.formatFileSize(currentItem.sizeBytes)
                        )
                    }

                    DetailRow(
                        label = Localization.detailsDate(isArabic),
                        value = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(currentItem.date * 1000L))
                    )

                    if (currentItem.video && currentItem.durationMs > 0L) {
                        DetailRow(
                            label = Localization.detailsDuration(isArabic),
                            value = GalleryTimelineRules.formatDuration(currentItem.durationMs)
                        )
                    }

                    DetailRow(
                        label = Localization.detailsPath(isArabic),
                        value = currentItem.uri.toString()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showDetailsSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Localization.close(isArabic))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(0.65f)
        )
    }
}

/** Zoomable Photo Item with pinch-to-zoom and double-tap zoom. */
@Composable
private fun ZoomablePhotoItem(
    item: GalleryMedia,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var targetScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "photoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (targetScale > 1.2f) {
                            targetScale = 1f
                            offset = Offset.Zero
                        } else {
                            targetScale = 2.5f
                            // Center on tap point
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            offset = Offset((cx - tapOffset.x) * 1.5f, (cy - tapOffset.y) * 1.5f)
                        }
                    },
                    onTap = { onToggleControls() }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (targetScale * zoom).coerceIn(1f, 5f)
                    if (newScale > 1f) {
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                        val newOffset = offset + pan
                        offset = Offset(
                            x = newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                            y = newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                    targetScale = newScale
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName.ifBlank { "Photo" },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

/** Fullscreen Video Player Item with Media3 ExoPlayer. */
@Composable
private fun FullscreenVideoPlayerItem(
    item: GalleryMedia,
    isCurrentPage: Boolean,
    showControls: Boolean,
    isArabic: Boolean,
    onToggleControls: () -> Unit,
    onEditVideo: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs) }
    var isMuted by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    // Playback Speed
    val speeds = remember { listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }
    var speedIndex by remember { mutableIntStateOf(2) } // 1.0f

    // Aspect Ratio modes: Fit, Zoom (Fill/Crop), Stretch
    val resizeModes = remember(isArabic) {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT to if (isArabic) "ملائم" else "Fit",
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM to if (isArabic) "تكبير" else "Zoom",
            AspectRatioFrameLayout.RESIZE_MODE_FILL to if (isArabic) "تمديد" else "Stretch"
        )
    }
    var resizeIndex by remember { mutableIntStateOf(0) }

    // Double-tap Seek Feedback Overlay state
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var seekFeedbackIsForward by remember { mutableStateOf(true) }
    var seekFeedbackTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(seekFeedbackTrigger) {
        if (seekFeedbackTrigger > 0) {
            delay(850L)
            seekFeedbackText = null
        }
    }

    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(item.uri))
            prepare()
            playWhenReady = true
        }
    }

    // Lifecycle observer to stop on pause
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == androidx.media3.common.Player.STATE_BUFFERING
                if (state == androidx.media3.common.Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isError = true
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto pause when swiped away
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            exoPlayer.pause()
        }
    }

    // Progress poller
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(250L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(durationMs) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { tapOffset ->
                        val isForward = tapOffset.x >= size.width / 2f
                        val deltaMs = if (isForward) 10_000L else -10_000L
                        val currentPos = exoPlayer.currentPosition
                        val total = exoPlayer.duration.coerceAtLeast(durationMs)
                        val targetPos = (currentPos + deltaMs).coerceIn(0L, total.coerceAtLeast(1L))
                        exoPlayer.seekTo(targetPos)
                        currentPositionMs = targetPos
                        seekFeedbackIsForward = isForward
                        seekFeedbackText = if (isForward) "+10s" else "-10s"
                        seekFeedbackTrigger++
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = if (isArabic) "تعذر تشغيل هذا الفيديو على الهاتف" else "Cannot play this video on device",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = resizeModes[resizeIndex].first
                    }
                },
                update = { view ->
                    view.resizeMode = resizeModes[resizeIndex].first
                },
                modifier = Modifier.fillMaxSize()
            )

            // Buffering progress indicator
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quick Play/Pause Center Button (Shown when controls are visible)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.currentPosition >= exoPlayer.duration && exoPlayer.duration > 0) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Double Tap Seeking Animated Pill Overlay
            if (seekFeedbackText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    contentAlignment = if (seekFeedbackIsForward) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.Black.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (seekFeedbackIsForward) Icons.Outlined.FastForward else Icons.Outlined.FastRewind,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = seekFeedbackText!!,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // In-video Scrubber & Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Top row inside video controls: Time, Speed, Resize, Mute, Studio Launch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = GalleryTimelineRules.formatDuration(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Direct Studio Launch Button
                                OutlinedButton(
                                    onClick = {
                                        exoPlayer.pause()
                                        onEditVideo()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MovieFilter,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) "الاستوديو" else "Studio",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Aspect ratio toggle button
                                TextButton(
                                    onClick = {
                                        resizeIndex = (resizeIndex + 1) % resizeModes.size
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AspectRatio,
                                        contentDescription = "Aspect Ratio",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = resizeModes[resizeIndex].second,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Playback speed chip
                                TextButton(
                                    onClick = {
                                        speedIndex = (speedIndex + 1) % speeds.size
                                        exoPlayer.setPlaybackSpeed(speeds[speedIndex])
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${speeds[speedIndex]}x",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Mute toggle
                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        exoPlayer.volume = if (isMuted) 0f else 1f
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "Mute Toggle",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = GalleryTimelineRules.formatDuration(durationMs),
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val total = durationMs.toFloat().coerceAtLeast(1f)
                        val progress = (currentPositionMs.toFloat() / total).coerceIn(0f, 1f)

                        Slider(
                            value = progress,
                            onValueChange = { frac ->
                                val seekTarget = (frac * total).toLong()
                                currentPositionMs = seekTarget
                                exoPlayer.seekTo(seekTarget)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }
                }
            }
        }
    }
}
