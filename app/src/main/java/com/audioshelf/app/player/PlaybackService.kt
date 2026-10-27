@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.audioshelf.app.MainActivity
import com.audioshelf.app.R
import com.audioshelf.app.container

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val shelf = container
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val built = MediaSession.Builder(this, shelf.engine.player)
            .setBitmapLoader(ShelfBitmapLoader(shelf.scope, shelf.covers, shelf.session::fileOf))
            .setCallback(SessionCallback(this, { resumeOf(shelf.session.resumeItems()) }, ::close, ::onClicks))
            .setSessionActivity(tap)
            .build()
        session = built
        val provider = DefaultMediaNotificationProvider.Builder(this).build()
        provider.setSmallIcon(R.drawable.ic_stat_player)
        setMediaNotificationProvider(provider)
        addSession(built)
    }

    private fun resumeOf(plan: ResumePlan) =
        MediaSession.MediaItemsWithStartPosition(plan.items, plan.index, plan.positionMs)

    private fun onClicks(count: Int) {
        val actions = container.session
        when (count) {
            1 -> actions.onToggle()
            2 -> actions.onForward()
            3 -> actions.onBack()
            else -> actions.onNext()
        }
    }

    private fun close() {
        container.engine.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        container.engine.flush()
        container.kv.flushNow()
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        container.kv.flushNow()
        container.engine.release()
        super.onDestroy()
    }
}
