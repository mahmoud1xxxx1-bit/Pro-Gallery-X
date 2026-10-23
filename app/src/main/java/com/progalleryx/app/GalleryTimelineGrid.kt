package com.progalleryx.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryTimelineGrid(
    items: List<GalleryMedia>,
    selectedIds: Set<Long>,
    isArabic: Boolean,
    gridState: LazyGridState,
    onItemClick: (GalleryMedia) -> Unit,
    onItemLongClick: (GalleryMedia) -> Unit,
    onSelectionToggle: (Long) -> Unit,
    onSelectDateGroup: (List<GalleryMedia>) -> Unit,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSelectionMode = selectedIds.isNotEmpty()

    // Group items by date efficiently
    val sections = remember(items, isArabic) {
        GalleryTimelineRules.groupByDate(items, isArabic)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier.fillMaxSize()
    ) {
        sections.forEach { section ->
            // Date Section Header
            item(
                key = "header_${section.dateKey}",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Localization.albumItemsCount(section.items.size, isArabic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelectionMode) {
                        TextButton(
                            onClick = { onSelectDateGroup(section.items) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val allSectionSelected = section.items.all { selectedIds.contains(it.id) }
                            Text(
                                text = if (allSectionSelected) Localization.deselectAll(isArabic) else Localization.selectAll(isArabic),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Grid Items for this Section
            items(
                count = section.items.size,
                key = { index -> section.items[index].id },
                contentType = { index -> if (section.items[index].video) "video" else "photo" }
            ) { index ->
                val item = section.items[index]
                val isSelected = selectedIds.contains(item.id)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    onSelectionToggle(item.id)
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onLongClick = {
                                onItemLongClick(item)
                            }
                        ),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Fast bounded thumbnail
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(360)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.displayName.ifBlank { "Media Item" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video overlay with duration badge
                    if (item.video) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                if (item.durationMs > 0L) {
                                    Text(
                                        text = GalleryTimelineRules.formatDuration(item.durationMs),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Selection Mode Checkbox Badge
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                                .padding(6.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.White, CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircleOutline,
                                    contentDescription = "Not Selected",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Loading Indicator
        if (loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
