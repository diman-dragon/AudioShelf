@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.audioshelf.app.diag

import android.app.Application
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.audioshelf.app.AppLog

private const val TAG = "AudioTrace"
private const val STEP_MS = 250L
private const val SAME_EVERY_MS = 1_000L
private const val QUIET_MS = 3_000L
private const val MAX_WINDOW_MS = 60_000L
private const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
private const val STREAM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
private const val EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
private const val EXTRA_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
private const val EXTRA_PREV_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"

// Usages other than plain media: a notification, an alarm, a ringtone, a call, an assistant, navigation.
private val OTHER_USAGES = setOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16)

/**
 * The "everything about where the sound goes" log. Whenever something that can move or cut the book's
 * sound happens (the focus is lost, the Bluetooth music stream is suspended or resumed while the book
 * plays, another app starts a notification, call, alarm or navigation stream) a window opens. In the
 * window, four times a second, one line says: what the book itself is doing (playing, position, volume,
 * session), the devices the system would use for media, for a notification and for a notification event,
 * every audio player on the phone with the device it is really routed to (the device id is read from the
 * system's own description of the player), the volumes and mutes, the mode, the ringer, the call link,
 * the Bluetooth stream state. A line is written when something changed, and one a second otherwise.
 * The window closes 3 s after the last trigger and ends with a summary: every event with its time since the
 * start, and what changed in the routes. All times are milliseconds from the start of the window, so the
 * lines of the focus, the Bluetooth and the audio-streams logs can be set next to each other.
 */
class AudioTrace(
    private val app: Application,
    private val player: ExoPlayer,
    private val system: SystemWatch,
) {
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val events = ArrayList<String>()
    private val routeChanges = ArrayList<String>()
    private var running = false
    private var startedAt = 0L
    private var lastTrigger = 0L
    private var lastLine = ""
    private var lastLineAt = 0L
    private var lastRoutes = ""
    private var windows = 0
    private var a2dp = "?"
    private var sco = "?"
    private var othersActive = false
    private var released = false

    private val tick = object : Runnable {
        override fun run() {
            if (released) return
            val now = SystemClock.elapsedRealtime()
            sample(now)
            if ((now - lastTrigger > QUIET_MS && !othersActive) || now - startedAt > MAX_WINDOW_MS) end(now) else handler.postDelayed(this, STEP_MS)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                handle(intent)
            } catch (e: Exception) {
                AppLog.w(TAG, "cannot read ${intent.action}", e)
            }
        }
    }

    private val playbackCallback = if (Build.VERSION.SDK_INT >= 26) {
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                val others = configs.filter { it.audioAttributes.usage in OTHER_USAGES && !ours(it) }
                othersActive = others.isNotEmpty()
                if (others.isNotEmpty() && player.isPlaying) {
                    begin("another stream starts while the book plays: " + others.joinToString { system.usage(it.audioAttributes.usage) })
                }
            }
        }
    } else {
        null
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(VOLUME_CHANGED)
            addAction(STREAM_MUTE_CHANGED)
        }
        // Exported on purpose: the Bluetooth broadcasts come from the Bluetooth app (see BluetoothWatch).
        try {
            ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            AppLog.w(TAG, "cannot listen to the audio and Bluetooth events", e)
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) audio.registerAudioPlaybackCallback(playbackCallback!!, handler)
        } catch (e: Exception) {
            AppLog.w(TAG, "cannot watch the audio players", e)
        }
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                audio.addOnModeChangedListener(ContextCompat.getMainExecutor(app)) { mode -> note("audio mode -> ${modeName(mode)}") }
                audio.addOnCommunicationDeviceChangedListener(ContextCompat.getMainExecutor(app)) { d ->
                    note("communication device -> ${d?.let { system.deviceLabel(it) } ?: "none"}")
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "cannot watch the mode and the communication device", e)
        }
        refreshA2dp()
    }

    /** Opens a window, or says that another trigger came in the one that is open. */
    fun begin(why: String) {
        if (released) return
        val now = SystemClock.elapsedRealtime()
        lastTrigger = now
        if (running) {
            note("trigger: $why")
            return
        }
        running = true
        windows++
        startedAt = now
        events.clear()
        routeChanges.clear()
        lastLine = ""
        lastRoutes = ""
        AppLog.i(TAG, "=== window #$windows opens: $why")
        refreshA2dp()
        AppLog.i(TAG, "+0 ms outputs of the phone: ${outputsWithIds()}")
        AppLog.i(TAG, "+0 ms players: ${playersRaw()}")
        sample(now, force = true)
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, STEP_MS)
    }

    fun release() {
        released = true
        handler.removeCallbacks(tick)
        runCatching { app.unregisterReceiver(receiver) }
        if (Build.VERSION.SDK_INT >= 26) runCatching { audio.unregisterAudioPlaybackCallback(playbackCallback!!) }
    }

    private fun end(now: Long) {
        running = false
        AppLog.i(TAG, "+${now - startedAt} ms window #$windows closes. events: ${events.joinToString("; ").ifEmpty { "none" }}")
        AppLog.i(TAG, "route changes in the window: ${routeChanges.joinToString("; ").ifEmpty { "none" }}")
    }

    /** One event with its time: written to the log and kept for the summary. */
    private fun note(text: String) {
        val t = if (running) SystemClock.elapsedRealtime() - startedAt else -1L
        val line = if (t >= 0) "+$t ms $text" else "(no window) $text"
        AppLog.i(TAG, line)
        if (running) events.add("+$t $text")
    }

    private fun handle(intent: Intent) {
        when (val action = intent.action) {
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val prev = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
                a2dp = if (state == BluetoothA2dp.STATE_PLAYING) "PLAYING" else "suspended"
                note("A2DP music stream ${if (state == BluetoothA2dp.STATE_PLAYING) "PLAYING" else "NOT PLAYING (suspended)"} (was $prev), the book ${if (player.isPlaying) "plays" else "does not play"}")
                if (player.isPlaying || running) begin("A2DP stream is $a2dp")
            }
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> note("A2DP connection ${intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)} (was ${intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)})")
            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                sco = when (state) {
                    BluetoothHeadset.STATE_AUDIO_CONNECTED -> "connected"
                    BluetoothHeadset.STATE_AUDIO_CONNECTING -> "connecting"
                    else -> "off"
                }
                note("headset call audio (SCO) $sco")
            }
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> note("headset profile connection ${intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)}")
            AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> note("SCO audio state ${intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)}")
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> note("audio becoming noisy")
            Intent.ACTION_HEADSET_PLUG -> note("wired headset plug state ${intent.getIntExtra("state", -1)}")
            BluetoothDevice.ACTION_ACL_CONNECTED -> note("Bluetooth link up")
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> note("Bluetooth link down")
            VOLUME_CHANGED -> note("volume of ${streamName(intent.getIntExtra(EXTRA_STREAM_TYPE, -1))}: ${intent.getIntExtra(EXTRA_PREV_VALUE, -1)} -> ${intent.getIntExtra(EXTRA_STREAM_VALUE, -1)}")
            STREAM_MUTE_CHANGED -> note("mute of ${streamName(intent.getIntExtra(EXTRA_STREAM_TYPE, -1))} changed")
            else -> note("event $action")
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun refreshA2dp() {
        val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return
        try {
            adapter.getProfileProxy(
                app,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        try {
                            if (proxy is BluetoothA2dp) a2dp = if (proxy.connectedDevices.any { proxy.isA2dpPlaying(it) }) "PLAYING" else "suspended/idle"
                        } catch (_: SecurityException) {
                            a2dp = "no permission"
                        } finally {
                            adapter.closeProfileProxy(profile, proxy)
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {}
                },
                BluetoothProfile.A2DP,
            )
        } catch (_: Exception) {
        }
    }

    private fun sample(now: Long, force: Boolean = false) {
        val t = now - startedAt
        val line = snapshot()
        if (!force && line == lastLine && now - lastLineAt < SAME_EVERY_MS) return
        AppLog.i(TAG, "+$t ms ${if (line == lastLine) "(same)" else line}")
        lastLine = line
        lastLineAt = now
    }

    private fun snapshot(): String {
        val sb = StringBuilder()
        val attrs = player.audioAttributes
        sb.append("book[play=${player.isPlaying} wanted=${player.playWhenReady} state=${player.playbackState} pos=${player.currentPosition / 1000}s ")
        sb.append("vol=${"%.2f".format(player.volume)} session=${player.audioSessionId} usage=${system.usage(attrs.usage)} content=${system.content(attrs.contentType)}] ")
        sb.append("a2dp=$a2dp sco=$sco ")
        val routes = routes()
        sb.append(routes).append(' ')
        if (routes != lastRoutes) {
            if (lastRoutes.isNotEmpty()) routeChanges.add("+${SystemClock.elapsedRealtime() - startedAt} ${diff(lastRoutes, routes)}")
            lastRoutes = routes
        }
        sb.append("players[${playersMapped()}] ")
        sb.append("vol[${volumes()}] ")
        @Suppress("DEPRECATION")
        sb.append("mode=${modeName(audio.mode)} ringer=${audio.ringerMode} music-active=${audio.isMusicActive} speakerphone=${audio.isSpeakerphoneOn}")
        if (Build.VERSION.SDK_INT >= 31) sb.append(" comm-device=${audio.communicationDevice?.let { system.deviceLabel(it) } ?: "none"}")
        return sb.toString()
    }

    private fun diff(before: String, after: String) = "[$before] -> [$after]"

    /** The devices the system would use for media, a notification and a notification event right now (Android 13+). */
    private fun routes(): String {
        if (Build.VERSION.SDK_INT < 33) return "routes[outputs=${outputsWithIds()}]"
        fun devices(usage: Int, content: Int): String = try {
            audio.getAudioDevicesForAttributes(AudioAttributes.Builder().setUsage(usage).setContentType(content).build())
                .joinToString("+") { system.deviceLabel(it) }.ifEmpty { "none" }
        } catch (e: Exception) {
            "error"
        }
        return "routes[media(speech)=${devices(AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_SPEECH)} " +
            "media(music)=${devices(AudioAttributes.USAGE_MEDIA, AudioAttributes.CONTENT_TYPE_MUSIC)} " +
            "notification=${devices(AudioAttributes.USAGE_NOTIFICATION, AudioAttributes.CONTENT_TYPE_SONIFICATION)} " +
            "event=${devices(AudioAttributes.USAGE_NOTIFICATION_EVENT, AudioAttributes.CONTENT_TYPE_SONIFICATION)}]"
    }

    private fun outputsWithIds(): String =
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString(", ") { system.deviceLabel(it) }.ifEmpty { "none" }

    private fun configs(): List<AudioPlaybackConfiguration> =
        if (Build.VERSION.SDK_INT >= 26) audio.activePlaybackConfigurations else emptyList()

    /** The system's own text for each player, as it is, for the cases where the parsing below misses something. */
    private fun playersRaw(): String =
        configs().joinToString(" | ") { it.toString().replace('\n', ' ').take(260) }.ifEmpty { "none" }

    private fun ours(c: AudioPlaybackConfiguration): Boolean = Regex("""u/pid:(\d+)/""").find(c.toString())?.groupValues?.get(1)?.toIntOrNull() == Process.myUid()

    private fun playersMapped(): String {
        val sinks = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return configs().joinToString("; ") { c ->
            val text = c.toString()
            val a = c.audioAttributes
            val owner = Regex("""u/pid:(\d+)/""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            val who = when {
                owner == null -> "?"
                owner == Process.myUid() -> "THE BOOK"
                else -> runCatching { app.packageManager.getNameForUid(owner) }.getOrNull() ?: "uid $owner"
            }
            val deviceId = Regex("""deviceId:(-?\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            val device = deviceId?.let { id -> sinks.firstOrNull { it.id == id }?.let { system.deviceLabel(it) } ?: "device $id" } ?: "device ?"
            val state = Regex("""state:(\w+)""").find(text)?.groupValues?.get(1) ?: "?"
            "$who ${system.usage(a.usage)}/${system.content(a.contentType)} flags=${a.flags} $state -> $device"
        }.ifEmpty { "none" }
    }

    private fun volumes(): String = listOf(
        "music" to AudioManager.STREAM_MUSIC,
        "notification" to AudioManager.STREAM_NOTIFICATION,
        "ring" to AudioManager.STREAM_RING,
        "alarm" to AudioManager.STREAM_ALARM,
        "call" to AudioManager.STREAM_VOICE_CALL,
    ).joinToString(" ") { (name, type) ->
        "$name=${audio.getStreamVolume(type)}/${audio.getStreamMaxVolume(type)}${if (audio.isStreamMute(type)) "(muted)" else ""}"
    }

    private fun streamName(type: Int) = when (type) {
        AudioManager.STREAM_VOICE_CALL -> "call"
        AudioManager.STREAM_SYSTEM -> "system"
        AudioManager.STREAM_RING -> "ring"
        AudioManager.STREAM_MUSIC -> "music"
        AudioManager.STREAM_ALARM -> "alarm"
        AudioManager.STREAM_NOTIFICATION -> "notification"
        AudioManager.STREAM_DTMF -> "dtmf"
        10 -> "accessibility"
        else -> "stream $type"
    }

    private fun modeName(mode: Int) = when (mode) {
        AudioManager.MODE_NORMAL -> "normal"
        AudioManager.MODE_RINGTONE -> "ringtone"
        AudioManager.MODE_IN_CALL -> "in-call"
        AudioManager.MODE_IN_COMMUNICATION -> "in-communication"
        else -> "mode $mode"
    }
}
