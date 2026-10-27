package com.audioshelf.app

import android.app.Application
import android.content.Context
import com.audioshelf.app.data.FolderStore
import com.audioshelf.app.data.HiddenStore
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.data.SqliteKv
import com.audioshelf.app.library.Background
import com.audioshelf.app.library.BookDescription
import com.audioshelf.app.library.CoverBytes
import com.audioshelf.app.library.CoverLoader
import com.audioshelf.app.library.DurationReader
import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.library.FileOps
import com.audioshelf.app.library.LibraryCache
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.LibraryRepo
import com.audioshelf.app.library.MetaStore
import com.audioshelf.app.library.NoteStore
import com.audioshelf.app.library.ProgressStore
import com.audioshelf.app.library.QueueStore
import com.audioshelf.app.player.ChapterScanner
import com.audioshelf.app.player.ChapterStore
import com.audioshelf.app.player.MarkStore
import com.audioshelf.app.player.OffloadControl
import com.audioshelf.app.player.PlayPrefs
import com.audioshelf.app.player.PlaybackEngine
import com.audioshelf.app.player.PlayerSession
import com.audioshelf.app.sound.EqStore
import com.audioshelf.app.sound.FxStore
import com.audioshelf.app.sound.SoundMix
import com.audioshelf.app.sound.SoundModel
import com.audioshelf.app.sound.SoundOutput
import com.audioshelf.app.sound.SoundPrefs
import com.audioshelf.app.stats.StatsRecorder
import com.audioshelf.app.stats.StatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val kv = Kv(SqliteKv(app), scope)
    val prefs = Prefs(kv)
    val folders = FolderStore(kv)
    val meta = MetaStore(kv)
    val hidden = HiddenStore(kv)
    val progress = ProgressStore(kv)
    val durations = DurationStore(kv)
    val background = Background()
    val screen = ScreenWatch(app)
    val coverBytes = CoverBytes(app)
    val covers = CoverLoader(app, coverBytes, background)
    val durationReader = DurationReader(app, background.dispatcher)
    private val fileOps = FileOps(app)
    private val repo = LibraryRepo(scope, app.contentResolver, folders, LibraryCache(kv), hidden)
    val library = LibraryController(scope, app, repo, prefs, folders, meta, progress, durations, hidden, fileOps)

    val playPrefs = PlayPrefs(kv)
    val chapters = ChapterStore(kv)
    val marks = MarkStore(kv)
    val eqStore = EqStore(kv)
    val fxStore = FxStore(kv)
    val stats = StatsStore(kv)
    val queue = QueueStore(kv)
    val notes = NoteStore(kv)
    val descriptions = BookDescription(app)
    private val statsRecorder = StatsRecorder(stats)
    val soundPrefs = SoundPrefs(kv)
    private val offload = OffloadControl()
    private val output = SoundOutput(offload::setNeutral)
    lateinit var session: PlayerSession
        private set

    val engine: PlaybackEngine = PlaybackEngine(
        app, scope, output, offload, background, screen,
        { prefs.skipBack.value * 1000L },
        { prefs.skipForward.value * 1000L },
        { periodic -> if (!periodic || screen.on.value) session.remember(periodic = periodic) },
    )
    init {
        session = PlayerSession(
            scope, app, engine, library, meta, progress, durations, prefs, playPrefs,
            chapters, ChapterScanner(app, chapters), marks, background, screen, durationReader, stats, statsRecorder, queue, notes,
        )
    }
    val sound: SoundModel = SoundModel(scope, session.track, eqStore, fxStore, soundPrefs, library)

    init {
        // Dark screen: save once, then keep every change in memory. Screen on: write what piled up and redraw.
        scope.launch {
            screen.on.collect { on ->
                if (on) {
                    kv.setDeferred(false)
                    session.remember()
                    engine.publish()
                    session.onScreen(true)
                } else {
                    session.remember()
                    kv.flushNow()
                    kv.setDeferred(true)
                }
            }
        }
        offload.setAllowed(prefs.offload.value)
        scope.launch { prefs.offload.collect { offload.setAllowed(it) } }
        SoundMix(scope, session.track, eqStore, fxStore, prefs.gain, output, engine::setSkipSilence).start()
    }
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
