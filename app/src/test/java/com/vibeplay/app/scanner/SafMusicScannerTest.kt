package com.vibeplay.app.scanner

import kotlinx.coroutines.runBlocking
import com.vibeplay.core.scanner.LocalMusicScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.io.InputStream

class SafMusicScannerTest {
    @Test
    fun scansNestedDocumentsAndPreservesPlaybackUri() = runBlocking {
        val flac = byteArrayOf(0x66, 0x4c, 0x61, 0x43, 0x80.toByte(), 0, 0, 34) + ByteArray(34)
        val tree = object : MusicDocumentTree {
            override fun children(id: String) = when (id) {
                "root" -> listOf(TreeEntry("album", "Album", true), TreeEntry("ignore", "notes.txt", false))
                "album" -> listOf(TreeEntry("song", "Song.FLAC", false), TreeEntry("bad", "bad.flac", false), TreeEntry("audio", "Song.mp3", false, "audio/mpeg"), TreeEntry("root", "cycle", true))
                else -> emptyList()
            }
            override fun uri(id: String) = "content://example/document/$id"
            override fun open(id: String): InputStream = if (id == "song") flac.inputStream() else throw IOException()
            override fun otherAudio(entry: TreeEntry, parentName: String?) =
                LocalMusicScanner.trackFromFlac(LocalMusicScanner.FlacInfo(44100, 24, 2, 0, 0, emptyMap()), entry.name, parentName, uri(entry.id), entry.id).copy(format = "MP3")
        }
        val tracks = scanMusicTree(tree, "root")
        assertEquals(2, tracks.size)
        assertEquals("Song", tracks[0].title)
        assertEquals("Album", tracks[0].album)
        assertEquals("content://example/document/song", tracks[0].localUri)
        assertEquals("MP3", tracks[1].format)
        assertEquals("content://example/document/audio", tracks[1].localUri)
    }

    @Test
    fun reportsFolderReadFailure() {
        val tree = object : MusicDocumentTree {
            override fun children(id: String): List<TreeEntry> = throw IOException("revoked")
            override fun uri(id: String) = id
            override fun open(id: String): InputStream? = null
        }
        assertThrows(IOException::class.java) { runBlocking { scanMusicTree(tree, "root") } }
    }
}
