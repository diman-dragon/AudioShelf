package com.audioshelf.app.player

import android.app.Application
import android.widget.Toast
import com.audioshelf.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Tells the user, in one line, which file failed, why, and whether playback moved on. */
class PlaybackNotices(
    private val app: Application,
    private val scope: CoroutineScope,
    private val failures: Flow<PlaybackFailure>,
) {
    fun start() {
        scope.launch { failures.collect { show(it) } }
    }

    private fun show(f: PlaybackFailure) {
        val head = f.title?.takeIf { it.isNotBlank() }?.let { app.getString(R.string.err_play_named, it) } ?: app.getString(R.string.err_play)
        val tail = if (f.skipped) " " + app.getString(R.string.err_skipped) else ""
        Toast.makeText(app, "$head: ${app.getString(reasonOf(f.kind))}.$tail", Toast.LENGTH_LONG).show()
    }

    private fun reasonOf(kind: FailureKind): Int = when (kind) {
        FailureKind.FileMissing -> R.string.err_file_missing
        FailureKind.NoAccess -> R.string.err_no_access
        FailureKind.Corrupt -> R.string.err_corrupt
        FailureKind.Decoder -> R.string.err_decoder
        FailureKind.AudioOutput -> R.string.err_audio_output
        FailureKind.Other -> R.string.err_other
    }
}
