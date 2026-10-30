package com.audioshelf.app.player

/** What went wrong, reduced to what the recovery decision needs. */
enum class FailureKind { FileMissing, NoAccess, Corrupt, Decoder, AudioOutput, Other }

/** Handed to the UI: which file failed and whether playback moved on to the next one. */
data class PlaybackFailure(val kind: FailureKind, val title: String?, val skipped: Boolean)

enum class Recovery { Retry, SkipFile, GiveUp }

// Media3 PlaybackException codes are grouped by thousands: 2xxx I/O, 3xxx parsing, 4xxx decoder, 5xxx audio sink.
private const val IO_FILE_NOT_FOUND = 2005
private const val IO_NO_PERMISSION = 2006
private const val DECODING_FORMAT_EXCEEDS_CAPABILITIES = 4004
private const val DECODING_FORMAT_UNSUPPORTED = 4005

fun failureKindOf(errorCode: Int): FailureKind = when (errorCode) {
    IO_FILE_NOT_FOUND -> FailureKind.FileMissing
    IO_NO_PERMISSION -> FailureKind.NoAccess
    DECODING_FORMAT_EXCEEDS_CAPABILITIES, DECODING_FORMAT_UNSUPPORTED -> FailureKind.Corrupt
    in 3000..3999 -> FailureKind.Corrupt
    in 4000..4999 -> FailureKind.Decoder
    in 5000..5999 -> FailureKind.AudioOutput
    else -> FailureKind.Other
}

/**
 * What to do after the player failed. A file that is gone or unreadable is not worth retrying, so
 * playback moves on to the next file; a hiccup in the decoder or the audio output gets one more
 * try first. When nothing is left to try the player stops and the user is told.
 */
class PlayerErrorPolicy(private val maxRetries: Int = 1) {
    fun decide(kind: FailureKind, attempt: Int, hasNextFile: Boolean): Recovery = when (kind) {
        FailureKind.FileMissing, FailureKind.NoAccess, FailureKind.Corrupt ->
            if (hasNextFile) Recovery.SkipFile else Recovery.GiveUp
        FailureKind.AudioOutput ->
            if (attempt < maxRetries) Recovery.Retry else Recovery.GiveUp
        FailureKind.Decoder, FailureKind.Other -> when {
            attempt < maxRetries -> Recovery.Retry
            hasNextFile -> Recovery.SkipFile
            else -> Recovery.GiveUp
        }
    }
}
