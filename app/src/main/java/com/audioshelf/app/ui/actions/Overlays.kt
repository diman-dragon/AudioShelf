package com.audioshelf.app.ui.actions

import com.audioshelf.app.AppContainer
import com.audioshelf.app.ui.components.OverlayHost
import com.audioshelf.app.ui.stats.StatsScreen

/** The statistics screen, as a full-screen overlay. */
fun OverlayHost.showStats(container: AppContainer) {
    show {
        StatsScreen(container.stats, container.library.allRows(), container.library::titleOf) { close() }
    }
}
