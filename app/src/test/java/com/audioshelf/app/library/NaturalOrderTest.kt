package com.audioshelf.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {
    @Test
    fun numbersCompareByValue() {
        assertEquals(listOf("2", "10", "100"), listOf("100", "2", "10").sortedWith(NaturalOrder))
    }

    @Test
    fun textAndNumbersMix() {
        val sorted = listOf("Глава 10", "Глава 2", "Глава 1").sortedWith(NaturalOrder)
        assertEquals(listOf("Глава 1", "Глава 2", "Глава 10"), sorted)
    }

    @Test
    fun caseIsIgnored() {
        assertEquals(0, NaturalOrder.compare("abc", "ABC"))
    }

    @Test
    fun leadingZerosDoNotMatter() {
        assertTrue(NaturalOrder.compare("track002", "track10") < 0)
    }
}
