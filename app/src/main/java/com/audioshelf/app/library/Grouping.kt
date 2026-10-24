package com.audioshelf.app.library

private val BY_NAME = Comparator<LibFile> { a, b -> NaturalOrder.compare(a.dir + "/" + a.name, b.dir + "/" + b.name) }

fun countUnder(files: List<LibFile>): Map<String, Int> {
    val under = HashMap<String, Int>()
    for (file in files) {
        if (file.dir.isEmpty()) continue
        val parts = file.dir.split('/')
        for (i in 1..parts.size) {
            val key = parts.take(i).joinToString("/")
            under[key] = (under[key] ?: 0) + 1
        }
    }
    return under
}

private fun single(file: LibFile) = Book(
    id = file.uri,
    folder = false,
    name = splitName(file.name).first,
    files = listOf(file),
    dirUri = file.dirUri,
    dirUris = listOf(file.dirUri),
    ownsDir = false,
    size = file.size,
    modified = file.modified,
)

private fun folderBook(dir: String, files: List<LibFile>, under: Map<String, Int>): Book {
    val sorted = files.sortedWith(BY_NAME)
    return Book(
        id = "dir:$dir",
        folder = true,
        name = dir.substringAfterLast('/'),
        files = sorted,
        dirUri = sorted[0].dirUri,
        dirUris = sorted.map { it.dirUri }.distinct(),
        ownsDir = under[dir] == sorted.size,
        size = sorted.sumOf { it.size },
        modified = sorted.maxOf { it.modified },
    )
}

fun groupBooks(files: List<LibFile>): List<Book> {
    val under = countUnder(files)
    val folders = LinkedHashMap<String, MutableList<LibFile>>()
    val books = ArrayList<Book>()
    for (file in files) {
        if (file.dir.isEmpty()) books.add(single(file))
        else folders.getOrPut(groupDir(file.dir)) { ArrayList() }.add(file)
    }
    for ((dir, list) in folders) {
        for (part in splitGroup(list)) {
            books.add(if (part.size == 1) single(part[0]) else folderBook(dir, part, under))
        }
    }
    val seen = HashSet<String>()
    return books.filter { seen.add(it.id) }
}
