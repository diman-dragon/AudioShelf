@file:OptIn(UnstableApi::class)

package com.audioshelf.app.sound

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class SoundProcessor(val chain: SoundChain) : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioFormat.NOT_SET
        chain.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        chain.refresh()
        val out = replaceOutputBuffer(size)
        if (chain.passthrough) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        val channels = chain.channelCount()
        var channel = 0
        while (inputBuffer.remaining() >= 2) {
            val y = chain.process(inputBuffer.short.toFloat(), channel)
            out.putShort(y.roundToInt().coerceIn(-32768, 32767).toShort())
            if (++channel == channels) channel = 0
        }
        out.flip()
    }
}
