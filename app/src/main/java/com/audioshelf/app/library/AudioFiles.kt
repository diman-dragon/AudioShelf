package com.audioshelf.app.library

import java.util.Locale

private val EXT = setOf(
    "mp3", "mp2", "mpga", "m4a", "m4b", "m4r", "aac", "adts", "ogg", "oga", "opus", "flac", "wav", "wave",
    "mka", "weba", "amr", "awb", "3ga", "ac3", "eac3", "ec3", "mp4", "webm", "3gp", "3gpp", "3g2",
)

private val UNPLAYABLE = setOf(
    "wma", "asf", "aif", "aiff", "aifc", "au", "snd", "mid", "midi", "kar", "xmf", "rtttl", "rtx", "ota",
    "imy", "dts", "dtshd", "ape", "wv", "tta", "mpc", "dsf", "dff", "ra", "ram", "caf", "spx",
)

fun extensionOf(name: String?): String {
    val dot = name?.lastIndexOf('.') ?: -1
    return if (dot < 0 || name == null) "" else name.substring(dot + 1).lowercase(Locale.ROOT)
}

fun isAudio(name: String?, mime: String?): Boolean {
    val ext = extensionOf(name)
    if (ext in UNPLAYABLE) return false
    if (mime != null && mime.startsWith("audio/")) return true
    return ext in EXT
}
