package com.audioshelf.app.data

enum class KindFilter(val id: String) {
    All("all"), Book("book"), Album("album");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: All
    }
}

enum class ProgressFilter(val id: String) {
    All("all"), New("new"), Started("started"), Done("done");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: All
    }
}

enum class SortMode(val id: String) {
    Date("date"), Name("name"), Played("played");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Date
    }
}

enum class LibView(val id: String) {
    Rows("list"), Cards("cards");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Rows
    }
}

const val LANG_AUTO = "auto"
val LANG_IDS = listOf("ru", "en")
val THEME_IDS = listOf("light", "dark")
val SKIP_OPTIONS = listOf(5, 10, 15, 30, 60)

fun nextSkip(current: Int): Int = SKIP_OPTIONS[(SKIP_OPTIONS.indexOf(current) + 1) % SKIP_OPTIONS.size]
const val SKIP_BACK_DEFAULT = 15
const val SKIP_FORWARD_DEFAULT = 30
