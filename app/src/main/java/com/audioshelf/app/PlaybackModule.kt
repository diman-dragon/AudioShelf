package com.audioshelf.app

import android.app.Application
import com.audioshelf.app.library.Background
import com.audioshelf.app.player.BookAdvancer
import com.audioshelf.app.player.BookLoader
import com.audioshelf.app.player.ChapterScanner
import com.audioshelf.app.player.CurrentBook
import com.audioshelf.app.player.DurationFiller
import com.audioshelf.app.player.EngineObserver
import com.audioshelf.app.player.MarkController
import com.audioshelf.app.player.OffloadControl
import com.audioshelf.app.player.PlaybackCommands
import com.audioshelf.app.player.PlaybackEngine
import com.audioshelf.app.player.PlaybackNotices
import com.audioshelf.app.player.PlayerSession
import com.audioshelf.app.player.PlayerUiPublisher
import com.audioshelf.app.player.ProgressRecorder
import com.audioshelf.app.player.SleepState
import com.audioshelf.app.player.TrackEvents
import com.audioshelf.app.sound.SoundMix
import com.audioshelf.app.sound.SoundModel
import com.audioshelf.app.sound.SoundOutput
import com.audioshelf.app.stats.StatsRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The player and everything that listens to it. This is the one place where its parts are put together. */
class PlaybackModule(
    app: Application,
    private val scope: CoroutineScope,
    background: Background,
    private val screen: ScreenWatch,
    storage: StorageModule,
    libraryModule: LibraryModule,
) {
    private val library = libraryModule.library
    private val offload = OffloadControl()
    private val output = SoundOutput(offload::setNeutral)

    // The engine saves through the recorder and the recorder reads the engine; the engine only calls it later.
    private lateinit var recorder: ProgressRecorder

    val engine = PlaybackEngine(
        app, scope, output, offload, background, screen,
        { storage.prefs.skipBack.value * 1000L },
        { storage.prefs.skipForward.value * 1000L },
        { periodic -> recorder.remember(periodic = periodic) },
    )
    val battery = BatteryGuard(app, storage.kv)
    val session: PlayerSession
    val sound: SoundModel

    init {
        val current = CurrentBook(engine, library, storage.chapters)
        val sleep = SleepState()
        recorder = ProgressRecorder(current, engine, storage.progress, storage.durations, storage.stats)
        val publisher = PlayerUiPublisher(
            current, engine, library, storage.progress, storage.durations, storage.prefs, storage.playPrefs, storage.marks, screen, sleep,
        )
        val filler = DurationFiller(scope, storage.durations, background, screen, libraryModule.durationReader, current, publisher)
        val loader = BookLoader(
            scope, engine, library, current, storage.progress, storage.marks, storage.durations, storage.playPrefs,
            ChapterScanner(app, storage.chapters), background, recorder, publisher, filler,
        )
        val advancer = BookAdvancer(storage.queue, library, storage.prefs, current, loader)
        val observer = EngineObserver(current, storage.durations, StatsRecorder(storage.stats), recorder, advancer, sleep, publisher)
        val events = TrackEvents(engine, current, storage.marks, storage.notes, storage.playPrefs, publisher)
        val commands = PlaybackCommands(
            scope, engine, current, recorder, storage.durations, storage.prefs, storage.playPrefs, sleep, publisher, loader,
        )
        session = PlayerSession(current, publisher, loader, commands, MarkController(scope, engine, current, storage.marks, loader), recorder)
        sound = SoundModel(scope, session.track, storage.eqStore, storage.fxStore, storage.soundPrefs, library)

        loader.restoreLast()
        publisher.publish()
        scope.launch { engine.state.collect { observer.onEngine(it) } }
        scope.launch {
            combine(storage.prefs.skipBack, storage.prefs.skipForward, storage.marks.version, storage.meta.version) { _, _, _, _ -> }
                .collect { publisher.publish() }
        }
        scope.launch { library.events.collect { events.onLibraryEvent(it) } }

        // Screen off: one safe save. Screen on: redraw the player from the current state.
        scope.launch {
            screen.on.collect { on ->
                if (on) {
                    engine.publish()
                    session.onScreen(true)
                } else {
                    session.remember()
                    storage.kv.flushNow()
                }
            }
        }
        offload.setAllowed(storage.prefs.offload.value)
        scope.launch { storage.prefs.offload.collect { offload.setAllowed(it) } }
        SoundMix(scope, session.track, storage.eqStore, storage.fxStore, output).start()
        PlaybackNotices(app, scope, engine.failures).start()

        // Once, at the first playback with the screen on: ask to lift the battery limit that makes the sound stutter with the screen off.
        scope.launch {
            engine.state.first { it.playing }
            if (screen.on.value && battery.shouldAsk()) {
                battery.markAsked()
                battery.open(app)
            }
        }
    }
}
