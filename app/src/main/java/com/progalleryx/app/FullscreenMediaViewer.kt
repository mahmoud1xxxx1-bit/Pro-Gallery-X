package com.progalleryx.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
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
                        onToggleControls = { showControls = !showControls }
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
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
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
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
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
                    scale = newScale
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
                    scaleX = scale,
                    scaleY = scale,
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
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs) }
    var isMuted by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(item.uri))
            prepare()
            playWhenReady = true
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
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
                    text = "تعذر تشغيل هذا الفيديو على الهاتف",
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
                    }
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

            // Quick Play/Pause Center Button
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
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // In-video Scrubber Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = GalleryTimelineRules.formatDuration(currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "Mute Toggle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = GalleryTimelineRules.formatDuration(durationMs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
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
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
