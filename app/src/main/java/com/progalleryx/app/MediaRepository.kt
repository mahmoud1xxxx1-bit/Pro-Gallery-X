package com.progalleryx.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GalleryMedia(val id: Long, val uri: Uri, val video: Boolean, val date: Long)

class MediaRepository(private val context: Context) {
    /** Each page contains at most 80 items. Images themselves are loaded lazily by Coil. */
    suspend fun page(offset: Int, count: Int = 80): List<GalleryMedia> = withContext(Dispatchers.IO) {
        val columns = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )
        val images = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        val videos = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val media = mutableListOf<GalleryMedia>()
        // Cursor navigation avoids allocating one object for every file in large libraries.
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"), columns, selection,
            arrayOf(images.toString(), videos.toString()),
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns._ID} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            if (cursor.moveToPosition(offset)) {
                do {
                    val id = cursor.getLong(idCol)
                    val isVideo = cursor.getInt(typeCol) == videos
                    val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                               else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    media.add(GalleryMedia(id, ContentUris.withAppendedId(base, id), isVideo, cursor.getLong(dateCol)))
                } while (media.size < count && cursor.moveToNext())
            }
        }
        media
    }
}
