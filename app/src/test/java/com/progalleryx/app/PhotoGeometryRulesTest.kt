package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class PhotoGeometryRulesTest {

    @Test
    fun previewAspectRatioPreservesNormalAspectWhenUnrotated() {
        // Landscape 1920x1080 -> 16:9 ~ 1.777f
        val aspect = PhotoGeometryRules.previewAspectRatio(1920, 1080, 0, 0f)
        assertEquals(16f / 9f, aspect, 0.01f)

        // Portrait 1080x1920 -> 9:16 ~ 0.5625f
        val portraitAspect = PhotoGeometryRules.previewAspectRatio(1080, 1920, 0, 0f)
        assertEquals(9f / 16f, portraitAspect, 0.01f)
    }

    @Test
    fun previewAspectRatioSwapsDimensionsAt90And270Degrees() {
        // Landscape 1920x1080 rotated 90 degrees becomes portrait (1080x1920)
        val rotated90 = PhotoGeometryRules.previewAspectRatio(1920, 1080, 90, 0f)
        assertEquals(1080f / 1920f, rotated90, 0.01f)

        val rotated270 = PhotoGeometryRules.previewAspectRatio(1920, 1080, 270, 0f)
        assertEquals(1080f / 1920f, rotated270, 0.01f)

        // Rotated 180 degrees keeps orientation
        val rotated180 = PhotoGeometryRules.previewAspectRatio(1920, 1080, 180, 0f)
        assertEquals(1920f / 1080f, rotated180, 0.01f)
    }

    @Test
    fun cropRatioOverridesSourceDimensions() {
        val squareCrop = PhotoGeometryRules.previewAspectRatio(1920, 1080, 0, 1f)
        assertEquals(1f, squareCrop, 0.001f)

        val fourThreeCrop = PhotoGeometryRules.previewAspectRatio(1920, 1080, 90, 4f / 3f)
        assertEquals(4f / 3f, fourThreeCrop, 0.001f)
    }

    @Test
    fun coordinateClampingAndValidation() {
        assertEquals(0f, PhotoGeometryRules.clampCoordinate(-0.5f), 0.001f)
        assertEquals(1f, PhotoGeometryRules.clampCoordinate(1.5f), 0.001f)
        assertEquals(0.42f, PhotoGeometryRules.clampCoordinate(0.42f), 0.001f)

        assertEquals(0.05f, PhotoGeometryRules.clampTextCoordinate(0.01f), 0.001f)
        assertEquals(0.95f, PhotoGeometryRules.clampTextCoordinate(0.99f), 0.001f)

        assertTrue(PhotoGeometryRules.isValidPoint(0.5f, 0.5f))
        assertFalse(PhotoGeometryRules.isValidPoint(-0.1f, 0.5f))
        assertFalse(PhotoGeometryRules.isValidPoint(0.5f, 1.1f))
    }

    @Test
    fun invalidAspectInputsThrow() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometryRules.previewAspectRatio(0, 1080, 0, 0f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometryRules.previewAspectRatio(1920, 1080, 45, 0f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometryRules.previewAspectRatio(1920, 1080, 0, -1f)
        }
    }
}
