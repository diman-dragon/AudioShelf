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
    fun raw(uri: Uri): ByteArray? {
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

    fun jpeg(uri: Uri, width: Int): ByteArray? {
        val raw = raw(uri) ?: return null
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
