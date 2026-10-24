package com.audioshelf.app.sound

import com.audioshelf.app.library.LibFile
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.player.PlayBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private class Pick(val scope: EqScope, val boost: Int, val normOn: Boolean)

private class Versions(val book: PlayBook?, val files: List<LibFile>)

class SoundModel(
    scope: CoroutineScope,
    private val library: LibraryController,
    private val track: StateFlow<PlayBook?>,
    private val eq: EqStore,
    private val gains: GainStore,
    private val prefs: SoundPrefs,
    private val normalizer: Normalizer,
) : SoundActions {
    private val inputs = combine(prefs.scope, prefs.boost, prefs.normOn) { s, b, n -> Pick(s, b, n) }
    private val context = combine(track, library.files) { book, files -> Versions(book, files) }
    private val changes = combine(eq.version, gains.version) { a, b -> a + b }

    val ui: StateFlow<SoundUi> = combine(inputs, context, changes, normalizer.state) { pick, ctx, _, norm -> build(pick, ctx, norm) }
        .stateIn(scope, SharingStarted.Eagerly, SoundUi())

    private fun build(pick: Pick, ctx: Versions, norm: NormState): SoundUi {
        val book = ctx.book
        val editing = if (pick.scope == EqScope.Track && book != null) book.id else null
        val shown = if (editing != null) eq.effective(editing) else eq.global()
        val pending = ctx.files.count { !gains.has(it.uri) }
        return SoundUi(
            scope = if (editing != null) EqScope.Track else EqScope.Global,
            trackAvailable = book != null,
            trackTitle = book?.let { library.metaOf(it.base).title.ifEmpty { it.base.name } } ?: "",
            eq = shown,
            ownEq = editing != null && eq.track(editing) != null,
            boost = pick.boost,
            normOn = pick.normOn,
            norm = norm,
            pending = pending,
            totalFiles = ctx.files.size,
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

    override fun onBoost(percent: Int) = prefs.setBoost(percent)

    override fun onNormOn(on: Boolean) = prefs.setNormOn(on)

    override fun onNormStart() = normalizer.start()

    override fun onNormStop() = normalizer.stop()
}
