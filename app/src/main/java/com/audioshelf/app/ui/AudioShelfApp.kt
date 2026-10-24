package com.audioshelf.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.audioshelf.app.AppContainer
import com.audioshelf.app.BuildConfig
import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.LibView
import com.audioshelf.app.data.SortMode
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.Meta
import com.audioshelf.app.player.PlayerActions
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.components.OverlayHost
import com.audioshelf.app.ui.components.OverlayLayer
import com.audioshelf.app.ui.components.LocalOverlay
import com.audioshelf.app.ui.components.message
import com.audioshelf.app.ui.components.rememberSwipeGuard
import com.audioshelf.app.ui.library.LibraryScreen
import com.audioshelf.app.ui.player.ChapterListDialog
import com.audioshelf.app.ui.player.MarksDialog
import com.audioshelf.app.ui.player.PlayerScreen
import com.audioshelf.app.ui.sound.SoundScreen
import com.audioshelf.app.ui.settings.SettingsActions
import com.audioshelf.app.ui.settings.SettingsScreen
import com.audioshelf.app.ui.settings.SettingsUi
import com.audioshelf.app.ui.theme.AudioShelfTheme

@Composable
fun AudioShelfApp(container: AppContainer, player: PlayerViewModel) {
    val prefs = container.prefs
    val library = container.library
    val themePref by prefs.theme.collectAsState()
    val language by prefs.language.collectAsState()
    val dark = themePref?.let { it == "dark" } ?: isSystemInDarkTheme()
    var screen by rememberSaveable { mutableStateOf(Screen.Library) }
    var direction by remember { mutableIntStateOf(0) }
    val guard = rememberSwipeGuard()
    val overlays = remember { OverlayHost() }

    val navigate = { target: Screen ->
        if (target != screen) {
            direction = if (target.ordinal > screen.ordinal) 1 else -1
            screen = target
        }
    }
    val goTo by rememberUpdatedState(navigate)

    SystemBarsEffect(dark)
    LocalizedContent(language) {
        AudioShelfTheme(dark) {
            CompositionLocalProvider(LocalOverlay provides overlays) {
                val pickTree = rememberLauncherForActivityResult(PickTree()) { uri: Uri? -> if (uri != null) library.chooseFolder(uri) }
                val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
                    library.addFiles(uris)
                }
                val playerActions = remember(player) {
                    object : PlayerActions by player {
                        override fun onToLibrary() = goTo(Screen.Library)

                        override fun onList() {
                            val kind = player.kind() ?: return
                            overlays.show {
                                ChapterListDialog(player.chapterRows(), kind, player::playChapter) { overlays.close() }
                            }
                        }

                        override fun onMarks() {
                            val kind = player.kind() ?: return
                            overlays.show {
                                MarksDialog(player, kind, player::jumpTo) { overlays.close() }
                            }
                        }
                    }
                }
                val libraryActions = remember(library) {
                    object : LibraryActions {
                        override fun onPlay(book: Book) {
                            player.open(book)
                            goTo(Screen.Player)
                        }

                        override fun onRescan() = library.rescan()
                        override fun onStop() = library.cancelScan()
                        override fun onAdd() = pickFiles.launch(arrayOf("audio/*"))
                        override fun onKind(kind: KindFilter) = prefs.setLibKind(kind)
                        override fun onSort(sort: SortMode) = prefs.setLibSort(sort)
                        override fun onView(view: LibView) = prefs.setLibView(view)
                        override fun onQuery(query: String) = library.setQuery(query)
                        override fun onOpenSettings() = goTo(Screen.Settings)
                        override fun onEdit(book: Book, meta: Meta, baseName: String) = library.saveEdit(book, meta, baseName)
                        override fun onHide(book: Book) = library.hide(book)
                        override fun onDelete(book: Book) = library.remove(book)
                        override fun onOpenExternal(book: Book) = library.openExternal(book)
                    }
                }
                val settingsActions = remember(prefs, library) {
                    object : SettingsActions {
                        override fun onPickFolder() = pickTree.launch(null)
                        override fun onForgetFolder() = library.forgetFolder()
                        override fun onRestoreHidden() = library.restoreHidden()
                        override fun onSkipBack(seconds: Int) = prefs.setSkipBack(seconds)
                        override fun onSkipForward(seconds: Int) = prefs.setSkipForward(seconds)
                        override fun onOffload(on: Boolean) = prefs.setOffload(on)
                        override fun onTheme(id: String) = prefs.setTheme(id)
                        override fun onLanguage(id: String) = prefs.setLanguage(id)
                    }
                }

                LaunchedEffect(library) {
                    library.messages.collect { overlays.message(it) }
                }

                BackHandler(enabled = screen != Screen.Library) { navigate(Screen.Library) }

                Box(Modifier.fillMaxSize()) {
                    AppFrame(screen, direction, guard, navigate) { current ->
                        when (current) {
                            Screen.Library -> {
                                val ui by library.ui.collectAsState()
                                LibraryScreen(ui, libraryActions, container.covers)
                            }
                            Screen.Player -> {
                                val ui by player.ui.collectAsState()
                                PlayerScreen(ui, playerActions)
                            }
                            Screen.Sound -> {
                                val ui by container.sound.ui.collectAsState()
                                SoundScreen(ui, container.sound)
                            }
                            Screen.Settings -> {
                                val ui = rememberSettingsUi(container, dark)
                                SettingsScreen(ui, settingsActions)
                            }
                        }
                    }
                    OverlayLayer(overlays)
                }
            }
        }
    }
}

@Composable
private fun rememberSettingsUi(container: AppContainer, dark: Boolean): SettingsUi {
    val prefs = container.prefs
    val folder by container.folders.current.collectAsState()
    val library by container.library.ui.collectAsState()
    val hidden by container.library.hiddenCount.collectAsState()
    val files by container.library.fileCount.collectAsState()
    val skipBack by prefs.skipBack.collectAsState()
    val skipForward by prefs.skipForward.collectAsState()
    val offload by prefs.offload.collectAsState()
    val language by prefs.language.collectAsState()
    return SettingsUi(
        folder = folder,
        status = library.status,
        scanning = library.scanning,
        scanned = library.progress.files,
        books = library.total,
        files = files,
        hidden = hidden,
        skipBack = skipBack,
        skipForward = skipForward,
        offload = offload,
        theme = if (dark) "dark" else "light",
        language = language,
        version = BuildConfig.VERSION_NAME,
    )
}
