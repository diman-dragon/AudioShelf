package com.audioshelf.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlatJsonTest {
    @Test
    fun readsOldAppFormat() {
        val m = FlatJson.parse("""{"kind":"book","title":"Война и мир","year":"1869"}""")!!
        assertEquals("book", m.str("kind"))
        assertEquals("Война и мир", m.str("title"))
    }

    @Test
    fun readsNumbersAndFlags() {
        val m = FlatJson.parse("""{"i":3,"p":12345,"t":1700000000000,"done":true,"x":0}""")!!
        assertEquals(3, m.int("i"))
        assertEquals(12345L, m.long("p"))
        assertEquals(1700000000000L, m.long("t"))
        assertTrue(m.flag("done"))
        assertFalse(m.flag("x"))
    }

    @Test
    fun roundTripsEscapes() {
        val source = linkedMapOf<String, Any?>("name" to "a \"b\" \\ c\nd\te", "n" to 5, "ok" to true, "none" to null)
        val back = FlatJson.parse(FlatJson.write(source))!!
        assertEquals("a \"b\" \\ c\nd\te", back.str("name"))
        assertEquals(5, back.int("n"))
        assertTrue(back.flag("ok"))
        assertNull(back["none"])
    }

    @Test
    fun readsUnicodeEscapes() {
        assertEquals("é", FlatJson.parse("""{"a":"é"}""")!!.str("a"))
    }

    @Test
    fun rejectsBrokenInput() {
        assertNull(FlatJson.parse(null))
        assertNull(FlatJson.parse("1"))
        assertNull(FlatJson.parse("{"))
        assertNull(FlatJson.parse("""{"a":}"""))
        assertNull(FlatJson.parse("""{"a":1} x"""))
    }
}
