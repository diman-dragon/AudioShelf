package com.audioshelf.app.media

class ArraySource(private val data: ByteArray) : ByteSource {
    override fun length(): Long = data.size.toLong()

    override fun read(pos: Long, buf: ByteArray, off: Int, len: Int): Int {
        if (pos >= data.size) return -1
        val n = minOf(len.toLong(), data.size - pos).toInt()
        System.arraycopy(data, pos.toInt(), buf, off, n)
        return n
    }
}
