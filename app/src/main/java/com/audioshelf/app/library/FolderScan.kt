package com.audioshelf.app.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val THREADS = 4
private const val REPORT_EVERY_MS = 150L

private val COLUMNS = arrayOf(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_SIZE,
    Document.COLUMN_LAST_MODIFIED,
)

data class ScanProgress(val files: Int = 0, val folders: Int = 0)

class ScanResult(val files: List<LibFile>, val cancelled: Boolean)

class FolderScan(private val resolver: ContentResolver) {
    @Volatile
    private var cancelled = false
    private val found = ConcurrentLinkedQueue<LibFile>()
    private val folders = AtomicInteger()
    private val fileCount = AtomicInteger()
    private val lastReport = AtomicLong()
    private val gate = Semaphore(THREADS)

    fun cancel() {
        cancelled = true
    }

    suspend fun scan(tree: Uri, onProgress: (ScanProgress) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        walk(tree, DocumentsContract.getTreeDocumentId(tree), "", true, onProgress)
        onProgress(ScanProgress(fileCount.get(), folders.get()))
        val sorted = found.sortedWith(compareBy({ it.dir }, { it.name }))
        ScanResult(sorted, cancelled)
    }

    private suspend fun walk(tree: Uri, dirId: String, path: String, root: Boolean, onProgress: (ScanProgress) -> Unit) {
        if (cancelled) return
        val subs = ArrayList<Pair<String, String>>()
        try {
            gate.withPermit { read(tree, dirId, path, subs, onProgress) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (root) throw e
            return
        }
        coroutineScope {
            subs.map { (id, sub) -> async { walk(tree, id, sub, false, onProgress) } }.awaitAll()
        }
    }

    private fun read(tree: Uri, dirId: String, path: String, subs: MutableList<Pair<String, String>>, onProgress: (ScanProgress) -> Unit) {
        folders.incrementAndGet()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId)
        val dirUri = DocumentsContract.buildDocumentUriUsingTree(tree, dirId).toString()
        val cursor = resolver.query(children, COLUMNS, null, null, null) ?: throw IllegalStateException("cannot read folder")
        cursor.use { c ->
            while (!cancelled && c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: ""
                val mime = c.getString(2)
                if (mime == Document.MIME_TYPE_DIR) {
                    subs.add(id to if (path.isEmpty()) name else "$path/$name")
                } else if (isAudio(name, mime)) {
                    found.add(
                        LibFile(
                            uri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(),
                            name = name,
                            dir = path,
                            dirUri = dirUri,
                            size = c.getLong(3),
                            modified = c.getLong(4),
                        ),
                    )
                    fileCount.incrementAndGet()
                }
                report(onProgress)
            }
        }
    }

    private fun report(onProgress: (ScanProgress) -> Unit) {
        val now = System.currentTimeMillis()
        val last = lastReport.get()
        if (now - last >= REPORT_EVERY_MS && lastReport.compareAndSet(last, now)) {
            onProgress(ScanProgress(fileCount.get(), folders.get()))
        }
    }
}
