package com.progalleryx.app

import android.content.Context
import android.net.Uri

/** Favorites are metadata saved on the device; media files are never copied or uploaded. */
class FavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences("gallery_favorites", Context.MODE_PRIVATE)
    fun contains(uri: Uri): Boolean = preferences.getBoolean(uri.toString(), false)
    fun toggle(uri: Uri): Boolean {
        val next = !contains(uri)
        preferences.edit().putBoolean(uri.toString(), next).apply()
        return next
    }
}
