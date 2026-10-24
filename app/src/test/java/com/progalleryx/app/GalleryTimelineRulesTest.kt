package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class GalleryTimelineRulesTest {

    data class TestMedia(val id: Long, val date: Long)

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", GalleryTimelineRules.formatDuration(0L))
        assertEquals("00:00", GalleryTimelineRules.formatDuration(-500L))
        assertEquals("00:05", GalleryTimelineRules.formatDuration(5000L))
        assertEquals("01:15", GalleryTimelineRules.formatDuration(75000L))
        assertEquals("10:00", GalleryTimelineRules.formatDuration(600000L))
        assertEquals("01:01:05", GalleryTimelineRules.formatDuration(3665000L))
        assertEquals("02:00:00", GalleryTimelineRules.formatDuration(7200000L))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", GalleryTimelineRules.formatFileSize(0L))
        assertEquals("0 B", GalleryTimelineRules.formatFileSize(-10L))
        assertEquals("500 B", GalleryTimelineRules.formatFileSize(500L))
        assertEquals("1.0 KB", GalleryTimelineRules.formatFileSize(1024L))
        assertEquals("2.5 MB", GalleryTimelineRules.formatFileSize((2.5 * 1024 * 1024).toLong()))
        assertEquals("1.0 GB", GalleryTimelineRules.formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun testStartOfDayEpochSeconds() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 23, 15, 30, 45)
            set(Calendar.MILLISECOND, 500)
        }
        val epochSeconds = cal.timeInMillis / 1000L
        val startOfDay = GalleryTimelineRules.startOfDayEpochSeconds(epochSeconds)

        val checkCal = Calendar.getInstance().apply { timeInMillis = startOfDay * 1000L }
        assertEquals(0, checkCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, checkCal.get(Calendar.MINUTE))
        assertEquals(0, checkCal.get(Calendar.SECOND))
        assertEquals(0, checkCal.get(Calendar.MILLISECOND))
        assertEquals(2026, checkCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, checkCal.get(Calendar.MONTH))
        assertEquals(23, checkCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testDateHeaderFormatting() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 23, 12, 0, 0)
        }
        val nowEpoch = nowCal.timeInMillis / 1000L

        // Today
        val todayHeaderAr = GalleryTimelineRules.formatDateHeader(nowEpoch, isArabic = true, nowEpochSeconds = nowEpoch)
        val todayHeaderEn = GalleryTimelineRules.formatDateHeader(nowEpoch, isArabic = false, nowEpochSeconds = nowEpoch)
        assertEquals("اليوم", todayHeaderAr)
        assertEquals("Today", todayHeaderEn)

        // Yesterday
        val yesterdayCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 12, 0, 0)
        }
        val yesterdayEpoch = yesterdayCal.timeInMillis / 1000L
        val yesterdayHeaderAr = GalleryTimelineRules.formatDateHeader(yesterdayEpoch, isArabic = true, nowEpochSeconds = nowEpoch)
        val yesterdayHeaderEn = GalleryTimelineRules.formatDateHeader(yesterdayEpoch, isArabic = false, nowEpochSeconds = nowEpoch)
        assertEquals("أمس", yesterdayHeaderAr)
        assertEquals("Yesterday", yesterdayHeaderEn)

        // Invalid / zero epoch
        assertEquals("غير محدد", GalleryTimelineRules.formatDateHeader(0L, isArabic = true, nowEpochSeconds = nowEpoch))
        assertEquals("Unknown Date", GalleryTimelineRules.formatDateHeader(-1L, isArabic = false, nowEpochSeconds = nowEpoch))
    }

    @Test
    fun testGroupByDate() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 23, 12, 0, 0)
        }
        val nowEpoch = nowCal.timeInMillis / 1000L

        val yesterdayCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 10, 0, 0)
        }
        val yesterdayEpoch = yesterdayCal.timeInMillis / 1000L

        val item1 = TestMedia(1L, nowEpoch)
        val item2 = TestMedia(2L, nowEpoch - 3600L) // earlier today
        val item3 = TestMedia(3L, yesterdayEpoch)

        val sections = GalleryTimelineRules.groupByDate(
            items = listOf(item1, item2, item3),
            dateSelector = { it.date },
            isArabic = true,
            nowEpochSeconds = nowEpoch
        )
        assertEquals(2, sections.size)

        assertEquals("اليوم", sections[0].title)
        assertEquals(2, sections[0].items.size)
        assertEquals(1L, sections[0].items[0].id)
        assertEquals(2L, sections[0].items[1].id)

        assertEquals("أمس", sections[1].title)
        assertEquals(1, sections[1].items.size)
        assertEquals(3L, sections[1].items[0].id)
    }
}
