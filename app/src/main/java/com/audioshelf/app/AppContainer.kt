package com.audioshelf.app

import android.app.Application
import android.content.Context
import com.audioshelf.app.diag.BluetoothWatch
import com.audioshelf.app.diag.LogShare
import com.audioshelf.app.diag.SystemWatch
import com.audioshelf.app.library.Background
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.SupervisorJob

/** The roots of the app, put together from three modules: storage, library, playback. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val background = Background()
    val screen = ScreenWatch(app)
    val system = SystemWatch(app).also { it.start() }
    val bluetooth = BluetoothWatch(app).also { it.start() }

    private val storage = StorageModule(app, scope, background)
    private val libraryModule = LibraryModule(app, scope, background, storage)
    private val playback = PlaybackModule(app, scope, background, screen, system, storage, libraryModule)

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
    val backup = BackupController(app, storage.kv)
    val logShare = LogShare(app) {
        system.describe() + "\n" + bluetooth.summary() + "\noffload: ${storage.prefs.offload.value}, auto-next: ${storage.prefs.autoNext.value}"
    }
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
        AppLog.attach(LogFile(File(filesDir, "logs")))
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            AppLog.e("Crash", "uncaught exception on ${thread.name}", error)
            AppLog.file?.flush(2_000L)
            previous?.uncaughtException(thread, error)
        }
        AppLog.i("App", "process started, AudioShelf ${BuildConfig.VERSION_NAME}")
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as ShelfApplication).container
