package com.progalleryx.app.studioplus.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progalleryx.app.studioplus.core.data.StudioFavoritesStore
import com.progalleryx.app.studioplus.core.data.StudioMediaRepository
import com.progalleryx.app.studioplus.core.model.StudioAlbumItem
import com.progalleryx.app.studioplus.core.model.StudioMediaItem
import com.progalleryx.app.studioplus.core.model.StudioTimelineSection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StudioTab {
    PICTURES,
    ALBUMS,
    STUDIO_PLUS
}

data class StudioPlusUiState(
    val currentTab: StudioTab = StudioTab.PICTURES,
    val allMedia: List<StudioMediaItem> = emptyList(),
    val timelineSections: List<StudioTimelineSection> = emptyList(),
    val albums: List<StudioAlbumItem> = emptyList(),
    val selectedAlbum: StudioAlbumItem? = null,
    val activeViewingIndex: Int? = null,
    val activeVideoItem: StudioMediaItem? = null,
    val directIncomingVideoUri: Uri? = null,
    val directIncomingImageUri: Uri? = null,
    val selectedItemIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val searchQuery: String = "",
    val gridColumns: Int = 3,
    val isLoading: Boolean = true,
    val isArabic: Boolean = false
)

class StudioPlusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudioMediaRepository(application)
    private val favoritesStore = StudioFavoritesStore(application)

    private val _uiState = MutableStateFlow(StudioPlusUiState())
    val uiState: StateFlow<StudioPlusUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun setLocaleArabic(isArabic: Boolean) {
        if (_uiState.value.isArabic != isArabic) {
            _uiState.value = _uiState.value.copy(isArabic = isArabic)
            refreshSections(_uiState.value.allMedia, isArabic)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            combine(repository.observeAllMedia(), favoritesStore.favorites) { mediaList, favSet ->
                mediaList.map { item ->
                    item.copy(isFavorite = favSet.contains(item.id))
                }
            }.collectLatest { enrichedMedia ->
                val albums = repository.extractAlbums(enrichedMedia)
                val sections = repository.groupIntoSections(enrichedMedia, _uiState.value.isArabic)
                _uiState.value = _uiState.value.copy(
                    allMedia = enrichedMedia,
                    timelineSections = sections,
                    albums = albums,
                    isLoading = false
                )
            }
        }
    }

    private fun refreshSections(items: List<StudioMediaItem>, isArabic: Boolean) {
        val sections = repository.groupIntoSections(items, isArabic)
        _uiState.value = _uiState.value.copy(timelineSections = sections)
    }

    fun selectTab(tab: StudioTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab, selectedAlbum = null)
    }

    fun openAlbum(album: StudioAlbumItem) {
        _uiState.value = _uiState.value.copy(selectedAlbum = album)
    }

    fun closeAlbum() {
        _uiState.value = _uiState.value.copy(selectedAlbum = null)
    }

    fun openImageViewer(index: Int) {
        _uiState.value = _uiState.value.copy(activeViewingIndex = index)
    }

    fun closeImageViewer() {
        _uiState.value = _uiState.value.copy(activeViewingIndex = null, directIncomingImageUri = null)
    }

    fun openVideoPlayer(item: StudioMediaItem) {
        _uiState.value = _uiState.value.copy(activeVideoItem = item)
    }

    fun openDirectIncomingVideo(uri: Uri) {
        _uiState.value = _uiState.value.copy(directIncomingVideoUri = uri)
    }

    fun openDirectIncomingImage(uri: Uri) {
        _uiState.value = _uiState.value.copy(directIncomingImageUri = uri)
    }

    fun closeVideoPlayer() {
        _uiState.value = _uiState.value.copy(activeVideoItem = null, directIncomingVideoUri = null)
    }

    fun toggleFavorite(item: StudioMediaItem) {
        favoritesStore.toggleFavorite(item.id)
    }

    fun setGridColumns(cols: Int) {
        _uiState.value = _uiState.value.copy(gridColumns = cols.coerceIn(2, 5))
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSelection(itemId: Long) {
        val current = _uiState.value.selectedItemIds.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _uiState.value = _uiState.value.copy(
            selectedItemIds = current,
            isSelectionMode = current.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItemIds = emptySet(),
            isSelectionMode = false
        )
    }
}
