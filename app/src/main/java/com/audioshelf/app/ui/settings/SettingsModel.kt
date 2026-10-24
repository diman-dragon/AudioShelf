package com.audioshelf.app.ui.settings

import com.audioshelf.app.data.Folder
import com.audioshelf.app.library.LibStatus

data class SettingsUi(
    val folder: Folder? = null,
    val status: LibStatus = LibStatus.NoFolder,
    val scanning: Boolean = false,
    val scanned: Int = 0,
    val books: Int = 0,
    val files: Int = 0,
    val hidden: Int = 0,
    val skipBack: Int = 15,
    val skipForward: Int = 30,
    val offload: Boolean = false,
    val theme: String = "dark",
    val language: String = "auto",
    val version: String = "",
)

interface SettingsActions {
    fun onPickFolder()
    fun onForgetFolder()
    fun onRestoreHidden()
    fun onSkipBack(seconds: Int)
    fun onSkipForward(seconds: Int)
    fun onOffload(on: Boolean)
    fun onTheme(id: String)
    fun onLanguage(id: String)

    companion object {
        val None = object : SettingsActions {
            override fun onPickFolder() {}
            override fun onForgetFolder() {}
            override fun onRestoreHidden() {}
            override fun onSkipBack(seconds: Int) {}
            override fun onSkipForward(seconds: Int) {}
            override fun onOffload(on: Boolean) {}
            override fun onTheme(id: String) {}
            override fun onLanguage(id: String) {}
        }
    }
}

fun folderPath(uri: String, name: String, internal: String, card: String): String {
    val match = Regex("externalstorage\\.documents/tree/([^/?#]+)").find(uri) ?: return name
    val decoded = java.net.URLDecoder.decode(match.groupValues[1], "UTF-8")
    val volume = decoded.substringBefore(':')
    val rest = decoded.substringAfter(':', "")
    val root = if (volume == "primary") internal else card
    return (listOf(root) + rest.split('/').filter { it.isNotEmpty() }).joinToString(" › ")
}
