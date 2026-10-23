package com.progalleryx.app

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Modern, gapless single-clip video player backed by Media3 ExoPlayer. */
@Composable
fun Media3SingleClipPlayer(
    source: Uri,
    trimStartMs: Long = 0L,
    trimEndMs: Long = Long.MAX_VALUE,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var hasError by remember { mutableStateOf(false) }

    val exoPlayer = remember(context, source) {
        ExoPlayer.Builder(context).build().apply {
            val clipBuilder = MediaItem.ClippingConfiguration.Builder()
            if (trimStartMs > 0L) clipBuilder.setStartPositionMs(trimStartMs)
            if (trimEndMs < Long.MAX_VALUE && trimEndMs > trimStartMs) clipBuilder.setEndPositionMs(trimEndMs)

            val mediaItem = MediaItem.Builder()
                .setUri(source)
                .setClippingConfiguration(clipBuilder.build())
                .build()

            setMediaItem(mediaItem)
            prepare()
            playWhenReady = autoPlay
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(trimStartMs, trimEndMs) {
        val clipBuilder = MediaItem.ClippingConfiguration.Builder()
        if (trimStartMs > 0L) clipBuilder.setStartPositionMs(trimStartMs)
        if (trimEndMs < Long.MAX_VALUE && trimEndMs > trimStartMs) clipBuilder.setEndPositionMs(trimEndMs)

        val mediaItem = MediaItem.Builder()
            .setUri(source)
            .setClippingConfiguration(clipBuilder.build())
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (isPlaying) exoPlayer.play()
    }

    Column(modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
        if (hasError) {
            Text(
                "تعذر تشغيل هذا المقطع بواسطة مشغل الوسائط",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

/** Gapless sequential playback of all trimmed timeline clips backed by Media3 ExoPlayer playlist. */
@Composable
fun Media3SequentialTimelinePlayer(
    segments: List<TimelineSegment<Uri>>,
    modifier: Modifier = Modifier
) {
    require(segments.size in 2..12)
    val context = LocalContext.current
    var currentClipIndex by remember(segments) { mutableIntStateOf(0) }
    var isPlaying by remember(segments) { mutableStateOf(false) }
    var isMuted by remember(segments) { mutableStateOf(false) }
    var hasError by remember(segments) { mutableStateOf(false) }

    val exoPlayer = remember(context, segments) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItems = segments.map { seg ->
                MediaItem.Builder()
                    .setUri(seg.source)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(seg.sourceStartMs)
                            .setEndPositionMs(seg.sourceEndMs)
                            .build()
                    )
                    .build()
            }
            setMediaItems(mediaItems)
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentClipIndex = exoPlayer.currentMediaItemIndex.coerceIn(0, segments.lastIndex)
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Column(modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "المقطع ${currentClipIndex + 1} من ${segments.size} — مدة المقطع: ${ClipSequenceTimelineRules.formatTime(segments[currentClipIndex].durationMs)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (isMuted) "صامت" else "صوت مفعّل",
                style = MaterialTheme.typography.labelSmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        if (exoPlayer.playbackState == Player.STATE_ENDED) {
                            exoPlayer.seekTo(0, 0L)
                        }
                        exoPlayer.play()
                    }
                },
                enabled = !hasError
            ) {
                Text(if (isPlaying) "إيقاف مؤقت" else "تشغيل الكل")
            }

            OutlinedButton(
                onClick = {
                    exoPlayer.seekTo(0, 0L)
                    currentClipIndex = 0
                },
                enabled = !hasError
            ) {
                Text("من البداية")
            }

            val prevIndex = ClipSequenceTimelineRules.previousClipIndex(currentClipIndex, segments.size)
            OutlinedButton(
                onClick = {
                    if (prevIndex != null) {
                        exoPlayer.seekTo(prevIndex, 0L)
                        currentClipIndex = prevIndex
                    }
                },
                enabled = !hasError && prevIndex != null
            ) {
                Text("السابق")
            }

            val nextIndex = ClipSequenceTimelineRules.nextClipIndex(currentClipIndex, segments.size)
            OutlinedButton(
                onClick = {
                    if (nextIndex != null) {
                        exoPlayer.seekTo(nextIndex, 0L)
                        currentClipIndex = nextIndex
                    }
                },
                enabled = !hasError && nextIndex != null
            ) {
                Text("التالي")
            }

            IconButton(
                onClick = {
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else 1f
                }
            ) {
                Text(if (isMuted) "🔇" else "🔊")
            }
        }

        if (hasError) {
            Text(
                "تعذر تشغيل أحد مقاطع التايملاين",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
