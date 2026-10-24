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
enum class MediaSort { NEWEST, OLDEST }

data class GalleryAlbum(
    val id: Long,
    val title: String,
    val count: Int,
    val coverUri: Uri? = null
)

data class GalleryMedia(
    val id: Long,
    val uri: Uri,
    val video: Boolean,
    val date: Long,
    val durationMs: Long = 0L,
    val displayName: String = "",
    val sizeBytes: Long = 0L,
    val bucketId: Long = 0L,
    val bucketName: String = ""
)

class MediaRepository(private val context: Context) {

    suspend fun albums(): List<GalleryAlbum> = withContext(Dispatchers.IO) {
        val buckets = linkedMapOf<Long, Triple<String, Int, Uri?>>()
        val idColName = MediaStore.Files.FileColumns._ID
        val typeColName = MediaStore.Files.FileColumns.MEDIA_TYPE
        val bucketIdColName = MediaStore.MediaColumns.BUCKET_ID
        val bucketNameColName = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        val dateColName = MediaStore.Files.FileColumns.DATE_ADDED
        val projection = arrayOf(idColName, typeColName, bucketIdColName, bucketNameColName, dateColName)

        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )
            )
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(dateColName))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
        }

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"), projection,
            args, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(idColName)
            val typeCol = cursor.getColumnIndexOrThrow(typeColName)
            val bucketIdCol = cursor.getColumnIndexOrThrow(bucketIdColName)
            val bucketNameCol = cursor.getColumnIndexOrThrow(bucketNameColName)
            val videos = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdCol)
                val id = cursor.getLong(idCol)
                val isVideo = cursor.getInt(typeCol) == videos
                val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val itemUri = ContentUris.withAppendedId(base, id)

                val previous = buckets[bucketId]
                if (previous == null) {
                    val name = cursor.getString(bucketNameCol).orEmpty().ifBlank { "مجلد" }
                    buckets[bucketId] = Triple(name, 1, itemUri)
                } else {
                    buckets[bucketId] = Triple(previous.first, previous.second + 1, previous.third ?: itemUri)
                }
            }
        }
        buckets.map { (id, data) -> GalleryAlbum(id, data.first, data.second, data.third) }
            .sortedBy { it.title.lowercase() }
    }

    /** Queries at most [count] records from MediaStore. Bounded and non-blocking. */
    suspend fun page(
        offset: Int,
        count: Int = 80,
        filter: MediaFilter = MediaFilter.ALL,
        albumId: Long? = null,
        search: String = "",
        sort: MediaSort = MediaSort.NEWEST
    ): List<GalleryMedia> = withContext(Dispatchers.IO) {
        require(offset >= 0 && count in 1..200)
        val idColumn = MediaStore.Files.FileColumns._ID
        val typeColumn = MediaStore.Files.FileColumns.MEDIA_TYPE
        val dateColumn = MediaStore.Files.FileColumns.DATE_ADDED
        val nameColumn = MediaStore.MediaColumns.DISPLAY_NAME
        val sizeColumn = MediaStore.MediaColumns.SIZE
        val bucketIdColumn = MediaStore.MediaColumns.BUCKET_ID
        val bucketNameColumn = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        val durationColumn = "duration"

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
        val searchText = search.trim().take(80)
        val searchWhere = if (searchText.isEmpty()) albumWhere else "($albumWhere) AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val searchValues = if (searchText.isEmpty()) albumValues else albumValues + "%$searchText%"

        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, searchWhere)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, searchValues)
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(dateColumn, idColumn))
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                if (sort == MediaSort.NEWEST) ContentResolver.QUERY_SORT_DIRECTION_DESCENDING else ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, count)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }

        val projection = arrayOf(
            idColumn, typeColumn, dateColumn, nameColumn, sizeColumn,
            bucketIdColumn, bucketNameColumn, durationColumn
        )

        val result = ArrayList<GalleryMedia>(count)
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"), projection,
            args, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(idColumn)
            val typeIndex = cursor.getColumnIndexOrThrow(typeColumn)
            val dateIndex = cursor.getColumnIndexOrThrow(dateColumn)
            val nameIndex = cursor.getColumnIndex(nameColumn)
            val sizeIndex = cursor.getColumnIndex(sizeColumn)
            val bucketIdIndex = cursor.getColumnIndex(bucketIdColumn)
            val bucketNameIndex = cursor.getColumnIndex(bucketNameColumn)
            val durationIndex = cursor.getColumnIndex(durationColumn)

            while (cursor.moveToNext() && result.size < count) {
                val id = cursor.getLong(idIndex)
                val isVideo = cursor.getInt(typeIndex) == videos
                val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val durationMs = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L
                val displayName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                val sizeBytes = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                val bId = if (bucketIdIndex >= 0) cursor.getLong(bucketIdIndex) else 0L
                val bName = if (bucketNameIndex >= 0) cursor.getString(bucketNameIndex).orEmpty() else ""

                result.add(
                    GalleryMedia(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id),
                        video = isVideo,
                        date = cursor.getLong(dateIndex),
                        durationMs = durationMs,
                        displayName = displayName,
                        sizeBytes = sizeBytes,
                        bucketId = bId,
                        bucketName = bName
                    )
                )
            }
        }
        result
    }
}
