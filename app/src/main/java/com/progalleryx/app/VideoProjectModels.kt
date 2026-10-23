package com.progalleryx.app

import android.net.Uri
import java.util.UUID

enum class VideoAspectRatio(val ratio: Float, val labelEn: String, val labelAr: String) {
    ORIGINAL(0f, "Original", "الأصلي"),
    RATIO_16_9(16f / 9f, "16:9", "١٦:٩"),
    RATIO_9_16(9f / 16f, "9:16", "٩:١٦"),
    RATIO_1_1(1f, "1:1", "١:١"),
    RATIO_4_3(4f / 3f, "4:3", "٤:٣")
}

enum class VideoOverlayType {
    TEXT,
    STICKER
}

data class VideoTimedOverlay(
    val id: String = UUID.randomUUID().toString(),
    val type: VideoOverlayType = VideoOverlayType.TEXT,
    val text: String = "",
    val stickerEmoji: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 3000L,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val backgroundColorArgb: Long = 0x88000000L,
    val fontIndex: Int = 0,
    val isBold: Boolean = false,
    val isItalic: Boolean = false
) {
    fun isVisibleAt(globalTimeMs: Long): Boolean =
        globalTimeMs in startMs..endMs

    fun durationMs(): Long = (endMs - startMs).coerceAtLeast(0L)
}

data class VideoClipItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri? = null,
    val displayName: String = "",
    val originalDurationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = originalDurationMs,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val rotationDegrees: Int = 0,
    val filterId: String = PhotoFilterPresets.ORIGINAL.id,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f
) {
    val rawTrimDurationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)

    val effectiveDurationMs: Long
        get() {
            val validSpeed = if (speed > 0f) speed else 1.0f
            return (rawTrimDurationMs / validSpeed).toLong()
        }
}

data class VideoAudioTrack(
    val uri: Uri,
    val displayName: String = "",
    val audioDurationMs: Long,
    val startOffsetMs: Long = 0L,
    val projectStartMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
)

data class VideoStudioProject(
    val clips: List<VideoClipItem> = emptyList(),
    val overlays: List<VideoTimedOverlay> = emptyList(),
    val soundtrack: VideoAudioTrack? = null,
    val masterOriginalVolume: Float = 1.0f,
    val isMasterOriginalMuted: Boolean = false,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.ORIGINAL,
    val targetHeight: Int = VideoQualityRules.HEIGHT_AUTO,
    val bitrate: Int = VideoQualityRules.AUTO,
    val selectedClipId: String? = null,
    val selectedOverlayId: String? = null,
    val playheadMs: Long = 0L
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }
}
