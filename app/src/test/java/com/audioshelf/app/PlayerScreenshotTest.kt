package com.audioshelf.app

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.onRoot
import com.audioshelf.app.data.Folder
import com.audioshelf.app.data.LibView
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.BookRow
import com.audioshelf.app.library.LibFile
import com.audioshelf.app.library.LibStatus
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.LibraryUi
import com.audioshelf.app.library.Meta
import com.audioshelf.app.library.NoCovers
import com.audioshelf.app.library.ProgressState
import com.audioshelf.app.library.ScanProgress
import com.audioshelf.app.player.FakeSession
import com.audioshelf.app.player.Kind
import com.audioshelf.app.ui.library.LibraryScreen
import com.audioshelf.app.sound.Eq
import com.audioshelf.app.sound.EqScope
import com.audioshelf.app.sound.NormState
import com.audioshelf.app.sound.NormStatus
import com.audioshelf.app.sound.SoundActions
import com.audioshelf.app.sound.SoundUi
import com.audioshelf.app.ui.settings.SettingsActions
import com.audioshelf.app.ui.sound.SoundScreen
import com.audioshelf.app.ui.settings.SettingsScreen
import com.audioshelf.app.ui.settings.SettingsUi
import com.audioshelf.app.player.PlayerActions
import com.audioshelf.app.player.PlayerUi
import com.audioshelf.app.player.SAMPLE_ALBUM
import com.audioshelf.app.player.SAMPLE_BOOK
import com.audioshelf.app.player.SleepChoice
import com.audioshelf.app.ui.AppFrame
import com.audioshelf.app.ui.Screen
import com.audioshelf.app.ui.components.SwipeGuard
import com.audioshelf.app.ui.player.PlayerScreen
import com.audioshelf.app.ui.screens.ComingSoonScreen
import com.audioshelf.app.ui.theme.AudioShelfTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ru-w390dp-h800dp-xhdpi")
class PlayerScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun reference() = FakeSession(SAMPLE_BOOK, positionMs = 1500, playing = true).toUi()

    private fun capture(name: String, dark: Boolean, screen: Screen = Screen.Player, ui: PlayerUi = reference()) =
        captureContent(name, dark, screen) { current ->
            if (current == Screen.Player) PlayerScreen(ui, PlayerActions.None) else ComingSoonScreen(current)
        }

    private fun captureContent(name: String, dark: Boolean, screen: Screen, content: @Composable (Screen) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AudioShelfTheme(dark) {
                AppFrame(screen, 0, SwipeGuard(), {}, content = content)
            }
        }
        repeat(10) { rule.mainClock.advanceTimeByFrame() }
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun playerBookDark() {
        capture("player-book-dark", dark = true)
        rule.onNodeWithText("Книга: 0% · осталось ~39:58").assertExists()
    }

    @Test
    fun playerBookLight() = capture("player-book-light", dark = false)

    @Test
    fun playerAlbumDark() {
        val ui = FakeSession(SAMPLE_ALBUM, positionMs = 61_000, playing = false, sleep = SleepChoice.Min30, sleepLeftMs = 1_800_000).toUi()
        capture("player-album-dark", dark = true, ui = ui)
    }

    @Test
    fun playerEmptyLight() = capture("player-empty-light", dark = false, ui = PlayerUi())

    @Test
    fun comingSoonDark() = capture("library-dark", dark = true, screen = Screen.Library)

    private fun row(title: String, kind: Kind, state: ProgressState, percent: Int?, author: String = "", folder: Boolean = false): BookRow {
        val f = LibFile("content://t/$title", "$title.mp3", if (folder) "Dir" else "", "content://t", 52_000_000, 1_700_000_000_000)
        val files = if (folder) List(12) { f.copy(uri = f.uri + it) } else listOf(f)
        val book = Book(f.uri, folder, title, files, f.dirUri, listOf(f.dirUri), folder, f.size, f.modified)
        return BookRow(book, Meta(), kind, title, author, "", "1869", percent, state, 0)
    }

    private fun libraryUi(view: LibView = LibView.Rows): LibraryUi {
        val rows = listOf(
            row("Война и мир", Kind.Book, ProgressState.Started, 37, "Толстой", folder = true),
            row("Лунная соната", Kind.Album, ProgressState.New, null, "Бетховен"),
            row("Идиот", Kind.Book, ProgressState.Done, 100, "Достоевский"),
        )
        return LibraryUi(LibStatus.Ok, false, ScanProgress(), rows.size, rows, view = view)
    }

    @Test
    fun libraryListDark() = captureContent("library-list-dark", true, Screen.Library) {
        LibraryScreen(libraryUi(), LibraryActions.None, NoCovers)
    }

    @Test
    fun libraryCardsLight() = captureContent("library-cards-light", false, Screen.Library) {
        LibraryScreen(libraryUi(LibView.Cards), LibraryActions.None, NoCovers)
    }

    @Test
    fun libraryEmptyDark() = captureContent("library-empty-dark", true, Screen.Library) {
        LibraryScreen(LibraryUi(), LibraryActions.None, NoCovers)
    }

    @Test
    fun libraryScanningLight() = captureContent("library-scan-light", false, Screen.Library) {
        LibraryScreen(LibraryUi(LibStatus.Unscanned, true, ScanProgress(120, 14)), LibraryActions.None, NoCovers)
    }

    @Test
    fun settingsDark() = captureContent("settings-dark", true, Screen.Settings) {
        val folder = Folder("content://com.android.externalstorage.documents/tree/primary%3AAudiobooks", "Audiobooks")
        SettingsScreen(SettingsUi(folder, LibStatus.Ok, books = 12, files = 240, hidden = 2, version = "1.1.0", language = "ru"), SettingsActions.None)
    }

    @Test
    fun settingsLight() = captureContent("settings-light", false, Screen.Settings) {
        SettingsScreen(SettingsUi(theme = "light", version = "1.1.0"), SettingsActions.None)
    }

    private fun soundUi() = SoundUi(
        scope = EqScope.Global,
        trackAvailable = true,
        trackTitle = "Война и мир",
        eq = Eq.of("voice"),
        boost = 125,
        norm = NormState(NormStatus.Running, done = 14, total = 40, current = "01 Глава.mp3"),
        pending = 26,
        totalFiles = 40,
    )

    @Test
    fun soundDark() = captureContent("sound-dark", true, Screen.Sound) { SoundScreen(soundUi(), SoundActions.None) }

    @Test
    fun soundLight() = captureContent("sound-light", false, Screen.Sound) { SoundScreen(soundUi().copy(norm = NormState()), SoundActions.None) }
}
