package com.audioshelf.app.ui.actions

import com.audioshelf.app.AppLog
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.ui.settings.SettingsActions

private const val TAG = "Setting"

/** Settings that write a preference or call the library go straight through; the ones that open a system picker or a screen come in as [launch]. */
class AppSettingsActions(
    private val prefs: Prefs,
    private val library: LibraryController,
    private val launch: Launchers,
) : SettingsActions {
    class Launchers(
        val pickFolder: () -> Unit,
        val openStats: () -> Unit,
        val backup: () -> Unit,
        val restore: () -> Unit,
        val shareLog: () -> Unit,
        val clearLog: () -> Unit,
    )

    override fun onPickFolder() {
        AppLog.i(TAG, "pick a folder")
        launch.pickFolder()
    }

    override fun onForgetFolder() {
        AppLog.i(TAG, "forget the folder")
        library.forgetFolder()
    }

    override fun onRestoreHidden() {
        AppLog.i(TAG, "restore hidden books")
        library.restoreHidden()
    }

    override fun onSkipBack(seconds: Int) {
        AppLog.i(TAG, "skip back: $seconds s")
        prefs.setSkipBack(seconds)
    }

    override fun onSkipForward(seconds: Int) {
        AppLog.i(TAG, "skip forward: $seconds s")
        prefs.setSkipForward(seconds)
    }

    override fun onOffload(on: Boolean) {
        AppLog.i(TAG, "energy saving (audio offload): $on")
        prefs.setOffload(on)
    }

    override fun onAutoNext(on: Boolean) {
        AppLog.i(TAG, "auto-next: $on")
        prefs.setAutoNext(on)
    }

    override fun onDuckNotifications(on: Boolean) {
        AppLog.i(TAG, "lower the book for notifications: $on")
        prefs.setDuckNotifications(on)
    }

    override fun onRewindAfterNotification(on: Boolean) {
        AppLog.i(TAG, "rewind 10 s after a notification: $on")
        prefs.setRewindAfterNotification(on)
    }

    override fun onOpenStats() {
        AppLog.i(TAG, "open statistics")
        launch.openStats()
    }

    override fun onBackup() {
        AppLog.i(TAG, "backup: save")
        launch.backup()
    }

    override fun onRestore() {
        AppLog.i(TAG, "backup: restore")
        launch.restore()
    }

    override fun onShareLog() {
        AppLog.i(TAG, "share the log")
        launch.shareLog()
    }

    override fun onClearLog() {
        launch.clearLog()
    }

    override fun onTheme(id: String) {
        AppLog.i(TAG, "theme: $id")
        prefs.setTheme(id)
    }

    override fun onLanguage(id: String) {
        AppLog.i(TAG, "language: $id")
        prefs.setLanguage(id)
    }
}
