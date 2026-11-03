package com.audioshelf.app

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

private const val PREFIX = "AudioShelf/"
private const val DEBUG = 3
private const val INFO = 4
private const val WARN = 5
private const val ERROR = 6

/**
 * The one place the app logs through. Every line goes to logcat under `AudioShelf/<tag>` and, once
 * [attach] has been called, to the log file that Settings can share. Tests swap [sink] to read what
 * was logged; a failing sink never breaks the caller.
 */
object AppLog {
    fun interface Sink {
        fun write(level: Int, tag: String, message: String)
    }

    private val logcat = Sink { level, tag, message -> Log.println(level, PREFIX + tag, message) }

    @Volatile
    var sink: Sink = logcat

    /** The file the lines are also written to, or null before [attach]. */
    @Volatile
    var file: LogFile? = null
        private set

    fun attach(target: LogFile) {
        file = target
        sink = Sink { level, tag, message ->
            logcat.write(level, tag, message)
            target.append(level, tag, message)
        }
    }

    fun d(tag: String, message: String) = emit(DEBUG, tag, message)

    fun i(tag: String, message: String) = emit(INFO, tag, message)

    fun w(tag: String, message: String, error: Throwable? = null) = emit(WARN, tag, withError(message, error))

    fun e(tag: String, message: String, error: Throwable? = null) = emit(ERROR, tag, withError(message, error))

    private fun withError(message: String, error: Throwable?): String {
        if (error == null) return message
        val trace = StringWriter()
        error.printStackTrace(PrintWriter(trace))
        return message + "\n" + trace.toString().trimEnd()
    }

    private fun emit(level: Int, tag: String, message: String) {
        try {
            sink.write(level, tag, message)
        } catch (_: Throwable) {
            // Plain JUnit has no android.util.Log; logging must never be the reason something fails.
        }
    }
}
