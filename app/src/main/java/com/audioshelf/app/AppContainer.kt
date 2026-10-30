package com.audioshelf.app

import android.app.Application
import android.content.Context
import com.audioshelf.app.library.Background
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The roots of the app, put together from three modules: storage, library, playback. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val background = Background()
    val screen = ScreenWatch(app)

    private val storage = StorageModule(app, scope, background)
    private val libraryModule = LibraryModule(app, scope, background, storage)
    private val playback = PlaybackModule(app, scope, background, screen, storage, libraryModule)

    val kv = storage.kv
    val prefs = storage.prefs
    val folders = storage.folders
    val meta = storage.meta
    val hidden = storage.hidden
    val progress = storage.progress
    val durations = storage.durations
    val playPrefs = storage.playPrefs
    val chapters = storage.chapters
    val marks = storage.marks
    val eqStore = storage.eqStore
    val fxStore = storage.fxStore
    val stats = storage.stats
    val queue = storage.queue
    val notes = storage.notes
    val soundPrefs = storage.soundPrefs

    val coverBytes = libraryModule.coverBytes
    val covers = libraryModule.covers
    val durationReader = libraryModule.durationReader
    val descriptions = libraryModule.descriptions
    val library = libraryModule.library

    val engine = playback.engine
    val session = playback.session
    val sound = playback.sound
    val battery = playback.battery
}

class ShelfApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as ShelfApplication).container
