package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VideoProjectPersistenceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSaveAndRestoreProject_preservesAllFieldsAndState() {
        val original = VideoStudioProject(
            id = "proj_test_12345",
            title = "Vacation Trip 2026",
            clips = listOf(
                VideoClipItem(
                    id = "c1",
                    sourceUri = null,
                    displayName = "Clip_001.mp4",
                    originalDurationMs = 5000L,
                    startMs = 500L,
                    endMs = 4500L,
                    rotationDegrees = 90,
                    speed = 1.5f,
                    volume = 0.8f,
                    isMuted = false,
                    filterId = PhotoFilterPresets.WARM.id,
                    brightness = 0.1f,
                    contrast = 1.2f,
                    saturation = 1.1f
                ),
                VideoClipItem(
                    id = "c2",
                    sourceUri = null,
                    displayName = "Clip_002.mp4",
                    originalDurationMs = 6000L,
                    startMs = 0L,
                    endMs = 6000L,
                    rotationDegrees = 180,
                    speed = 0.5f,
                    volume = 0.0f,
                    isMuted = true,
                    filterId = PhotoFilterPresets.MONO.id,
                    brightness = -0.05f,
                    contrast = 1.0f,
                    saturation = 0.0f
                )
            ),
            overlays = listOf(
                VideoTimedOverlay(
                    id = "ov1",
                    type = VideoOverlayType.TEXT,
                    text = "Welcome to Paradise",
                    startMs = 2000L,
                    endMs = 5000L,
                    normalizedX = 0.5f,
                    normalizedY = 0.3f,
                    scale = 1.2f,
                    rotationDegrees = 5.0f,
                    textColorArgb = 0xFFFFFF00L,
                    backgroundColorArgb = 0xAA000000L,
                    fontIndex = 2,
                    isBold = true,
                    isItalic = true
                ),
                VideoTimedOverlay(
                    id = "ov2",
                    type = VideoOverlayType.STICKER,
                    stickerEmoji = "🌴",
                    startMs = 4000L,
                    endMs = 8000L,
                    normalizedX = 0.85f,
                    normalizedY = 0.85f,
                    scale = 1.8f,
                    rotationDegrees = -10.0f
                )
            ),
            soundtrack = null,
            masterOriginalVolume = 0.95f,
            isMasterOriginalMuted = false,
            aspectRatio = VideoAspectRatio.RATIO_9_16,
            targetHeight = VideoQualityRules.HEIGHT_1080P,
            bitrate = VideoQualityRules.HIGH,
            createdAt = 1710000000000L
        )

        // 1. Serialize to JSON string
        val jsonString = VideoProjectStorage.saveProjectToJsonString(original)
        assertTrue(jsonString.contains("proj_test_12345"))
        assertTrue(jsonString.contains("Vacation Trip 2026"))
        assertTrue(jsonString.contains("Welcome to Paradise"))
        assertTrue(jsonString.contains("🌴"))

        // 2. Write to a real physical file on disk
        val projectFile = File(tempFolder.root, "${original.id}.json")
        projectFile.writeText(jsonString)
        assertTrue(projectFile.exists())
        assertTrue(projectFile.length() > 0)

        // 3. Read back from disk file
        val restored = VideoProjectStorage.loadProject(projectFile)
        assertNotNull("Restored project should not be null", restored)
        restored!!

        // 4. Assert fidelity of all top-level project properties
        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.aspectRatio, restored.aspectRatio)
        assertEquals(original.targetHeight, restored.targetHeight)
        assertEquals(original.bitrate, restored.bitrate)
        assertEquals(original.isMasterOriginalMuted, restored.isMasterOriginalMuted)
        assertEquals(original.masterOriginalVolume, restored.masterOriginalVolume, 0.001f)
        assertEquals(original.createdAt, restored.createdAt)

        // 5. Assert clips fidelity
        assertEquals(original.clips.size, restored.clips.size)
        for (i in original.clips.indices) {
            val oc = original.clips[i]
            val rc = restored.clips[i]
            assertEquals(oc.id, rc.id)
            assertEquals(oc.displayName, rc.displayName)
            assertEquals(oc.originalDurationMs, rc.originalDurationMs)
            assertEquals(oc.startMs, rc.startMs)
            assertEquals(oc.endMs, rc.endMs)
            assertEquals(oc.rotationDegrees, rc.rotationDegrees)
            assertEquals(oc.speed, rc.speed, 0.001f)
            assertEquals(oc.volume, rc.volume, 0.001f)
            assertEquals(oc.isMuted, rc.isMuted)
            assertEquals(oc.filterId, rc.filterId)
            assertEquals(oc.brightness, rc.brightness, 0.001f)
            assertEquals(oc.contrast, rc.contrast, 0.001f)
            assertEquals(oc.saturation, rc.saturation, 0.001f)
        }

        // 6. Assert overlays fidelity
        assertEquals(original.overlays.size, restored.overlays.size)
        for (i in original.overlays.indices) {
            val oo = original.overlays[i]
            val ro = restored.overlays[i]
            assertEquals(oo.id, ro.id)
            assertEquals(oo.type, ro.type)
            assertEquals(oo.text, ro.text)
            assertEquals(oo.stickerEmoji, ro.stickerEmoji)
            assertEquals(oo.startMs, ro.startMs)
            assertEquals(oo.endMs, ro.endMs)
            assertEquals(oo.normalizedX, ro.normalizedX, 0.001f)
            assertEquals(oo.normalizedY, ro.normalizedY, 0.001f)
            assertEquals(oo.scale, ro.scale, 0.001f)
            assertEquals(oo.rotationDegrees, ro.rotationDegrees, 0.001f)
            assertEquals(oo.textColorArgb, ro.textColorArgb)
            assertEquals(oo.backgroundColorArgb, ro.backgroundColorArgb)
            assertEquals(oo.fontIndex, ro.fontIndex)
            assertEquals(oo.isBold, ro.isBold)
            assertEquals(oo.isItalic, ro.isItalic)
        }
    }

    @Test
    fun testCorruptedJson_returnsNullGracefully() {
        val corruptedFile = File(tempFolder.root, "corrupted.json")
        corruptedFile.writeText("{ this is invalid json content }")
        val result = VideoProjectStorage.loadProject(corruptedFile)
        assertEquals(null, result)
    }
}
