package com.vibeplay.core.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class LocalMusicScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun parseFlacFile_withMissingFile_returnsNull() {
        val nonExistentFile = File("this_file_does_not_exist.flac")
        val result = LocalMusicScanner.parseFlacFile(nonExistentFile)
        assertNull(result)
    }

    @Test
    fun parseFlacFile_withSmallFile_returnsNull() {
        val smallFile = tempFolder.newFile("small.flac")
        smallFile.writeBytes(ByteArray(40)) // Less than 42 bytes
        val result = LocalMusicScanner.parseFlacFile(smallFile)
        assertNull(result)
    }

    @Test
    fun parseFlacFile_withInvalidMagic_returnsNull() {
        val invalidMagicFile = tempFolder.newFile("invalid_magic.flac")
        val content = ByteArray(42)
        // Not starting with "fLaC"
        content[0] = 'f'.code.toByte()
        content[1] = '0'.code.toByte()
        content[2] = 'o'.code.toByte()
        content[3] = 'o'.code.toByte()
        invalidMagicFile.writeBytes(content)

        val result = LocalMusicScanner.parseFlacFile(invalidMagicFile)
        assertNull(result)
    }

    @Test
    fun parseFlacFile_withValidFlacFile_parsesSuccessfully() {
        val validFlacFile = tempFolder.newFile("valid.flac")

        // Construct a valid FLAC file structure programmatically
        val outBytes = mutableListOf<Byte>()

        // 1. Magic "fLaC"
        outBytes.addAll("fLaC".toByteArray(StandardCharsets.ISO_8859_1).toList())

        // 2. STREAMINFO block (Type 0, length 34, isLast = false)
        // Header byte (0x00 for type 0, MSB 0 for isLast = false)
        outBytes.add(0x00.toByte())
        // Length 34 bytes (0x00 0x00 0x22)
        outBytes.add(0x00.toByte())
        outBytes.add(0x00.toByte())
        outBytes.add(0x22.toByte())

        // Construct 34 byte STREAMINFO block
        val streamInfo = ByteArray(34)
        // byte 10-17:
        // sample rate: 44100 (0xAC44) -> 20 bits
        // channels: 2 (value 1 since it's channels-1) -> 3 bits
        // bits per sample: 16 (value 15 since it's bits-1) -> 5 bits
        // total samples: 441000 (10 seconds) -> 36 bits

        // 44100 = 1010110001000100
        // 20 bits: 0000 1010 1100 0100 0100
        // 3 bits channels: 001
        // 5 bits bitdepth: 01111
        // 36 bits total samples: 0000 0000 0000 0000 0000 0110 1011 1010 1000

        streamInfo[10] = 0x0A.toByte()
        streamInfo[11] = 0xC4.toByte()
        streamInfo[12] = 0x42.toByte() // 0100 (rest of sample rate) | 001 (channels) | 0 (bitdepth start)
        streamInfo[13] = 0xF0.toByte() // 1111 (bit depth end) | 0000 (total samples start)
        streamInfo[14] = 0x00.toByte()
        streamInfo[15] = 0x06.toByte()
        streamInfo[16] = 0xBA.toByte()
        streamInfo[17] = 0xA8.toByte()

        outBytes.addAll(streamInfo.toList())

        // 3. VORBIS_COMMENT block (Type 4, length var, isLast = true)
        // We will add TITLE=Test Track and ARTIST=Test Artist
        val vendorString = "reference libFLAC 1.3.2 20170101"
        val comments = listOf("TITLE=Test Track", "ARTIST=Test Artist")

        val commentsSize = comments.sumOf { 4 + it.toByteArray(StandardCharsets.UTF_8).size }
        val vorbisCommentLength = 4 + vendorString.toByteArray(StandardCharsets.UTF_8).size + 4 + commentsSize

        // Header byte (0x84: 1000 0100 -> isLast = true, type = 4)
        outBytes.add(0x84.toByte())
        // Length 3 bytes
        outBytes.add((vorbisCommentLength ushr 16).toByte())
        outBytes.add((vorbisCommentLength ushr 8).toByte())
        outBytes.add((vorbisCommentLength).toByte())

        // VORBIS_COMMENT data
        val buffer = ByteBuffer.allocate(vorbisCommentLength).order(ByteOrder.LITTLE_ENDIAN)
        // Vendor length & string
        val vendorBytes = vendorString.toByteArray(StandardCharsets.UTF_8)
        buffer.putInt(vendorBytes.size)
        buffer.put(vendorBytes)
        // User comment count
        buffer.putInt(comments.size)
        for (comment in comments) {
            val cb = comment.toByteArray(StandardCharsets.UTF_8)
            buffer.putInt(cb.size)
            buffer.put(cb)
        }

        outBytes.addAll(buffer.array().toList())

        validFlacFile.writeBytes(outBytes.toByteArray())

        val result = LocalMusicScanner.parseFlacFile(validFlacFile)

        assertNotNull(result)
        assertEquals(44100, result!!.sampleRate)
        assertEquals(2, result.channels)
        assertEquals(16, result.bitDepth)
        assertEquals(441000L, result.totalSamples)
        assertEquals(10000L, result.durationMs)
        assertEquals("Test Track", result.tags["TITLE"])
        assertEquals("Test Artist", result.tags["ARTIST"])
    }
}
