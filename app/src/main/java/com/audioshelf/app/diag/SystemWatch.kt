package com.audioshelf.app.diag

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.audioshelf.app.AppLog
import com.audioshelf.app.BuildConfig

private const val TAG = "System"
private const val LIGHT_IDLE = "android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED"

/**
 * Everything around the app that can take CPU time or audio away from it, written to the log with its
 * time: doze, battery saver, the app's standby bucket, heat, the network, the audio devices, and the
 * audio streams other apps start (a "silent" notification may still play a stream, and the system
 * ducks or pauses media around it).
 */
class SystemWatch(private val app: Application) {
    private val power = app.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var lastStreams = ""
    private var lastNetwork = ""

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AppLog.i(TAG, describe(intent))
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(LIGHT_IDLE)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        guarded("receivers") { ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED) }
        guarded("network") { watchNetwork() }
        guarded("thermal") { watchThermal() }
        guarded("audio streams") { watchStreams() }
        guarded("audio devices") { watchDevices() }
        AppLog.i(TAG, "watching\n" + describe())
    }

    /** One line with the state that decides whether a background app gets CPU time. */
    fun powerLine(): String =
        "power[doze=${power.isDeviceIdleMode}${lightIdle()?.let { " light-idle=$it" }.orEmpty()} saver=${power.isPowerSaveMode} " +
            "exempt=${exempt()} bucket=${bucket()} thermal=${thermal()}]"

    /** Light doze (the screen is off, the phone still, the network and jobs are held back); known from Android 13. */
    private fun lightIdle(): Boolean? = if (Build.VERSION.SDK_INT >= 33) power.isDeviceLightIdleMode else null

    /** Where the media of this app comes out: Bluetooth music first, then wires, then the phone's own speaker. */
    fun mediaOutput(): String {
        val sinks = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val order = listOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        )
        val used = order.firstNotNullOfOrNull { type -> sinks.firstOrNull { it.type == type } }
        return used?.let { deviceName(it) } ?: "unknown"
    }

    /** One line with battery and memory, the other things that make a phone slow down. */
    fun resourceLine(): String {
        val battery = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val memory = ActivityManager.MemoryInfo().also { (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) }
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) shr 20
        return "device[battery=${battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}% charging=${battery.isCharging} " +
            "heap=${usedMb}/${runtime.maxMemory() shr 20}MB free-ram=${memory.availMem shr 20}MB low-memory=${memory.lowMemory}]"
    }

    /** One line with where the sound goes: call/music mode, Bluetooth links, the outputs and the one used for calls. */
    @Suppress("DEPRECATION")
    fun routeLine(): String {
        val mode = when (audio.mode) {
            AudioManager.MODE_NORMAL -> "normal"
            AudioManager.MODE_RINGTONE -> "ringtone"
            AudioManager.MODE_IN_CALL -> "in call"
            AudioManager.MODE_IN_COMMUNICATION -> "in communication"
            else -> "mode ${audio.mode}"
        }
        val communication = if (Build.VERSION.SDK_INT >= 31) " communication-device=${audio.communicationDevice?.let { deviceName(it) } ?: "none"}" else ""
        return "route[mode=$mode bluetooth-sco=${audio.isBluetoothScoOn} bluetooth-a2dp=${audio.isBluetoothA2dpOn} outputs=${outputs()}$communication]"
    }

    /** The header of a shared log: what the phone is and how it is set up. */
    fun describe(): String = buildString {
        appendLine("AudioShelf ${BuildConfig.VERSION_NAME}")
        appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE}), Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine(powerLine())
        appendLine("background restricted: ${restricted()}")
        appendLine("ringer: ${ringer(audio.ringerMode)}")
        appendLine(routeLine())
        appendLine(
            "audio: output ${audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)} Hz, " +
                "${audio.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)} frames per buffer, outputs: ${outputs()}",
        )
    }.trimEnd()

    private fun exempt() = power.isIgnoringBatteryOptimizations(app.packageName)

    private fun bucket(): String {
        if (Build.VERSION.SDK_INT < 28) return "n/a"
        return try {
            val manager = app.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            when (val b = manager.appStandbyBucket) {
                10 -> "ACTIVE"
                20 -> "WORKING_SET"
                30 -> "FREQUENT"
                40 -> "RARE"
                45 -> "RESTRICTED"
                50 -> "NEVER"
                else -> "bucket $b"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun thermal(): String {
        if (Build.VERSION.SDK_INT < 29) return "n/a"
        return thermalName(power.currentThermalStatus)
    }

    private fun thermalName(status: Int) = when (status) {
        0 -> "none"
        1 -> "light"
        2 -> "moderate"
        3 -> "severe"
        4 -> "critical"
        5 -> "emergency"
        6 -> "shutdown"
        else -> "status $status"
    }

    private fun restricted(): String {
        if (Build.VERSION.SDK_INT < 28) return "n/a"
        return (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted.toString()
    }

    private fun ringer(mode: Int) = when (mode) {
        AudioManager.RINGER_MODE_SILENT -> "silent"
        AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
        else -> "normal"
    }

    private fun describe(intent: Intent): String = when (val action = intent.action) {
        PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> "doze (deep idle): ${power.isDeviceIdleMode}"
        LIGHT_IDLE -> lightIdle()?.let { "doze (light idle): $it" } ?: "doze (light idle) changed"
        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> "battery saver: ${power.isPowerSaveMode}"
        NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            "do not disturb filter: ${nm.currentInterruptionFilter} (1 all, 2 priority, 3 none, 4 alarms)"
        }
        AudioManager.RINGER_MODE_CHANGED_ACTION -> "ringer: ${ringer(audio.ringerMode)}"
        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> "audio becoming noisy (headphones or Bluetooth gone)"
        Intent.ACTION_USER_PRESENT -> "phone unlocked"
        else -> action.orEmpty().substringAfterLast('.').lowercase()
    }

    private fun watchNetwork() {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = AppLog.i("Network", "default network available")

            override fun onLost(network: Network) {
                lastNetwork = ""
                AppLog.i("Network", "default network lost")
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val now = describe(caps)
                if (now == lastNetwork) return
                lastNetwork = now
                AppLog.i("Network", now)
            }
        })
    }

    private fun describe(caps: NetworkCapabilities): String {
        val transport = listOf(
            NetworkCapabilities.TRANSPORT_WIFI to "wifi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "cellular",
            NetworkCapabilities.TRANSPORT_BLUETOOTH to "bluetooth",
            NetworkCapabilities.TRANSPORT_ETHERNET to "ethernet",
            NetworkCapabilities.TRANSPORT_VPN to "vpn",
        ).filter { caps.hasTransport(it.first) }.joinToString("+") { it.second }
        val metered = if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) "unmetered" else "metered"
        val validated = if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "validated" else "not validated"
        return "network: $transport, $metered, $validated"
    }

    private fun watchThermal() {
        if (Build.VERSION.SDK_INT < 29) return
        power.addThermalStatusListener(ContextCompat.getMainExecutor(app)) { status ->
            AppLog.i("Thermal", "status: ${thermalName(status)}")
        }
    }

    private fun watchStreams() {
        if (Build.VERSION.SDK_INT < 26) return
        audio.registerAudioPlaybackCallback(
            object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                    val now = configs.map { usage(it.audioAttributes.usage) + "/" + content(it.audioAttributes.contentType) }
                        .sorted().joinToString(", ")
                    if (now == lastStreams) return
                    lastStreams = now
                    AppLog.i("AudioStreams", "audio players on the phone (all apps): [${now}]")
                }
            },
            handler,
        )
    }

    private fun watchDevices() {
        audio.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    addedDevices?.filter { it.isSink }?.forEach { AppLog.i("AudioDevices", "output added: ${deviceName(it, detail = true)}; now ${routeLine()}") }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    removedDevices?.filter { it.isSink }?.forEach { AppLog.i("AudioDevices", "output removed: ${deviceName(it, detail = true)}; now ${routeLine()}") }
                }
            },
            handler,
        )
    }

    private fun outputs(): String =
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString(", ") { deviceName(it) }.ifEmpty { "none" }

    private fun deviceName(d: AudioDeviceInfo, detail: Boolean = false): String {
        val type = when (d.type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "earpiece"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "wired headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "wired headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "bluetooth SCO (call quality)"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "bluetooth A2DP"
            AudioDeviceInfo.TYPE_HDMI -> "hdmi"
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "usb"
            else -> "type ${d.type}"
        }
        val name = d.productName?.takeIf { it.isNotBlank() }?.let { " \"$it\"" } ?: ""
        if (!detail) return type + name
        val address = if (Build.VERSION.SDK_INT >= 28 && d.address.isNotEmpty()) " ${d.address}" else ""
        val rates = d.sampleRates.takeIf { it.isNotEmpty() }?.joinToString("/", " rates=")?.plus(" Hz").orEmpty()
        val encodings = d.encodings.takeIf { it.isNotEmpty() }?.joinToString("/", " encodings=").orEmpty()
        val channels = d.channelCounts.takeIf { it.isNotEmpty() }?.joinToString("/", " channels=").orEmpty()
        return type + name + address + rates + channels + encodings
    }

    private fun usage(usage: Int) = when (usage) {
        1 -> "MEDIA"
        2 -> "VOICE_COMMUNICATION"
        3 -> "VOICE_SIGNALLING"
        4 -> "ALARM"
        5 -> "NOTIFICATION"
        6 -> "RINGTONE"
        7 -> "NOTIFICATION_COMMUNICATION_REQUEST"
        8 -> "NOTIFICATION_COMMUNICATION_INSTANT"
        9 -> "NOTIFICATION_COMMUNICATION_DELAYED"
        10 -> "NOTIFICATION_EVENT"
        11 -> "ACCESSIBILITY"
        12 -> "NAVIGATION"
        13 -> "SONIFICATION"
        14 -> "GAME"
        16 -> "ASSISTANT"
        else -> "usage$usage"
    }

    private fun content(type: Int) = when (type) {
        1 -> "SPEECH"
        2 -> "MUSIC"
        3 -> "MOVIE"
        4 -> "SONIFICATION"
        else -> "unknown"
    }

    private inline fun guarded(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            AppLog.w(TAG, "cannot watch $what", e)
        }
    }
}
