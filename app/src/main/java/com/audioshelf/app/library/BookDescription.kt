package com.audioshelf.app.library

import android.content.Context
import com.audioshelf.app.media.CueSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_CHARS = 8000

class BookDescription(context: Context) {
    private val siblings = SiblingFiles(context.contentResolver)

    /** `info.txt` inside a book folder, or `<file name>.txt` next to a single file. */
    suspend fun of(book: Book): String? = withContext(Dispatchers.IO) {
        val file = book.files.firstOrNull() ?: return@withContext null
        val dir = if (book.folder) book.dirUri else file.dirUri
        val wanted = if (book.folder) "info.txt" else file.name.substringBeforeLast('.').lowercase() + ".txt"
        val entry = siblings.list(dir).firstOrNull { it.name.lowercase() == wanted } ?: return@withContext null
        val bytes = siblings.read(entry.uri, 64 * 1024) ?: return@withContext null
        CueSheet.decode(bytes).trim().take(MAX_CHARS).ifEmpty { null }
    }
}
