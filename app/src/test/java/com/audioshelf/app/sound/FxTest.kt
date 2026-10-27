package com.audioshelf.app.sound

import com.audioshelf.app.data.testKv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FxTest {
    @Test
    fun codecRoundTripsClampsAndIgnoresOldKeys() {
        val fx = Fx(6, true)
        assertEquals(fx, Fx.decode(Fx.encode(fx)))
        val wild = Fx.decode("{\"gain\":99,\"pitch\":-99,\"noise\":9,\"skip\":false}")!!
        assertEquals(12, wild.gainDb)
        assertFalse(wild.skipSilence)
        assertNull(Fx.decode("nope"))
    }

    @Test
    fun neutralMeansNothingToDo() {
        assertTrue(Fx.Off.neutral)
        assertFalse(Fx.Off.withGain(3).neutral)
        assertFalse(Fx.Off.withSkipSilence(true).neutral)
    }

    @Test
    fun trackOverridesGlobal() {
        val store = FxStore(testKv())
        assertEquals(Fx.Off, store.effective("a"))
        store.setGlobal(Fx.Off.withGain(6))
        assertEquals(6, store.effective("a").gainDb)
        store.setTrack("a", Fx.Off.withSkipSilence(true))
        assertTrue(store.effective("a").skipSilence)
        assertEquals(6, store.effective("b").gainDb)
        store.clearTrack("a")
        assertEquals(6, store.effective("a").gainDb)
    }

    @Test
    fun chainBoostsAndLimits() {
        val chain = SoundChain()
        chain.configure(44100, 1)
        chain.setBoost(12)
        chain.refresh()
        assertFalse(chain.passthrough)
        assertEquals(3981f, chain.process(1000f, 0), 40f)
        assertTrue(chain.process(30000f, 0) <= 32767f)
        chain.setBoost(0)
        chain.refresh()
        assertTrue(chain.passthrough)
    }
}
