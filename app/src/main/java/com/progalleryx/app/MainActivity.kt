package com.progalleryx.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

enum class GalleryTab {
    PICTURES,
    ALBUMS,
    STUDIO
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProGalleryApp()
        }
    }
}

@Composable
fun ProGalleryApp(vm: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val preferences = remember { AppPreferences(context) }

    var currentThemeMode by remember { mutableStateOf(preferences.themeMode) }
    var currentLanguage by remember { mutableStateOf(preferences.language) }

    val systemLocaleIsArabic = remember {
        Locale.getDefault().language.equals("ar", ignoreCase = true)
    }
    val isArabic = remember(currentLanguage, systemLocaleIsArabic) {
        AppPreferences.isArabicLanguage(currentLanguage, systemLocaleIsArabic)
    }
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Permissions handling
    val neededPermissions = remember {
        if (Build.VERSION.SDK_INT >= 34) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun permissionGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

    var permitted by remember { mutableStateOf(neededPermissions.any(::permissionGranted)) }
    var partialAccess by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= 34 &&
            permissionGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            !permissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permitted = neededPermissions.any(::permissionGranted)
        partialAccess = Build.VERSION.SDK_INT >= 34 &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (permitted) {
            vm.reload()
            vm.loadAlbums()
        }
    }

    // Refresh on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val current = neededPermissions.any(::permissionGranted)
                val limited = Build.VERSION.SDK_INT >= 34 &&
                    permissionGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
                    !permissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
                val changed = current != permitted || limited != partialAccess
                permitted = current
                partialAccess = limited
                if (changed && current) {
                    vm.reload()
                    vm.loadAlbums()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(permitted) {
        if (permitted) {
            vm.reload()
            vm.loadAlbums()
        }
    }

    // Navigation and screen states
    var currentTab by remember { mutableStateOf(GalleryTab.PICTURES) }
    var viewingIndex by remember { mutableStateOf<Int?>(null) }
    var directEditingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var directEditingVideoUri by remember { mutableStateOf<Uri?>(null) }

    var searchActive by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    LaunchedEffect(searchInput) {
        kotlinx.coroutines.delay(300L)
        vm.setSearch(searchInput)
    }

    // Trash handling
    var pendingTrashItems by remember { mutableStateOf<List<GalleryMedia>>(emptyList()) }
    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewingIndex = null
            pendingTrashItems = emptyList()
            vm.clearSelection()
            vm.reload()
        }
    }

    val gridState = rememberLazyGridState()

    // Infinite scroll pagination
    LaunchedEffect(gridState, state.items.size, state.loading, state.end, state.filter) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collectLatest { lastIndex ->
            if (state.items.isNotEmpty() && lastIndex >= state.items.lastIndex - 15 && !state.loading && !state.end) {
                vm.loadMore()
            }
        }
    }

    // Apply layout direction and theme
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        ProGalleryTheme(themeMode = currentThemeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when {
                    // 1. Direct Photo Editor Mode
                    directEditingPhotoUri != null -> {
                        var editorBusy by remember { mutableStateOf(false) }
                        var editorMessage by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            if (editorMessage.isNotBlank()) {
                                Text(
                                    text = editorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            PhotoEditorSection(
                                busy = editorBusy,
                                onBusyChange = { editorBusy = it },
                                onMessageChange = { editorMessage = it },
                                initialImageUri = directEditingPhotoUri,
                                onClose = {
                                    directEditingPhotoUri = null
                                    vm.reload()
                                }
                            )
                        }
                    }

                    // 2. Direct Video Editor Mode
                    directEditingVideoUri != null -> {
                        var editorBusy by remember { mutableStateOf(false) }
                        var editorMessage by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            if (editorMessage.isNotBlank()) {
                                Text(
                                    text = editorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            VideoEditorSection(
                                busy = editorBusy,
                                onBusyChange = { editorBusy = it },
                                onMessageChange = { editorMessage = it },
                                initialVideoUri = directEditingVideoUri,
                                onClose = {
                                    directEditingVideoUri = null
                                    vm.reload()
                                },
                                isArabic = isArabic
                            )
                        }
                    }

                    // 3. Fullscreen Media Viewer Mode
                    viewingIndex != null && state.items.isNotEmpty() -> {
                        FullscreenMediaViewer(
                            mediaList = state.items,
                            initialIndex = viewingIndex!!,
                            isArabic = isArabic,
                            onClose = { viewingIndex = null },
                            onEditPhoto = { item -> directEditingPhotoUri = item.uri },
                            onEditVideo = { item -> directEditingVideoUri = item.uri },
                            onToggleFavorite = { item -> vm.toggleFavorite(item) },
                            onTrashItem = { item -> pendingTrashItems = listOf(item) },
                            isFavorite = { item -> vm.isFavorite(item) }
                        )
                    }

                    // 4. Main Gallery Interface
                    else -> {
                        Scaffold(
                            topBar = {
                                GalleryTopBar(
                                    isSelectionMode = state.isSelectionMode,
                                    selectedCount = state.selectedIds.size,
                                    isArabic = isArabic,
                                    searchActive = searchActive,
                                    searchInput = searchInput,
                                    currentAlbum = state.albums.firstOrNull { it.id == state.albumId },
                                    currentThemeMode = currentThemeMode,
                                    currentLanguage = currentLanguage,
                                    onSearchActiveChange = { searchActive = it },
                                    onSearchInputChange = { searchInput = it },
                                    onClearAlbum = { vm.setAlbum(null) },
                                    onClearSelection = { vm.clearSelection() },
                                    onSelectAll = { vm.selectAll() },
                                    onThemeToggle = {
                                        val next = when (currentThemeMode) {
                                            ThemeMode.SYSTEM -> ThemeMode.DARK
                                            ThemeMode.DARK -> ThemeMode.LIGHT
                                            ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                        }
                                        currentThemeMode = next
                                        preferences.themeMode = next
                                    },
                                    onLanguageToggle = {
                                        val next = when (currentLanguage) {
                                            AppLanguage.SYSTEM -> if (systemLocaleIsArabic) AppLanguage.ENGLISH else AppLanguage.ARABIC
                                            AppLanguage.ARABIC -> AppLanguage.ENGLISH
                                            AppLanguage.ENGLISH -> AppLanguage.ARABIC
                                        }
                                        currentLanguage = next
                                        preferences.language = next
                                    },
                                    onRefresh = { if (permitted) vm.reload() }
                                )
                            },
                            bottomBar = {
                                if (state.isSelectionMode) {
                                    // Selection Action Bar
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 8.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedItems = state.items.filter { state.selectedIds.contains(it.id) }

                                            // Share Selected
                                            IconButton(
                                                onClick = {
                                                    if (selectedItems.isNotEmpty()) {
                                                        val uris = ArrayList(selectedItems.map { it.uri })
                                                        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                                            type = "*/*"
                                                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(
                                                            Intent.createChooser(shareIntent, Localization.share(isArabic))
                                                        )
                                                    }
                                                }
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Outlined.Share, contentDescription = Localization.share(isArabic))
                                                    Text(Localization.share(isArabic), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }

                                            // Trash Selected
                                            IconButton(
                                                onClick = {
                                                    if (selectedItems.isNotEmpty()) {
                                                        pendingTrashItems = selectedItems
                                                    }
                                                }
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = Localization.delete(isArabic), tint = Color(0xFFFF5252))
                                                    Text(Localization.delete(isArabic), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF5252))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Samsung Gallery Standard Bottom Navigation
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 6.dp
                                    ) {
                                        NavigationBarItem(
                                            selected = currentTab == GalleryTab.PICTURES,
                                            onClick = { currentTab = GalleryTab.PICTURES },
                                            icon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                                            label = { Text(Localization.tabPictures(isArabic)) }
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == GalleryTab.ALBUMS,
                                            onClick = {
                                                currentTab = GalleryTab.ALBUMS
                                                vm.loadAlbums()
                                            },
                                            icon = { Icon(Icons.Outlined.PhotoAlbum, contentDescription = null) },
                                            label = { Text(Localization.tabAlbums(isArabic)) }
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == GalleryTab.STUDIO,
                                            onClick = { currentTab = GalleryTab.STUDIO },
                                            icon = { Icon(Icons.Outlined.AutoFixHigh, contentDescription = null) },
                                            label = { Text(Localization.tabStudio(isArabic)) }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (!permitted) {
                                    // Permission Prompt
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Outlined.Collections,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(68.dp)
                                        )
                                        Spacer(Modifier.height(20.dp))
                                        Text(
                                            text = Localization.permissionTitle(isArabic),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = Localization.permissionBody(isArabic),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(24.dp))
                                        Button(onClick = { permissionLauncher.launch(neededPermissions) }) {
                                            Text(Localization.permissionButton(isArabic))
                                        }
                                    }
                                } else {
                                    when (currentTab) {
                                        GalleryTab.PICTURES -> {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                // Quick Filter Chips Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    FilterChip(
                                                        selected = state.filter == MediaFilter.ALL,
                                                        onClick = { vm.setFilter(MediaFilter.ALL) },
                                                        label = { Text(Localization.filterAll(isArabic)) }
                                                    )
                                                    FilterChip(
                                                        selected = state.filter == MediaFilter.PHOTOS,
                                                        onClick = { vm.setFilter(MediaFilter.PHOTOS) },
                                                        label = { Text(Localization.filterPhotos(isArabic)) }
                                                    )
                                                    FilterChip(
                                                        selected = state.filter == MediaFilter.VIDEOS,
                                                        onClick = { vm.setFilter(MediaFilter.VIDEOS) },
                                                        label = { Text(Localization.filterVideos(isArabic)) }
                                                    )
                                                    FilterChip(
                                                        selected = state.filter == MediaFilter.FAVORITES,
                                                        onClick = { vm.setFilter(MediaFilter.FAVORITES) },
                                                        label = { Text(Localization.filterFavorites(isArabic)) }
                                                    )

                                                    Spacer(Modifier.weight(1f))

                                                    IconButton(
                                                        onClick = {
                                                            val nextSort = if (state.sort == MediaSort.NEWEST) MediaSort.OLDEST else MediaSort.NEWEST
                                                            vm.setSort(nextSort)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = if (state.sort == MediaSort.NEWEST) Icons.Outlined.South else Icons.Outlined.North,
                                                            contentDescription = if (state.sort == MediaSort.NEWEST) Localization.sortNewest(isArabic) else Localization.sortOldest(isArabic),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                if (partialAccess) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = Localization.limitedPermission(isArabic),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        TextButton(onClick = { permissionLauncher.launch(neededPermissions) }) {
                                                            Text(Localization.updatePermission(isArabic))
                                                        }
                                                    }
                                                }

                                                // Timeline Grid
                                                if (state.items.isEmpty() && !state.loading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(32.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.PhotoLibrary,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                                modifier = Modifier.size(56.dp)
                                                            )
                                                            Text(
                                                                text = if (state.search.isNotBlank()) Localization.emptySearch(isArabic)
                                                                else if (state.filter == MediaFilter.FAVORITES) Localization.emptyFavorites(isArabic)
                                                                else Localization.emptyGallery(isArabic),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    GalleryTimelineGrid(
                                                        items = state.items,
                                                        selectedIds = state.selectedIds,
                                                        isArabic = isArabic,
                                                        gridState = gridState,
                                                        onItemClick = { item ->
                                                            val idx = state.items.indexOfFirst { it.id == item.id }
                                                            if (idx >= 0) viewingIndex = idx
                                                        },
                                                        onItemLongClick = { item ->
                                                            vm.toggleSelection(item.id)
                                                        },
                                                        onSelectionToggle = { id -> vm.toggleSelection(id) },
                                                        onSelectDateGroup = { groupItems ->
                                                            val allSelected = groupItems.all { state.selectedIds.contains(it.id) }
                                                            groupItems.forEach { item ->
                                                                if (allSelected) {
                                                                    if (state.selectedIds.contains(item.id)) vm.toggleSelection(item.id)
                                                                } else {
                                                                    if (!state.selectedIds.contains(item.id)) vm.toggleSelection(item.id)
                                                                }
                                                            }
                                                        },
                                                        loading = state.loading
                                                    )
                                                }
                                            }
                                        }

                                        GalleryTab.ALBUMS -> {
                                            GalleryAlbumsView(
                                                albums = state.albums,
                                                isArabic = isArabic,
                                                onAlbumSelected = { albumId ->
                                                    vm.setAlbum(albumId)
                                                    currentTab = GalleryTab.PICTURES
                                                }
                                            )
                                        }

                                        GalleryTab.STUDIO -> {
                                            OfflineToolsPanel(onClose = { currentTab = GalleryTab.PICTURES })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Safe Trash Dialog
                if (pendingTrashItems.isNotEmpty()) {
                    AlertDialog(
                        onDismissRequest = { pendingTrashItems = emptyList() },
                        title = { Text(Localization.confirmTrashTitle(isArabic)) },
                        text = { Text(Localization.confirmTrashBody(isArabic)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val items = pendingTrashItems
                                    pendingTrashItems = emptyList()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        runCatching {
                                            val request = MediaStore.createTrashRequest(
                                                context.contentResolver,
                                                items.map { it.uri },
                                                true
                                            )
                                            trashLauncher.launch(
                                                IntentSenderRequest.Builder(request.intentSender).build()
                                            )
                                        }
                                    }
                                }
                            ) {
                                Text(Localization.continueAction(isArabic))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingTrashItems = emptyList() }) {
                                Text(Localization.cancel(isArabic))
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Professional Gallery Top Bar inspired by Samsung OneUI */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    isArabic: Boolean,
    searchActive: Boolean,
    searchInput: String,
    currentAlbum: GalleryAlbum?,
    currentThemeMode: ThemeMode,
    currentLanguage: AppLanguage,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchInputChange: (String) -> Unit,
    onClearAlbum: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onThemeToggle: () -> Unit,
    onLanguageToggle: () -> Unit,
    onRefresh: () -> Unit
) {
    if (isSelectionMode) {
        TopAppBar(
            title = {
                Text(
                    text = Localization.selectedCount(selectedCount, isArabic),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Outlined.Close, contentDescription = Localization.close(isArabic))
                }
            },
            actions = {
                TextButton(onClick = onSelectAll) {
                    Text(Localization.selectAll(isArabic))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    } else if (searchActive) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = onSearchInputChange,
                    placeholder = { Text(Localization.searchPlaceholder(isArabic)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    onSearchActiveChange(false)
                    onSearchInputChange("")
                }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = Localization.back(isArabic))
                }
            },
            actions = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = { onSearchInputChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = Localization.clearSearch(isArabic))
                    }
                }
            }
        )
    } else {
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Localization.appTitle(isArabic),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (currentAlbum != null) {
                            Text(
                                text = " • ${currentAlbum.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = Localization.appSubtitle(isArabic),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                if (currentAlbum != null) {
                    IconButton(onClick = onClearAlbum) {
                        Icon(Icons.Outlined.FilterAltOff, contentDescription = "Clear Album Filter")
                    }
                }

                // Search Toggle
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                }

                // Theme Toggle
                IconButton(onClick = onThemeToggle) {
                    val icon = when (currentThemeMode) {
                        ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                        ThemeMode.LIGHT -> Icons.Filled.LightMode
                        ThemeMode.DARK -> Icons.Filled.DarkMode
                    }
                    Icon(icon, contentDescription = "Toggle Theme", tint = MaterialTheme.colorScheme.onSurface)
                }

                // Language Toggle (AR / EN)
                TextButton(onClick = onLanguageToggle) {
                    Text(
                        text = if (isArabic) "EN" else "عربي",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Refresh Button
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = Localization.refresh(isArabic))
                }
            }
        )
    }
}
