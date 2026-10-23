package com.progalleryx.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTextLayerTest {
    @Test fun validatesTextLayerBoundaries() {
        assertTrue(PhotoTextLayer("caption", 0.5f, 0.5f, 32, false).valid())
        assertTrue(PhotoTextLayer("", 0.05f, 0.95f, 16, true).valid())
        assertFalse(PhotoTextLayer("caption", -0.1f, 0.5f, 32, false).valid())
        assertFalse(PhotoTextLayer("caption", 0.5f, 1.1f, 32, false).valid())
        assertFalse(PhotoTextLayer("caption", 0.5f, 0.5f, 73, false).valid())
        assertFalse(PhotoTextLayer("x".repeat(81)).valid())
    }
}
