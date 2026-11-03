package com.audioshelf.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.audioshelf.app.AppLog
import com.audioshelf.app.BuildConfig
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.actions.AppLibraryActions
import com.audioshelf.app.ui.actions.AppPlayerActions
import com.audioshelf.app.ui.actions.AppSettingsActions
import com.audioshelf.app.ui.actions.showStats
import com.audioshelf.app.ui.components.LocalOverlay
import com.audioshelf.app.ui.components.OverlayHost
import com.audioshelf.app.ui.components.OverlayLayer
import com.audioshelf.app.ui.components.message
import com.audioshelf.app.ui.components.rememberSwipeGuard
import com.audioshelf.app.ui.library.LibraryScreen
import com.audioshelf.app.ui.player.PlayerScreen
import com.audioshelf.app.ui.settings.SettingsScreen
import com.audioshelf.app.ui.settings.SettingsUi
import com.audioshelf.app.ui.sound.SoundScreen
import com.audioshelf.app.ui.theme.AudioShelfTheme

/** The shell: theme, language, which screen is shown, and the overlay layer. What each button does lives in [ui.actions]. */
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
            AppLog.i("Nav", "$screen -> $target")
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
                val backup = rememberBackupLaunchers(container.backup, overlays)
                val log = rememberLogActions(container.logShare, overlays)
                val playerActions = remember(player) { AppPlayerActions(player, overlays) { goTo(it) } }
                val libraryActions = remember(library) {
                    AppLibraryActions(library, prefs, container.queue, container.covers, player, overlays) { goTo(it) }
                }
                val settingsActions = remember(prefs, library, backup, log) {
                    AppSettingsActions(
                        prefs, library,
                        AppSettingsActions.Launchers(
                            pickFolder = { pickTree.launch(null) },
                            openStats = { overlays.showStats(container) },
                            backup = backup.save,
                            restore = backup.restore,
                            shareLog = log.share,
                            clearLog = log.clear,
                        ),
                    )
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
                                PlayerScreen(ui, playerActions, covers = container.covers)
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
    val autoNext by prefs.autoNext.collectAsState()
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
        autoNext = autoNext,
        theme = if (dark) "dark" else "light",
        language = language,
        version = BuildConfig.VERSION_NAME,
    )
}
