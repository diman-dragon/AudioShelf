package com.audioshelf.app.ui.actions

import com.audioshelf.app.AppLog
import com.audioshelf.app.R
import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.LibView
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.data.ProgressFilter
import com.audioshelf.app.data.SortMode
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.CoverSource
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.Meta
import com.audioshelf.app.library.QueueStore
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.Screen
import com.audioshelf.app.ui.components.OverlayHost
import com.audioshelf.app.ui.components.message
import com.audioshelf.app.ui.library.HistoryDialog

private const val TAG = "Action"

class AppLibraryActions(
    private val library: LibraryController,
    private val prefs: Prefs,
    private val queue: QueueStore,
    private val covers: CoverSource,
    private val player: PlayerViewModel,
    private val overlays: OverlayHost,
    private val goTo: (Screen) -> Unit,
) : LibraryActions {
    override fun onPlay(book: Book) {
        AppLog.i(TAG, "play book \"${book.name}\" (${book.files.size} files)")
        player.open(book)
        goTo(Screen.Player)
    }

    override fun onRescan() {
        AppLog.i(TAG, "rescan the library")
        library.rescan()
    }

    override fun onStop() {
        AppLog.i(TAG, "stop the scan")
        library.cancelScan()
    }

    override fun onKind(kind: KindFilter) {
        AppLog.i(TAG, "filter by kind: $kind")
        prefs.setLibKind(kind)
    }

    override fun onProgress(filter: ProgressFilter) {
        AppLog.i(TAG, "filter by progress: $filter")
        prefs.setLibProgress(filter)
    }

    override fun onQueue(book: Book) {
        val added = queue.add(book.id)
        AppLog.i(TAG, "queue \"${book.name}\": added=$added")
        overlays.message(if (added) R.string.queue_added else R.string.queue_exists)
    }

    override fun onOpenHistory() {
        AppLog.i(TAG, "open history")
        overlays.show { HistoryDialog(library.allRows(), queue, covers, ::onPlay) { overlays.close() } }
    }

    override fun onSort(sort: SortMode) {
        AppLog.i(TAG, "sort: $sort")
        prefs.setLibSort(sort)
    }

    override fun onView(view: LibView) {
        AppLog.i(TAG, "view: $view")
        prefs.setLibView(view)
    }

    override fun onQuery(query: String) {
        AppLog.d(TAG, "search: \"$query\"")
        library.setQuery(query)
    }

    override fun onOpenSettings() {
        AppLog.i(TAG, "open settings")
        goTo(Screen.Settings)
    }

    override fun onEdit(book: Book, meta: Meta, baseName: String) {
        AppLog.i(TAG, "edit \"${book.name}\"")
        library.saveEdit(book, meta, baseName)
    }

    override fun onHide(book: Book) {
        AppLog.i(TAG, "hide \"${book.name}\"")
        library.hide(book)
    }

    override fun onDelete(book: Book) {
        AppLog.i(TAG, "delete \"${book.name}\"")
        library.remove(book)
    }

    override fun onOpenExternal(book: Book) {
        AppLog.i(TAG, "open \"${book.name}\" in another app")
        library.openExternal(book)
    }
}
