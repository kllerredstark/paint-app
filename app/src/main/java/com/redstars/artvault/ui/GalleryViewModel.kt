package com.redstars.artvault.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redstars.artvault.data.Artwork
import com.redstars.artvault.data.ArtworkRepository
import com.redstars.artvault.data.Meta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode(val label: String) { ADDED("최근 추가"), MADE("제작일"), TITLE("제목순") }

sealed interface Filter {
    data object All : Filter
    data object Fav : Filter
    data class Album(val name: String) : Filter
}

data class PendingItem(val uri: Uri, val title: String)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    val repo = ArtworkRepository(app)

    val items: StateFlow<List<Artwork>> = repo.items
    val query = MutableStateFlow("")
    val filter = MutableStateFlow<Filter>(Filter.All)
    val sort = MutableStateFlow(SortMode.ADDED)

    val loaded = MutableStateFlow(false)
    val pending = MutableStateFlow<List<PendingItem>>(emptyList())
    val progress = MutableStateFlow<Float?>(null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val albums: StateFlow<List<Pair<String, Int>>> = repo.items
        .map { list ->
            list.filter { it.album.isNotBlank() }
                .groupingBy { it.album }.eachCount()
                .toList().sortedBy { it.first }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visible: StateFlow<List<Artwork>> =
        combine(repo.items, query, filter, sort) { list, q, f, s -> applyView(list, q, f, s) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            repo.load()
            loaded.value = true
        }
    }

    private fun applyView(list: List<Artwork>, q: String, f: Filter, s: SortMode): List<Artwork> {
        val needle = q.trim().lowercase()
        val filtered = list.filter { a ->
            val passFilter = when (f) {
                Filter.All -> true
                Filter.Fav -> a.fav
                is Filter.Album -> a.album == f.name
            }
            passFilter && (needle.isEmpty() ||
                listOf(a.title, a.artist, a.note, a.album, a.tags.joinToString(" "))
                    .any { it.lowercase().contains(needle) })
        }
        return when (s) {
            SortMode.ADDED -> filtered.sortedByDescending { it.addedAt }
            SortMode.MADE -> filtered.sortedByDescending { it.made }
            SortMode.TITLE -> filtered.sortedBy { it.title }
        }
    }

    // ---------- 추가 ----------

    fun startAdd(uris: List<Uri>) {
        if (uris.isEmpty() || progress.value != null) return
        viewModelScope.launch {
            pending.value = withContext(Dispatchers.IO) {
                uris.map { PendingItem(it, defaultTitle(it)) }
            }
        }
    }

    fun cancelAdd() {
        if (progress.value == null) pending.value = emptyList()
    }

    fun confirmAdd(titles: List<String>, meta: Meta) {
        val list = pending.value
        if (list.isEmpty() || progress.value != null) return
        viewModelScope.launch {
            progress.value = 0f
            var ok = 0
            var fail = 0
            list.forEachIndexed { i, p ->
                try {
                    repo.add(p.uri, titles.getOrElse(i) { p.title }, meta, i)
                    ok++
                } catch (e: Exception) {
                    fail++
                }
                progress.value = (i + 1f) / list.size
            }
            progress.value = null
            pending.value = emptyList()
            _messages.emit(if (fail == 0) "${ok}점 저장했어요" else "${ok}점 저장, ${fail}점 실패")
        }
    }

    private fun defaultTitle(uri: Uri): String {
        val name = try {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (e: Exception) {
            null
        }
        return name?.substringBeforeLast('.')?.replace(Regex("[_-]+"), " ")?.trim()?.take(80).orEmpty()
    }

    // ---------- 편집 / 삭제 ----------

    fun update(art: Artwork) = viewModelScope.launch {
        try { repo.update(art) } catch (e: Exception) { _messages.emit("저장하지 못했어요") }
    }

    fun toggleFav(art: Artwork) = update(art.copy(fav = !art.fav))

    fun delete(art: Artwork) = viewModelScope.launch {
        try {
            repo.delete(art.id)
            _messages.emit("삭제했어요")
        } catch (e: Exception) {
            _messages.emit("삭제하지 못했어요")
        }
    }
}
