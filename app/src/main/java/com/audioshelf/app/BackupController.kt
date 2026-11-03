package com.audioshelf.app

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Process
import com.audioshelf.app.data.Backup
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.KvRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Backup"

/** Saving the data to a file and restoring it: the file I/O and the log, without any screen. */
class BackupController(private val context: Context, private val kv: Kv) {
    suspend fun save(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = Backup.rows(kv)
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("no output stream")
            stream.use { it.write(Backup.encode(rows).toByteArray(Charsets.UTF_8)) }
            AppLog.i(TAG, "saved ${rows.size} rows")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(TAG, "save failed", e)
            false
        }
    }

    /** The rows of the backup at [uri], or null when it cannot be read or is not an AudioShelf backup. */
    suspend fun read(uri: Uri): List<KvRow>? = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
            val rows = Backup.decode(text)
            if (rows == null) AppLog.w(TAG, "file is not an AudioShelf backup") else AppLog.i(TAG, "read ${rows.size} rows")
            rows
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(TAG, "read failed", e)
            null
        }
    }

    /** Replaces the stored data with [rows] and writes it to disk. The stores keep what they loaded at start, so the app has to be started again. */
    suspend fun restore(rows: List<KvRow>): Int = withContext(Dispatchers.IO) {
        val count = Backup.apply(kv, rows)
        AppLog.i(TAG, "restored $count rows")
        count
    }

    /** Ends the process so every store loads the restored data on the next start. */
    fun exit(activity: Activity?) {
        AppLog.i(TAG, "closing the app after a restore")
        activity?.finishAffinity()
        Process.killProcess(Process.myPid())
    }
}
