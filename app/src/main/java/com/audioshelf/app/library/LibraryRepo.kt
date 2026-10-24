package com.audioshelf.app.library

import android.content.ContentResolver
import android.net.Uri
import com.audioshelf.app.data.Folder
import com.audioshelf.app.data.FolderStore
import com.audioshelf.app.data.HiddenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibStatus { NoFolder, Unscanned, Ok, Error }

class LibraryRepo(
    private val scope: CoroutineScope,
    private val resolver: ContentResolver,
    private val folders: FolderStore,
    private val cache: LibraryCache,
    private val hidden: HiddenStore,
) {
    private val filesFlow = MutableStateFlow<List<LibFile>>(emptyList())
    private val statusFlow = MutableStateFlow(LibStatus.NoFolder)
    private val scanningFlow = MutableStateFlow(false)
    private val progressFlow = MutableStateFlow(ScanProgress())
    private var run: FolderScan? = null
    private var again = false

    val files: StateFlow<List<LibFile>> = filesFlow.asStateFlow()
    val status: StateFlow<LibStatus> = statusFlow.asStateFlow()
    val scanning: StateFlow<Boolean> = scanningFlow.asStateFlow()
    val progress: StateFlow<ScanProgress> = progressFlow.asStateFlow()

    val books: StateFlow<List<Book>> = combine(
        filesFlow.map { groupBooks(it) }.flowOn(Dispatchers.Default),
        hidden.version,
    ) { grouped, _ -> grouped.filter { !hidden.isHidden(it.id) }.sortedByDescending { it.modified } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        restore()
        scope.launch {
            folders.current.drop(1).collect {
                restore()
                rescan()
            }
        }
    }

    fun restore() {
        val folder = folders.current.value
        val saved = folder?.let { cache.load(it.uri) }
        filesFlow.value = saved ?: emptyList()
        statusFlow.value = when {
            folder == null -> LibStatus.NoFolder
            saved != null -> LibStatus.Ok
            else -> LibStatus.Unscanned
        }
    }

    fun cancelScan() {
        run?.cancel()
    }

    fun rescan() {
        val folder = folders.current.value
        if (folder == null) {
            restore()
            return
        }
        if (scanningFlow.value) {
            again = true
            run?.cancel()
            return
        }
        scanningFlow.value = true
        progressFlow.value = ScanProgress()
        val scan = FolderScan(resolver)
        run = scan
        scope.launch {
            var found: List<LibFile>? = null
            var failed = false
            try {
                val result = scan.scan(Uri.parse(folder.uri)) { progressFlow.value = it }
                if (!again && (!result.cancelled || statusFlow.value != LibStatus.Ok)) found = result.files
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = !again
            }
            finish(folder, found, failed)
            if (again) {
                again = false
                rescan()
            }
        }
    }

    private fun finish(folder: Folder, found: List<LibFile>?, failed: Boolean) {
        val same = folders.current.value?.uri == folder.uri
        if (same && failed) {
            statusFlow.value = LibStatus.Error
        } else if (same && found != null) {
            filesFlow.value = found
            statusFlow.value = LibStatus.Ok
            cache.save(folder.uri, found)
        }
        scanningFlow.value = false
    }

    fun patchFiles(change: (List<LibFile>) -> List<LibFile>) {
        filesFlow.value = change(filesFlow.value)
        folders.current.value?.let { cache.save(it.uri, filesFlow.value) }
    }
}
