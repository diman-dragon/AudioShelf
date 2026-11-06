package com.audioshelf.app.diag

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.audioshelf.app.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Bluetooth"
private const val CODEC_CHANGED = "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED"
private const val PROFILE_CHECK_DELAY_MS = 500L

// Not in the public API, but sent by the Bluetooth app as a protected broadcast (extra BATTERY_LEVEL, 0-100).
private const val BATTERY_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

/**
 * Every Bluetooth event that touches the audio: the adapter switching on and off, a device connecting
 * and disconnecting (and why), pairing, the A2DP music link and its codec, whether the stream is
 * really playing or suspended, the call-quality SCO link, and which devices are connected. A
 * Bluetooth headset or car is a common reason for stutter, a thin sound and a "raspy" voice (SBC
 * codec, a weak link, the phone switching between the music and the call profile).
 *
 * Android 12 and newer deliver most of these only to an app that holds the Bluetooth permission;
 * without it the log says so and still records the audio routes and outputs.
 */
@SuppressLint("MissingPermission")
class BluetoothWatch(private val app: Application) {
    private val adapter: BluetoothAdapter? = (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false
    private var permitted = false
    private val connectedFlow = MutableStateFlow("")

    /** The connected music devices (headphones, a speaker) with their charge when they report it, for Settings. */
    val connected: StateFlow<String> = connectedFlow.asStateFlow()

    // Events come in bursts (a call switching the headset gave 6 in a row); one check after the burst is enough.
    private val profileCheck = Runnable { logProfiles() }
    private var lastBatteryLine = ""

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val line = describe(intent)
            if (intent.action == BATTERY_CHANGED) {
                if (line == lastBatteryLine) return
                lastBatteryLine = line
            }
            AppLog.i(TAG, line)
            if (intent.action in PROFILE_ACTIONS) {
                handler.removeCallbacks(profileCheck)
                handler.postDelayed(profileCheck, PROFILE_CHECK_DELAY_MS)
            }
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun start() {
        if (adapter == null) {
            AppLog.i(TAG, "this phone has no Bluetooth")
            return
        }
        permitted = hasPermission()
        register()
        AppLog.i(TAG, summary())
        logProfiles()
    }

    /** The permission can be given in the system settings while the app is away; pick it up when the app comes back. */
    fun onForeground() {
        if (adapter == null || permitted || !hasPermission()) return
        onPermissionResult(true)
    }

    /** The app's page in the system settings, where "Nearby devices" can be allowed after the dialog stopped appearing. */
    fun openPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri())
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AppLog.i(TAG, "opening the app settings to allow Bluetooth")
        if (runCatching { context.startActivity(intent) }.isFailure) AppLog.w(TAG, "cannot open the app settings")
    }

    /** Called with the answer to the permission question; with the permission the events carry device names and more. */
    fun onPermissionResult(granted: Boolean) {
        AppLog.i(TAG, if (granted) "Bluetooth permission granted" else "Bluetooth permission denied: device names and some events are not available")
        permitted = granted
        if (adapter == null || !granted) return
        unregister()
        register()
        AppLog.i(TAG, summary())
        logProfiles()
    }

    /** What the log header says about Bluetooth. */
    fun summary(): String {
        val bt = adapter ?: return "bluetooth: none"
        val bonded = try {
            bt.bondedDevices.joinToString("; ") { describe(it) }.ifEmpty { "none" }
        } catch (e: SecurityException) {
            "unavailable (no Bluetooth permission)"
        }
        return "bluetooth: adapter ${adapterState(bt.state)}, permission=${hasPermission()}, paired: $bonded"
    }

    @Suppress("DEPRECATION")
    private fun register() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            addAction(CODEC_CHANGED)
            addAction(BATTERY_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        try {
            // Not RECEIVER_NOT_EXPORTED: these are sent by the Bluetooth app (its own uid, not the system's), and a
            // not-exported receiver drops them. They are protected broadcasts, only the system can send them, and
            // the receiver only writes a log line.
            ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            registered = true
        } catch (e: Exception) {
            AppLog.w(TAG, "cannot listen to Bluetooth events", e)
        }
    }

    private fun unregister() {
        if (!registered) return
        runCatching { app.unregisterReceiver(receiver) }
        registered = false
    }

    private fun logProfiles() {
        val bt = adapter ?: return
        for (profile in intArrayOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)) {
            try {
                bt.getProfileProxy(
                    app,
                    object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                            try {
                                val devices = proxy.connectedDevices
                                if (proxy is BluetoothA2dp) {
                                    connectedFlow.value = devices.joinToString(", ") { device ->
                                        (device.name ?: "?") + (batteryOf(device)?.let { " · $it%" }.orEmpty())
                                    }
                                }
                                val text = devices.joinToString("; ") { device ->
                                    describe(device) + when (proxy) {
                                        is BluetoothA2dp -> if (proxy.isA2dpPlaying(device)) ", stream PLAYING" else ", stream not playing"
                                        is BluetoothHeadset -> if (proxy.isAudioConnected(device)) ", call audio connected" else ", no call audio"
                                        else -> ""
                                    }
                                }
                                AppLog.i(TAG, "${profileName(profile)} connected devices: ${text.ifEmpty { "none" }}")
                            } catch (e: SecurityException) {
                                AppLog.w(TAG, "${profileName(profile)} devices unavailable (no Bluetooth permission)")
                            } finally {
                                bt.closeProfileProxy(profile, proxy)
                            }
                        }

                        override fun onServiceDisconnected(profile: Int) {}
                    },
                    profile,
                )
            } catch (e: Exception) {
                AppLog.w(TAG, "cannot query ${profileName(profile)}", e)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun describe(intent: Intent): String {
        val action = intent.action.orEmpty()
        val extras = intent.extras
        val parts = ArrayList<String>()
        IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)?.let { parts += describe(it) }
        val state = extras?.getInt(BluetoothProfile.EXTRA_STATE, -1) ?: -1
        val before = extras?.getInt(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1) ?: -1
        when (action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val now = extras?.getInt(BluetoothAdapter.EXTRA_STATE, -1) ?: -1
                val was = extras?.getInt(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1) ?: -1
                parts += "adapter ${adapterState(now)} (was ${adapterState(was)})"
            }
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
            -> parts += "${profileState(state)} (was ${profileState(before)})"
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED ->
                parts += "A2DP stream " + (if (state == BluetoothA2dp.STATE_PLAYING) "PLAYING" else "NOT PLAYING (suspended)") + " (was ${before})"
            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED ->
                parts += "call audio (SCO) " + when (state) {
                    BluetoothHeadset.STATE_AUDIO_CONNECTED -> "connected"
                    BluetoothHeadset.STATE_AUDIO_CONNECTING -> "connecting"
                    BluetoothHeadset.STATE_AUDIO_DISCONNECTED -> "disconnected"
                    else -> "state $state"
                }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val now = extras?.getInt(BluetoothDevice.EXTRA_BOND_STATE, -1) ?: -1
                val was = extras?.getInt(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1) ?: -1
                parts += "pairing ${bondState(now)} (was ${bondState(was)})"
            }
        }
        // Everything else in the broadcast, as it came: the codec status and the reason of a disconnect live here.
        val known = setOf(
            BluetoothDevice.EXTRA_DEVICE, BluetoothProfile.EXTRA_STATE, BluetoothProfile.EXTRA_PREVIOUS_STATE,
            BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.EXTRA_PREVIOUS_STATE,
            BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
        )
        extras?.keySet()?.filter { it !in known }?.forEach { key ->
            parts += "${key.substringAfterLast('.')}=${try { extras?.get(key) } catch (e: Exception) { "unreadable" }}"
        }
        return action.substringAfterLast('.').lowercase() + ": " + parts.joinToString("; ")
    }

    private fun describe(d: BluetoothDevice): String = try {
        val type = when (d.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "LE"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "dual"
            else -> "type ${d.type}"
        }
        val major = d.bluetoothClass?.majorDeviceClass?.let { ", class $it" }.orEmpty()
        val battery = batteryOf(d)?.let { ", battery $it%" }.orEmpty()
        "\"${d.name ?: "?"}\" ${d.address} ($type, ${bondState(d.bondState)}$major$battery)"
    } catch (e: SecurityException) {
        "${d.address} (no permission for the name)"
    }

    /** The charge of the device in percent, if it reports one. The call is hidden in the SDK: where Android refuses it, null. */
    private fun batteryOf(d: BluetoothDevice): Int? = try {
        (BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(d) as? Int)?.takeIf { it in 0..100 }
    } catch (e: Throwable) {
        null
    }

    private fun adapterState(state: Int) = when (state) {
        BluetoothAdapter.STATE_OFF -> "OFF"
        BluetoothAdapter.STATE_TURNING_ON -> "turning on"
        BluetoothAdapter.STATE_ON -> "ON"
        BluetoothAdapter.STATE_TURNING_OFF -> "turning off"
        else -> "state $state"
    }

    private fun profileState(state: Int) = when (state) {
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "connecting"
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "disconnecting"
        else -> "state $state"
    }

    private fun bondState(state: Int) = when (state) {
        BluetoothDevice.BOND_NONE -> "not paired"
        BluetoothDevice.BOND_BONDING -> "pairing"
        BluetoothDevice.BOND_BONDED -> "paired"
        else -> "bond $state"
    }

    private fun profileName(profile: Int) = when (profile) {
        BluetoothProfile.A2DP -> "A2DP (music)"
        BluetoothProfile.HEADSET -> "headset (calls)"
        else -> "profile $profile"
    }

    private companion object {
        val PROFILE_ACTIONS = setOf(
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            BATTERY_CHANGED,
        )
    }
}
