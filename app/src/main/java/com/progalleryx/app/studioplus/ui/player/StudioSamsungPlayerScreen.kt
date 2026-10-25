package com.progalleryx.app.studioplus.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.progalleryx.app.studioplus.core.player.StudioVideoPlayerController
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun StudioSamsungPlayerScreen(
    videoUri: Uri,
    title: String = "",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember(videoUri) {
        StudioVideoPlayerController(context, videoUri)
    }

    DisposableEffect(controller) {
        onDispose {
            controller.release()
        }
    }

    val state by controller.state.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }

    // HUD overlays for gestures
    var volumeHudText by remember { mutableStateOf<String?>(null) }
    var brightnessHudText by remember { mutableStateOf<String?>(null) }
    var scrubHudText by remember { mutableStateOf<String?>(null) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // Gesture detection for Volume (right half) and Brightness (left half) and Scrubbing (horizontal)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(state.screenLocked) {
                if (state.screenLocked) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible }
                    )
                } else {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                controller.seekBackward(10_000)
                            } else {
                                controller.seekForward(10_000)
                            }
                        }
                    )
                }
            }
            .pointerInput(state.screenLocked) {
                if (!state.screenLocked) {
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var isHorizontalDrag = false
                    var startSeekPos = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragX = 0f
                            totalDragY = 0f
                            isHorizontalDrag = false
                            startSeekPos = state.currentPositionMs
                        },
                        onDragEnd = {
                            if (isHorizontalDrag && state.durationMs > 0) {
                                controller.seekTo(startSeekPos + (totalDragX * 50).toLong())
                            }
                            volumeHudText = null
                            brightnessHudText = null
                            scrubHudText = null
                        },
                        onDragCancel = {
                            volumeHudText = null
                            brightnessHudText = null
                            scrubHudText = null
                        },
                        onDrag = { change, dragAmount ->
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (!isHorizontalDrag && abs(totalDragX) > abs(totalDragY) && abs(totalDragX) > 40) {
                                isHorizontalDrag = true
                            }

                            if (isHorizontalDrag) {
                                val deltaSec = (totalDragX / 15).toInt()
                                val targetMs = (startSeekPos + deltaSec * 1000L).coerceIn(0L, state.durationMs)
                                val sign = if (deltaSec >= 0) "+$deltaSec" else "$deltaSec"
                                scrubHudText = "$sign s (${formatTime(targetMs)} / ${formatTime(state.durationMs)})"
                            } else {
                                val isRightSide = change.position.x > size.width / 2
                                if (isRightSide) {
                                    // Volume adjustment
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = (-dragAmount.y / 40).toInt()
                                    if (delta != 0) {
                                        val newVol = (currentVol + delta).coerceIn(0, maxVolume)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        volumeHudText = "الصوت: ${(newVol * 100) / maxVolume}%"
                                    }
                                } else {
                                    // Brightness adjustment
                                    (context as? Activity)?.let { activity ->
                                        val window = activity.window
                                        val lp = window.attributes
                                        var curBrightness = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                        curBrightness = (curBrightness - (dragAmount.y / 800f)).coerceIn(0.01f, 1.0f)
                                        lp.screenBrightness = curBrightness
                                        window.attributes = lp
                                        brightnessHudText = "السطوع: ${(curBrightness * 100).toInt()}%"
                                    }
                                }
                            }
                        }
                    )
                }
            }
    ) {
        // Media3 Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = controller.exoPlayer
                    useController = false
                    resizeMode = state.resizeMode
                }
            },
            update = { view ->
                view.resizeMode = state.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center HUD Alerts (Brightness, Volume, Scrub)
        volumeHudText?.let { text ->
            GestureHudCard(text = text, icon = Icons.AutoMirrored.Filled.VolumeUp)
        }
        brightnessHudText?.let { text ->
            GestureHudCard(text = text, icon = Icons.Default.BrightnessMedium)
        }
        scrubHudText?.let { text ->
            GestureHudCard(text = text, icon = Icons.Default.FastForward)
        }

        // Top Controls Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = title.ifBlank { "مشغل الفيديو" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                // Aspect ratio cycle button
                IconButton(
                    onClick = { controller.cycleAspectRatio() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White
                    )
                }
            }
        }

        // Center Big Play/Pause/Replay Button
        AnimatedVisibility(
            visible = controlsVisible && !state.screenLocked,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Rewind 10s
                IconButton(
                    onClick = { controller.seekBackward(10_000) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Play / Pause / Replay
                IconButton(
                    onClick = { controller.togglePlayPause() },
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = when {
                            state.isEnded -> Icons.Default.Replay
                            state.isPlaying -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Forward 10s
                IconButton(
                    onClick = { controller.seekForward(10_000) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // Bottom Controls Bar (Samsung One UI player style)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF14181F).copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Scrubber slider and time labels
                if (!state.screenLocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(state.currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = if (state.durationMs > 0) {
                                (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { frac ->
                                controller.seekTo((frac * state.durationMs).toLong())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            )
                        )

                        Text(
                            text = formatTime(state.durationMs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Action buttons row (Lock, Speed, Mute)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock Screen button
                    IconButton(
                        onClick = { controller.toggleScreenLock() }
                    ) {
                        Icon(
                            imageVector = if (state.screenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (state.screenLocked) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    if (!state.screenLocked) {
                        // Playback Speed button
                        TextButton(
                            onClick = {
                                val nextSpeed = when (state.speed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.75f
                                    else -> 1.0f
                                }
                                controller.setSpeed(nextSpeed)
                            }
                        ) {
                            Text(
                                text = "${state.speed}x",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Mute button
                        IconButton(
                            onClick = { controller.toggleMute() }
                        ) {
                            Icon(
                                imageVector = if (state.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.GestureHudCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.align(Alignment.Center),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
