package com.progalleryx.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class StickerCategory(
    val id: String,
    val title: String,
    val items: List<String>
)

@Composable
fun PhotoStickerPicker(
    onDismiss: () -> Unit,
    onSelectEmoji: (String) -> Unit,
    onSelectShape: (ShapeType, Long) -> Unit,
    onSelectCustomImage: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val recents = remember { preferences.recentStickers }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedShapeColor by remember { mutableLongStateOf(0xFFFF3B30L) }
    var searchQuery by remember { mutableStateOf("") }

    val handleSelectEmoji: (String) -> Unit = { emoji ->
        preferences.addRecentSticker(emoji)
        onSelectEmoji(emoji)
    }

    val customImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onSelectCustomImage(uri)
            onDismiss()
        }
    }

    val categories = remember(recents) {
        val list = mutableListOf<StickerCategory>()
        if (recents.isNotEmpty()) {
            list.add(
                StickerCategory(
                    id = "recents",
                    title = "الأخيرة",
                    items = recents
                )
            )
        }
        list.addAll(
            listOf(
                StickerCategory(
                    id = "expressions",
                    title = "تعبيرات",
                    items = listOf(
                        "😂", "😍", "🥰", "😎", "🤩", "🥳", "🥺", "😊",
                        "🤣", "😋", "😜", "🤪", "😇", "🤠", "🧐", "🤫",
                        "🤔", "🤯", "😱", "🥶", "🥵", "😴", "🤤", "🥱",
                        "😷", "🤒", "🤑", "😈", "👻", "👽", "🤖", "🙈"
                    )
                ),
            StickerCategory(
                id = "hearts",
                title = "قلوب",
                items = listOf(
                    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                    "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
                    "💘", "💝", "💟", "💌", "💐", "🌹", "🌷", "🌺"
                )
            ),
            StickerCategory(
                id = "celebrations",
                title = "احتفالات",
                items = listOf(
                    "🎉", "🎊", "🎈", "🎂", "🍰", "🍾", "🥂", "🏆",
                    "🥇", "🥈", "🥉", "🎖️", "👑", "💎", "🌟", "✨",
                    "🎇", "🎆", "🎁", "🎗️", "🎯", "🎮", "🎪", "🎨"
                )
            ),
            StickerCategory(
                id = "hands",
                title = "أيدي",
                items = listOf(
                    "👍", "👎", "👏", "🙌", "👐", "🤝", "✌️", "🤞",
                    "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝️",
                    "👊", "✊", "🤛", "🤜", "🤚", "👋", "✍️", "🙏"
                )
            ),
            StickerCategory(
                id = "nature",
                title = "طبيعة وطاقة",
                items = listOf(
                    "🔥", "⚡", "☀️", "🌙", "⭐", "🌈", "☁️", "🌧️",
                    "❄️", "🌊", "🌸", "🍀", "🍁", "🍄", "🦋", "🐝",
                    "🌴", "🌵", "🚀", "💡", "☕", "🍕", "🍔", "🍦"
                )
            ),
            StickerCategory(
                id = "arrows",
                title = "أسهم",
                items = listOf(
                    "➡️", "⬅️", "⬆️", "⬇️", "↗️", "↘️", "↙️", "↖️",
                    "↕️", "↔️", "↩️", "↪️", "⤴️", "⤵️", "🔄", "🔁"
                )
            ),
            StickerCategory(
                id = "symbols",
                title = "رموز وعلامات",
                items = listOf(
                    "✅", "❌", "💯", "⚠️", "⛔", "🚫", "❗", "❓",
                    "💤", "💬", "🗨️", "🗯️", "💭", "🔔", "🔕", "📢",
                    "🔒", "🔓", "🔑", "🛡️", "⏰", "⏳", "⌛", "📸"
                )
            ),
            StickerCategory(
                id = "speech",
                title = "فقاعات ونصوص",
                items = listOf(
                    "💬", "💭", "🗨️", "🗯️", "💌", "📝", "📢", "📣",
                    "💥", "💫", "💨", "💢", "✨", "🔥", "⚡", "🎯"
                )
            )
        ))
        list
    }

    val shapes = remember {
        listOf(
            ShapeType.HEART to "قلب",
            ShapeType.STAR to "نجمة",
            ShapeType.CIRCLE to "دائرة",
            ShapeType.RECTANGLE to "مستطيل",
            ShapeType.ARROW to "سهم",
            ShapeType.HEXAGON to "مسدس",
            ShapeType.DIAMOND to "معين",
            ShapeType.SPEECH_BUBBLE to "فقاعة محادثة"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Header with Close and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        "مكتبة الملصقات والأشكال",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Custom PNG import shortcut
                    IconButton(
                        onClick = { customImagePicker.launch("image/*") }
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "استيراد ملصق من الصور",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 2. Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في الملصقات...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 3. Top Mode Selector: Emojis / Shapes / Custom PNG
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Chips
                    categories.forEachIndexed { index, cat ->
                        FilterChip(
                            selected = (selectedCategoryIndex == index && searchQuery.isEmpty()),
                            onClick = {
                                selectedCategoryIndex = index
                                searchQuery = ""
                            },
                            label = { Text(cat.title) }
                        )
                    }

                    // Shapes Tab Chip
                    FilterChip(
                        selected = (selectedCategoryIndex == categories.size && searchQuery.isEmpty()),
                        onClick = {
                            selectedCategoryIndex = categories.size
                            searchQuery = ""
                        },
                        label = { Text("الأشكال الهندسية") }
                    )

                    // Custom Sticker Action Chip
                    AssistChip(
                        onClick = { customImagePicker.launch("image/*") },
                        label = { Text("ملصق خاص (PNG)") },
                        leadingIcon = {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                // 4. Content Area
                if (searchQuery.isNotBlank()) {
                    // Search across all emoji categories
                    val query = searchQuery.trim()
                    val searchResults = categories.flatMap { it.items }.distinct().filter {
                        it.contains(query)
                    }

                    if (searchResults.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد ملصقات مطابقة للبحث", color = Color.Gray)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            handleSelectEmoji(emoji)
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 28.sp)
                                }
                            }
                        }
                    }
                } else if (selectedCategoryIndex < categories.size) {
                    // Active Emoji Category
                    val currentItems = categories[selectedCategoryIndex].items
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentItems) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        handleSelectEmoji(emoji)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 28.sp)
                            }
                        }
                    }
                } else {
                    // Shapes & Custom Color Selection
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("اختر لون الشكل", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorPaletteRules.STUDIO_COLORS.forEach { colorVal ->
                                val isSelected = selectedShapeColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedShapeColor = colorVal }
                                )
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(shapes) { (shape, name) ->
                                Button(
                                    onClick = {
                                        onSelectShape(shape, selectedShapeColor)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .height(56.dp)
                                        .fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(selectedShapeColor))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        name,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
