package com.vibeplay.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class LibraryDirectoryTest {
    @Test fun configuredDirectoryTakesPrecedence() {
        val selected = resolveLibraryDirectory("custom/music") { error("Default should not be used") }
        assertEquals(File("custom/music"), selected)
    }

    @Test fun absentConfiguredDirectoryUsesAvailableDefault() {
        val default = File("external/music")
        assertEquals(default, resolveLibraryDirectory(null) { default })
        assertNull(resolveLibraryDirectory(null) { null })
    }
}
