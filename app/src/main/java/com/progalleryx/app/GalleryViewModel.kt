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
    val filter: MediaFilter = MediaFilter.ALL,
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

    fun setFilter(filter: MediaFilter) {
        if (mutable.value.filter != filter) reload(filter)
    }

    fun reload(filter: MediaFilter = mutable.value.filter) {
        request?.cancel()
        generation++
        mutable.value = GalleryState(filter = filter)
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
                    // Favorite metadata is local; query a bounded batch of saved items.
                    repository.page(current.items.size, filter = MediaFilter.ALL)
                } else repository.page(current.items.size, filter = current.filter)
                if (token != generation) return@launch
                // Some Android providers ignore offset arguments. Do not create
                // an infinite list of repeated items in that situation.
                val existing = current.items.mapTo(HashSet()) { it.uri }
                val unique = batch.filter { existing.add(it.uri) && (current.filter != MediaFilter.FAVORITES || favorites.contains(it.uri)) }
                mutable.value = current.copy(
                    items = current.items + unique,
                    loading = false,
                    end = batch.size < 80 || (unique.isEmpty() && current.filter != MediaFilter.FAVORITES)
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
