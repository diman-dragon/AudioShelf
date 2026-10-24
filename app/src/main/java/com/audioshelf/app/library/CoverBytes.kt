package com.audioshelf.app.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.audioshelf.app.media.ChannelSource
import com.audioshelf.app.media.FileTags
import java.io.ByteArrayOutputStream

private const val JPEG_QUALITY = 82

class CoverBytes(private val context: Context) {
    private val folders = FolderImages(context.contentResolver)

    fun raw(file: LibFile, track: Boolean): ByteArray? {
        val listing = runCatching { folders.of(file.dirUri) }.getOrNull()
        val own = listing?.let { SiblingArt.forTrack(file.name, it.images) }
        val general = listing?.let { SiblingArt.general(it.images, it.tracks) }
        val order: List<() -> Any?> =
            if (track) listOf({ own }, { embedded(file) }, { general }) else listOf({ general }, { embedded(file) }, { own })
        for (step in order) {
            val found = when (val v = step()) {
                is ArtFile -> readImage(v)
                is ByteArray -> v
                else -> null
            }
            if (found != null) return found
        }
        return null
    }

    private fun readImage(art: ArtFile): ByteArray? = try {
        context.contentResolver.openInputStream(Uri.parse(art.uri))?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }

    private fun embedded(file: LibFile): ByteArray? = raw(Uri.parse(file.uri))

    private fun raw(uri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { return it }
        } catch (e: Exception) {
        } finally {
            runCatching { retriever.release() }
        }
        return attached(uri)
    }

    fun jpeg(file: LibFile, width: Int, track: Boolean = false): ByteArray? {
        val raw = raw(file, track) ?: return null
        val bitmap = decode(raw, width) ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun attached(uri: Uri): ByteArray? = try {
        ChannelSource.open(context, uri).use { FileTags.read(it, true).cover }
    } catch (e: Throwable) {
        null
    }

    private fun decode(raw: ByteArray, width: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= width) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
    }
}
