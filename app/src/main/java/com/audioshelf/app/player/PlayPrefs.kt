package com.audioshelf.app.player

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Kv

private const val SPEED = "audioshelf.speed"
private const val LAST = "audioshelf.last2"
private const val MODE_PREFIX = "audioshelf.mode."

class PlayPrefs(private val kv: Kv) {
    fun speed(): Float = kv.get(KV_STORE, SPEED)?.toFloatOrNull()?.takeIf { it in SPEEDS } ?: 1f

    fun setSpeed(speed: Float) = kv.put(KV_STORE, SPEED, speed.toString())

    fun mode(kind: Kind): PlayMode {
        val saved = kv.get(KV_STORE, MODE_PREFIX + kind.name.lowercase())
        return MODES.getValue(kind).firstOrNull { it.name.lowercase() == saved } ?: MODES.getValue(kind).first()
    }

    fun setMode(kind: Kind, mode: PlayMode) = kv.put(KV_STORE, MODE_PREFIX + kind.name.lowercase(), mode.name.lowercase())

    fun nextMode(kind: Kind): PlayMode {
        val list = MODES.getValue(kind)
        val next = list[(list.indexOf(mode(kind)) + 1) % list.size]
        setMode(kind, next)
        return next
    }

    fun last(): String? = kv.get(KV_STORE, LAST)

    fun setLast(json: String?) {
        if (json == null) kv.del(KV_STORE, LAST) else kv.put(KV_STORE, LAST, json)
    }
}
