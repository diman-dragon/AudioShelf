package com.audioshelf.app.library

import com.audioshelf.app.AppLog
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DurationReader(private val context: Context, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun read(uri: String): Long? = withContext(dispatcher) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri.toUri())
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            AppLog.d("DurationReader", "no duration for $uri: ${e.message}")
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
