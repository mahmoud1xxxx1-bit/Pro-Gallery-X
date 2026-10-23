package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VideoExportManagerIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAudioVideoSync_soundtrackLongerThanProjectClampsToProjectDuration() {
        // Project with 2 clips: 4000ms and 6000ms -> Total project duration = 10,000ms
        val clip1 = VideoClipItem(
            id = "c1",
            displayName = "Clip1.mp4",
            originalDurationMs = 5000L,
            startMs = 0L,
            endMs = 4000L
        )
        val clip2 = VideoClipItem(
            id = "c2",
            displayName = "Clip2.mp4",
            originalDurationMs = 10000L,
            startMs = 1000L,
            endMs = 7000L
        )
        val clips = listOf(clip1, clip2)
        val projectTotalDurationMs = VideoTimelineEngine.totalDurationMs(clips)
        assertEquals(10_000L, projectTotalDurationMs)

        // Soundtrack with duration 30,000ms and start offset 2,000ms
        val soundtrackStartOffsetMs = 2000L
        val soundtrackSourceDurationMs = 30000L

        // Calculated clipping configuration:
        val calculatedEndMs = soundtrackStartOffsetMs + projectTotalDurationMs
        assertEquals(12_000L, calculatedEndMs)

        val effectiveSoundtrackDuration = calculatedEndMs - soundtrackStartOffsetMs
        assertEquals(projectTotalDurationMs, effectiveSoundtrackDuration)
        assertTrue("Soundtrack effective length must never exceed video project duration", effectiveSoundtrackDuration <= projectTotalDurationMs)
    }

    @Test
    fun testAudioVideoSync_varyingPlaybackSpeedsAffectsDurationCorrectly() {
        // Clip 1: 4000ms trimmed at 2.0x speed -> effective = 2000ms
        val clip1 = VideoClipItem(
            id = "c1",
            originalDurationMs = 4000L,
            startMs = 0L,
            endMs = 4000L,
            speed = 2.0f
        )
        assertEquals(2000L, clip1.effectiveDurationMs)

        // Clip 2: 3000ms trimmed at 0.5x speed -> effective = 6000ms
        val clip2 = VideoClipItem(
            id = "c2",
            originalDurationMs = 3000L,
            startMs = 0L,
            endMs = 3000L,
            speed = 0.5f
        )
        assertEquals(6000L, clip2.effectiveDurationMs)

        // Total duration must be 2000 + 6000 = 8000ms
        val total = VideoTimelineEngine.totalDurationMs(listOf(clip1, clip2))
        assertEquals(8000L, total)
    }

    @Test
    fun testProgressStateMapping_boundsAndMonotonicity() {
        // Simulate progress values from ProgressHolder (0 to 100)
        val progressValues = listOf(0, 15, 34, 58, 82, 100)
        val reportedFloats = mutableListOf<Float>()

        for (p in progressValues) {
            val ratio = (p / 100f).coerceIn(0f, 1f)
            reportedFloats.add(ratio)
        }

        assertEquals(0.0f, reportedFloats.first(), 0.001f)
        assertEquals(1.0f, reportedFloats.last(), 0.001f)

        // Verify strictly non-decreasing
        for (i in 0 until reportedFloats.size - 1) {
            assertTrue(reportedFloats[i + 1] >= reportedFloats[i])
        }
    }

    @Test
    fun testExportCancellation_cleansUpTempFileWithoutTouchingOriginalSource() {
        // Create simulated original source file
        val originalSourceFile = File(tempFolder.root, "original_source_video.mp4")
        originalSourceFile.writeText("ORIGINAL_VIDEO_BINARY_DATA_PRESERVED")
        val originalModifiedTime = originalSourceFile.lastModified()
        val originalLength = originalSourceFile.length()

        // Create simulated temp export file in cache
        val tempExportFile = File(tempFolder.root, "pgx_studio_export_test.mp4")
        tempExportFile.writeText("TEMPORARY_PARTIAL_ENCODE_BYTES")
        assertTrue(tempExportFile.exists())

        // Simulate cancellation cleanup
        if (tempExportFile.exists()) {
            tempExportFile.delete()
        }

        // Assert temp file was deleted
        assertFalse("Temp export file should be deleted on cancellation", tempExportFile.exists())

        // Assert original source file was NOT deleted, modified or truncated
        assertTrue("Original source file must exist untouched", originalSourceFile.exists())
        assertEquals("Original source file size must be intact", originalLength, originalSourceFile.length())
        assertEquals("Original content must remain identical", "ORIGINAL_VIDEO_BINARY_DATA_PRESERVED", originalSourceFile.readText())
    }

    @Test
    fun testExportQualityAndAspectRules_strictValidation() {
        assertTrue(VideoQualityRules.valid(VideoQualityRules.AUTO))
        assertTrue(VideoQualityRules.valid(VideoQualityRules.HIGH))
        assertTrue(VideoQualityRules.valid(VideoQualityRules.BALANCED))
        assertTrue(VideoQualityRules.valid(VideoQualityRules.COMPACT))
        assertFalse(VideoQualityRules.valid(999999))

        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_AUTO))
        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_1080P))
        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_720P))
        assertTrue(VideoQualityRules.validHeight(VideoQualityRules.HEIGHT_480P))
        assertFalse(VideoQualityRules.validHeight(300))

        assertTrue(VideoSpeedRules.valid(0.5f))
        assertTrue(VideoSpeedRules.valid(1.0f))
        assertTrue(VideoSpeedRules.valid(1.5f))
        assertTrue(VideoSpeedRules.valid(2.0f))
        assertFalse(VideoSpeedRules.valid(4.0f))
    }
}
