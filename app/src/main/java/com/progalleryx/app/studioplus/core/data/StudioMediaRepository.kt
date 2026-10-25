package com.progalleryx.app.studioplus.core.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.progalleryx.app.studioplus.core.model.StudioAlbumItem
import com.progalleryx.app.studioplus.core.model.StudioMediaItem
import com.progalleryx.app.studioplus.core.model.StudioTimelineSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Enterprise-grade MediaStore repository for Studio Plus +.
 * Completely isolated from legacy code.
 * Zero-crash, high-throughput, and supports real-time ContentObserver updates.
 */
class StudioMediaRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Flow of all media items with real-time observation of MediaStore changes.
     */
    fun observeAllMedia(): Flow<List<StudioMediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                trySend(fetchAllMedia())
            }
        }

        // Send initial data
        trySend(fetchAllMedia())

        // Register observers for both Images and Videos
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Synchronous fetch on IO thread.
     */
    suspend fun loadAllMedia(): List<StudioMediaItem> = withContext(Dispatchers.IO) {
        fetchAllMedia()
    }

    private fun fetchAllMedia(): List<StudioMediaItem> {
        val items = ArrayList<StudioMediaItem>(500)
        fetchImages(items)
        fetchVideos(items)
        // Sort descending by date (newest first)
        items.sortWith { a, b ->
            val timeA = if (a.dateTakenMs > 0) a.dateTakenMs else a.dateModifiedSec * 1000L
            val timeB = if (b.dateTakenMs > 0) b.dateTakenMs else b.dateModifiedSec * 1000L
            timeB.compareTo(timeA)
        }
        return items
    }

    private fun fetchImages(dest: MutableList<StudioMediaItem>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateModCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val orientCol = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val name = cursor.getString(nameCol) ?: "IMG_$id.jpg"
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val orient = if (orientCol >= 0) cursor.getInt(orientCol) else 0
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else ""
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Pictures" else "Pictures"

                    dest.add(
                        StudioMediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            mimeType = mime,
                            dateTakenMs = dateTaken,
                            dateModifiedSec = dateMod,
                            sizeBytes = size,
                            durationMs = 0L,
                            width = width,
                            height = height,
                            orientation = orient,
                            bucketId = bucketId,
                            bucketDisplayName = bucketName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Graceful fallback: return partial or empty list without crashing
        }
    }

    private fun fetchVideos(dest: MutableList<StudioMediaItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val name = cursor.getString(nameCol) ?: "VID_$id.mp4"
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val dur = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else ""
                    val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Videos" else "Videos"

                    dest.add(
                        StudioMediaItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            mimeType = mime,
                            dateTakenMs = dateTaken,
                            dateModifiedSec = dateMod,
                            sizeBytes = size,
                            durationMs = dur,
                            width = width,
                            height = height,
                            orientation = 0,
                            bucketId = bucketId,
                            bucketDisplayName = bucketName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    /**
     * Groups media items into timeline sections (Today, Yesterday, Date).
     */
    fun groupIntoSections(items: List<StudioMediaItem>, isArabic: Boolean): List<StudioTimelineSection> {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayYear = calendar.get(Calendar.YEAR)
        val yesterdayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        val groups = LinkedHashMap<String, MutableList<StudioMediaItem>>()
        val groupTitles = HashMap<String, String>()

        val itemCal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("d MMMM yyyy", if (isArabic) Locale.forLanguageTag("ar") else Locale.getDefault())

        for (item in items) {
            val timestamp = if (item.dateTakenMs > 0) item.dateTakenMs else item.dateModifiedSec * 1000L
            itemCal.timeInMillis = timestamp
            val y = itemCal.get(Calendar.YEAR)
            val d = itemCal.get(Calendar.DAY_OF_YEAR)

            val key = "$y-$d"
            val title = when {
                y == todayYear && d == todayDayOfYear -> if (isArabic) "اليوم" else "Today"
                y == yesterdayYear && d == yesterdayDayOfYear -> if (isArabic) "أمس" else "Yesterday"
                else -> dateFormat.format(Date(timestamp))
            }

            groups.getOrPut(key) { ArrayList() }.add(item)
            groupTitles[key] = title
        }

        return groups.map { (key, sectionItems) ->
            StudioTimelineSection(
                title = groupTitles[key] ?: key,
                dateKey = key,
                items = sectionItems
            )
        }
    }

    /**
     * Aggregates media items into album items.
     */
    fun extractAlbums(items: List<StudioMediaItem>): List<StudioAlbumItem> {
        val map = LinkedHashMap<String, MutableList<StudioMediaItem>>()
        for (item in items) {
            val bId = if (item.bucketId.isNotBlank()) item.bucketId else "default"
            map.getOrPut(bId) { ArrayList() }.add(item)
        }

        return map.map { (bucketId, bucketItems) ->
            val first = bucketItems.first()
            StudioAlbumItem(
                bucketId = bucketId,
                name = first.bucketDisplayName.ifBlank { "Album" },
                coverUri = first.uri,
                itemCount = bucketItems.size,
                lastModified = bucketItems.maxOfOrNull { it.dateModifiedSec } ?: 0L
            )
        }.sortedByDescending { it.lastModified }
    }

    /**
     * Safe media deletion conforming to Android Scoped Storage.
     * On Android 11+ (API 30+), returns a PendingIntent that the Activity must launch.
     * On Android 10 and below, deletes directly.
     */
    suspend fun createDeleteRequest(uris: List<Uri>): PendingIntent? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createDeleteRequest(contentResolver, uris)
            } catch (e: Exception) {
                null
            }
        } else {
            uris.forEach { uri ->
                try {
                    contentResolver.delete(uri, null, null)
                } catch (_: Exception) {}
            }
            null
        }
    }
}
