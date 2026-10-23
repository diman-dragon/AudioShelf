package com.audioshelf.app

import android.graphics.Bitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.onRoot
import com.audioshelf.app.player.FakeSession
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

    private fun capture(name: String, dark: Boolean, screen: Screen = Screen.Player, ui: PlayerUi = reference()) {
        rule.setContent {
            AudioShelfTheme(dark) {
                AppFrame(screen, 0, SwipeGuard(), {}) { current ->
                    if (current == Screen.Player) PlayerScreen(ui, PlayerActions.None) else ComingSoonScreen(current)
                }
            }
        }
        rule.waitForIdle()
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
}
