@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.audioshelf.app.library.CoverSource
import com.audioshelf.app.library.LibFile
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Resolves [artUriOf] pointers through the cover cache, so the lock screen art never blocks playback. */
class ShelfBitmapLoader(
    private val scope: CoroutineScope,
    private val covers: CoverSource,
    private val fileOf: (String) -> LibFile?,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch(Dispatchers.Default) {
            val bitmap = runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull()
            if (bitmap != null) future.set(bitmap) else future.setException(IllegalArgumentException("not an image"))
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val source = if (uri.scheme == ART_SCHEME) uri.getQueryParameter("u") else null
        if (source == null) {
            future.setException(IllegalArgumentException("unsupported artwork: $uri"))
            return future
        }
        val job = scope.launch {
            val file = fileOf(source)
            val bitmap = if (file == null) null else runCatching { covers.load(file) }.getOrNull()
            if (bitmap != null) future.set(bitmap) else future.setException(IllegalStateException("no cover"))
        }
        future.addListener({ if (future.isCancelled) job.cancel() }, MoreExecutors.directExecutor())
        return future
    }
}
