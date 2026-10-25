package com.audioshelf.app.sound

import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.player.PlayBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SoundModel(
    scope: CoroutineScope,
    private val track: StateFlow<PlayBook?>,
    private val eq: EqStore,
    private val fxStore: FxStore,
    private val prefs: SoundPrefs,
    private val library: LibraryController,
) : SoundActions {
    val ui: StateFlow<SoundUi> = combine(prefs.scope, track, eq.version, fxStore.version) { pick, book, _, _ -> build(pick, book) }
        .stateIn(scope, SharingStarted.Eagerly, SoundUi())

    private fun build(pick: EqScope, book: PlayBook?): SoundUi {
        val editing = if (pick == EqScope.Track && book != null) book.id else null
        val shown = if (editing != null) eq.effective(editing) else eq.global()
        return SoundUi(
            scope = if (editing != null) EqScope.Track else EqScope.Global,
            trackAvailable = book != null,
            trackTitle = book?.let { library.metaOf(it.base).title.ifEmpty { it.base.name } } ?: "",
            eq = shown,
            ownEq = editing != null && eq.track(editing) != null,
            fx = if (editing != null) fxStore.effective(editing) else fxStore.global(),
            ownFx = editing != null && fxStore.track(editing) != null,
        )
    }

    private fun editingId(): String? {
        val book = track.value ?: return null
        return if (prefs.scope.value == EqScope.Track) book.id else null
    }

    private fun write(next: Eq) {
        val id = editingId()
        if (id != null) eq.setTrack(id, next) else eq.setGlobal(next)
    }

    private fun shown(): Eq = editingId()?.let { eq.effective(it) } ?: eq.global()

    override fun onScope(scope: EqScope) = prefs.setScope(scope)

    override fun onBand(index: Int, db: Int) {
        val current = shown()
        if (current.bands[index] == db) return
        write(current.withBand(index, db))
    }

    override fun onPreset(id: String) = write(Eq.of(id))

    override fun onReset() = onPreset("flat")

    override fun onUseGlobal() {
        editingId()?.let { eq.clearTrack(it) }
    }

    private fun shownFx(): Fx = editingId()?.let { fxStore.effective(it) } ?: fxStore.global()

    private fun writeFx(next: Fx) {
        val id = editingId()
        if (id != null) fxStore.setTrack(id, next) else fxStore.setGlobal(next)
    }

    override fun onGain(db: Int) = writeFx(shownFx().withGain(db))

    override fun onPitch(semitones: Int) = writeFx(shownFx().withPitch(semitones))

    override fun onNoise(level: Int) = writeFx(shownFx().withNoise(level))

    override fun onSkipSilence(on: Boolean) = writeFx(shownFx().withSkipSilence(on))

    override fun onFxUseGlobal() {
        editingId()?.let { fxStore.clearTrack(it) }
    }
}
