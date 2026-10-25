package com.audioshelf.app.media

object FileTags {
    fun read(src: ByteSource, wantCover: Boolean): FileInfo = when {
        MkaFile.matches(src) -> MkaFile.read(src, wantCover)
        Mp4Chapters.matches(src) -> FileInfo(Mp4Chapters.read(src))
        Id3Chapters.matches(src) -> FileInfo(Id3Chapters.read(src))
        VorbisChapters.matches(src) -> FileInfo(VorbisChapters.read(src))
        else -> FileInfo()
    }
}
