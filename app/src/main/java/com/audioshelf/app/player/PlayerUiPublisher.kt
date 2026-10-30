package com.audioshelf.app.player

import com.audioshelf.app.ScreenWatch
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.ProgressStore
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val POSITION_STEP_MS = 200L

/** Turns the engine state and the stored data into what the player screen shows. Nothing is drawn behind a dark screen. */
class PlayerUiPublisher(
    private val current: CurrentBook,
    private val engine: PlaybackEngine,
    private val library: LibraryController,
    private val progress: ProgressStore,
    private val durations: DurationStore,
    private val prefs: Prefs,
    private val playPrefs: PlayPrefs,
    private val marks: MarkStore,
    private val screen: ScreenWatch,
    private val sleep: SleepState,
) {
    private val flow = MutableStateFlow(PlayerUi())
    private var shown: PlayState? = null

    val ui: StateFlow<PlayerUi> = flow.asStateFlow()

    /** The first thing on wake-up is a fresh state. */
    fun onScreen(on: Boolean) {
        if (on) publish()
    }

    /** Fader, sleep timer and listeners all poke the engine; redraw only for a real change or a position step. */
    fun onEngine(s: PlayState) {
        val last = shown
        if (last == null || s.copy(positionMs = last.positionMs) != last || abs(s.positionMs - last.positionMs) >= POSITION_STEP_MS) publish()
    }

    fun publish() {
        if (!screen.on.value) return
        shown = engine.state.value
        val t = current.get()
        if (t == null) {
            flow.value = PlayerUi()
            return
        }
        val s = engine.state.value
        val loaded = current.isLoaded(t, s)
        val (index, position) = if (loaded) 0 to 0L else progress.resume(t.id)
        val m = library.metaOf(t.base)
        val kind = current.kindOf(t)
        flow.value = buildPlayerUi(
            UiInput(
                track = t,
                state = s,
                loaded = loaded,
                resumeIndex = index,
                resumePositionMs = position,
                kind = kind,
                title = m.title.ifEmpty { t.base.name },
                author = m.author,
                known = durations.get(t.id),
                skipBackSec = prefs.skipBack.value,
                skipForwardSec = prefs.skipForward.value,
                speed = playPrefs.speed(),
                sleep = sleep.choice,
                mode = playPrefs.mode(kind),
                marks = marks.count(t.id),
            ),
        )
    }
}
