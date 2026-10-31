package com.vibeplay.core.scanner.metadata

import android.media.MediaMetadataRetriever
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AudioTagExtractorTest {

    private lateinit var tempDir: File
    private lateinit var cacheDir: File
    private lateinit var extractor: AudioTagExtractor

    @Before
    fun setup() {
        tempDir = File.createTempFile("audiotag", "dir")
        tempDir.delete()
        tempDir.mkdir()

        cacheDir = File(tempDir, "cache")
        cacheDir.mkdir()

        extractor = AudioTagExtractor()
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    private fun createDummyFlacFile(file: File) {
        FileOutputStream(file).use { fos ->
            // fLaC magic marker
            fos.write("fLaC".toByteArray(StandardCharsets.ISO_8859_1))

            // STREAMINFO block (Type 0, length 34 bytes)
            // Header: Type=0 (0x00), Length=34 (0x000022)
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x22))

            val streamInfo = ByteArray(34)
            // We need to set sample rate, channels, bit depth
            // Bytes 10-17:
            // 48000 Hz = 0xBB80 (20 bits)
            // 2 channels = 001 (3 bits)
            // 24 bit depth = 10111 (5 bits)
            // sampleRate (20) | channels (3) | bps (5)
            // 0xBB80 (20) = 1011 1011 1000 0000 0000
            // sampleRate = 48000 -> 0xBB80.
            // 20 bits: 0000 1011 1011 1000 0000 -> 0x0B, 0xB8, 0x0_

            // To simplify, let's just write the specific bytes
            // byte 10: sampleRate >> 12 = 48000 >> 12 = 11 = 0x0B
            streamInfo[10] = 0x0B.toByte()
            // byte 11: (sampleRate >> 4) & 0xFF = (48000 >> 4) & 0xFF = 3000 & 0xFF = 0xB8
            streamInfo[11] = 0xB8.toByte()
            // byte 12: ((sampleRate & 0x0F) << 4) | (((channels-1) & 0x07) << 1) | (((bitDepth-1) >> 4) & 0x01)
            // sampleRate & 0x0F = 0
            // channels - 1 = 1 -> 1 << 1 = 2
            // bitDepth - 1 = 23 -> 23 >> 4 = 1
            // 0 | 2 | 1 = 3 -> 0x03
            streamInfo[12] = 0x03.toByte()
            // byte 13: ((bitDepth-1) & 0x0F) << 4
            // 23 & 0x0F = 7 -> 7 << 4 = 112 -> 0x70
            streamInfo[13] = 0x70.toByte()

            fos.write(streamInfo)

            // VORBIS_COMMENT block (Type 4, length dynamic)
            // Header: Type=4 (0x04) but with Last-metadata-block flag (0x80)
            // So Type = 0x84
            val vendorString = "dummy vendor"
            val vendorBytes = vendorString.toByteArray(StandardCharsets.UTF_8)

            val comments = listOf(
                "TITLE=Test Title",
                "ALBUMARTIST=Test Artist",
                "DATE=2023",
                "GENRE=Electronic",
                "BPM=128",
                "REPLAYGAIN_TRACK_GAIN=-5.5 dB",
                "REPLAYGAIN_ALBUM_GAIN=-4.2 dB"
            )

            var commentsLength = 4 // user comment list length
            for (c in comments) {
                commentsLength += 4 + c.toByteArray(StandardCharsets.UTF_8).size
            }

            val blockLength = 4 + vendorBytes.size + commentsLength

            fos.write(byteArrayOf(0x84.toByte(), (blockLength shr 16).toByte(), (blockLength shr 8).toByte(), blockLength.toByte()))

            val buffer = ByteBuffer.allocate(blockLength).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(vendorBytes.size)
            buffer.put(vendorBytes)
            buffer.putInt(comments.size)
            for (c in comments) {
                val cBytes = c.toByteArray(StandardCharsets.UTF_8)
                buffer.putInt(cBytes.size)
                buffer.put(cBytes)
            }

            fos.write(buffer.array())
        }
    }

    @Test
    fun `extractMetadata returns default when file does not exist`() {
        val file = File(tempDir, "nonexistent.mp3")
        val metadata = extractor.extractMetadata(file, cacheDir)

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
    fun `extractMetadata parses FLAC streaminfo and vorbis comments`() {
        val file = File(tempDir, "test.flac")
        createDummyFlacFile(file)

        val dataSource = DataSource.toDataSource(file.absolutePath)
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_TITLE, "MMR Title")
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, "MMR Artist")

        val metadata = extractor.extractMetadata(file, cacheDir)

        assertEquals("Test Title", metadata.trackTitle)
        assertEquals("Test Artist", metadata.albumArtist)
        assertEquals("2023", metadata.year)
        assertEquals("Electronic", metadata.genre)
        assertEquals(128, metadata.bpm)
        assertEquals(-5.5, metadata.trackGainDb)
        assertEquals(-4.2, metadata.albumGainDb)
        assertEquals(48000, metadata.sampleRateHz)
        assertEquals(24, metadata.bitDepth)
        assertEquals(2, metadata.channelCount)
    }

    @Test
    fun `extractMetadata falls back to MediaMetadataRetriever for non-FLAC files`() {
        val file = File(tempDir, "test.mp3")
        file.createNewFile()

        val dataSource = DataSource.toDataSource(file.absolutePath)
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_TITLE, "MP3 Title")
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, "MP3 Artist")
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_YEAR, "2024")
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_GENRE, "Pop")
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_SAMPLERATE, "96000")

        val metadata = extractor.extractMetadata(file, cacheDir)

        assertEquals("MP3 Title", metadata.trackTitle)
        assertEquals("MP3 Artist", metadata.albumArtist)
        assertEquals("2024", metadata.year)
        assertEquals("Pop", metadata.genre)
        assertEquals(96000, metadata.sampleRateHz)
    }

    @Test
    fun `extractMetadata uses ARTIST if ALBUMARTIST is not available in MMR`() {
        val file = File(tempDir, "test.m4a")
        file.createNewFile()

        val dataSource = DataSource.toDataSource(file.absolutePath)
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_ARTIST, "Fallback Artist")

        val metadata = extractor.extractMetadata(file, cacheDir)

        assertEquals("Fallback Artist", metadata.albumArtist)
    }

    @Test
    fun `extractMetadata uses DATE if YEAR is not available in MMR`() {
        val file = File(tempDir, "test.ogg")
        file.createNewFile()

        val dataSource = DataSource.toDataSource(file.absolutePath)
        ShadowMediaMetadataRetriever.addMetadata(dataSource, MediaMetadataRetriever.METADATA_KEY_DATE, "2022-01-01")

        val metadata = extractor.extractMetadata(file, cacheDir)

        assertEquals("2022-01-01", metadata.year)
    }

    @Test
    fun `extractReplayGain parses correct double values`() {
        // We have to test it through the flac parse path since extractReplayGain is private
        val file = File(tempDir, "gain_test.flac")
        FileOutputStream(file).use { fos ->
            fos.write("fLaC".toByteArray(StandardCharsets.ISO_8859_1))
            fos.write(byteArrayOf(0x00, 0x00, 0x00, 0x22))
            val streamInfo = ByteArray(34)
            streamInfo[10] = 0x0B.toByte()
            streamInfo[11] = 0xB8.toByte()
            streamInfo[12] = 0x03.toByte()
            streamInfo[13] = 0x70.toByte()
            fos.write(streamInfo)

            val vendorBytes = "vendor".toByteArray(StandardCharsets.UTF_8)
            val comments = listOf(
                "REPLAYGAIN_TRACK_GAIN=+2.5",
                "REPLAYGAIN_ALBUM_GAIN=-10.23 dB"
            )

            var commentsLength = 4
            for (c in comments) {
                commentsLength += 4 + c.toByteArray(StandardCharsets.UTF_8).size
            }

            val blockLength = 4 + vendorBytes.size + commentsLength
            fos.write(byteArrayOf(0x84.toByte(), (blockLength shr 16).toByte(), (blockLength shr 8).toByte(), blockLength.toByte()))

            val buffer = ByteBuffer.allocate(blockLength).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(vendorBytes.size)
            buffer.put(vendorBytes)
            buffer.putInt(comments.size)
            for (c in comments) {
                val cBytes = c.toByteArray(StandardCharsets.UTF_8)
                buffer.putInt(cBytes.size)
                buffer.put(cBytes)
            }

            fos.write(buffer.array())
        }

        val metadata = extractor.extractMetadata(file, cacheDir)

        assertEquals(2.5, metadata.trackGainDb!!, 0.001)
        assertEquals(-10.23, metadata.albumGainDb!!, 0.001)
    }
}
