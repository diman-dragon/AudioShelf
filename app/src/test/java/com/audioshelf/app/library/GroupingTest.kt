package com.audioshelf.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupingTest {
    @Test
    fun looseFileIsOwnBook() {
        val books = groupBooks(listOf(file("", "Song.mp3")))
        assertEquals(1, books.size)
        assertFalse(books[0].folder)
        assertEquals("Song", books[0].name)
    }

    @Test
    fun folderWithChaptersIsOneBook() {
        val files = (1..3).map { file("Tolstoy/War", "ch$it.mp3", modified = it * 10L) }
        val books = groupBooks(files)
        assertEquals(1, books.size)
        val book = books[0]
        assertTrue(book.folder)
        assertEquals("dir:Tolstoy/War", book.id)
        assertEquals("War", book.name)
        assertEquals(3, book.files.size)
        assertEquals(30L, book.modified)
        assertTrue(book.ownsDir)
    }

    @Test
    fun chaptersSortNaturally() {
        val files = listOf(file("B", "ch10.mp3"), file("B", "ch2.mp3"), file("B", "ch1.mp3"))
        assertEquals(listOf("ch1.mp3", "ch2.mp3", "ch10.mp3"), groupBooks(files)[0].files.map { it.name })
    }

    @Test
    fun discFoldersMergeIntoOneBook() {
        val files = listOf(file("Book/CD1", "a1.mp3"), file("Book/CD2", "a2.mp3"), file("Book/Диск 3", "a3.mp3"))
        val books = groupBooks(files)
        assertEquals(1, books.size)
        assertEquals("dir:Book", books[0].id)
        assertEquals(3, books[0].dirUris.size)
    }

    @Test
    fun m4bInsideFolderStaysSeparate() {
        val files = listOf(file("Mix", "whole.m4b"), file("Mix", "a1.mp3"), file("Mix", "a2.mp3"))
        val books = groupBooks(files)
        assertEquals(2, books.size)
        assertEquals(1, books.count { !it.folder })
    }

    @Test
    fun bigUnnumberedFilesBecomeSeparateBooks() {
        val big = 60L * 1048576
        val files = listOf(file("Albums", "alpha.mp3", big), file("Albums", "beta.mp3", big))
        assertEquals(2, groupBooks(files).size)
    }

    @Test
    fun folderDoesNotOwnDirWhenSubfoldersHoldMoreFiles() {
        val files = listOf(file("A", "1.mp3"), file("A", "2.mp3"), file("A/Extra", "x.mp3"))
        val a = groupBooks(files).first { it.id == "dir:A" }
        assertFalse(a.ownsDir)
    }

    @Test
    fun countUnderCountsEveryLevel() {
        val under = countUnder(listOf(file("A/B", "1.mp3"), file("A", "2.mp3")))
        assertEquals(2, under["A"])
        assertEquals(1, under["A/B"])
    }

    @Test
    fun groupDirOnlyStripsDiscSuffix() {
        assertEquals("Book", groupDir("Book/Part 2"))
        assertEquals("Book/Chapters", groupDir("Book/Chapters"))
        assertEquals("CD1", groupDir("CD1"))
    }
}
