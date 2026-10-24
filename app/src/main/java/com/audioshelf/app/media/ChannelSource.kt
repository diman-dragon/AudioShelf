package com.audioshelf.app.media

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

class ChannelSource private constructor(private val pfd: ParcelFileDescriptor) : ByteSource, Closeable {
    private val input = FileInputStream(pfd.fileDescriptor)
    private val channel = input.channel

    override fun length(): Long = channel.size()

    override fun read(pos: Long, buf: ByteArray, off: Int, len: Int): Int =
        channel.read(ByteBuffer.wrap(buf, off, len), pos)

    override fun close() {
        runCatching { input.close() }
        runCatching { pfd.close() }
    }

    companion object {
        fun open(context: Context, uri: Uri): ChannelSource {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException("cannot open $uri")
            return ChannelSource(pfd)
        }
    }
}
