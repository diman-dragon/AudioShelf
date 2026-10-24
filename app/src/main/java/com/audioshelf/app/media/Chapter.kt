package com.audioshelf.app.media

class Chapter(val title: String, val startMs: Long, val endMs: Long = -1) {
    companion object {
        fun tidy(raw: List<Chapter>): List<Chapter> {
            val sorted = raw.sortedBy { it.startMs }
            val unique = ArrayList<Chapter>()
            for (c in sorted) if (unique.isEmpty() || c.startMs > unique.last().startMs) unique.add(c)
            if (unique.size < 2) return emptyList()
            return unique.mapIndexed { i, c ->
                val title = c.title.trim().ifEmpty { (i + 1).toString() }
                val end = if (i + 1 < unique.size) unique[i + 1].startMs else -1L
                Chapter(title, if (i == 0) 0 else c.startMs, end)
            }
        }
    }
}
