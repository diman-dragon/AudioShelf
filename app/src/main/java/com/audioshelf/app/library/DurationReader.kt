package com.audioshelf.app.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DurationReader(private val context: Context) {
    suspend fun read(uri: String): Long? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
