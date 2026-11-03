@file:OptIn(UnstableApi::class)

package com.audioshelf.app.sound

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.audioshelf.app.AppLog
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "SoundProcessor"

// A 16-bit sample this close to full scale is flat-topped: the file itself is clipped.
private const val CLIP_LEVEL = 32760

class SoundProcessor(val chain: SoundChain) : BaseAudioProcessor() {
    val stats = SoundStats()

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            AppLog.w(
                TAG,
                "input is ${encodingName(inputAudioFormat.encoding)} at ${inputAudioFormat.sampleRate} Hz, not 16-bit: " +
                    "the equalizer and the boost are bypassed",
            )
            return AudioFormat.NOT_SET
        }
        chain.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        AppLog.i(TAG, "configured: ${inputAudioFormat.sampleRate} Hz, ${inputAudioFormat.channelCount} ch, 16-bit")
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        chain.refresh()
        val out = replaceOutputBuffer(size)
        var samples = 0
        var sourceClipped = 0
        var peakIn = 0
        var peakOut = 0
        if (chain.passthrough) {
            val scan = inputBuffer.duplicate().order(inputBuffer.order())
            while (scan.remaining() >= 2) {
                val a = abs(scan.short.toInt())
                samples++
                if (a > peakIn) peakIn = a
                if (a >= CLIP_LEVEL) sourceClipped++
            }
            out.put(inputBuffer)
            out.flip()
            stats.record(samples, sourceClipped, 0, 0, peakIn, peakIn)
            return
        }
        chain.resetHits()
        val channels = chain.channelCount()
        var channel = 0
        while (inputBuffer.remaining() >= 2) {
            val s = inputBuffer.short.toInt()
            val a = abs(s)
            samples++
            if (a > peakIn) peakIn = a
            if (a >= CLIP_LEVEL) sourceClipped++
            val y = chain.process(s.toFloat(), channel).roundToInt().coerceIn(-32768, 32767)
            val ay = abs(y)
            if (ay > peakOut) peakOut = ay
            out.putShort(y.toShort())
            if (++channel == channels) channel = 0
        }
        out.flip()
        stats.record(samples, sourceClipped, chain.limiterHits, chain.overHits, peakIn, peakOut)
    }

    private fun encodingName(encoding: Int) = when (encoding) {
        C.ENCODING_PCM_16BIT -> "16-bit"
        C.ENCODING_PCM_24BIT -> "24-bit PCM"
        C.ENCODING_PCM_32BIT -> "32-bit PCM"
        C.ENCODING_PCM_FLOAT -> "float PCM"
        else -> "encoding $encoding"
    }
}
