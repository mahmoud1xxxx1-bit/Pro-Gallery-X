package com.progalleryx.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryState(
    val items: List<GalleryMedia> = emptyList(),
    val loading: Boolean = false,
    val end: Boolean = false,
    val error: String? = null
)

class GalleryViewModel(app: Application): AndroidViewModel(app) {
    private val repository = MediaRepository(app)
    private val mutable = MutableStateFlow(GalleryState())
    val state = mutable.asStateFlow()
    fun reload() {
        mutable.value = GalleryState()
        loadMore()
    }
    fun loadMore() {
        val current = mutable.value
        if (current.loading || current.end) return
        mutable.value = current.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val batch = repository.page(current.items.size)
                mutable.value = mutable.value.copy(
                    items = current.items + batch,
                    loading = false,
                    end = batch.size < 80
                )
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(loading = false, error = e.localizedMessage ?: "خطأ في قراءة الملفات")
            }
        }
    }
}
