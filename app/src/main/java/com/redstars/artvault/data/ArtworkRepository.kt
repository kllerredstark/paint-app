package com.redstars.artvault.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 모든 작품은 앱 내부 저장소에 보관된다.
 *  - files/art/<id>.jpg   : 긴 변 2400px 이하로 줄인 원본
 *  - files/art/<id>_t.jpg : 긴 변 640px 썸네일 (목록용)
 *  - files/artworks.json  : 작품 정보 목록
 */
class ArtworkRepository(private val context: Context) {

    private val dir = File(context.filesDir, "art").apply { mkdirs() }
    private val store = AtomicFile(File(context.filesDir, "artworks.json"))
    private val mutex = Mutex()

    private val _items = MutableStateFlow<List<Artwork>>(emptyList())
    val items: StateFlow<List<Artwork>> = _items.asStateFlow()

    fun imageFile(id: String) = File(dir, "$id.jpg")
    fun thumbFile(id: String) = File(dir, "${id}_t.jpg")

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock { _items.value = readAll() }
    }

    suspend fun add(uri: Uri, title: String, meta: Meta, order: Int): Artwork = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val full = decode(uri, 2400)
        val thumb = scaleDown(full, 640)
        try {
            writeJpeg(full, imageFile(id), 90)
            writeJpeg(thumb, thumbFile(id), 82)
        } catch (e: Exception) {
            imageFile(id).delete(); thumbFile(id).delete()
            throw e
        }
        val art = Artwork(
            id = id,
            title = title.trim().ifEmpty { "제목 없음" }.take(80),
            fav = false,
            w = full.width,
            h = full.height,
            addedAt = System.currentTimeMillis() + order,
        ).withMeta(meta)
        if (thumb !== full) thumb.recycle()
        full.recycle()
        mutex.withLock {
            val next = listOf(art) + _items.value
            writeAll(next)
            _items.value = next
        }
        art
    }

    suspend fun update(art: Artwork) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = _items.value.map { if (it.id == art.id) art else it }
            writeAll(next)
            _items.value = next
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = _items.value.filterNot { it.id == id }
            writeAll(next)
            _items.value = next
        }
        imageFile(id).delete()
        thumbFile(id).delete()
    }

    // ---------- 파일 입출력 ----------

    private fun readAll(): List<Artwork> {
        if (!store.baseFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(String(store.readFully(), Charsets.UTF_8))
            List(arr.length()) { Artwork.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeAll(list: List<Artwork>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val out = store.startWrite()
        try {
            out.write(arr.toString().toByteArray(Charsets.UTF_8))
            store.finishWrite(out)
        } catch (e: Exception) {
            store.failWrite(out)
            throw e
        }
    }

    // ---------- 이미지 처리 ----------

    /** EXIF 회전을 반영해서 디코딩하고, 긴 변이 maxSide를 넘지 않게 줄인다. 투명 영역은 흰 바탕으로. */
    private fun decode(uri: Uri, maxSide: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val s = min(1f, maxSide.toFloat() / max(w, h))
            if (s < 1f) {
                decoder.setTargetSize(
                    (w * s).roundToInt().coerceAtLeast(1),
                    (h * s).roundToInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val flat = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(decoded, 0f, 0f, null)
        }
        decoded.recycle()
        return flat
    }

    private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val s = maxSide.toFloat() / max(src.width, src.height)
        if (s >= 1f) return src
        return Bitmap.createScaledBitmap(
            src,
            (src.width * s).roundToInt().coerceAtLeast(1),
            (src.height * s).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun writeJpeg(bmp: Bitmap, file: File, quality: Int) {
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }
}
