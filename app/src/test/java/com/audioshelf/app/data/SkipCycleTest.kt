package com.audioshelf.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SkipCycleTest {
    @Test
    fun cyclesThroughOptionsAndWraps() {
        assertEquals(10, nextSkip(5))
        assertEquals(30, nextSkip(15))
        assertEquals(5, nextSkip(60))
        assertEquals(5, nextSkip(999))
    }
}
