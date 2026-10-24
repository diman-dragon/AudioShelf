package com.audioshelf.app

import android.app.Application
import android.content.Context
import com.audioshelf.app.data.FolderStore
import com.audioshelf.app.data.HiddenStore
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.data.SqliteKv
import com.audioshelf.app.library.CoverBytes
import com.audioshelf.app.library.CoverLoader
import com.audioshelf.app.library.DurationReader
import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.library.FileOps
import com.audioshelf.app.library.LibraryCache
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.LibraryRepo
import com.audioshelf.app.library.MetaStore
import com.audioshelf.app.library.ProgressStore
import com.audioshelf.app.player.ChapterScanner
import com.audioshelf.app.player.ChapterStore
import com.audioshelf.app.player.MarkStore
import com.audioshelf.app.player.OffloadControl
import com.audioshelf.app.player.PlayPrefs
import com.audioshelf.app.player.PlaybackEngine
import com.audioshelf.app.player.PlayerSession
import com.audioshelf.app.sound.AutoNormalize
import com.audioshelf.app.sound.EqStore
import com.audioshelf.app.sound.GainStore
import com.audioshelf.app.sound.Normalizer
import com.audioshelf.app.sound.SoundMix
import com.audioshelf.app.sound.SoundModel
import com.audioshelf.app.sound.SoundOutput
import com.audioshelf.app.sound.SoundPrefs
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
    val coverBytes = CoverBytes(app)
    val covers = CoverLoader(app, coverBytes)
    val durationReader = DurationReader(app)
    private val fileOps = FileOps(app)
    private val repo = LibraryRepo(scope, app.contentResolver, folders, LibraryCache(kv), hidden)
    val library = LibraryController(scope, app, repo, prefs, folders, meta, progress, durations, hidden, fileOps)

    val playPrefs = PlayPrefs(kv)
    val chapters = ChapterStore(kv)
    val marks = MarkStore(kv)
    val eqStore = EqStore(kv)
    val gains = GainStore(kv)
    val soundPrefs = SoundPrefs(kv)
    private val offload = OffloadControl()
    private val output = SoundOutput(offload::setNeutral)
    lateinit var session: PlayerSession
        private set

    val engine: PlaybackEngine = PlaybackEngine(
        app, scope, output, offload,
        { prefs.skipBack.value * 1000L },
        { prefs.skipForward.value * 1000L },
        { session.remember() },
    )
    init {
        session = PlayerSession(
            scope, app, engine, library, meta, progress, durations, prefs, playPrefs,
            chapters, ChapterScanner(app, chapters), marks, coverBytes, durationReader,
        )
    }
    val normalizer: Normalizer = Normalizer(scope, app, { library.files.value }, gains)
    val sound: SoundModel = SoundModel(scope, library, session.track, eqStore, gains, soundPrefs, normalizer)

    init {
        offload.setAllowed(prefs.offload.value)
        scope.launch { prefs.offload.collect { offload.setAllowed(it) } }
        SoundMix(scope, session.track, eqStore, gains, soundPrefs, output).start()
        AutoNormalize(
            scope, normalizer, soundPrefs.normOn, library.libraryStatus, library.scanning,
            engine.state.map { it.playing }.stateIn(scope, SharingStarted.Eagerly, false),
            library.files,
        ).start()
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
