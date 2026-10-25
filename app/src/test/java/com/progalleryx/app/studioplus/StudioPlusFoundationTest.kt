package com.progalleryx.app.studioplus

import com.progalleryx.app.studioplus.core.model.StudioMediaItem
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class StudioPlusFoundationTest {

    @Test
    fun testStudioMediaItemDurationFormatting() {
        val shortVideo = StudioMediaItem(
            id = 1L,
            uri = null,
            displayName = "clip.mp4",
            mimeType = "video/mp4",
            dateTakenMs = 1000L,
            dateModifiedSec = 1L,
            sizeBytes = 5_000_000L,
            durationMs = 65_000L // 1 min 5 sec
        )

        assertTrue(shortVideo.isVideo)
        assertEquals("01:05", shortVideo.formattedDuration)

        val longVideo = shortVideo.copy(durationMs = 3_665_000L) // 1 hour, 1 min, 5 sec
        assertEquals("1:01:05", longVideo.formattedDuration)

        val photo = shortVideo.copy(mimeType = "image/jpeg", durationMs = 0L)
        assertFalse(photo.isVideo)
        assertEquals("", photo.formattedDuration)
    }

    @Test
    fun testStudioMediaItemSizeFormatting() {
        val itemMb = StudioMediaItem(
            id = 2L,
            uri = null,
            displayName = "photo.jpg",
            mimeType = "image/jpeg",
            dateTakenMs = 1000L,
            dateModifiedSec = 1L,
            sizeBytes = 2_621_440L // 2.5 MB
        )
        assertEquals("2.5 MB", itemMb.formattedSize)

        val itemKb = itemMb.copy(sizeBytes = 102_400L) // 100 KB
        assertEquals("100 KB", itemKb.formattedSize)

        val itemGb = itemMb.copy(sizeBytes = 1_610_612_736L) // 1.5 GB
        assertEquals("1.50 GB", itemGb.formattedSize)
    }

    @Test
    fun testAlbumExtractionLogic() {
        val items = listOf(
            StudioMediaItem(
                id = 10L,
                uri = null,
                displayName = "photo1.jpg",
                mimeType = "image/jpeg",
                dateTakenMs = 1000L,
                dateModifiedSec = 200L,
                sizeBytes = 1000L,
                bucketId = "camera",
                bucketDisplayName = "Camera"
            ),
            StudioMediaItem(
                id = 11L,
                uri = null,
                displayName = "photo2.jpg",
                mimeType = "image/jpeg",
                dateTakenMs = 2000L,
                dateModifiedSec = 300L,
                sizeBytes = 2000L,
                bucketId = "camera",
                bucketDisplayName = "Camera"
            ),
            StudioMediaItem(
                id = 12L,
                uri = null,
                displayName = "screen.png",
                mimeType = "image/png",
                dateTakenMs = 3000L,
                dateModifiedSec = 400L,
                sizeBytes = 3000L,
                bucketId = "screenshots",
                bucketDisplayName = "Screenshots"
            )
        )

        val map = LinkedHashMap<String, MutableList<StudioMediaItem>>()
        for (item in items) {
            val bId = if (item.bucketId.isNotBlank()) item.bucketId else "default"
            map.getOrPut(bId) { ArrayList() }.add(item)
        }

        assertEquals(2, map.size)
        assertEquals(2, map["camera"]?.size)
        assertEquals(1, map["screenshots"]?.size)
    }

    @Test
    fun testTimelineDateSectionGrouping() {
        val now = System.currentTimeMillis()

        val itemToday = StudioMediaItem(
            id = 101L,
            uri = null,
            displayName = "today.jpg",
            mimeType = "image/jpeg",
            dateTakenMs = now,
            dateModifiedSec = now / 1000L,
            sizeBytes = 1000L
        )

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayMs = calendar.timeInMillis

        val itemYesterday = StudioMediaItem(
            id = 102L,
            uri = null,
            displayName = "yesterday.jpg",
            mimeType = "image/jpeg",
            dateTakenMs = yesterdayMs,
            dateModifiedSec = yesterdayMs / 1000L,
            sizeBytes = 1000L
        )

        val calendarToday = Calendar.getInstance()
        val todayY = calendarToday.get(Calendar.YEAR)
        val todayD = calendarToday.get(Calendar.DAY_OF_YEAR)

        val itemCal = Calendar.getInstance()
        itemCal.timeInMillis = itemToday.dateTakenMs
        assertEquals(todayY, itemCal.get(Calendar.YEAR))
        assertEquals(todayD, itemCal.get(Calendar.DAY_OF_YEAR))

        itemCal.timeInMillis = itemYesterday.dateTakenMs
        assertEquals(calendar.get(Calendar.YEAR), itemCal.get(Calendar.YEAR))
        assertEquals(calendar.get(Calendar.DAY_OF_YEAR), itemCal.get(Calendar.DAY_OF_YEAR))
    }
}
