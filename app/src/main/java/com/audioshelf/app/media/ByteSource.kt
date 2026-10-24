package com.audioshelf.app.media

private const val MAX_READ = 96 shl 20

interface ByteSource {
    fun length(): Long
    fun read(pos: Long, buf: ByteArray, off: Int, len: Int): Int
}

fun ByteSource.bytes(pos: Long, len: Int): ByteArray? {
    if (pos < 0 || len < 0 || len > MAX_READ) return null
    val out = ByteArray(len)
    var got = 0
    while (got < len) {
        val n = read(pos + got, out, got, len - got)
        if (n <= 0) return null
        got += n
    }
    return out
}

class ArraySource(private val data: ByteArray) : ByteSource {
    override fun length(): Long = data.size.toLong()

    override fun read(pos: Long, buf: ByteArray, off: Int, len: Int): Int {
        if (pos >= data.size) return -1
        val n = minOf(len.toLong(), data.size - pos).toInt()
        System.arraycopy(data, pos.toInt(), buf, off, n)
        return n
    }
}
