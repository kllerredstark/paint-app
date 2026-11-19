package com.redstars.artvault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.redstars.artvault.data.Artwork
import com.redstars.artvault.data.Meta

/** 작가·제작일·앨범·태그·메모 입력 상태. */
private class MetaState(meta: Meta) {
    var artist by mutableStateOf(meta.artist)
    var made by mutableStateOf(meta.made)
    var album by mutableStateOf(meta.album)
    var tags by mutableStateOf(meta.tags.joinToString(", "))
    var note by mutableStateOf(meta.note)

    fun toMeta() = Meta(
        artist = artist.trim().take(100),
        made = made.trim().take(30),
        album = album.trim().take(60),
        tags = Meta.parseTags(tags),
        note = note.trim().take(4000),
    )
}

@Composable
private fun MetaFields(s: MetaState, albums: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Field("작가", s.artist, { s.artist = it }, "나 / 작가 이름", Modifier.weight(1f))
        Field("제작일", s.made, { s.made = it }, "2026-11", Modifier.weight(1f))
    }
    Field("앨범", s.album, { s.album = it }, "예: 수채화, 드로잉북 2026")
    if (albums.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
            items(albums) { a -> SuggestionChip(onClick = { s.album = a }, label = { Text(a) }) }
        }
    }
    Field("태그 (쉼표로 구분)", s.tags, { s.tags = it }, "풍경, 인물, 연필")
    Field("메모", s.note, { s.note = it }, "재료, 크기, 작업 이야기 등", singleLine = false)
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(hint) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 14.dp))
}

// ---------------- 추가 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    pending: List<PendingItem>,
    albums: List<String>,
    progress: Float?,
    onCancel: () -> Unit,
    onSave: (List<String>, Meta) -> Unit,
) {
    val saving by rememberUpdatedState(progress != null)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !saving },
    )
    val titles = remember(pending) { mutableStateListOf(*pending.map { it.title }.toTypedArray()) }
    val meta = remember(pending) { MetaState(Meta()) }
    val multi = pending.size > 1

    ModalBottomSheet(onDismissRequest = { if (!saving) onCancel() }, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            SheetTitle(if (multi) "작품 ${pending.size}점 추가" else "작품 추가")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                itemsIndexed(pending) { _, p ->
                    AsyncImage(
                        model = p.uri, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(if (multi) 84.dp else 140.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
            if (multi) {
                Text(
                    "작품별 제목", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                titles.indices.forEach { i ->
                    Field("${i + 1}번째 제목", titles[i], { titles[i] = it }, "작품 제목")
                }
                Text(
                    "아래 정보는 모든 작품에 똑같이 들어가요.", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                )
            } else {
                Field("제목", titles[0], { titles[0] = it }, "작품 제목")
            }
            MetaFields(meta, albums)

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onCancel, enabled = progress == null, modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("취소")
                }
                Button(
                    onClick = { onSave(titles.toList(), meta.toMeta()) },
                    enabled = progress == null,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) { Text(if (progress == null) "저장" else "저장 중…") }
            }
        }
    }
}

// ---------------- 편집 ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(
    art: Artwork,
    albums: List<String>,
    onDismiss: () -> Unit,
    onSave: (Artwork) -> Unit,
    onDelete: (Artwork) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(art.id) { mutableStateOf(art.title) }
    val meta = remember(art.id) { MetaState(art.meta) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            SheetTitle("작품 정보 편집")
            Field("제목", title, { title = it }, "작품 제목")
            MetaFields(meta, albums)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(50.dp)) { Text("취소") }
                Button(
                    onClick = {
                        onSave(art.copy(title = title.trim().ifEmpty { "제목 없음" }.take(80)).withMeta(meta.toMeta()))
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                ) { Text("저장") }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("이 작품 삭제", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("“${art.title}” 삭제") },
            text = { Text("이미지와 정보가 휴대폰에서 영구히 지워져요. 되돌릴 수 없어요.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete(art) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}
