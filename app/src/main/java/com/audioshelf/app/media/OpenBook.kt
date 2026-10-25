package com.audioshelf.app.media

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import com.audioshelf.app.data.asNumber

/** Chapters from the Libby / OverDrive `openbook.json` table of contents. */
object OpenBook {
    fun parse(text: String): Map<String, List<Chapter>> {
        val root = Json.parse(text).asMap() ?: return emptyMap()
        val toc = root["nav"].asMap()?.get("toc").asList() ?: return emptyMap()
        val perFile = LinkedHashMap<String, MutableList<Chapter>>()
        fun walk(items: List<Any?>) {
            for (item in items) {
                val m = item.asMap() ?: continue
                val title = m["title"] as? String ?: ""
                val path = m["path"] as? String
                if (path != null) {
                    val file = path.substringBefore('#').substringBefore('?').substringAfterLast('/').lowercase()
                    val offset = path.substringAfter('#', "").toDoubleOrNull() ?: 0.0
                    if (file.isNotEmpty()) perFile.getOrPut(file) { ArrayList() }.add(Chapter(title, (offset * 1000).toLong()))
                }
                m["contents"].asList()?.let { walk(it) }
            }
        }
        walk(toc)
        val out = LinkedHashMap<String, List<Chapter>>()
        for ((file, list) in perFile) out[file] = Chapter.tidy(list)
        return out
    }
}

/** OverDrive mp3 chapter markers stored as XML in a TXXX frame. */
object OverdriveMarkers {
    fun parse(xml: String): List<Chapter> {
        val out = ArrayList<Chapter>()
        val marker = Regex("<Marker>(.*?)</Marker>", RegexOption.DOT_MATCHES_ALL)
        for (m in marker.findAll(xml)) {
            val body = m.groupValues[1]
            val name = Regex("<Name>(.*?)</Name>", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)?.trim() ?: ""
            val time = Regex("<Time>(.*?)</Time>").find(body)?.groupValues?.get(1)?.trim() ?: continue
            val ms = VorbisChapters.parseTime(time) ?: continue
            out.add(Chapter(name, ms))
        }
        return Chapter.tidy(out)
    }
}
