package com.progalleryx.app.studioplus.core.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StudioPlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val speed: Float = 1.0f,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val isMuted: Boolean = false,
    val screenLocked: Boolean = false
)

/**
 * Robust, Samsung-styled Video Player Controller based on Media3 ExoPlayer.
 * Zero connection to legacy video players.
 */
@OptIn(UnstableApi::class)
class StudioVideoPlayerController(
    private val context: Context,
    private val videoUri: Uri
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(videoUri))
        prepare()
        playWhenReady = true
    }

    private val _state = MutableStateFlow(StudioPlayerState())
    val state: StateFlow<StudioPlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startProgressPolling()
            } else {
                stopProgressPolling()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.value = _state.value.copy(isBuffering = true)
                }
                Player.STATE_READY -> {
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        isEnded = false
                    )
                }
                Player.STATE_ENDED -> {
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        isEnded = true,
                        currentPositionMs = exoPlayer.duration
                    )
                    stopProgressPolling()
                }
                Player.STATE_IDLE -> {
                    _state.value = _state.value.copy(isBuffering = false)
                }
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                _state.value = _state.value.copy(
                    currentPositionMs = pos,
                    durationMs = dur
                )
                delay(200)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (_state.value.isEnded) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _state.value.durationMs)
        exoPlayer.seekTo(bounded)
        _state.value = _state.value.copy(currentPositionMs = bounded)
    }

    fun seekForward(deltaMs: Long = 10_000L) {
        seekTo(_state.value.currentPositionMs + deltaMs)
    }

    fun seekBackward(deltaMs: Long = 10_000L) {
        seekTo(_state.value.currentPositionMs - deltaMs)
    }

    fun setSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun cycleAspectRatio(): Int {
        val nextMode = when (_state.value.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        _state.value = _state.value.copy(resizeMode = nextMode)
        return nextMode
    }

    fun toggleMute(): Boolean {
        val newMute = !_state.value.isMuted
        exoPlayer.volume = if (newMute) 0.0f else 1.0f
        _state.value = _state.value.copy(isMuted = newMute)
        return newMute
    }

    fun toggleScreenLock(): Boolean {
        val newLock = !_state.value.screenLocked
        _state.value = _state.value.copy(screenLocked = newLock)
        return newLock
    }

    fun release() {
        stopProgressPolling()
        scope.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
