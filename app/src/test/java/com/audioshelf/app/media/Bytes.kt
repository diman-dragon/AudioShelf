package com.audioshelf.app.media

import java.io.ByteArrayOutputStream

fun cat(vararg parts: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    parts.forEach { out.write(it) }
    return out.toByteArray()
}

fun ebml(id: Long, payload: ByteArray): ByteArray {
    var len = 1
    while (id shr (8 * len) != 0L) len++
    val idBytes = ByteArray(len) { (id shr (8 * (len - 1 - it))).toByte() }
    val size = ByteArray(8)
    size[0] = 1
    for (i in 1..7) size[i] = (payload.size.toLong() shr (8 * (7 - i))).toByte()
    return cat(idBytes, size, payload)
}

fun text(s: String): ByteArray = s.toByteArray(Charsets.UTF_8)

fun uint(v: Long): ByteArray = ByteArray(8) { (v shr (8 * (7 - it))).toByte() }

fun be32(v: Long): ByteArray = ByteArray(4) { (v shr (8 * (3 - it))).toByte() }

fun box(type: String, payload: ByteArray): ByteArray = cat(be32(8L + payload.size), text(type), payload)
