package com.progalleryx.app.studioplus.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.progalleryx.app.R
import com.progalleryx.app.studioplus.core.model.StudioMediaItem

@Composable
fun StudioImageViewerScreen(
    items: List<StudioMediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onToggleFavorite: (StudioMediaItem) -> Unit,
    onDeleteRequest: (StudioMediaItem) -> Unit,
    onPlayVideo: ((StudioMediaItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safeIndex = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { items.size.coerceAtLeast(1) }
    )

    var controlsVisible by remember { mutableStateOf(true) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val currentItem = items.getOrNull(pagerState.currentPage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (items.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val mediaItem = items[page]
                StudioZoomableImagePage(
                    item = mediaItem,
                    onTap = { controlsVisible = !controlsVisible },
                    onSwipeDismiss = onBack,
                    onPlayVideo = { onPlayVideo?.invoke(mediaItem) }
                )
            }
        }

        // Top Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                if (currentItem != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${items.size}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentItem.formattedDate,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { showDetailsDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Bar (Samsung One UI minimal bar)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1E222A).copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Share
                IconButton(
                    onClick = {
                        currentItem?.let { item ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = item.mimeType
                                putExtra(Intent.EXTRA_STREAM, item.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.action_share),
                        tint = Color.White
                    )
                }

                // Favorite
                IconButton(
                    onClick = { currentItem?.let(onToggleFavorite) }
                ) {
                    val isFav = currentItem?.isFavorite == true
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.action_favorite),
                        tint = if (isFav) Color(0xFFFF5252) else Color.White
                    )
                }

                // Delete
                IconButton(
                    onClick = { currentItem?.let(onDeleteRequest) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
        }

        // Details dialog
        if (showDetailsDialog && currentItem != null) {
            StudioMediaDetailsDialog(
                item = currentItem,
                onDismiss = { showDetailsDialog = false }
            )
        }
    }
}

@Composable
fun StudioZoomableImagePage(
    item: StudioMediaItem,
    onTap: () -> Unit,
    onSwipeDismiss: () -> Unit,
    onPlayVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4.5f)
        if (scale > 1f) {
            val maxOffsetX = (scale - 1f) * 500f
            val maxOffsetY = (scale - 1f) * 800f
            offsetX = (offsetX + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = (offsetY + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .transformable(state = transformState),
        contentAlignment = Alignment.Center
    ) {
        val imageRequest = remember(item.uri) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )

        // If Video: show play button overlay
        if (item.isVideo && scale <= 1.1f) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayVideo
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
fun StudioMediaDetailsDialog(
    item: StudioMediaItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                text = "تفاصيل الملف",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "الاسم", value = item.displayName)
                DetailRow(label = "التاريخ", value = item.formattedDate)
                DetailRow(label = "الحجم", value = item.formattedSize)
                DetailRow(label = "النوع", value = item.mimeType)
                if (item.width > 0 && item.height > 0) {
                    DetailRow(label = "الأبعاد", value = "${item.width} × ${item.height}")
                }
                if (item.isVideo && item.durationMs > 0) {
                    DetailRow(label = "المدة", value = item.formattedDuration)
                }
                DetailRow(label = "المجلد", value = item.bucketDisplayName)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
