package com.audioshelf.app.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.audioshelf.app.R
import com.audioshelf.app.data.Folder
import com.audioshelf.app.data.FolderStore
import com.audioshelf.app.data.HiddenStore
import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.LibView
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.data.SortMode
import com.audioshelf.app.player.Kind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private class Core(val books: List<Book>, val status: LibStatus, val scanning: Boolean, val progress: ScanProgress)

private class Shown(val kind: KindFilter, val sort: SortMode, val view: LibView, val query: String)

class LibraryController(
    private val scope: CoroutineScope,
    private val context: Context,
    private val repo: LibraryRepo,
    private val prefs: Prefs,
    private val folders: FolderStore,
    private val meta: MetaStore,
    private val progress: ProgressStore,
    private val durations: DurationStore,
    private val hidden: HiddenStore,
    private val fileOps: FileOps,
) {
    private val queryFlow = MutableStateFlow("")
    private val messageFlow = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    private val eventFlow = MutableSharedFlow<LibraryEvent>(extraBufferCapacity = 8)

    val messages: SharedFlow<Int> = messageFlow.asSharedFlow()
    val events: SharedFlow<LibraryEvent> = eventFlow.asSharedFlow()

    val ui: StateFlow<LibraryUi> = combine(
        combine(repo.books, repo.status, repo.scanning, repo.progress) { b, s, sc, p -> Core(b, s, sc, p) },
        combine(prefs.libKind, prefs.libSort, prefs.libView, queryFlow) { k, s, v, q -> Shown(k, s, v, q) },
        combine(meta.version, progress.version) { a, b -> a + b },
    ) { core, shown, _ -> build(core, shown) }
        .stateIn(scope, SharingStarted.Eagerly, LibraryUi())

    val files: StateFlow<List<LibFile>> get() = repo.files

    val libraryStatus: StateFlow<LibStatus> get() = repo.status

    val scanning: StateFlow<Boolean> get() = repo.scanning

    val fileCount: StateFlow<Int> = repo.files.map { it.size }.stateIn(scope, SharingStarted.Eagerly, 0)

    val hiddenCount: StateFlow<Int> = hidden.version.map { hidden.count() }
        .stateIn(scope, SharingStarted.Eagerly, hidden.count())

    private fun build(core: Core, shown: Shown): LibraryUi {
        val rows = if (core.status == LibStatus.Ok) {
            filterRows(core.books.map { buildRow(it, meta.get(it.id), progress, durations) }, shown.kind, shown.sort, shown.query)
        } else {
            emptyList()
        }
        return LibraryUi(core.status, core.scanning, core.progress, core.books.size, rows, shown.kind, shown.sort, shown.view, shown.query)
    }

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun rescan() = repo.rescan()

    fun cancelScan() = repo.cancelScan()

    fun hide(book: Book) = hidden.hide(book.id)

    fun restoreHidden() = hidden.restoreAll()

    fun fail(e: Throwable) {
        val text = e.message ?: ""
        if (text.contains("cancel")) return
        messageFlow.tryEmit(if (e is NoViewerException) R.string.lib_no_viewer else R.string.lib_fail)
    }

    fun openExternal(book: Book) {
        try {
            fileOps.open(book.files[0].uri)
        } catch (e: Exception) {
            fail(e)
        }
    }

    fun remove(book: Book) {
        val targets = if (book.folder && book.ownsDir) book.dirUris else book.files.map { it.uri }
        scope.launch {
            try {
                targets.forEach { fileOps.remove(it) }
                meta.forget(book.id)
                progress.forget(book.id)
                durations.forget(book.id)
                hidden.forget(book.id)
                val gone = book.files.mapTo(HashSet()) { it.uri }
                repo.patchFiles { list -> list.filter { it.uri !in gone } }
                eventFlow.tryEmit(LibraryEvent.Gone(book.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun saveEdit(book: Book, edited: Meta, baseName: String) {
        scope.launch {
            val file = book.files[0]
            val (oldBase, ext) = splitName(file.name)
            val clean = cleanFileName(baseName)
            var id = book.id
            try {
                if (!book.folder && clean.isNotEmpty() && clean != oldBase) {
                    val uri = fileOps.rename(file.uri, clean + ext)
                    meta.move(id, uri)
                    progress.move(id, uri)
                    durations.move(id, uri)
                    hidden.move(id, uri)
                    val renamed = file.copy(uri = uri, name = clean + ext)
                    repo.patchFiles { list -> list.map { if (it.uri == file.uri) renamed else it } }
                    eventFlow.tryEmit(LibraryEvent.Moved(id, uri, renamed))
                    id = uri
                }
                meta.set(id, edited)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun addFiles(sources: List<Uri>) {
        val folder = folders.current.value ?: return
        if (sources.isEmpty()) return
        scope.launch {
            try {
                val added = fileOps.add(sources, folder.uri)
                repo.patchFiles { it + added }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun chooseFolder(tree: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: Exception) {
            runCatching { context.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        folders.set(Folder(tree.toString(), runCatching { fileOps.treeName(tree) }.getOrDefault("")))
    }

    fun forgetFolder() {
        val folder = folders.current.value ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(folder.uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        eventFlow.tryEmit(LibraryEvent.FolderCleared)
        folders.clear()
    }

    fun kindOfBook(book: Book): Kind = kindOf(book, meta.get(book.id), durations.get(book.id)[0] ?: 0L)

    fun metaOf(book: Book): Meta = meta.get(book.id)
}
