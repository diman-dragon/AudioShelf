package com.audioshelf.app.player

import android.content.Context
import android.net.Uri
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibFile
import com.audioshelf.app.library.SiblingFiles
import com.audioshelf.app.media.Chapter
import com.audioshelf.app.media.ChannelSource
import com.audioshelf.app.media.CueSheet
import com.audioshelf.app.media.FileTags
import com.audioshelf.app.media.OpenBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SCANNABLE = Regex(".*\\.(mka|m4b|m4a|mp4|mp3|ogg|oga|opus|flac)$", RegexOption.IGNORE_CASE)
private const val MAX_FILES = 20

class ChapterScanner(private val context: Context, private val store: ChapterStore) {
    private val siblings = SiblingFiles(context.contentResolver)

    private fun wants(book: Book, file: LibFile) = book.files.size <= MAX_FILES && SCANNABLE.matches(file.name)

    suspend fun scan(book: Book): Boolean = withContext(Dispatchers.IO) {
        val todo = book.files.filter { wants(book, it) && store.of(it) == null }
        if (todo.isEmpty()) return@withContext false
        todo.forEach { file ->
            var list = runCatching { read(file) }.getOrDefault(emptyList())
            if (list.size < 2) list = external(book, file)
            store.set(file, list.map { ChapterRec(it.title, it.startMs, it.endMs) })
        }
        todo.any { (store.of(it)?.size ?: 0) >= 2 }
    }

    private fun read(file: LibFile) =
        ChannelSource.open(context, Uri.parse(file.uri)).use { FileTags.read(it, false).chapters }

    private fun external(book: Book, file: LibFile): List<Chapter> = runCatching {
        val entries = siblings.list(file.dirUri)
        val base = file.name.substringBeforeLast('.').lowercase()
        val single = book.files.size == 1
        val cues = entries.filter { it.name.endsWith(".cue", ignoreCase = true) }
        val cue = cues.firstOrNull { it.name.substringBeforeLast('.').equals(base, ignoreCase = true) }
            ?: cues.singleOrNull()?.takeIf { book.folder }
        val fromCue = cue?.let { siblings.read(it.uri) }?.let { pick(CueSheet.parse(CueSheet.decode(it)), file, single) } ?: emptyList()
        if (fromCue.size >= 2) {
            fromCue
        } else {
            val openbook = entries.firstOrNull { it.name.equals("openbook.json", ignoreCase = true) }?.takeIf { book.folder }
            val fromBook = openbook?.let { siblings.read(it.uri) }
                ?.let { pick(OpenBook.parse(String(it, Charsets.UTF_8)), file, single) } ?: emptyList()
            if (fromBook.size >= 2) fromBook else emptyList()
        }
    }.getOrDefault(emptyList())

    private fun pick(map: Map<String, List<Chapter>>, file: LibFile, single: Boolean): List<Chapter> {
        val name = file.name.lowercase()
        val base = name.substringBeforeLast('.')
        return map[name]
            ?: map.entries.firstOrNull { it.key.substringBeforeLast('.') == base }?.value
            ?: if (single && map.size == 1) map.values.first() else emptyList()
    }
}
