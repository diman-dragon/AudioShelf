package com.audioshelf.app.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val WIDTH = 360
private const val MEMORY_KB = 24 * 1024
private const val MAX_FILES = 300
private const val DROP_FILES = 60

interface CoverSource {
    fun peek(file: LibFile, track: Boolean = false): Bitmap?
    suspend fun load(file: LibFile, track: Boolean = false): Bitmap?
}

object NoCovers : CoverSource {
    override fun peek(file: LibFile, track: Boolean): Bitmap? = null
    override suspend fun load(file: LibFile, track: Boolean): Bitmap? = null
}

/** A cover read back from the disk cache; [bitmap] is null for a remembered "no cover". */
private class Cached(val bitmap: Bitmap?)

class CoverLoader(
    private val context: Context,
    private val art: CoverBytes,
    private val background: Background = Background(),
) : CoverSource {
    private val memory = object : LruCache<String, Bitmap>(MEMORY_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val missing: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val gate = Mutex()

    private fun keyOf(file: LibFile, track: Boolean) = "${file.uri}|${file.modified}|${file.size}|${if (track) "t" else "b"}"

    override fun peek(file: LibFile, track: Boolean): Bitmap? = memory.get(keyOf(file, track))

    override suspend fun load(file: LibFile, track: Boolean): Bitmap? {
        val key = keyOf(file, track)
        memory.get(key)?.let { return it }
        if (key in missing) return null
        return gate.withLock {
            memory.get(key) ?: fetch(file, key, track).also { bitmap ->
                if (bitmap != null) memory.put(key, bitmap) else missing.add(key)
            }
        }
    }

    private suspend fun fetch(file: LibFile, key: String, track: Boolean): Bitmap? {
        val name = sha1("$key|$WIDTH")
        val cached = withContext(background.dispatcher) { fromDisk(name) }
        if (cached != null) return cached.bitmap
        // Extracting a cover opens the audio file and lists its folder through the storage
        // provider, the same one the player reads from, so it waits until playback is steady.
        background.calm()
        return withContext(background.dispatcher) { extract(file, name, track) }
    }

    private fun dir(): File = File(context.cacheDir, "covers").apply { mkdirs() }

    private fun fromDisk(name: String): Cached? {
        val jpg = File(dir(), "$name.jpg")
        if (jpg.isFile) return Cached(BitmapFactory.decodeFile(jpg.path))
        if (File(dir(), "$name.none").isFile) return Cached(null)
        return null
    }

    private fun extract(file: LibFile, name: String, track: Boolean): Bitmap? = try {
        val bytes = art.jpeg(file, WIDTH, track) ?: throw IllegalStateException("no cover")
        File(dir(), "$name.jpg").writeBytes(bytes)
        prune()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        runCatching { File(dir(), "$name.none").writeBytes(ByteArray(0)) }
        null
    }

    private fun prune() {
        val all = dir().listFiles() ?: return
        if (all.size <= MAX_FILES) return
        all.sortedBy { it.lastModified() }.take(DROP_FILES).forEach { it.delete() }
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
