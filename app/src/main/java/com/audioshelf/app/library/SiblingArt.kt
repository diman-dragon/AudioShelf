package com.audioshelf.app.library

private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp")
private val GENERAL = setOf("cover", "folder", "front", "albumart", "album", "poster", "thumb", "обложка")
private val ZEROS = Regex("0+")

class ArtFile(val name: String, val uri: String)

object SiblingArt {
    fun isImage(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in IMAGE_EXT

    fun baseOf(name: String): String = name.substringBeforeLast('.', name).trim().lowercase()

    fun forTrack(trackName: String, images: List<ArtFile>): ArtFile? {
        val base = baseOf(trackName)
        return images.firstOrNull { baseOf(it.name) == base }
    }

    fun general(images: List<ArtFile>, trackNames: List<String>): ArtFile? {
        val named = images.firstOrNull { baseOf(it.name).let { b -> b in GENERAL || ZEROS.matches(b) } }
        if (named != null) return named
        val taken = trackNames.map { baseOf(it) }.toSet()
        val free = images.filter { baseOf(it.name) !in taken }
        return free.singleOrNull()
    }
}
