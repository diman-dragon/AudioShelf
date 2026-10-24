package com.audioshelf.app.library

data class Book(
    val id: String,
    val folder: Boolean,
    val name: String,
    val files: List<LibFile>,
    val dirUri: String,
    val dirUris: List<String>,
    val ownsDir: Boolean,
    val size: Long,
    val modified: Long,
)
