package com.vibeplay.core.scanner

import com.vibeplay.core.scanner.metadata.AudioTagExtractor
import com.vibeplay.core.scanner.metadata.ReplayGainNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import kotlin.math.abs
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioTagExtractorTest {

    @Test
    fun testReplayGainNormalizer_ScaleFactor() {
        val scaleFactor1 = ReplayGainNormalizer.computeScaleFactor(-5.0)
        assertEquals(0.89125, scaleFactor1, 0.0001)

        val scaleFactor2 = ReplayGainNormalizer.computeScaleFactor(0.0)
        assertEquals(1.412537, scaleFactor2, 0.0001)

        val scaleFactor3 = ReplayGainNormalizer.computeScaleFactor(1.0)
        assertEquals(1.412537, scaleFactor3, 0.0001)

        val scaleFactor4 = ReplayGainNormalizer.computeScaleFactor(-10.0)
        assertEquals(0.501187, scaleFactor4, 0.0001)
    }

    @Test
    fun testAudioTagExtractor_extractMetadata_FileNotExists() {
        val extractor = AudioTagExtractor()
        val result = extractor.extractMetadata(File("does_not_exist.flac"), null)
        assertNull(result.trackTitle)
        assertNull(result.albumArtist)
        assertEquals(44100, result.sampleRateHz)
        assertEquals(16, result.bitDepth)
    }

    @Test
    fun testExtractMetadata_ValidFlac() {
        val tempFile = File.createTempFile("test_valid", ".flac")
        tempFile.deleteOnExit()

        RandomAccessFile(tempFile, "rw").use { raf ->
            // fLaC magic
            raf.write("fLaC".toByteArray(Charsets.ISO_8859_1))

            // STREAMINFO block
            // isLast=false(0), blockType=0(0) => headerByte = 0x00
            raf.write(0x00)
            // length = 34 bytes (STREAMINFO is exactly 34 bytes long usually)
            raf.write(byteArrayOf(0x00, 0x00, 34))

            // 34 bytes of streaminfo
            val streamInfo = ByteArray(34)

            // sampleRate=48000(0xBB80) 20 bits
            // channels=2 (0x01) 3 bits
            // bitDepth=16 (0x0F) 5 bits
            // totalSamples = 0 (36 bits)
            streamInfo[10] = 0x0B
            streamInfo[11] = 0xB8.toByte()
            streamInfo[12] = 0x02
            streamInfo[13] = 0xF0.toByte()

            raf.write(streamInfo)

            // VORBIS_COMMENT block
            // isLast=true(1), blockType=4(4) => headerByte = 0x80 | 0x04 = 0x84
            raf.write(0x84)

            val vendorString = "test vendor"
            val comments = listOf(
                "TITLE=Test Title",
                "ARTIST=Test Artist",
                "ALBUM=Test Album",
                "DATE=2024",
                "GENRE=Electronic",
                "BPM=120",
                "REPLAYGAIN_TRACK_GAIN=-5.5 dB",
                "REPLAYGAIN_ALBUM_GAIN=-4.0 dB"
            )

            val vendorBytes = vendorString.toByteArray(Charsets.UTF_8)
            var commentDataLength = 4 + vendorBytes.size + 4
            val commentBytesList = comments.map { it.toByteArray(Charsets.UTF_8) }
            for (cb in commentBytesList) {
                commentDataLength += 4 + cb.size
            }

            val lengthBytes = byteArrayOf(
                ((commentDataLength ushr 16) and 0xFF).toByte(),
                ((commentDataLength ushr 8) and 0xFF).toByte(),
                (commentDataLength and 0xFF).toByte()
            )
            raf.write(lengthBytes)

            val buffer = ByteBuffer.allocate(commentDataLength).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(vendorBytes.size)
            buffer.put(vendorBytes)
            buffer.putInt(comments.size)
            for (cb in commentBytesList) {
                buffer.putInt(cb.size)
                buffer.put(cb)
            }
            raf.write(buffer.array())
        }

        val extractor = AudioTagExtractor()
        val result = extractor.extractMetadata(tempFile, null)

        assertEquals("Test Title", result.trackTitle)
        assertEquals("Test Artist", result.albumArtist)
        assertEquals("2024", result.year)
        assertEquals("Electronic", result.genre)
        assertEquals(120, result.bpm)
        assertEquals(48000, result.sampleRateHz)
        assertEquals(16, result.bitDepth)
        assertEquals(2, result.channelCount)
        assertEquals(-5.5, result.trackGainDb!!, 0.001)
        assertEquals(-4.0, result.albumGainDb!!, 0.001)
    }

    @Test
    fun testExtractMetadata_OtherExtension() {
        val tempFile = File.createTempFile("test_other", ".mp3")
        tempFile.deleteOnExit()

        // Write some dummy data so it's a valid file
        FileOutputStream(tempFile).use { fos ->
            fos.write("dummy content".toByteArray())
        }

        val extractor = AudioTagExtractor()
        val result = extractor.extractMetadata(tempFile, null)

        // Because MediaMetadataRetriever isn't mocked/shadowed with real implementation here,
        // it falls back to defaults for non-FLAC files
        assertNull(result.trackTitle)
        assertNull(result.albumArtist)
        assertEquals(44100, result.sampleRateHz)
    }
}
