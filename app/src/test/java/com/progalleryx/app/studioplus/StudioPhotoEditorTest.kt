package com.progalleryx.app.studioplus

import com.progalleryx.app.studioplus.feature.photoeditor.model.*
import org.junit.Assert.*
import org.junit.Test

class StudioPhotoEditorTest {

    @Test
    fun testDefaultStateHasNoModifications() {
        val state = StudioPhotoEditState()
        assertFalse(state.hasModifications)
        assertEquals(0f, state.brightness, 0.001f)
        assertEquals(0f, state.contrast, 0.001f)
        assertEquals(0f, state.saturation, 0.001f)
        assertEquals(0f, state.warmth, 0.001f)
        assertEquals(0f, state.vignette, 0.001f)
        assertEquals(StudioFilterType.ORIGINAL, state.filterType)
        assertEquals(0, state.rotationSteps90)
        assertEquals(0f, state.fineRotationAngle, 0.001f)
        assertFalse(state.flipHorizontal)
        assertFalse(state.flipVertical)
        assertTrue(state.textLayers.isEmpty())
        assertTrue(state.doodlePaths.isEmpty())
    }

    @Test
    fun testStateModificationFlags() {
        val brightState = StudioPhotoEditState(brightness = 25f)
        assertTrue(brightState.hasModifications)

        val rotatedState = StudioPhotoEditState(rotationSteps90 = 1)
        assertTrue(rotatedState.hasModifications)
        assertEquals(90f, rotatedState.totalRotationDegrees, 0.001f)

        val fineAngleState = StudioPhotoEditState(rotationSteps90 = 1, fineRotationAngle = 15f)
        assertEquals(105f, fineAngleState.totalRotationDegrees, 0.001f)

        val filterState = StudioPhotoEditState(filterType = StudioFilterType.CINEMATIC)
        assertTrue(filterState.hasModifications)

        val textState = StudioPhotoEditState(textLayers = listOf(StudioTextLayer(text = "مرحبا بالعالم")))
        assertTrue(textState.hasModifications)
    }

    @Test
    fun testFilterColorMatrixGeneration() {
        for (filter in StudioFilterType.values()) {
            val matrix = filter.getColorMatrix(1.0f)
            assertNotNull(matrix)
            assertTrue(filter.labelAr.isNotBlank())
            assertTrue(filter.labelEn.isNotBlank())

            val partialMatrix = filter.getColorMatrix(0.5f)
            assertNotNull(partialMatrix)
        }
    }

    @Test
    fun testCropRectDimensions() {
        val fullCrop = StudioCropRect()
        assertEquals(1.0f, fullCrop.width, 0.001f)
        assertEquals(1.0f, fullCrop.height, 0.001f)

        val partialCrop = StudioCropRect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        assertEquals(0.7f, partialCrop.width, 0.001f)
        assertEquals(0.7f, partialCrop.height, 0.001f)
    }

    @Test
    fun testArabicTextLayerModel() {
        val arabicLayer = StudioTextLayer(
            text = "تصميم احترافي سامسونج One UI",
            style = StudioTextStyle.BOLD,
            backgroundMode = StudioTextBackgroundMode.SOLID
        )
        assertEquals("تصميم احترافي سامسونج One UI", arabicLayer.text)
        assertEquals(StudioTextStyle.BOLD, arabicLayer.style)
        assertEquals(StudioTextBackgroundMode.SOLID, arabicLayer.backgroundMode)
        assertEquals(0.5f, arabicLayer.normalizedX, 0.001f)
        assertEquals(0.5f, arabicLayer.normalizedY, 0.001f)
    }

    @Test
    fun testDoodlePathModel() {
        val points = listOf(0.1f to 0.1f, 0.2f to 0.2f, 0.3f to 0.5f)
        val doodle = StudioDoodlePath(
            points = points,
            isHighlighter = true
        )
        assertEquals(3, doodle.points.size)
        assertTrue(doodle.isHighlighter)
        assertFalse(doodle.isEraser)
    }

    @Test
    fun testUnifiedColorMatrixCalculation() {
        val state = StudioPhotoEditState(
            brightness = 15f,
            contrast = 20f,
            saturation = 10f,
            warmth = 5f,
            filterType = StudioFilterType.VIVID
        )
        val matrix = state.buildUnifiedColorMatrix()
        assertNotNull(matrix)
    }
}
