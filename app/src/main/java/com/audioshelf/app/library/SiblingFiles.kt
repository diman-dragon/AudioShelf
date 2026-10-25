package com.audioshelf.app.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

class SiblingEntry(val name: String, val uri: String)

class SiblingFiles(private val resolver: ContentResolver) {
    fun list(dirUri: String): List<SiblingEntry> {
        val dir = Uri.parse(dirUri)
        val out = ArrayList<SiblingEntry>()
        try {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(dir, DocumentsContract.getDocumentId(dir))
            val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
            resolver.query(children, columns, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    if (c.getString(2) == Document.MIME_TYPE_DIR) continue
                    out.add(SiblingEntry(name, DocumentsContract.buildDocumentUriUsingTree(dir, id).toString()))
                }
            }
        } catch (e: Exception) {
        }
        return out
    }

    fun read(uri: String, limit: Int = 1 shl 20): ByteArray? = try {
        resolver.openInputStream(Uri.parse(uri))?.use { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (buffer.size() < limit) {
                val n = input.read(chunk)
                if (n <= 0) break
                buffer.write(chunk, 0, n)
            }
            buffer.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}
