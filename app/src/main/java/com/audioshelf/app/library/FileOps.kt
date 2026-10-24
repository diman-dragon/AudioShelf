package com.audioshelf.app.library

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoViewerException : Exception("no_viewer")

private const val COPY_BUFFER = 64 * 1024

class FileOps(private val context: Context) {
    private val resolver get() = context.contentResolver

    suspend fun remove(uri: String) = withContext(Dispatchers.IO) {
        if (!DocumentsContract.deleteDocument(resolver, Uri.parse(uri))) throw IOException("delete failed")
    }

    suspend fun rename(uri: String, name: String): String = withContext(Dispatchers.IO) {
        DocumentsContract.renameDocument(resolver, Uri.parse(uri), name)?.toString() ?: throw IOException("rename failed")
    }

    fun open(uri: String) {
        val target = Uri.parse(uri)
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

    suspend fun add(sources: List<Uri>, tree: String): List<LibFile> = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        sources.mapNotNull { copyOne(it, parent) }
    }

    private fun copyOne(source: Uri, parent: Uri): LibFile? {
        var target: Uri? = null
        try {
            val name = displayName(source) ?: "audio"
            target = DocumentsContract.createDocument(resolver, parent, mimeFor(name), name) ?: return null
            val input = resolver.openInputStream(source) ?: throw IOException("cannot read")
            val output = resolver.openOutputStream(target) ?: throw IOException("cannot write")
            input.use { src -> output.use { dst -> src.copyTo(dst, COPY_BUFFER) } }
            return describe(target, parent)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            target?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
            return null
        }
    }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    private fun describe(doc: Uri, parent: Uri): LibFile? {
        val columns = arrayOf(Document.COLUMN_DISPLAY_NAME, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED)
        return resolver.query(doc, columns, null, null, null)?.use { c ->
            if (!c.moveToFirst()) null
            else LibFile(doc.toString(), c.getString(0) ?: "", "", parent.toString(), c.getLong(1), c.getLong(2))
        }
    }

    private fun mimeFor(name: String): String {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extensionOf(name))
        return if (mime != null && mime.startsWith("audio/")) mime else "audio/*"
    }

    fun treeName(tree: Uri): String {
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return resolver.query(root, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: ""
    }
}
