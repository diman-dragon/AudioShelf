package com.audioshelf.app.player

import android.content.Context
import android.net.Uri
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibFile
import com.audioshelf.app.media.ChannelSource
import com.audioshelf.app.media.FileTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SCANNABLE = Regex(".*\\.(mka|m4b|m4a|mp4|mp3)$", RegexOption.IGNORE_CASE)
private const val MAX_FILES = 20

class ChapterScanner(private val context: Context, private val store: ChapterStore) {
    private fun wants(book: Book, file: LibFile) = book.files.size <= MAX_FILES && SCANNABLE.matches(file.name)

    suspend fun scan(book: Book): Boolean = withContext(Dispatchers.IO) {
        val todo = book.files.filter { wants(book, it) && store.of(it) == null }
        if (todo.isEmpty()) return@withContext false
        todo.forEach { file ->
            val list = runCatching { read(file) }.getOrDefault(emptyList())
            store.set(file, list.map { ChapterRec(it.title, it.startMs, it.endMs) })
        }
        todo.any { (store.of(it)?.size ?: 0) >= 2 }
    }

    private fun read(file: LibFile) =
        ChannelSource.open(context, Uri.parse(file.uri)).use { FileTags.read(it, false).chapters }
}
