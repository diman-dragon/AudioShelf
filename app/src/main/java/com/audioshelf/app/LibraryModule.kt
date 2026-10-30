package com.audioshelf.app

import android.app.Application
import com.audioshelf.app.library.Background
import com.audioshelf.app.library.BookDescription
import com.audioshelf.app.library.CoverBytes
import com.audioshelf.app.library.CoverLoader
import com.audioshelf.app.library.DurationReader
import com.audioshelf.app.library.FileOps
import com.audioshelf.app.library.LibraryCache
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.LibraryRepo
import kotlinx.coroutines.CoroutineScope

/** The library: scanning the folder, covers, durations and the controller the screens talk to. */
class LibraryModule(app: Application, scope: CoroutineScope, background: Background, storage: StorageModule) {
    val coverBytes = CoverBytes(app)
    val covers = CoverLoader(app, coverBytes, background)
    val durationReader = DurationReader(app, background.dispatcher)
    val descriptions = BookDescription(app)
    private val fileOps = FileOps(app)
    private val repo = LibraryRepo(scope, app.contentResolver, storage.folders, LibraryCache(storage.kv), storage.hidden)
    val library = LibraryController(
        scope, app, repo, storage.prefs, storage.folders, storage.meta, storage.progress, storage.durations, storage.hidden, fileOps,
    )
}
