package com.audioshelf.app.sound

import com.audioshelf.app.data.testKv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqStoreTest {
    @Test
    fun codecRoundTripsAndRejectsBadShapes() {
        val eq = Eq("custom", listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -12))
        assertEquals(eq, Eq.decode(Eq.encode(eq)))
        assertNull(Eq.decode("{\"preset\":\"x\",\"bands\":[1,2]}"))
        assertNull(Eq.decode("nope"))
        assertEquals(12, Eq.decode("{\"bands\":[99,0,0,0,0,0,0,0,0,0]}")!!.bands[0])
    }

    @Test
    fun trackOverridesGlobal() {
        val store = EqStore(testKv())
        assertEquals(Eq.Flat, store.effective("a"))
        store.setGlobal(Eq.of("rock"))
        assertEquals("rock", store.effective("a").preset)
        store.setTrack("a", Eq.of("bass"))
        assertEquals("bass", store.effective("a").preset)
        assertEquals("rock", store.effective("b").preset)
        store.moveTrack("a", "c")
        assertEquals("bass", store.effective("c").preset)
        store.clearTrack("c")
        assertEquals("rock", store.effective("c").preset)
    }

    @Test
    fun bandEditMarksCustom() {
        val eq = Eq.of("voice").withBand(0, 99)
        assertEquals("custom", eq.preset)
        assertEquals(12, eq.bands[0])
    }

    @Test
    fun scopeIsRemembered() {
        val kv = testKv()
        assertEquals(EqScope.Global, SoundPrefs(kv).scope.value)
        SoundPrefs(kv).setScope(EqScope.Track)
        assertEquals(EqScope.Track, SoundPrefs(kv).scope.value)
    }
}
