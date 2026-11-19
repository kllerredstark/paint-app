package com.redstars.artvault.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.redstars.artvault.data.Artwork
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewerScreen(
    list: List<Artwork>,
    startId: String,
    imageFile: (String) -> File,
    onClose: () -> Unit,
    onFav: (Artwork) -> Unit,
    onEdit: (Artwork) -> Unit,
) {
    val startIndex = remember(startId) { list.indexOfFirst { it.id == startId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = startIndex) { list.size }
    var chrome by remember { mutableStateOf(true) }

    BackHandler(onBack = onClose)
    LaunchedEffect(list.isEmpty()) { if (list.isEmpty()) onClose() }
    if (list.isEmpty()) return

    val current = list[pagerState.currentPage.coerceIn(0, list.lastIndex)]
    val iconColors = IconButtonDefaults.iconButtonColors(
        containerColor = Color.White.copy(alpha = 0.12f),
        contentColor = Color.White,
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF0D0C0B))) {
        HorizontalPager(
            state = pagerState,
            key = { list[it].id },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableImage(
                file = imageFile(list[page].id),
                description = list[page].title,
                onTap = { chrome = !chrome },
            )
        }

        AnimatedVisibility(
            visible = chrome, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, colors = iconColors) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onFav(current) }, colors = iconColors) {
                    Icon(
                        if (current.fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "즐겨찾기",
                    )
                }
                IconButton(onClick = { onEdit(current) }, colors = iconColors) {
                    Icon(Icons.Filled.Edit, contentDescription = "편집")
                }
            }
        }

        AnimatedVisibility(
            visible = chrome, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 18.dp)
            ) {
                Text(
                    current.title.ifBlank { "제목 없음" },
                    color = Color.White, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                )
                val meta = listOf(current.artist, current.made, current.album).filter { it.isNotBlank() }
                val pos = if (list.size > 1) "${pagerState.currentPage + 1}/${list.size}" else ""
                val line = (meta + listOfNotNull(pos.ifEmpty { null })).joinToString(" · ")
                if (line.isNotEmpty()) {
                    Text(line, color = Color(0xFFBBBBBB), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (current.note.isNotBlank()) {
                    Text(
                        current.note, color = Color(0xFFDDDDDD), fontSize = 14.sp, lineHeight = 21.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
                if (current.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        current.tags.forEach {
                            Text(
                                "#$it", color = Color.White, fontSize = 12.sp,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                                    .padding(horizontal = 9.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 한 손가락 좌우 드래그는 페이저(다음 작품)로 넘기고,
 * 두 손가락 핀치·확대 상태의 드래그만 이 이미지가 처리한다. 두 번 탭하면 2.5배 확대/복귀.
 */
@Composable
private fun ZoomableImage(file: File, description: String, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f; offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers > 1 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale <= 1f) Offset.Zero else {
                                val maxX = size.width * (scale - 1f) / 2f
                                val maxY = size.height * (scale - 1f) / 2f
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
