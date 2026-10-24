package com.audioshelf.app.player

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibFile

object BookJson {
    fun encode(book: Book): String = Json.write(
        linkedMapOf(
            "id" to book.id,
            "folder" to book.folder,
            "name" to book.name,
            "files" to book.files.map {
                linkedMapOf(
                    "uri" to it.uri,
                    "name" to it.name,
                    "dir" to it.dir,
                    "dirUri" to it.dirUri,
                    "size" to it.size,
                    "modified" to it.modified,
                )
            },
            "dirUri" to book.dirUri,
            "dirUris" to book.dirUris,
            "ownsDir" to book.ownsDir,
            "size" to book.size,
            "modified" to book.modified,
        ),
    )

    fun decode(text: String?): Book? {
        val m = Json.parse(text).asMap() ?: return null
        val files = m["files"].asList()?.mapNotNull { file(it.asMap()) } ?: return null
        if (files.isEmpty()) return null
        return Book(
            id = m["id"] as? String ?: return null,
            folder = m["folder"] as? Boolean ?: false,
            name = m["name"] as? String ?: "",
            files = files,
            dirUri = m["dirUri"] as? String ?: files[0].dirUri,
            dirUris = m["dirUris"].asList()?.filterIsInstance<String>() ?: listOf(files[0].dirUri),
            ownsDir = m["ownsDir"] as? Boolean ?: false,
            size = (m["size"] as? Number)?.toLong() ?: files.sumOf { it.size },
            modified = (m["modified"] as? Number)?.toLong() ?: 0,
        )
    }

    private fun file(m: Map<String, Any?>?): LibFile? {
        m ?: return null
        return LibFile(
            uri = m["uri"] as? String ?: return null,
            name = m["name"] as? String ?: return null,
            dir = m["dir"] as? String ?: "",
            dirUri = m["dirUri"] as? String ?: "",
            size = (m["size"] as? Number)?.toLong() ?: 0,
            modified = (m["modified"] as? Number)?.toLong() ?: 0,
        )
    }
}
