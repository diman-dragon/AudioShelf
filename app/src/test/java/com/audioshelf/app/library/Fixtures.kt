package com.audioshelf.app.library

fun file(dir: String, name: String, size: Long = 1_000_000, modified: Long = 1000) =
    LibFile(
        uri = "content://tree/${if (dir.isEmpty()) name else "$dir/$name"}",
        name = name,
        dir = dir,
        dirUri = "content://tree/$dir",
        size = size,
        modified = modified,
    )
