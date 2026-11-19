package com.redstars.artvault.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.redstars.artvault.data.Artwork
import java.io.File

@Composable
fun GalleryApp(vm: GalleryViewModel) {
    val all by vm.items.collectAsStateWithLifecycle()
    val visible by vm.visible.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()

    var viewerStartId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    // 사라진 앨범/즐겨찾기 필터는 '전체'로 되돌림
    LaunchedEffect(albums, all, filter) {
        val f = filter
        if (f is Filter.Album && albums.none { it.first == f.name }) vm.filter.value = Filter.All
        if (f == Filter.Fav && all.none { it.fav }) vm.filter.value = Filter.All
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris -> vm.startAdd(uris) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopArea(
                    total = all.size,
                    albums = albums,
                    favCount = all.count { it.fav },
                    filter = filter,
                    sort = sort,
                    onQuery = { vm.query.value = it },
                    onFilter = { vm.filter.value = it },
                    onSort = { vm.sort.value = it },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("작품 추가") },
                )
            },
        ) { pad ->
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = pad.calculateTopPadding() + 4.dp,
                    bottom = pad.calculateBottomPadding() + 96.dp,
                ),
                verticalItemSpacing = 14.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (loaded && visible.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        EmptyState(hasAny = all.isNotEmpty())
                    }
                }
                items(visible, key = { it.id }) { art ->
                    ArtCard(art, vm.repo.thumbFile(art.id)) { viewerStartId = art.id }
                }
            }
        }

        val startId = viewerStartId
        if (startId != null) {
            ViewerScreen(
                list = visible,
                startId = startId,
                imageFile = { vm.repo.imageFile(it) },
                onClose = { viewerStartId = null },
                onFav = { vm.toggleFav(it) },
                onEdit = { editingId = it.id },
            )
        }

        if (pending.isNotEmpty()) {
            AddSheet(
                pending = pending,
                albums = albums.map { it.first },
                progress = progress,
                onCancel = { vm.cancelAdd() },
                onSave = { titles, meta -> vm.confirmAdd(titles, meta) },
            )
        }

        val editing = all.firstOrNull { it.id == editingId }
        if (editing != null) {
            EditSheet(
                art = editing,
                albums = albums.map { it.first },
                onDismiss = { editingId = null },
                onSave = { vm.update(it); editingId = null },
                onDelete = { vm.delete(it); editingId = null },
            )
        }
    }
}

@Composable
private fun TopArea(
    total: Int,
    albums: List<Pair<String, Int>>,
    favCount: Int,
    filter: Filter,
    sort: SortMode,
    onQuery: (String) -> Unit,
    onFilter: (Filter) -> Unit,
    onSort: (SortMode) -> Unit,
) {
    var q by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
    ) {
        Text(
            "나의 작품 보관함",
            fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp,
        )
        Text(
            "작품 ${total}점" + if (albums.isNotEmpty()) " · 앨범 ${albums.size}개" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it; onQuery(it) },
                placeholder = { Text("제목, 작가, 태그 검색") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (q.isNotEmpty()) IconButton(onClick = { q = ""; onQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "지우기")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            SortMenu(sort, onSort)
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            item {
                FilterChip(
                    selected = filter == Filter.All,
                    onClick = { onFilter(Filter.All) },
                    label = { Text("전체 $total") },
                )
            }
            if (favCount > 0) item {
                FilterChip(
                    selected = filter == Filter.Fav,
                    onClick = { onFilter(Filter.Fav) },
                    label = { Text("♥ 즐겨찾기 $favCount") },
                )
            }
            items(albums, key = { it.first }) { (name, n) ->
                FilterChip(
                    selected = filter == Filter.Album(name),
                    onClick = { onFilter(Filter.Album(name)) },
                    label = { Text("$name $n") },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(sort: SortMode, onSort: (SortMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(sort.label, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortMode.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.label, fontWeight = if (m == sort) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onSort(m); open = false },
                )
            }
        }
    }
}

@Composable
private fun ArtCard(art: Artwork, thumb: File, onClick: () -> Unit) {
    val ratio = if (art.w > 0 && art.h > 0) (art.w.toFloat() / art.h).coerceIn(0.4f, 2.5f) else 1f
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = thumb,
                contentDescription = art.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (art.fav) {
                Icon(
                    Icons.Filled.Favorite, contentDescription = "즐겨찾기",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                )
            }
        }
        Text(
            art.title.ifBlank { "제목 없음" },
            fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
        val sub = listOf(art.artist, art.made).filter { it.isNotBlank() }.joinToString(" · ")
        if (sub.isNotEmpty()) {
            Text(
                sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(hasAny: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 70.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (hasAny) "찾는 작품이 없어요" else "아직 보관된 작품이 없어요",
            fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasAny) "검색어나 필터를 바꿔 보세요."
            else "아래 '작품 추가'로 그림 사진을 올려 보세요.\n갤러리 앱에서 '공유 → 작품 보관함'으로도 추가할 수 있어요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 22.sp,
        )
    }
}
