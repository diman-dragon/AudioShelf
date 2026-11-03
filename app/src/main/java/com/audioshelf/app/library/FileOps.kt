package com.audioshelf.app.library

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoViewerException : Exception("no_viewer")

class FileOps(private val context: Context) {
    private val resolver get() = context.contentResolver

    suspend fun remove(uri: String) = withContext(Dispatchers.IO) {
        if (!DocumentsContract.deleteDocument(resolver, uri.toUri())) throw IOException("delete failed")
    }

    suspend fun rename(uri: String, name: String): String = withContext(Dispatchers.IO) {
        DocumentsContract.renameDocument(resolver, uri.toUri(), name)?.toString() ?: throw IOException("rename failed")
    }

    fun open(uri: String) {
        val target = uri.toUri()
        val mime = resolver.getType(target)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(target, if (mime != null && mime.startsWith("audio/")) mime else "audio/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw NoViewerException()
        }
    }

    fun treeName(tree: Uri): String {
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return resolver.query(root, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: ""
    }
}
