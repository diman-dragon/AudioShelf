package com.audioshelf.app.library

fun splitName(name: String): Pair<String, String> {
    val dot = name.lastIndexOf('.')
    return if (dot > 0) name.substring(0, dot) to name.substring(dot) else name to ""
}

fun cleanFileName(name: String): String = name.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
