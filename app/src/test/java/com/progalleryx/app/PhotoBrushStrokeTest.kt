package com.progalleryx.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoBrushStrokeTest {
    @Test fun normalizedBrushPointsValidate() {
        assertTrue(BrushStroke(listOf(BrushPoint(0f, 1f)), width = 0.01f).valid())
        assertFalse(BrushStroke(emptyList()).valid())
        assertFalse(BrushStroke(listOf(BrushPoint(-0.1f, 0.5f))).valid())
        assertFalse(BrushStroke(listOf(BrushPoint(0.5f, 1.5f))).valid())
        assertFalse(BrushStroke(listOf(BrushPoint(0.5f, 0.5f)), width = 0.06f).valid())
    }
}
