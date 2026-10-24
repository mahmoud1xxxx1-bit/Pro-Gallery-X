package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class PhotoFilterPresetsTest {

    @Test
    fun allPresetsHaveValidBoundedValues() {
        val presets = PhotoFilterPresets.all
        assertTrue(presets.isNotEmpty())
        assertEquals(8, presets.size)

        for (preset in presets) {
            assertTrue("ID must not be blank", preset.id.isNotBlank())
            assertTrue("Name must not be blank", preset.displayName.isNotBlank())
            assertTrue("Brightness out of bounds: ${preset.brightness}", preset.brightness in -100f..100f)
            assertTrue("Contrast out of bounds: ${preset.contrast}", preset.contrast in 0.5f..1.5f)
            assertTrue("Saturation out of bounds: ${preset.saturation}", preset.saturation in 0f..2f)
            assertTrue("Temperature out of bounds: ${preset.temperature}", preset.temperature in -100f..100f)
            assertTrue("Tint out of bounds: ${preset.tint}", preset.tint in -100f..100f)
        }
    }

    @Test
    fun findPresetByIdWorks() {
        assertEquals(PhotoFilterPresets.ORIGINAL, PhotoFilterPresets.find("original"))
        assertEquals(PhotoFilterPresets.VIVID, PhotoFilterPresets.find("vivid"))
        assertEquals(PhotoFilterPresets.MONO, PhotoFilterPresets.find("mono"))
        assertEquals(PhotoFilterPresets.WARM, PhotoFilterPresets.find("warm"))
        assertEquals(PhotoFilterPresets.COOL, PhotoFilterPresets.find("cool"))
        assertNull(PhotoFilterPresets.find("non_existent_preset"))
    }

    @Test
    fun applyPresetUpdatesColorFieldsWithoutAffectingTransforms() {
        val initial = PhotoAdjustments(
            brightness = 0f,
            contrast = 1f,
            saturation = 1f,
            rotation = 90,
            flipHorizontal = true,
            cropRatio = 1f,
            jpegQuality = 85,
            caption = "Test Caption"
        )

        val applied = PhotoFilterPresets.applyTo(initial, PhotoFilterPresets.VIVID)

        // Color properties updated
        assertEquals(PhotoFilterPresets.VIVID.brightness, applied.brightness, 0.001f)
        assertEquals(PhotoFilterPresets.VIVID.contrast, applied.contrast, 0.001f)
        assertEquals(PhotoFilterPresets.VIVID.saturation, applied.saturation, 0.001f)
        assertEquals(PhotoFilterPresets.VIVID.temperature, applied.temperature, 0.001f)
        assertEquals(PhotoFilterPresets.VIVID.tint, applied.tint, 0.001f)

        // Non-color properties preserved
        assertEquals(90, applied.rotation)
        assertTrue(applied.flipHorizontal)
        assertEquals(1f, applied.cropRatio, 0.001f)
        assertEquals(85, applied.jpegQuality)
        assertEquals("Test Caption", applied.caption)
    }

    @Test
    fun monoPresetHasZeroSaturation() {
        val mono = PhotoFilterPresets.MONO
        assertEquals(0f, mono.saturation, 0.001f)
    }
}
