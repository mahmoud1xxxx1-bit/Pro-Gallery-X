package com.progalleryx.app.studioplus.core.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudioFavoritesStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("studio_plus_favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        val stringSet = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        _favorites.value = stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun isFavorite(mediaId: Long): Boolean = _favorites.value.contains(mediaId)

    fun toggleFavorite(mediaId: Long): Boolean {
        val current = _favorites.value.toMutableSet()
        val newState = if (current.contains(mediaId)) {
            current.remove(mediaId)
            false
        } else {
            current.add(mediaId)
            true
        }
        _favorites.value = current
        prefs.edit().putStringSet("favorite_ids", current.map { it.toString() }.toSet()).apply()
        return newState
    }
}
