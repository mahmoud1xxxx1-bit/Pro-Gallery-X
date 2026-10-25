package com.progalleryx.app.studioplus.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progalleryx.app.AboutAppDialog
import com.progalleryx.app.R
import com.progalleryx.app.studioplus.core.data.StudioMediaRepository
import com.progalleryx.app.studioplus.core.model.StudioMediaItem
import com.progalleryx.app.studioplus.core.theme.StudioPitchBlack
import com.progalleryx.app.studioplus.core.theme.StudioPlusTheme
import com.progalleryx.app.studioplus.ui.gallery.StudioAlbumsScreen
import com.progalleryx.app.studioplus.ui.gallery.StudioTimelineScreen
import com.progalleryx.app.studioplus.ui.hub.StudioPlusHubScreen
import com.progalleryx.app.studioplus.ui.player.StudioSamsungPlayerScreen
import com.progalleryx.app.studioplus.ui.viewer.StudioImageViewerScreen
import com.progalleryx.app.studioplus.feature.photoeditor.ui.StudioPhotoEditorScreen
import android.widget.Toast
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun StudioPlusApp(
    viewModel: StudioPlusViewModel = viewModel(),
    intentState: State<Intent?>? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val repository = remember { StudioMediaRepository(context) }

    // Locale and layout direction (Arabic RTL support)
    val systemLocaleIsArabic = remember {
        Locale.getDefault().language.equals("ar", ignoreCase = true)
    }
    LaunchedEffect(systemLocaleIsArabic) {
        viewModel.setLocaleArabic(systemLocaleIsArabic)
    }
    val layoutDirection = if (systemLocaleIsArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Permissions check
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

    fun hasPermission(p: String): Boolean =
        context.checkSelfPermission(p) == android.content.pm.PackageManager.PERMISSION_GRANTED

    var isPermitted by remember { mutableStateOf(neededPermissions.any(::hasPermission)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isPermitted = neededPermissions.any(::hasPermission)
    }

    // Refresh on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isPermitted = neededPermissions.any(::hasPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Deletion launcher for Scoped Storage (Android 11+)
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.closeImageViewer()
        }
    }

    // System Intent Dispatching (ACTION_VIEW, ACTION_PICK, ACTION_GET_CONTENT)
    val currentIntent = intentState?.value ?: (context as? Activity)?.intent
    LaunchedEffect(currentIntent) {
        val intent = currentIntent ?: return@LaunchedEffect
        val action = intent.action
        val data = intent.data
        val mimeType = intent.type ?: data?.let { context.contentResolver.getType(it) }

        if (data != null && data.scheme == "content") {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    data,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        if (action == Intent.ACTION_VIEW && data != null) {
            if (mimeType?.startsWith("video/") == true) {
                viewModel.openDirectIncomingVideo(data)
            } else if (mimeType?.startsWith("image/") == true) {
                viewModel.openDirectIncomingImage(data)
            }
        }

        if (action == Intent.ACTION_EDIT && data != null) {
            if (mimeType?.startsWith("image/") == true) {
                viewModel.openPhotoEditor(data)
            }
        }

        val extraTab = intent.getStringExtra("extra_tab")
        if (!extraTab.isNullOrBlank()) {
            runCatching {
                val targetTab = StudioTab.valueOf(extraTab)
                viewModel.selectTab(targetTab)
            }
        }
    }

    var showAboutDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        StudioPlusTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // If permission is missing, show clean permission request banner
                if (!isPermitted) {
                    StudioPermissionScreen(
                        onRequestPermission = {
                            permissionLauncher.launch(neededPermissions)
                        }
                    )
                    return@Surface
                }

                // Main Scaffolding
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top App Bar (Samsung One UI Large Header)
                        StudioTopBar(
                            title = when {
                                uiState.selectedAlbum != null -> uiState.selectedAlbum!!.name
                                uiState.currentTab == StudioTab.PICTURES -> stringResource(R.string.tab_pictures)
                                uiState.currentTab == StudioTab.ALBUMS -> stringResource(R.string.tab_albums)
                                else -> stringResource(R.string.tab_studio_plus)
                            },
                            subtitle = if (uiState.selectedAlbum != null) "${uiState.selectedAlbum!!.itemCount} عنصر" else null,
                            showBack = uiState.selectedAlbum != null,
                            onBack = { viewModel.closeAlbum() },
                            onAboutClick = { showAboutDialog = true }
                        )

                        // Main Content Tabs
                        Box(modifier = Modifier.weight(1f)) {
                            when (uiState.currentTab) {
                                StudioTab.PICTURES -> {
                                    val displayedMedia = if (uiState.selectedAlbum != null) {
                                        uiState.allMedia.filter { it.bucketId == uiState.selectedAlbum!!.bucketId }
                                    } else {
                                        uiState.allMedia
                                    }

                                    val displayedSections = if (uiState.selectedAlbum != null) {
                                        repository.groupIntoSections(displayedMedia, uiState.isArabic)
                                    } else {
                                        uiState.timelineSections
                                    }

                                    StudioTimelineScreen(
                                        sections = displayedSections,
                                        allMedia = displayedMedia,
                                        columns = uiState.gridColumns,
                                        isSelectionMode = uiState.isSelectionMode,
                                        selectedIds = uiState.selectedItemIds,
                                        onMediaClick = { item, index ->
                                            val act = context as? Activity
                                            val actIntent = act?.intent
                                            val isPick = actIntent?.action == Intent.ACTION_PICK ||
                                                    actIntent?.action == Intent.ACTION_GET_CONTENT

                                            if (isPick) {
                                                // Handle system picker response!
                                                act.setResult(Activity.RESULT_OK, Intent().apply {
                                                    this.data = item.uri
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                })
                                                act.finish()
                                            } else if (item.isVideo) {
                                                viewModel.openVideoPlayer(item)
                                            } else {
                                                viewModel.openImageViewer(index)
                                            }
                                        },
                                        onMediaLongClick = { item ->
                                            viewModel.toggleSelection(item.id)
                                        },
                                        onColumnsChange = { cols ->
                                            viewModel.setGridColumns(cols)
                                        }
                                    )
                                }
                                StudioTab.ALBUMS -> {
                                    StudioAlbumsScreen(
                                        albums = uiState.albums,
                                        onAlbumClick = { album ->
                                            viewModel.openAlbum(album)
                                            viewModel.selectTab(StudioTab.PICTURES)
                                        }
                                    )
                                }
                                StudioTab.STUDIO_PLUS -> {
                                    StudioPlusHubScreen(
                                        onOpenPhotoEditor = {
                                            val firstPhoto = uiState.allMedia.firstOrNull { !it.isVideo && it.uri != null }
                                            if (firstPhoto != null) {
                                                viewModel.openPhotoEditor(firstPhoto.uri!!)
                                            } else {
                                                Toast.makeText(context, "الرجاء التقاط أو إضافة صورة أولاً لتعديلها", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom Navigation Bar (Samsung One UI Navigation)
                        StudioBottomNavigation(
                            currentTab = uiState.currentTab,
                            onTabSelected = { tab ->
                                viewModel.selectTab(tab)
                            }
                        )
                    }

                    // Fullscreen Image Viewer Overlay
                    if (uiState.activeViewingIndex != null) {
                        StudioImageViewerScreen(
                            items = uiState.allMedia,
                            initialIndex = uiState.activeViewingIndex!!,
                            onBack = { viewModel.closeImageViewer() },
                            onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                            onDeleteRequest = { item ->
                                scope.launch {
                                    val uris = listOfNotNull(item.uri)
                                    val pendingIntent = repository.createDeleteRequest(uris)
                                    if (pendingIntent != null) {
                                        deleteLauncher.launch(
                                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                        )
                                    } else {
                                        viewModel.closeImageViewer()
                                    }
                                }
                            },
                            onPlayVideo = { item ->
                                viewModel.openVideoPlayer(item)
                            },
                            onEdit = { item ->
                                item.uri?.let { viewModel.openPhotoEditor(it) }
                            }
                        )
                    }

                    // Direct Incoming Image Viewer Overlay
                    if (uiState.directIncomingImageUri != null) {
                        val singleItem = remember(uiState.directIncomingImageUri) {
                            StudioMediaItem(
                                id = -1L,
                                uri = uiState.directIncomingImageUri!!,
                                displayName = "Image",
                                mimeType = "image/*",
                                dateTakenMs = System.currentTimeMillis(),
                                dateModifiedSec = System.currentTimeMillis() / 1000L,
                                sizeBytes = 0L
                            )
                        }
                        StudioImageViewerScreen(
                            items = listOf(singleItem),
                            initialIndex = 0,
                            onBack = { viewModel.closeImageViewer() },
                            onToggleFavorite = {},
                            onDeleteRequest = { viewModel.closeImageViewer() },
                            onEdit = { item ->
                                item.uri?.let { viewModel.openPhotoEditor(it) }
                            }
                        )
                    }

                    // Fullscreen Samsung Video Player Overlay
                    if (uiState.activeVideoItem != null && uiState.activeVideoItem!!.uri != null) {
                        StudioSamsungPlayerScreen(
                            videoUri = uiState.activeVideoItem!!.uri!!,
                            title = uiState.activeVideoItem!!.displayName,
                            onBack = { viewModel.closeVideoPlayer() }
                        )
                    }

                    // Direct Incoming Video Player Overlay
                    if (uiState.directIncomingVideoUri != null) {
                        StudioSamsungPlayerScreen(
                            videoUri = uiState.directIncomingVideoUri!!,
                            title = "تشغيل الفيديو",
                            onBack = { viewModel.closeVideoPlayer() }
                        )
                    }

                    // Fullscreen Photo Editor Overlay
                    if (uiState.editingImageUri != null) {
                        StudioPhotoEditorScreen(
                            imageUri = uiState.editingImageUri!!,
                            onClose = { viewModel.closePhotoEditor() },
                            onSaved = { _ ->
                                viewModel.closePhotoEditor()
                            }
                        )
                    }
                }

                if (showAboutDialog) {
                    AboutAppDialog(
                        isArabic = systemLocaleIsArabic,
                        onDismiss = { showAboutDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun StudioTopBar(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        IconButton(
            onClick = onAboutClick,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "About",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StudioBottomNavigation(
    currentTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentTab == StudioTab.PICTURES,
            onClick = { onTabSelected(StudioTab.PICTURES) },
            icon = {
                Icon(
                    imageVector = if (currentTab == StudioTab.PICTURES) Icons.Filled.Photo else Icons.Outlined.Photo,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_pictures)) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == StudioTab.ALBUMS,
            onClick = { onTabSelected(StudioTab.ALBUMS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == StudioTab.ALBUMS) Icons.Filled.Folder else Icons.Outlined.Folder,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_albums)) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == StudioTab.STUDIO_PLUS,
            onClick = { onTabSelected(StudioTab.STUDIO_PLUS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == StudioTab.STUDIO_PLUS) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = null
                )
            },
            label = { Text(stringResource(R.string.tab_studio_plus)) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun StudioPermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.permission_required_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.permission_required_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.grant_permission),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
