package com.audioshelf.app

import com.audioshelf.app.player.fmtSpeed
import com.audioshelf.app.player.fmtTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun secondsAreFloored() {
        assertEquals("0:01", fmtTime(1500))
        assertEquals("0:00", fmtTime(999))
        assertEquals("39:58", fmtTime(2_398_500))
    }

    @Test
    fun hoursAppearOnlyWhenNeeded() {
        assertEquals("59:59", fmtTime(3_599_000))
        assertEquals("1:00:00", fmtTime(3_600_000))
        assertEquals("10:05:09", fmtTime(36_309_000))
    }

    @Test
    fun negativeIsZero() {
        assertEquals("0:00", fmtTime(-5000))
    }

    @Test
    fun speedLabels() {
        assertEquals("1×", fmtSpeed(1f))
        assertEquals("1.25×", fmtSpeed(1.25f))
        assertEquals("0.75×", fmtSpeed(0.75f))
        assertEquals("2×", fmtSpeed(2f))
    }
}
