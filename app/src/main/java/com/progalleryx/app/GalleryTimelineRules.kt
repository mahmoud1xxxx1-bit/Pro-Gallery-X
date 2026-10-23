package com.progalleryx.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TimelineSection<T>(
    val title: String,
    val dateKey: Long,
    val items: List<T>
)

object GalleryTimelineRules {

    /** Formats milliseconds duration into mm:ss or hh:mm:ss string. */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L

        return if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** Formats file size in bytes to a human-readable string. */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024.0 && unitIndex < units.size - 1) {
            size /= 1024.0
            unitIndex++
        }
        return if (unitIndex == 0) "${bytes} B" else String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }

    /** Returns start of day timestamp in seconds for grouping. */
    fun startOfDayEpochSeconds(epochSeconds: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochSeconds * 1000L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / 1000L
    }

    /** Formats date header title based on locale (Today, Yesterday, or Full Date). */
    fun formatDateHeader(
        epochSeconds: Long,
        isArabic: Boolean,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): String {
        if (epochSeconds <= 0L) return if (isArabic) "غير محدد" else "Unknown Date"

        val targetCal = Calendar.getInstance().apply { timeInMillis = epochSeconds * 1000L }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowEpochSeconds * 1000L }

        val targetYear = targetCal.get(Calendar.YEAR)
        val nowYear = nowCal.get(Calendar.YEAR)
        val targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR)
        val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        if (targetYear == nowYear) {
            if (targetDayOfYear == nowDayOfYear) {
                return if (isArabic) "اليوم" else "Today"
            }
            if (targetDayOfYear == nowDayOfYear - 1) {
                return if (isArabic) "أمس" else "Yesterday"
            }
        }

        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val pattern = if (targetYear == nowYear) "d MMMM" else "d MMMM yyyy"
        return SimpleDateFormat(pattern, locale).format(Date(epochSeconds * 1000L))
    }

    /** Generic grouping of items by date into chronological timeline sections. */
    fun <T> groupByDate(
        items: List<T>,
        dateSelector: (T) -> Long,
        isArabic: Boolean,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): List<TimelineSection<T>> {
        if (items.isEmpty()) return emptyList()

        val grouped = linkedMapOf<Long, MutableList<T>>()
        for (item in items) {
            val dayKey = startOfDayEpochSeconds(dateSelector(item))
            grouped.getOrPut(dayKey) { mutableListOf() }.add(item)
        }

        return grouped.map { (dayKey, sectionItems) ->
            TimelineSection(
                title = formatDateHeader(dayKey, isArabic, nowEpochSeconds),
                dateKey = dayKey,
                items = sectionItems
            )
        }
    }

    /** Convenience overload for GalleryMedia items. */
    fun groupByDate(
        items: List<GalleryMedia>,
        isArabic: Boolean,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): List<TimelineSection<GalleryMedia>> =
        groupByDate(items, { it.date }, isArabic, nowEpochSeconds)
}
