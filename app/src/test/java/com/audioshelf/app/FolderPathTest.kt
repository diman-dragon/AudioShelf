package com.audioshelf.app

import com.audioshelf.app.ui.settings.folderPath
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderPathTest {
    @Test
    fun internalStorage() {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3AAudiobooks%2FRu"
        assertEquals("Device storage › Audiobooks › Ru", folderPath(uri, "Ru", "Device storage", "Memory card"))
    }

    @Test
    fun memoryCard() {
        val uri = "content://com.android.externalstorage.documents/tree/1A2B-3C4D%3AMusic"
        assertEquals("Memory card › Music", folderPath(uri, "Music", "Device storage", "Memory card"))
    }

    @Test
    fun unknownProviderFallsBackToName() {
        assertEquals("Books", folderPath("content://other/tree/x", "Books", "I", "C"))
    }
}
