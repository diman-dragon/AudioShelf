package com.audioshelf.app

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val MAX_QUEUE = 20_000
private const val DEFAULT_WAIT_MS = 15_000L
private val DAY_FILE = Regex("audioshelf-\\d{4}-\\d{2}-\\d{2}\\.log")

/**
 * The log on disk, one file per day: `audioshelf-2026-11-03.log` for the 3rd, `audioshelf-2026-11-04.log`
 * for the 4th. A line goes to the file of the day it was logged; the file of a new day starts at midnight.
 * The last [keepDays] days are kept, older files are deleted. A day's file stops growing at [maxDayBytes]
 * (it says so once), so one noisy day cannot fill the phone.
 *
 * Callers only queue a line; one background thread does the I/O, so logging never waits for the disk.
 * If the disk stalls and the queue fills up, new lines are dropped rather than the app slowed down.
 */
class LogFile(
    private val dir: File,
    private val keepDays: Int = 7,
    private val maxDayBytes: Long = 30_000_000L,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val queue = LinkedBlockingQueue<Runnable>(MAX_QUEUE)
    private val worker = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, queue,
        ThreadFactory { r -> Thread(r, "audioshelf-log").apply { isDaemon = true } },
    )
    private val droppedLines = AtomicLong()
    private val overflowReported = AtomicBoolean()
    private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)
    }
    private val dayFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    }
    private var writer: BufferedWriter? = null // worker thread only
    private var openDay: String? = null // worker thread only
    private var size = 0L // worker thread only
    private var full = false // worker thread only

    fun append(level: Int, tag: String, message: String) {
        val at = Date(clock())
        val day = dayFormat.get()!!.format(at)
        val line = timeFormat.get()!!.format(at) + " " + letter(level) + "/" + tag +
            " [" + Thread.currentThread().name + "] " + message + "\n"
        val dropped = droppedLines.getAndSet(0)
        val completeLine = if (dropped > 0) "--- log queue dropped $dropped line(s) under load ---\n$line" else line
        try {
            worker.execute { write(day, completeLine) }
            overflowReported.set(false)
        } catch (e: RejectedExecutionException) {
            droppedLines.addAndGet(dropped + 1)
            if (overflowReported.compareAndSet(false, true)) report("log queue is full; dropping log lines", e)
        }
    }

    /** Waits until everything queued so far is on disk. */
    fun flush(timeoutMs: Long = DEFAULT_WAIT_MS) {
        onWorker(timeoutMs) { writer?.flush() }
    }

    /** The day files that exist, oldest first. */
    fun days(): List<String> = onWorker(DEFAULT_WAIT_MS) {
        writer?.flush()
        dayFiles().map { it.name.removePrefix("audioshelf-").removeSuffix(".log") }
    } ?: emptyList()

    /** Writes a zip with [header] as `device.txt` and one `audioshelf-<day>.log` per day to [target]. */
    fun export(target: File, header: String): Boolean = onWorker(DEFAULT_WAIT_MS) {
        writer?.flush()
        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("device.txt"))
            zip.write(header.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (file in dayFiles()) {
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        true
    } == true

    fun clear() {
        onWorker(DEFAULT_WAIT_MS) {
            writer?.close()
            writer = null
            openDay = null
            dayFiles().forEach { it.delete() }
            size = 0L
            full = false
        }
    }

    private fun dayFiles(): List<File> =
        (dir.listFiles() ?: emptyArray()).filter { DAY_FILE.matches(it.name) }.sortedBy { it.name }

    private fun write(day: String, line: String) {
        try {
            if (day != openDay) switchTo(day)
            val w = writer ?: return
            if (full) return
            val lineBytes = line.toByteArray(Charsets.UTF_8).size
            if (size + lineBytes > maxDayBytes) {
                full = true
                val limitMessage = "--- this day's log reached the size limit, the rest of the day is not recorded ---\n"
                w.write(limitMessage)
                size += limitMessage.toByteArray(Charsets.UTF_8).size
                w.flush()
                return
            }
            w.write(line)
            size += lineBytes
            if (queue.isEmpty()) w.flush()
        } catch (e: Exception) {
            report("could not write the app log", e)
            try {
                writer?.close()
            } catch (closeError: Exception) {
                report("could not close the app log after a write failure", closeError)
            }
            writer = null
            openDay = null
        }
    }

    private fun switchTo(day: String) {
        try {
            writer?.close()
        } catch (e: Exception) {
            report("could not close the previous app log", e)
        }
        writer = null
        openDay = day
        dir.mkdirs()
        val file = File(dir, "audioshelf-$day.log")
        size = file.length()
        full = size >= maxDayBytes
        writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8))
        dayFiles().dropLast(keepDays).forEach { it.delete() }
    }

    private fun <T> onWorker(timeoutMs: Long, task: () -> T): T? {
        val job = FutureTask(Callable { task() })
        try {
            worker.execute(job)
        } catch (e: RejectedExecutionException) {
            report("could not queue a log-file operation", e)
            return null
        }
        return try {
            job.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            job.cancel(false)
            report("log-file operation timed out after ${timeoutMs}ms", e)
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            report("interrupted while waiting for a log-file operation", e)
            null
        } catch (e: ExecutionException) {
            report("log-file operation failed", e.cause ?: e)
            null
        }
    }

    private fun report(message: String, error: Throwable) {
        try {
            android.util.Log.e("AudioShelf/LogFile", message, error)
        } catch (_: RuntimeException) {
            System.err.println("AudioShelf/LogFile: $message: $error")
        }
    }

    private fun letter(level: Int) = when (level) {
        3 -> 'D'
        4 -> 'I'
        5 -> 'W'
        else -> 'E'
    }
}
