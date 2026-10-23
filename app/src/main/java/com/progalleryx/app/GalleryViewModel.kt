package com.progalleryx.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryState(
    val items: List<GalleryMedia> = emptyList(),
    val scanOffset: Int = 0,
    val albums: List<GalleryAlbum> = emptyList(),
    val albumId: Long? = null,
    val filter: MediaFilter = MediaFilter.ALL,
    val search: String = "",
    val loading: Boolean = false,
    val end: Boolean = false,
    val error: String? = null
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = MediaRepository(app)
    private val favorites = FavoritesStore(app)
    private val mutable = MutableStateFlow(GalleryState())
    val state = mutable.asStateFlow()
    private var request: Job? = null
    private var generation = 0L

    fun isFavorite(item: GalleryMedia): Boolean = favorites.contains(item.uri)

    fun toggleFavorite(item: GalleryMedia) {
        favorites.toggle(item.uri)
        if (mutable.value.filter == MediaFilter.FAVORITES) reload() 
    }

    fun loadAlbums() { viewModelScope.launch { runCatching { repository.albums() }.onSuccess { mutable.value = mutable.value.copy(albums = it) } } }
    fun setAlbum(albumId: Long?) { reload(album = albumId) }
    fun setSearch(value: String) {
        val normalized = value.trim().take(80)
        if (mutable.value.search != normalized) reload(search = normalized)
    }

    fun setFilter(filter: MediaFilter) {
        if (mutable.value.filter != filter) reload(filter)
    }

    fun reload(filter: MediaFilter = mutable.value.filter, album: Long? = mutable.value.albumId, search: String = mutable.value.search) {
        request?.cancel()
        generation++
        mutable.value = GalleryState(filter = filter, albumId = album, search = search, albums = mutable.value.albums)
        loadMore()
    }

    fun loadMore() {
        val current = mutable.value
        if (current.loading || current.end) return
        val token = generation
        mutable.value = current.copy(loading = true, error = null)
        request = viewModelScope.launch {
            try {
                val batch = if (current.filter == MediaFilter.FAVORITES) {
                    favorites.savedUris().drop(current.scanOffset).take(80).mapNotNull { uri ->
                        runCatching {
                            GalleryMedia(android.content.ContentUris.parseId(uri), uri, uri.pathSegments.contains("video"), 0L)
                        }.getOrNull()
                    }
                } else repository.page(current.scanOffset, filter = current.filter, albumId = current.albumId, search = current.search)
                if (token != generation) return@launch
                // Some Android providers ignore offset arguments. Do not create
                // an infinite list of repeated items in that situation.
                val existing = current.items.mapTo(HashSet()) { it.uri }
                val unique = batch.filter { existing.add(it.uri) }
                mutable.value = current.copy(
                    items = current.items + unique,
                    scanOffset = current.scanOffset + batch.size,
                    loading = false,
                    end = batch.size < 80 || unique.isEmpty()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (token == generation)
                    mutable.value = current.copy(loading = false, error = e.localizedMessage ?: "تعذر قراءة الصور")
            }
        }
    }
}
