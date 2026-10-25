package com.progalleryx.app.studioplus.core.model

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Immutable domain model representing a photo or video in Studio Plus +.
 * Completely independent of any legacy code.
 */
data class StudioMediaItem(
    val id: Long,
    val uri: Uri? = null,
    val displayName: String,
    val mimeType: String,
    val dateTakenMs: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val orientation: Int = 0,
    val bucketId: String = "",
    val bucketDisplayName: String = "",
    val isFavorite: Boolean = false
) {
    val isVideo: Boolean
        get() = mimeType.startsWith("video/", ignoreCase = true)

    val formattedDuration: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    val formattedDate: String
        get() {
            val timestamp = if (dateTakenMs > 0) dateTakenMs else dateModifiedSec * 1000L
            return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
}

data class StudioAlbumItem(
    val bucketId: String,
    val name: String,
    val coverUri: Uri? = null,
    val itemCount: Int,
    val lastModified: Long
)

data class StudioTimelineSection(
    val title: String,
    val dateKey: String,
    val items: List<StudioMediaItem>
)
