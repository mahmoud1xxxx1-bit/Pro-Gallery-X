package com.progalleryx.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.Intent
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.text.style.TextOverflow

private val Navy = Color(0xFF17263B)
private val Blue = Color(0xFF2563EB)
private val Back = Color(0xFFF7F9FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProGalleryScreen() }
    }
}
@Composable
fun ProGalleryScreen(vm: GalleryViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val needed = remember {
        if (Build.VERSION.SDK_INT >= 34)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        else if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    var permitted by remember { mutableStateOf(needed.any { ctx.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = needed.any { p -> ctx.checkSelfPermission(p) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (permitted) vm.reload()
    }
    val favorites = remember { FavoritesStore(ctx) }
    var selected by remember { mutableStateOf<GalleryMedia?>(null) }
    var favoriteRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(permitted) { if (permitted) vm.reload() }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Back, surface = Color.White)) {
        Column(Modifier.fillMaxSize().background(Back)) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Pro Gallery X", fontWeight = FontWeight.Bold, color = Navy, style = MaterialTheme.typography.headlineSmall); Text("معرضك الخاص • بدون سحابة", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Blue)
            }
            if (!permitted) {
                Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Collections, null, tint = Blue, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("صورك تبقى على هاتفك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("امنح إذن الوصول لعرض الصور والفيديوهات محليًا.", color = Color.Gray)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { launcher.launch(needed) }) { Text("السماح بالوصول") }
                }
            } else {
                Text("الصور والفيديوهات", Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Navy)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.filter == MediaFilter.ALL, onClick = { vm.setFilter(MediaFilter.ALL) }, label = { Text("الكل") })
                    FilterChip(selected = state.filter == MediaFilter.PHOTOS, onClick = { vm.setFilter(MediaFilter.PHOTOS) }, label = { Text("الصور") })
                    FilterChip(selected = state.filter == MediaFilter.VIDEOS, onClick = { vm.setFilter(MediaFilter.VIDEOS) }, label = { Text("الفيديوهات") })
                }
                val gridState = rememberLazyGridState()
                LaunchedEffect(gridState, state.items.size, state.loading, state.end, state.filter) {
                    snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collectLatest { last ->
                        if (state.items.isNotEmpty() && last >= state.items.lastIndex - 18 && !state.loading && !state.end) vm.loadMore()
                    }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(3), state = gridState, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(state.items, key = { _, item -> item.id }) { _, item ->
                        Box(Modifier.aspectRatio(1f).clickable { selected = item }, contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = ImageRequest.Builder(ctx).data(item.uri).size(360).crossfade(true).build(),
                                contentDescription = "صورة أو فيديو",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(Color(0xFFE7EDF5), RoundedCornerShape(4.dp))
                            )
                            if (item.video) Icon(Icons.Outlined.PlayCircle, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                    }
                    if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    if (state.error != null) item(span = { GridItemSpan(maxLineSpan) }) { Text(state.error ?: "", Modifier.clickable { vm.loadMore() }.padding(18.dp), color = Color.Red) }
                    if (state.end && state.items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text("لا توجد ملفات متاحة", Modifier.padding(24.dp)) }
                }
            }
        }
        selected?.let { item ->
            Dialog(onDismissRequest = { selected = null }) {
                Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(12.dp)) {
                    if (item.video) {
                        Text("الفيديو جاهز للفتح باستخدام مشغل الهاتف", Modifier.padding(12.dp))
                    } else {
                        AsyncImage(model = item.uri, contentDescription = "معاينة الصورة", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp))
                    }
                    val isFavorite = remember(item.uri, favoriteRevision) { favorites.contains(item.uri) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { favorites.toggle(item.uri); favoriteRevision++ }) { Text(if (isFavorite) "★ مفضلة" else "☆ للمفضلة") }
                        TextButton(onClick = {
                            val i = Intent(Intent.ACTION_VIEW).setDataAndType(item.uri, if (item.video) "video/*" else "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching { ctx.startActivity(i) }
                        }) { Text("فتح") }
                        TextButton(onClick = {
                            val i = Intent(Intent.ACTION_SEND).setType(if (item.video) "video/*" else "image/*").putExtra(Intent.EXTRA_STREAM, item.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            ctx.startActivity(Intent.createChooser(i, "مشاركة"))
                        }) { Text("مشاركة") }
                        TextButton(onClick = { selected = null }) { Text("إغلاق") }
                    }
                }
            }
        }
    }
}
