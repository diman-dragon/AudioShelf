package com.audioshelf.app.sound

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

const val TARGET_DB = -16.0
const val MAX_GAIN_DB = 12.0
private const val SILENCE_DB = -50.0
private const val CEILING_DB = -1.0
private const val BLOCK_DIVISOR = 10

class LoudnessStats(sampleRate: Int, channels: Int) {
    private val silence = 10.0.pow(SILENCE_DB / 10)
    private val blockSamples = maxOf(1, sampleRate / BLOCK_DIVISOR).toLong() * channels
    private var blockSum = 0.0
    private var blockCount = 0L
    private var energySum = 0.0
    private var energyBlocks = 0L
    private var peak = 0.0

    fun add(data: ByteBuffer, isFloat: Boolean) {
        val buf = data.duplicate().order(ByteOrder.nativeOrder())
        val width = if (isFloat) 4 else 2
        while (buf.remaining() >= width) {
            val x = if (isFloat) buf.float.toDouble() else buf.short / 32768.0
            peak = maxOf(peak, abs(x))
            blockSum += x * x
            if (++blockCount == blockSamples) {
                val energy = blockSum / blockCount
                if (energy > silence) {
                    energySum += energy
                    energyBlocks++
                }
                blockSum = 0.0
                blockCount = 0
            }
        }
    }

    fun gainDb(): Double {
        if (energyBlocks == 0L || peak <= 0) return 0.0
        val loud = 10 * log10(energySum / energyBlocks)
        var gain = (TARGET_DB - loud).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB)
        val headroom = CEILING_DB - 20 * log10(peak)
        if (gain > headroom) gain = maxOf(-MAX_GAIN_DB, headroom)
        return (gain * 10).roundToLong() / 10.0
    }
}
