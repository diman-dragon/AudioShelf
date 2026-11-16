@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.audioshelf.app.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.audioshelf.app.AppLog

/**
 * Keeps the book on Bluetooth headphones while they are connected. Left to the system, a notification
 * sound can move the media to the phone's speaker for the length of the sound (seen in the logs: the A2DP
 * stream was suspended for 5 s under a notification while the book went on). A preferred output device
 * is the one thing an app can say about that. When the headphones are gone the preference is dropped and
 * the system picks the output as before; the "becoming noisy" pause is not affected.
 */
class HeadphonePin(context: Context, private val player: ExoPlayer) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var pinnedId = NONE

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = update()

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = update()
    }

    init {
        audio.registerAudioDeviceCallback(callback, handler)
        update()
    }

    private fun update() {
        val a2dp = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        val id = a2dp?.id ?: NONE
        if (id == pinnedId) return
        pinnedId = id
        player.setPreferredAudioDevice(a2dp)
        AppLog.i("HeadphonePin", if (a2dp != null) "the book is pinned to the Bluetooth headphones \"${a2dp.productName}\"" else "no Bluetooth headphones: output left to the system")
    }

    fun release() {
        audio.unregisterAudioDeviceCallback(callback)
        pinnedId = NONE
    }

    private companion object {
        const val NONE = -1
    }
}
