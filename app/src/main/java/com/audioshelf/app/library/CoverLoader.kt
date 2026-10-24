package com.audioshelf.app.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val WIDTH = 360
private const val MEMORY_KB = 24 * 1024
private const val MAX_FILES = 300
private const val DROP_FILES = 60

interface CoverSource {
    fun peek(file: LibFile): Bitmap?
    suspend fun load(file: LibFile): Bitmap?
}

object NoCovers : CoverSource {
    override fun peek(file: LibFile): Bitmap? = null
    override suspend fun load(file: LibFile): Bitmap? = null
}

class CoverLoader(private val context: Context, private val art: CoverBytes) : CoverSource {
    private val memory = object : LruCache<String, Bitmap>(MEMORY_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val missing: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val gate = Mutex()

    private fun keyOf(file: LibFile) = "${file.uri}|${file.modified}|${file.size}"

    override fun peek(file: LibFile): Bitmap? = memory.get(keyOf(file))

    override suspend fun load(file: LibFile): Bitmap? {
        val key = keyOf(file)
        memory.get(key)?.let { return it }
        if (key in missing) return null
        return gate.withLock {
            memory.get(key) ?: withContext(Dispatchers.IO) { read(file, key) }.also { bitmap ->
                if (bitmap != null) memory.put(key, bitmap) else missing.add(key)
            }
        }
    }

    private fun dir(): File = File(context.cacheDir, "covers").apply { mkdirs() }

    private fun read(file: LibFile, key: String): Bitmap? {
        val name = sha1("$key|$WIDTH")
        val jpg = File(dir(), "$name.jpg")
        val none = File(dir(), "$name.none")
        if (jpg.isFile) return BitmapFactory.decodeFile(jpg.path)
        if (none.isFile) return null
        return try {
            val bytes = extract(Uri.parse(file.uri))
            jpg.writeBytes(bytes)
            prune()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            runCatching { none.writeBytes(ByteArray(0)) }
            null
        }
    }

    private fun extract(uri: Uri): ByteArray = art.jpeg(uri, WIDTH) ?: throw IllegalStateException("no cover")

    private fun prune() {
        val all = dir().listFiles() ?: return
        if (all.size <= MAX_FILES) return
        all.sortedBy { it.lastModified() }.take(DROP_FILES).forEach { it.delete() }
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
