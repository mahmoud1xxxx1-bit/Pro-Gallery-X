package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class PhotoBatchRulesTest {

    @Test
    fun batchSizeValidation() {
        assertFalse(PhotoBatchRules.validBatchSize(0))
        assertTrue(PhotoBatchRules.validBatchSize(1))
        assertTrue(PhotoBatchRules.validBatchSize(10))
        assertTrue(PhotoBatchRules.validBatchSize(25))
        assertFalse(PhotoBatchRules.validBatchSize(26))
        assertFalse(PhotoBatchRules.validBatchSize(-5))
    }

    @Test
    fun generateBatchFileNameSanitization() {
        val name1 = PhotoBatchRules.generateBatchFileName(0, "vacation_photo.jpg")
        assertEquals("PGX_vacation_photo_1.jpg", name1)

        val name2 = PhotoBatchRules.generateBatchFileName(3, "family & friends (2026).png")
        assertEquals("PGX_family___friends__2026_4.jpg", name2)

        val nameNull = PhotoBatchRules.generateBatchFileName(5, null)
        assertEquals("PGX_photo_6.jpg", nameNull)

        val nameBlank = PhotoBatchRules.generateBatchFileName(1, "   ")
        assertEquals("PGX_photo_2.jpg", nameBlank)
    }

    @Test
    fun adjustmentsFromPresetCreatesConsistentSettings() {
        val preset = PhotoFilterPresets.VIVID
        val adj = PhotoBatchRules.adjustmentsFromPreset(preset, jpegQuality = 90, maxDimension = 2400)

        assertEquals(preset.brightness, adj.brightness, 0.001f)
        assertEquals(preset.contrast, adj.contrast, 0.001f)
        assertEquals(preset.saturation, adj.saturation, 0.001f)
        assertEquals(90, adj.jpegQuality)
        assertEquals(2400, adj.maxDimension)
        assertEquals(0, adj.rotation)
        assertFalse(adj.flipHorizontal)
        assertTrue(adj.textLayers.isEmpty())
        assertTrue(adj.brushStrokes.isEmpty())
    }

    @Test
    fun invalidBatchQualityOrDimensionThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoBatchRules.adjustmentsFromPreset(PhotoFilterPresets.ORIGINAL, jpegQuality = 60)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoBatchRules.adjustmentsFromPreset(PhotoFilterPresets.ORIGINAL, maxDimension = 500)
        }
    }
}
