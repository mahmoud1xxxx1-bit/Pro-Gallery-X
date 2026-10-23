package com.progalleryx.app

object GallerySelectionRules {

    /** Toggles an item ID in the selection set. */
    fun toggle(selected: Set<Long>, id: Long): Set<Long> {
        return if (selected.contains(id)) {
            selected - id
        } else {
            selected + id
        }
    }

    /** Selects all IDs from a collection of IDs. */
    fun selectAllIds(ids: Collection<Long>): Set<Long> = ids.toSet()

    /** Selects all items from the given item list. */
    fun selectAll(allItems: List<GalleryMedia>): Set<Long> {
        return allItems.map { it.id }.toSet()
    }

    /** Clears all selections. */
    fun clear(): Set<Long> = emptySet()

    /** Determines if multi-selection mode is active. */
    fun isSelectionActive(selected: Set<Long>): Boolean = selected.isNotEmpty()

    /** Verifies if an action on selected items can proceed. */
    fun canPerformAction(selectedCount: Int, maxAllowed: Int = 100): Boolean {
        return selectedCount in 1..maxAllowed
    }
}
