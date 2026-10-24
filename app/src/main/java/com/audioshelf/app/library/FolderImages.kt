package com.audioshelf.app.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.util.concurrent.ConcurrentHashMap

private const val MAX_DIRS = 64

class FolderListing(val images: List<ArtFile>, val tracks: List<String>)

class FolderImages(private val resolver: ContentResolver) {
    private val cache = ConcurrentHashMap<String, FolderListing>()

    fun of(dirUri: String): FolderListing {
        cache[dirUri]?.let { return it }
        val listing = read(Uri.parse(dirUri))
        if (cache.size >= MAX_DIRS) cache.clear()
        cache[dirUri] = listing
        return listing
    }

    private fun read(dir: Uri): FolderListing {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(dir, DocumentsContract.getDocumentId(dir))
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        val images = ArrayList<ArtFile>()
        val tracks = ArrayList<String>()
        try {
            resolver.query(children, columns, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2)
                    if (mime == Document.MIME_TYPE_DIR) continue
                    if (SiblingArt.isImage(name)) {
                        images.add(ArtFile(name, DocumentsContract.buildDocumentUriUsingTree(dir, id).toString()))
                    } else if (isAudio(name, mime)) {
                        tracks.add(name)
                    }
                }
            }
        } catch (e: Exception) {
        }
        images.sortBy { it.name }
        return FolderListing(images, tracks)
    }
}
