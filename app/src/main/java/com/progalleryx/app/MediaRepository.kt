package com.progalleryx.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.content.ContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class MediaFilter { ALL, PHOTOS, VIDEOS, FAVORITES }
data class GalleryMedia(val id: Long, val uri: Uri, val video: Boolean, val date: Long)

class MediaRepository(private val context: Context) {
    /** Queries at most [count] records from MediaStore. No image bitmap is decoded here. */
    suspend fun page(offset: Int, count: Int = 80, filter: MediaFilter = MediaFilter.ALL): List<GalleryMedia> =
        withContext(Dispatchers.IO) {
            require(offset >= 0 && count in 1..200)
            val idColumn = MediaStore.Files.FileColumns._ID
            val typeColumn = MediaStore.Files.FileColumns.MEDIA_TYPE
            val dateColumn = MediaStore.Files.FileColumns.DATE_ADDED
            val images = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            val videos = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            val (where, values) = when (filter) {
                MediaFilter.ALL -> "$typeColumn IN (?, ?)" to arrayOf(images.toString(), videos.toString())
                MediaFilter.PHOTOS -> "$typeColumn = ?" to arrayOf(images.toString())
                MediaFilter.VIDEOS -> "$typeColumn = ?" to arrayOf(videos.toString())
                MediaFilter.FAVORITES -> "$typeColumn IN (?, ?)" to arrayOf(images.toString(), videos.toString())
            }
            val args = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, where)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, values)
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(dateColumn, idColumn))
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                putInt(ContentResolver.QUERY_ARG_LIMIT, count)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            val result = ArrayList<GalleryMedia>(count)
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"), arrayOf(idColumn, typeColumn, dateColumn),
                args, null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(idColumn)
                val typeIndex = cursor.getColumnIndexOrThrow(typeColumn)
                val dateIndex = cursor.getColumnIndexOrThrow(dateColumn)
                while (cursor.moveToNext() && result.size < count) {
                    val id = cursor.getLong(idIndex)
                    val isVideo = cursor.getInt(typeIndex) == videos
                    val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    result.add(GalleryMedia(id, ContentUris.withAppendedId(base, id), isVideo, cursor.getLong(dateIndex)))
                }
            }
            result
        }
}
