package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoCropRulesTest {
    @Test fun unchangedAspectKeepsFullImage() {
        assertEquals(CropArea(0, 0, 1920, 1080), PhotoCropRules.area(1920, 1080, 0f, 0.3f, 0.9f))
    }
    @Test fun squareCropUsesFocus() {
        assertEquals(CropArea(0, 0, 1080, 1080), PhotoCropRules.area(1920, 1080, 1f, 0f, 0.5f))
        assertEquals(CropArea(840, 0, 1080, 1080), PhotoCropRules.area(1920, 1080, 1f, 1f, 0.5f))
        assertEquals(CropArea(420, 0, 1080, 1080), PhotoCropRules.area(1920, 1080, 1f, 0.5f, 0.5f))
    }
    @Test fun portraitCropUsesVerticalFocus() {
        assertEquals(CropArea(0, 0, 900, 1200), PhotoCropRules.area(900, 1600, 3f/4f, 0.5f, 0f))
        assertEquals(CropArea(0, 400, 900, 1200), PhotoCropRules.area(900, 1600, 3f/4f, 0.5f, 1f))
    }
    @Test fun rejectsInvalidSizesAndFocus() {
        assertThrows(IllegalArgumentException::class.java) { PhotoCropRules.area(0, 100, 1f, 0.5f, 0.5f) }
        assertThrows(IllegalArgumentException::class.java) { PhotoCropRules.area(100, 100, Float.NaN, 0.5f, 0.5f) }
        assertThrows(IllegalArgumentException::class.java) { PhotoCropRules.area(100, 100, 1f, -0.1f, 0.5f) }
    }
}
