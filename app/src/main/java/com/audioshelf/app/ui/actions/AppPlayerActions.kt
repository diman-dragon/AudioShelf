package com.audioshelf.app.ui.actions

import com.audioshelf.app.player.PlayerActions
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.Screen
import com.audioshelf.app.ui.components.OverlayHost
import com.audioshelf.app.ui.player.ChapterListDialog
import com.audioshelf.app.ui.player.MarksDialog

/** The player's own actions come from the view model; the three that open a dialog or change the screen are added here. */
class AppPlayerActions(
    private val player: PlayerViewModel,
    private val overlays: OverlayHost,
    private val goTo: (Screen) -> Unit,
) : PlayerActions by player {
    override fun onToLibrary() = goTo(Screen.Library)

    override fun onList() {
        val kind = player.kind() ?: return
        overlays.show { ChapterListDialog(player.chapterRows(), kind, player::playChapter) { overlays.close() } }
    }

    override fun onMarks() {
        val kind = player.kind() ?: return
        overlays.show { MarksDialog(player, kind, player::jumpTo) { overlays.close() } }
    }
}
