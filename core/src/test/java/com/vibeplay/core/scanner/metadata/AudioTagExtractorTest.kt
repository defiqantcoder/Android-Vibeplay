package com.vibeplay.core.scanner.metadata

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class AudioTagExtractorTest {

    private lateinit var tempDir: File
    private lateinit var flacFile: File
    private lateinit var nonFlacFile: File
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("audio_test").toFile()
        flacFile = File(tempDir, "test.flac")
        nonFlacFile = File(tempDir, "test.mp3")
        cacheDir = File(tempDir, "cache")
        cacheDir.mkdirs()
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testExtractMetadata_MissingFile() {
        val extractor = AudioTagExtractor()
        val file = File(tempDir, "missing.flac")
        val metadata = extractor.extractMetadata(file, null)

        assertNull(metadata.albumArtist)
        assertNull(metadata.trackTitle)
        assertNull(metadata.year)
        assertNull(metadata.genre)
        assertNull(metadata.bpm)
        assertNull(metadata.trackGainDb)
        assertNull(metadata.albumGainDb)
        assertEquals(44100, metadata.sampleRateHz)
        assertEquals(16, metadata.bitDepth)
        assertEquals(2, metadata.channelCount)
        assertNull(metadata.artworkCachePath)
    }

    @Test
    fun testExtractMetadata_NotAFile() {
        val extractor = AudioTagExtractor()
        val metadata = extractor.extractMetadata(tempDir, null)

        assertNull(metadata.albumArtist)
        assertNull(metadata.trackTitle)
        assertNull(metadata.year)
        assertNull(metadata.genre)
        assertNull(metadata.bpm)
        assertNull(metadata.trackGainDb)
        assertNull(metadata.albumGainDb)
        assertEquals(44100, metadata.sampleRateHz)
        assertEquals(16, metadata.bitDepth)
        assertEquals(2, metadata.channelCount)
        assertNull(metadata.artworkCachePath)
    }

    @Test
    fun testExtractMetadata_FlacFile() {
        createFlacFile(
            file = flacFile,
            sampleRate = 48000,
            channels = 1,
            bitDepth = 24,
            tags = mapOf(
                "TITLE" to "Test Title",
                "ARTIST" to "Test Artist",
                "ALBUMARTIST" to "Test Album Artist",
                "DATE" to "2023",
                "GENRE" to "Electronic",
                "BPM" to "120",
                "REPLAYGAIN_TRACK_GAIN" to "-5.0 dB",
                "REPLAYGAIN_ALBUM_GAIN" to "-4.5 dB"
            )
        )

        val extractor = AudioTagExtractor()
        // Run with mocked framework dependency, which is expected to fail Android's MediaMetadataRetriever,
        // but should parse FLAC natively!
        val metadata = extractor.extractMetadata(flacFile, cacheDir)

        assertEquals("Test Title", metadata.trackTitle)
        assertEquals("Test Album Artist", metadata.albumArtist)
        assertEquals("2023", metadata.year)
        assertEquals("Electronic", metadata.genre)
        assertEquals(120, metadata.bpm)
        assertEquals(-5.0, metadata.trackGainDb)
        assertEquals(-4.5, metadata.albumGainDb)
        assertEquals(48000, metadata.sampleRateHz)
        assertEquals(24, metadata.bitDepth)
        assertEquals(1, metadata.channelCount)
    }

    @Test
    fun testExtractMetadata_FlacFile_FallbackTags() {
        createFlacFile(
            file = flacFile,
            sampleRate = 44100,
            channels = 2,
            bitDepth = 16,
            tags = mapOf(
                "ALBUM_ARTIST" to "Alt Album Artist",
                "YEAR" to "2022"
            )
        )

        val extractor = AudioTagExtractor()
        val metadata = extractor.extractMetadata(flacFile, cacheDir)

        assertEquals("Alt Album Artist", metadata.albumArtist)
        assertEquals("2022", metadata.year)
    }

    @Test
    fun testExtractMetadata_FlacFile_ArtistFallback() {
        createFlacFile(
            file = flacFile,
            sampleRate = 44100,
            channels = 2,
            bitDepth = 16,
            tags = mapOf(
                "ARTIST" to "Artist Only"
            )
        )

        val extractor = AudioTagExtractor()
        val metadata = extractor.extractMetadata(flacFile, cacheDir)

        assertEquals("Artist Only", metadata.albumArtist)
    }

    @Test
    fun testExtractMetadata_FlacFile_BadReplayGain() {
        createFlacFile(
            file = flacFile,
            sampleRate = 44100,
            channels = 2,
            bitDepth = 16,
            tags = mapOf(
                "REPLAYGAIN_TRACK_GAIN" to "invalid db",
                "REPLAYGAIN_ALBUM_GAIN" to ""
            )
        )

        val extractor = AudioTagExtractor()
        val metadata = extractor.extractMetadata(flacFile, cacheDir)

        assertNull(metadata.trackGainDb)
        assertNull(metadata.albumGainDb)
    }

    @Test
    fun testExtractMetadata_NonFlacFile() {
        // Create a dummy mp3 file
        FileOutputStream(nonFlacFile).use { fos ->
            fos.write(byteArrayOf(0, 1, 2, 3))
        }

        val extractor = AudioTagExtractor()
        // Run with non-FLAC file, Android's MediaMetadataRetriever is expected to fail
        val metadata = extractor.extractMetadata(nonFlacFile, cacheDir)

        // Should return defaults/nulls for everything when Android MMR fails
        assertNull(metadata.albumArtist)
        assertNull(metadata.trackTitle)
        assertNull(metadata.year)
        assertNull(metadata.genre)
        assertNull(metadata.bpm)
        assertNull(metadata.trackGainDb)
        assertNull(metadata.albumGainDb)
        assertEquals(44100, metadata.sampleRateHz)
        assertEquals(16, metadata.bitDepth)
        assertEquals(2, metadata.channelCount)
        assertNull(metadata.artworkCachePath)
    }

    private fun createFlacFile(
        file: File,
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        tags: Map<String, String>
    ) {
        val out = ByteArrayOutputStream()
        out.write("fLaC".toByteArray(StandardCharsets.ISO_8859_1))

        out.write(0)
        val streamInfoLen = 34
        out.write(byteArrayOf(0, 0, streamInfoLen.toByte()))
        val streamInfo = ByteArray(streamInfoLen)
        val blockSize = 4096
        streamInfo[0] = (blockSize ushr 8).toByte()
        streamInfo[1] = blockSize.toByte()
        streamInfo[2] = (blockSize ushr 8).toByte()
        streamInfo[3] = blockSize.toByte()

        val sr20 = sampleRate and 0xFFFFF
        val ch3 = (channels - 1) and 0x07
        val bd5 = (bitDepth - 1) and 0x1F

        streamInfo[10] = (sr20 ushr 12).toByte()
        streamInfo[11] = ((sr20 ushr 4) and 0xFF).toByte()
        streamInfo[12] = (((sr20 and 0x0F) shl 4) or (ch3 shl 1) or ((bd5 ushr 4) and 0x01)).toByte()
        streamInfo[13] = (((bd5 and 0x0F) shl 4) or 0).toByte()

        out.write(streamInfo)

        out.write(0x84) // Final metadata block
        val vcOut = ByteArrayOutputStream()
        val vendorStr = "dummy"
        vcOut.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(vendorStr.length).array())
        vcOut.write(vendorStr.toByteArray(StandardCharsets.UTF_8))

        vcOut.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(tags.size).array())
        for ((k, v) in tags) {
            val commentStr = "$k=$v"
            val commentBytes = commentStr.toByteArray(StandardCharsets.UTF_8)
            vcOut.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(commentBytes.size).array())
            vcOut.write(commentBytes)
        }

        val vcBytes = vcOut.toByteArray()
        val vcLen = vcBytes.size
        out.write(byteArrayOf((vcLen ushr 16).toByte(), (vcLen ushr 8).toByte(), vcLen.toByte()))
        out.write(vcBytes)

        FileOutputStream(file).use { fos ->
            fos.write(out.toByteArray())
        }
    }
}
