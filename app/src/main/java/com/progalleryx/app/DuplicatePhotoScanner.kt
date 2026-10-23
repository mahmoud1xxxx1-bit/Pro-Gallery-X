package com.progalleryx.app

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class DuplicatePhoto(val uri: Uri, val name: String, val bytes: Long)
data class DuplicateGroup(val photos: List<DuplicatePhoto>) {
    val reclaimableBytes: Long get() = photos.drop(1).sumOf { it.bytes }
}
data class DuplicateScan(val groups: List<DuplicateGroup>, val examined: Int, val skipped: Int, val truncated: Boolean) {
    val reclaimableBytes: Long get() = groups.sumOf { it.reclaimableBytes }
}

/** Exact-byte duplicate detection. Never deletes or modifies any media. */
object DuplicatePhotoScanner {
    suspend fun scan(context: Context, maxCandidates: Int = 3000): DuplicateScan = withContext(Dispatchers.IO) {
        require(maxCandidates in 1..5000)
        val resolver = context.contentResolver
        val entries = mutableListOf<DuplicatePhoto>()
        var truncated = false
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DISPLAY_NAME)
        resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
            "${MediaStore.Images.Media.SIZE} > 0", null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val size = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                if (entries.size == maxCandidates) { truncated = true; break }
                entries += DuplicatePhoto(
                    android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(id)),
                    cursor.getString(name).orEmpty(), cursor.getLong(size))
            }
        }
        val hashes = mutableMapOf<String, MutableList<DuplicatePhoto>>()
        var skipped = 0
        for (bucket in entries.groupBy { it.bytes }.values.filter { it.size > 1 }) {
            for (photo in bucket) {
                currentCoroutineContext().ensureActive()
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    resolver.openInputStream(photo.uri)?.use { stream ->
                        val bytes = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val amount = stream.read(bytes)
                            if (amount < 0) break
                            if (amount > 0) digest.update(bytes, 0, amount)
                        }
                    } ?: throw java.io.IOException("Cannot read image")
                    val fingerprint = photo.bytes.toString() + ":" + digest.digest().joinToString("") { "%02x".format(it) }
                    hashes.getOrPut(fingerprint) { mutableListOf() }.add(photo)
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { skipped++ }
            }
        }
        val matches = hashes.values.filter { it.size > 1 }
            .map { DuplicateGroup(it.toList()) }
            .sortedByDescending { it.reclaimableBytes }
        DuplicateScan(matches, entries.size, skipped, truncated)
    }
}
