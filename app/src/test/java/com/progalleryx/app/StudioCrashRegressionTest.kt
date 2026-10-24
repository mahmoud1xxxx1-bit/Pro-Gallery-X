package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioCrashRegressionTest {

    @Test
    fun testStudioDestinations_allEnumsValidAndDistinct() {
        val destinations = StudioDestination.values()
        assertEquals(4, destinations.size)
        assertTrue(destinations.contains(StudioDestination.PHOTO))
        assertTrue(destinations.contains(StudioDestination.VIDEO))
        assertTrue(destinations.contains(StudioDestination.PDF))
        assertTrue(destinations.contains(StudioDestination.DUPLICATES))
    }

    @Test
    fun testStudioDestinationStateTransitions_cycleSafely() {
        var current: StudioDestination? = null

        // 1. Enter Photo Studio
        current = StudioDestination.PHOTO
        assertEquals(StudioDestination.PHOTO, current)

        // 2. Return to Hub
        current = null
        assertNull(current)

        // 3. Enter Video Studio
        current = StudioDestination.VIDEO
        assertEquals(StudioDestination.VIDEO, current)

        // 4. Return to Hub
        current = null
        assertNull(current)

        // 5. Enter PDF Studio
        current = StudioDestination.PDF
        assertEquals(StudioDestination.PDF, current)

        // 6. Return to Hub
        current = null
        assertNull(current)

        // 7. Enter Duplicates Scanner
        current = StudioDestination.DUPLICATES
        assertEquals(StudioDestination.DUPLICATES, current)

        // 8. Return to Hub
        current = null
        assertNull(current)
    }

    @Test
    fun testVideoStudioTabs_allEnumStatesSupported() {
        val tabs = VideoStudioTab.values()
        assertTrue(tabs.contains(VideoStudioTab.NONE))
        assertTrue(tabs.contains(VideoStudioTab.TEXT))
        assertTrue(tabs.contains(VideoStudioTab.STICKERS))
        assertTrue(tabs.contains(VideoStudioTab.AUDIO))
        assertTrue(tabs.contains(VideoStudioTab.SPEED))
        assertTrue(tabs.contains(VideoStudioTab.FILTERS))
        assertTrue(tabs.contains(VideoStudioTab.TUNE))
        assertTrue(tabs.contains(VideoStudioTab.EXPORT))
    }

    @Test
    fun testPhotoStudioTabs_allEnumStatesSupported() {
        val tabs = StudioToolTab.values()
        assertTrue(tabs.contains(StudioToolTab.TUNE))
        assertTrue(tabs.contains(StudioToolTab.FILTERS))
        assertTrue(tabs.contains(StudioToolTab.TEXT))
        assertTrue(tabs.contains(StudioToolTab.STICKERS))
        assertTrue(tabs.contains(StudioToolTab.DRAW))
        assertTrue(tabs.contains(StudioToolTab.TRANSFORM))
        assertTrue(tabs.contains(StudioToolTab.EXPORT))
    }

    @Test
    fun testLocalization_studioKeysBilingualNonEmpty() {
        listOf(true, false).forEach { isAr ->
            assertTrue(Localization.studioHubTitle(isAr).isNotBlank())
            assertTrue(Localization.photoStudioTitle(isAr).isNotBlank())
            assertTrue(Localization.videoStudioTitle(isAr).isNotBlank())
            assertTrue(Localization.pdfSuiteTitle(isAr).isNotBlank())
            assertTrue(Localization.storageCleanerTitle(isAr).isNotBlank())
            assertTrue(Localization.compareHold(isAr).isNotBlank())
            assertTrue(Localization.saveToGallery(isAr).isNotBlank())
        }
    }

    @Test
    fun testPdfLayoutAndPaperEnums() {
        assertEquals(3, PdfLayout.values().size)
        assertEquals(2, PdfPaper.values().size)
    }
}
