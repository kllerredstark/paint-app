package com.redstars.artvault.data

import org.json.JSONArray
import org.json.JSONObject

/** 작품 한 점의 정보. 이미지 파일은 id로 찾는다 (art/<id>.jpg, art/<id>_t.jpg). */
data class Artwork(
    val id: String,
    val title: String,
    val artist: String = "",
    val made: String = "",
    val album: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val fav: Boolean = false,
    val w: Int = 0,
    val h: Int = 0,
    val addedAt: Long = 0L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist)
        put("made", made)
        put("album", album)
        put("tags", JSONArray(tags))
        put("note", note)
        put("fav", fav)
        put("w", w)
        put("h", h)
        put("addedAt", addedAt)
    }

    fun withMeta(meta: Meta): Artwork = copy(
        artist = meta.artist,
        made = meta.made,
        album = meta.album,
        tags = meta.tags,
        note = meta.note,
    )

    val meta: Meta get() = Meta(artist, made, album, tags, note)

    companion object {
        fun fromJson(o: JSONObject): Artwork {
            val tagsArr = o.optJSONArray("tags") ?: JSONArray()
            return Artwork(
                id = o.getString("id"),
                title = o.optString("title"),
                artist = o.optString("artist"),
                made = o.optString("made"),
                album = o.optString("album"),
                tags = List(tagsArr.length()) { tagsArr.optString(it) }.filter { it.isNotBlank() },
                note = o.optString("note"),
                fav = o.optBoolean("fav"),
                w = o.optInt("w"),
                h = o.optInt("h"),
                addedAt = o.optLong("addedAt"),
            )
        }
    }
}

/** 여러 작품에 한꺼번에 적용하는 부가 정보. */
data class Meta(
    val artist: String = "",
    val made: String = "",
    val album: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
) {
    companion object {
        fun parseTags(text: String): List<String> =
            text.split(',', '，').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinct().take(20)
    }
}
