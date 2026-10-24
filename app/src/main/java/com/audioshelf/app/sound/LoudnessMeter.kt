package com.audioshelf.app.sound

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.IOException

private const val WINDOW_US = 5L * 60 * 1_000_000
private const val WAIT_US = 10_000L
private const val DEFAULT_RATE = 44100
private const val DEFAULT_CHANNELS = 2

class LoudnessMeter(private val context: Context) {
    fun gainFor(uri: Uri, stop: () -> Boolean): Double {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val format = selectAudio(extractor) ?: throw IOException("no audio track")
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IOException("no mime")
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            val total = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val start = if (total > 3 * WINDOW_US) total / 2 - WINDOW_US / 2 else 0L
            if (start > 0) extractor.seekTo(start, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            return decode(extractor, decoder, format, start + WINDOW_US, stop)
        } catch (e: IllegalStateException) {
            throw IOException(e)
        } catch (e: IllegalArgumentException) {
            throw IOException(e)
        } finally {
            codec?.let {
                runCatching { it.stop() }
                it.release()
            }
            extractor.release()
        }
    }

    private fun selectAudio(extractor: MediaExtractor): MediaFormat? {
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                extractor.selectTrack(i)
                return f
            }
        }
        return null
    }

    private fun decode(ex: MediaExtractor, codec: MediaCodec, input: MediaFormat, endUs: Long, stop: () -> Boolean): Double {
        val info = MediaCodec.BufferInfo()
        val rate = if (input.containsKey(MediaFormat.KEY_SAMPLE_RATE)) input.getInteger(MediaFormat.KEY_SAMPLE_RATE) else DEFAULT_RATE
        val channels = if (input.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) input.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else DEFAULT_CHANNELS
        val stats = LoudnessStats(rate, channels)
        var isFloat = false
        var inDone = false
        var outDone = false
        while (!outDone && !stop()) {
            if (!inDone) {
                val i = codec.dequeueInputBuffer(WAIT_US)
                if (i >= 0) {
                    val buf = codec.getInputBuffer(i)
                    val size = if (buf == null) -1 else ex.readSampleData(buf, 0)
                    val time = ex.sampleTime
                    if (size < 0 || time > endUs) {
                        codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inDone = true
                    } else {
                        codec.queueInputBuffer(i, 0, size, time, 0)
                        ex.advance()
                    }
                }
            }
            val o = codec.dequeueOutputBuffer(info, WAIT_US)
            if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val out = codec.outputFormat
                isFloat = out.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                    out.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
            } else if (o >= 0) {
                val buf = codec.getOutputBuffer(o)
                if (buf != null && info.size > 0) {
                    buf.position(info.offset).limit(info.offset + info.size)
                    stats.add(buf, isFloat)
                }
                codec.releaseOutputBuffer(o, false)
                outDone = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
            }
        }
        return stats.gainDb()
    }
}
