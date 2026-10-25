package com.progalleryx.app.studioplus.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.progalleryx.app.R
import com.progalleryx.app.studioplus.core.model.StudioMediaItem
import com.progalleryx.app.studioplus.core.model.StudioTimelineSection
import com.progalleryx.app.studioplus.core.theme.SamsungBlue

@Composable
fun StudioTimelineScreen(
    sections: List<StudioTimelineSection>,
    allMedia: List<StudioMediaItem>,
    columns: Int,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onMediaClick: (StudioMediaItem, Int) -> Unit,
    onMediaLongClick: (StudioMediaItem) -> Unit,
    onColumnsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Pinch-to-zoom columns gesture handler
    var zoomScale by remember { mutableFloatStateOf(1f) }
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        zoomScale *= zoomChange
        if (zoomScale > 1.35f && columns > 2) {
            onColumnsChange(columns - 1)
            zoomScale = 1f
        } else if (zoomScale < 0.75f && columns < 4) {
            onColumnsChange(columns + 1)
            zoomScale = 1f
        }
    }

    if (allMedia.isEmpty()) {
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
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.empty_gallery_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.empty_gallery_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .transformable(transformableState),
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        sections.forEach { section ->
            // Section Header
            item(
                key = "section_${section.dateKey}",
                span = { GridItemSpan(columns) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${section.items.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Media Items in Section
            itemsIndexed(
                items = section.items,
                key = { _, item -> item.id }
            ) { _, item ->
                val overallIndex = remember(item.id) { allMedia.indexOfFirst { it.id == item.id } }
                StudioThumbnailCell(
                    item = item,
                    isSelected = selectedIds.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            onMediaLongClick(item)
                        } else {
                            onMediaClick(item, if (overallIndex >= 0) overallIndex else 0)
                        }
                    },
                    onLongClick = { onMediaLongClick(item) }
                )
            }
        }
    }
}

@Composable
fun StudioThumbnailCell(
    item: StudioMediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(true)
            .size(320)
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for indicators
        if (item.isVideo || item.isFavorite) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            startY = 140f
                        )
                    )
            )
        }

        // Video badge
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = item.formattedDuration,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Favorite Heart badge
        if (item.isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(14.dp)
            )
        }

        // Selection Checkmark
        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) SamsungBlue else Color.Black.copy(alpha = 0.4f))
                    .border(
                        width = 1.5.dp,
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
