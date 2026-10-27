package com.audioshelf.app.sound

import com.audioshelf.app.data.GAIN_OPTIONS
import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.data.nextGain
import com.audioshelf.app.data.testKv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FxTest {
    @Test
    fun codecRoundTripsAndIgnoresOldKeys() {
        val fx = Fx(true)
        assertEquals(fx, Fx.decode(Fx.encode(fx)))
        val old = Fx.decode("{\"gain\":6,\"pitch\":-3,\"noise\":2,\"skip\":true}")!!
        assertTrue(old.skipSilence)
        assertFalse(Fx.decode("{\"gain\":12,\"pitch\":4,\"noise\":3}")!!.skipSilence)
        assertNull(Fx.decode("nope"))
    }

    @Test
    fun neutralMeansNothingToDo() {
        assertTrue(Fx.Off.neutral)
        assertFalse(Fx.Off.withSkipSilence(true).neutral)
    }

    @Test
    fun trackOverridesGlobal() {
        val store = FxStore(testKv())
        assertEquals(Fx.Off, store.effective("a"))
        store.setGlobal(Fx(true))
        assertTrue(store.effective("a").skipSilence)
        store.setTrack("a", Fx.Off)
        assertFalse(store.effective("a").skipSilence)
        assertTrue(store.effective("b").skipSilence)
        store.clearTrack("a")
        assertTrue(store.effective("a").skipSilence)
    }

    @Test
    fun gainCyclesThroughTheStepsAndWraps() {
        var db = GAIN_OPTIONS.first()
        val seen = ArrayList<Int>()
        repeat(GAIN_OPTIONS.size) {
            seen.add(db)
            db = nextGain(db)
        }
        assertEquals(GAIN_OPTIONS, seen)
        assertEquals(GAIN_OPTIONS.first(), db)
    }

    @Test
    fun gainIsStoredAndMigratesFromTheOldEffectsRecord() {
        val kv = testKv()
        assertEquals(0, Prefs(kv).gain.value)
        kv.put(KV_STORE, "audioshelf.fx.global", "{\"gain\":6,\"pitch\":2,\"noise\":1,\"skip\":false}")
        assertEquals(6, Prefs(kv).gain.value)
        val prefs = Prefs(kv)
        prefs.setGain(9)
        assertEquals(9, prefs.gain.value)
        assertEquals(9, Prefs(kv).gain.value)
        prefs.setGain(7)
        assertEquals(0, prefs.gain.value)
    }

    @Test
    fun chainBoostsAndLimits() {
        val chain = SoundChain()
        chain.configure(44100, 1)
        chain.setBoost(12)
        chain.refresh()
        assertFalse(chain.passthrough)
        val y = chain.process(1000f, 0)
        assertEquals(3981f, y, 40f)
        assertTrue(chain.process(30000f, 0) <= 32767f)
        chain.setBoost(0)
        chain.refresh()
        assertTrue(chain.passthrough)
    }
}
