package com.audioshelf.app.sound

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal class Fft(private val n: Int) {
    private val levels = Integer.numberOfTrailingZeros(n)
    private val cosT = FloatArray(n / 2) { cos(2.0 * PI * it / n).toFloat() }
    private val sinT = FloatArray(n / 2) { sin(2.0 * PI * it / n).toFloat() }
    private val rev = IntArray(n) { Integer.reverse(it) ushr (32 - levels) }

    fun forward(re: FloatArray, im: FloatArray) {
        for (i in 0 until n) {
            val j = rev[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var i = 0
            while (i < n) {
                var k = 0
                for (j in i until i + half) {
                    val l = j + half
                    val tr = re[l] * cosT[k] + im[l] * sinT[k]
                    val ti = im[l] * cosT[k] - re[l] * sinT[k]
                    re[l] = re[j] - tr
                    im[l] = im[j] - ti
                    re[j] += tr
                    im[j] += ti
                    k += step
                }
                i += size
            }
            size *= 2
        }
    }

    fun inverse(re: FloatArray, im: FloatArray) {
        for (i in 0 until n) im[i] = -im[i]
        forward(re, im)
        val scale = 1f / n
        for (i in 0 until n) re[i] *= scale
    }
}

private const val FRAME = 512
private const val HOP = FRAME / 2
private const val BINS = FRAME / 2 + 1
private const val RISE = 1.0015f
private const val BIAS = 2.0f
private const val EPS = 1e-3f

private val ALPHA = floatArrayOf(0f, 1.2f, 1.8f, 2.5f)
private val FLOOR = floatArrayOf(1f, 0.5f, 0.3f, 0.16f)

/**
 * Spectral noise reduction for one channel. The noise floor is tracked per bin as a slowly
 * rising minimum of the smoothed power, then a Wiener-style gain with a floor is applied
 * (50% overlap, sqrt-Hann windows). Output is delayed by [LATENCY] samples.
 */
class Denoiser {
    private val fft = Fft(FRAME)
    private val window = FloatArray(FRAME) { sin(PI * (it + 0.5) / FRAME).toFloat() }
    private val input = FloatArray(FRAME)
    private val acc = FloatArray(FRAME)
    private val re = FloatArray(FRAME)
    private val im = FloatArray(FRAME)
    private val smooth = FloatArray(BINS)
    private val noise = FloatArray(BINS)
    private val gain = FloatArray(BINS) { 1f }
    private val shaped = FloatArray(BINS)
    private var pos = 0
    private var count = 0
    private var primed = false

    @Volatile var level = 0

    fun process(sample: Float): Float {
        val out = acc[pos]
        acc[pos] = 0f
        input[pos] = sample
        pos = (pos + 1) % FRAME
        if (++count == HOP) {
            count = 0
            frame()
        }
        return out
    }

    private fun frame() {
        for (i in 0 until FRAME) {
            re[i] = input[(pos + i) % FRAME] * window[i]
            im[i] = 0f
        }
        fft.forward(re, im)
        val strength = level.coerceIn(0, ALPHA.size - 1)
        val alpha = ALPHA[strength]
        val floor = FLOOR[strength]
        for (k in 0 until BINS) {
            val p = re[k] * re[k] + im[k] * im[k]
            if (!primed) {
                smooth[k] = p
                noise[k] = p
            } else {
                smooth[k] = 0.7f * smooth[k] + 0.3f * p
                noise[k] = if (smooth[k] < noise[k]) smooth[k] else noise[k] * RISE
            }
            val q = 0.6f * smooth[k] + 0.4f * p
            val raw = if (alpha == 0f) 1f else (1f - alpha * BIAS * noise[k] / (q + EPS)).coerceIn(floor, 1f)
            val rate = if (raw > gain[k]) 0.6f else 0.25f
            gain[k] += rate * (raw - gain[k])
        }
        primed = true
        for (k in 0 until BINS) {
            val a = gain[max(0, k - 1)]
            val c = gain[minOf(BINS - 1, k + 1)]
            shaped[k] = 0.25f * a + 0.5f * gain[k] + 0.25f * c
        }
        for (k in 0 until BINS) {
            re[k] *= shaped[k]
            im[k] *= shaped[k]
            if (k in 1 until FRAME / 2) {
                re[FRAME - k] = re[k]
                im[FRAME - k] = -im[k]
            }
        }
        fft.inverse(re, im)
        for (i in 0 until FRAME) {
            val at = (pos + i) % FRAME
            acc[at] += re[i] * window[i]
        }
    }

    companion object {
        const val LATENCY = FRAME
    }
}
