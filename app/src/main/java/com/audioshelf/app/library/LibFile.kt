package com.audioshelf.app.library

import com.audioshelf.app.data.FlatJson
import com.audioshelf.app.data.int
import com.audioshelf.app.data.long
import com.audioshelf.app.data.str

data class LibFile(
    val uri: String,
    val name: String,
    val dir: String,
    val dirUri: String,
    val size: Long,
    val modified: Long,
)

fun encodeFile(file: LibFile, n: Int): String = FlatJson.write(
    linkedMapOf(
        "uri" to file.uri,
        "name" to file.name,
        "dir" to file.dir,
        "dirUri" to file.dirUri,
        "size" to file.size,
        "modified" to file.modified,
        "n" to n,
    ),
)

fun decodeFile(json: String): Pair<Int, LibFile>? {
    val m = FlatJson.parse(json) ?: return null
    val file = LibFile(
        uri = m.str("uri") ?: return null,
        name = m.str("name") ?: return null,
        dir = m.str("dir") ?: "",
        dirUri = m.str("dirUri") ?: "",
        size = m.long("size") ?: 0,
        modified = m.long("modified") ?: 0,
    )
    return (m.int("n") ?: 0) to file
}
