package com.progalleryx.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class GalleryViewModel(app: Application): AndroidViewModel(app) {
    private val repository = MediaRepository(app)
    private val mutable = MutableStateFlow(GalleryState())
    val state = mutable.asStateFlow()
    private var request: Job? = null
    fun setFilter(filter: MediaFilter) {
        if (mutable.value.filter == filter) return
        reload(filter)
    }
    fun reload(filter: MediaFilter = mutable.value.filter) {
        request?.cancel()
        mutable.value = GalleryState(filter = filter)
        loadMore()
    }
    fun loadMore() {
        val current = mutable.value
        if (current.loading || current.end) return
        mutable.value = current.copy(loading = true, error = null)
        request = viewModelScope.launch {
            try {
                val batch = repository.page(current.items.size, filter = current.filter)
                // Ignore stale results after permission changes or filter switches.
                if (mutable.value.filter != current.filter) return@launch
                mutable.value = mutable.value.copy(
                    items = current.items + batch,
                    loading = false,
                    end = batch.size < 80
                )
            } catch (e: Exception) {
                if (mutable.value.filter == current.filter)
                    mutable.value = mutable.value.copy(loading = false, error = e.localizedMessage ?: "خطأ في قراءة الملفات")
            }
        }
    }
}
