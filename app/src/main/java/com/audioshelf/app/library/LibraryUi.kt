package com.audioshelf.app.library

import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.LibView
import com.audioshelf.app.data.ProgressFilter
import com.audioshelf.app.data.SortMode

data class LibraryUi(
    val status: LibStatus = LibStatus.NoFolder,
    val scanning: Boolean = false,
    val progress: ScanProgress = ScanProgress(),
    val total: Int = 0,
    val rows: List<BookRow> = emptyList(),
    val kind: KindFilter = KindFilter.All,
    val sort: SortMode = SortMode.Date,
    val view: LibView = LibView.Rows,
    val query: String = "",
    val progressFilter: ProgressFilter = ProgressFilter.All,
) {
    val hasBooks: Boolean get() = status == LibStatus.Ok && total > 0
    val barVisible: Boolean get() = status != LibStatus.NoFolder && status != LibStatus.Error
}

sealed interface LibraryEvent {
    class Gone(val id: String) : LibraryEvent
    class Moved(val from: String, val to: String, val file: LibFile) : LibraryEvent
    object FolderCleared : LibraryEvent
}

interface LibraryActions {
    fun onPlay(book: Book)
    fun onRescan()
    fun onStop()
    fun onKind(kind: KindFilter)
    fun onProgress(filter: ProgressFilter)
    fun onQueue(book: Book)
    fun onOpenHistory()
    fun onSort(sort: SortMode)
    fun onView(view: LibView)
    fun onQuery(query: String)
    fun onOpenSettings()
    fun onEdit(book: Book, meta: Meta, baseName: String)
    fun onHide(book: Book)
    fun onDelete(book: Book)
    fun onOpenExternal(book: Book)

    companion object {
        val None = object : LibraryActions {
            override fun onPlay(book: Book) {}
            override fun onRescan() {}
            override fun onStop() {}
            override fun onKind(kind: KindFilter) {}
            override fun onProgress(filter: ProgressFilter) {}
            override fun onQueue(book: Book) {}
            override fun onOpenHistory() {}
            override fun onSort(sort: SortMode) {}
            override fun onView(view: LibView) {}
            override fun onQuery(query: String) {}
            override fun onOpenSettings() {}
            override fun onEdit(book: Book, meta: Meta, baseName: String) {}
            override fun onHide(book: Book) {}
            override fun onDelete(book: Book) {}
            override fun onOpenExternal(book: Book) {}
        }
    }
}
