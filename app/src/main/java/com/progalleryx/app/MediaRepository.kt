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
data class GalleryAlbum(val id: Long, val title: String, val count: Int)
data class GalleryMedia(val id: Long, val uri: Uri, val video: Boolean, val date: Long)

class MediaRepository(private val context: Context) {
    suspend fun albums(): List<GalleryAlbum> = withContext(Dispatchers.IO) {
        val buckets = linkedMapOf<Long, Pair<String, Int>>()
        val projection = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.MediaColumns.BUCKET_ID, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"), projection,
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)",
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()),
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val previous = buckets[id]
                buckets[id] = (previous?.first ?: cursor.getString(nameCol).orEmpty().ifBlank { "مجلد" }) to ((previous?.second ?: 0) + 1)
            }
        }
        buckets.map { (id, data) -> GalleryAlbum(id, data.first, data.second) }.sortedBy { it.title.lowercase() }
    }

    /** Queries at most [count] records from MediaStore. No image bitmap is decoded here. */
    suspend fun page(offset: Int, count: Int = 80, filter: MediaFilter = MediaFilter.ALL, albumId: Long? = null): List<GalleryMedia> =
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
            val albumWhere = if (albumId == null) where else "($where) AND ${MediaStore.MediaColumns.BUCKET_ID} = ?"
            val albumValues = if (albumId == null) values else values + albumId.toString()
            val args = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, albumWhere)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, albumValues)
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
