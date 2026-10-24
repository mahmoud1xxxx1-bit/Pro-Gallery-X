package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class GallerySelectionRulesTest {

    @Test
    fun testToggle() {
        val initial = setOf(1L, 2L)
        val afterAdd = GallerySelectionRules.toggle(initial, 3L)
        assertEquals(setOf(1L, 2L, 3L), afterAdd)

        val afterRemove = GallerySelectionRules.toggle(afterAdd, 2L)
        assertEquals(setOf(1L, 3L), afterRemove)
    }

    @Test
    fun testSelectAllAndClear() {
        val ids = listOf(10L, 20L, 30L)
        val all = GallerySelectionRules.selectAllIds(ids)
        assertEquals(setOf(10L, 20L, 30L), all)

        val cleared = GallerySelectionRules.clear()
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun testIsSelectionActive() {
        assertFalse(GallerySelectionRules.isSelectionActive(emptySet()))
        assertTrue(GallerySelectionRules.isSelectionActive(setOf(1L)))
    }

    @Test
    fun testCanPerformAction() {
        assertFalse(GallerySelectionRules.canPerformAction(0))
        assertTrue(GallerySelectionRules.canPerformAction(1))
        assertTrue(GallerySelectionRules.canPerformAction(50))
        assertTrue(GallerySelectionRules.canPerformAction(100))
        assertFalse(GallerySelectionRules.canPerformAction(101))
    }
}
