package com.audioshelf.app.diag

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.audioshelf.app.AppLog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "LogShare"

/** Packs the log, one file per day, with a header about the phone into one zip for the system share sheet. */
class LogShare(private val app: Application, private val header: () -> String) {
    /** The shareable file, or null when there is no log or it could not be written. */
    suspend fun prepare(): Uri? = withContext(Dispatchers.IO) {
        val log = AppLog.file ?: return@withContext null
        try {
            val target = File(File(app.cacheDir, "logs"), "audioshelf-logs.zip")
            if (!log.export(target, header())) return@withContext null
            AppLog.i(TAG, "prepared ${target.length()} bytes for sharing")
            FileProvider.getUriForFile(app, app.packageName + ".logs", target)
        } catch (e: Exception) {
            AppLog.e(TAG, "could not prepare the log", e)
            null
        }
    }

    fun chooser(uri: Uri, title: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AudioShelf log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title)
    }

    fun clear() {
        AppLog.file?.clear()
        runCatching { File(File(app.cacheDir, "logs"), "audioshelf-logs.zip").delete() }
        AppLog.i(TAG, "log cleared by the user")
    }
}
