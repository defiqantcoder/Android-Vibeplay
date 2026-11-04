package com.vibeplay.core.scanner

import com.vibeplay.core.model.Track
import java.io.File
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.sin

object LocalMusicScanner {

    data class FlacInfo(
        val sampleRate: Int,
        val bitDepth: Int,
        val channels: Int,
        val totalSamples: Long,
        val durationMs: Long,
        val tags: Map<String, String>
    )

    /**
     * Pure Kotlin FLAC binary header parser without any external native dependencies.
     * Parses the 'fLaC' marker, STREAMINFO block (sample rate, bit depth, channels, total samples),
     * and METADATA_BLOCK_VORBIS_COMMENT block (TITLE, ARTIST, ALBUM, DATE, GENRE, BPM, INITIALKEY).
     */
    fun parseFlacFile(file: File): FlacInfo? {
        if (!file.exists() || file.length() < 42) return null

        return file.inputStream().use { parseFlacStream(it) }
    }

    /** Parse FLAC metadata from a stream without closing it. */
    fun parseFlacStream(input: InputStream): FlacInfo? {
        val stream = DataInputStream(input)
        try {
            val magic = ByteArray(4)
            stream.readFully(magic)
            if (String(magic, StandardCharsets.ISO_8859_1) != "fLaC") {
                return null
            }

            var sampleRate = 44100
            var bitDepth = 16
            var channels = 2
            var totalSamples = 0L
            var durationMs = 0L
            val tags = mutableMapOf<String, String>()

            var isLast = false
            var hasStreamInfo = false
            while (!isLast) {
                val headerByte = stream.read()
                if (headerByte == -1) throw EOFException()
                isLast = (headerByte and 0x80) != 0
                val blockType = headerByte and 0x7F

                val lengthBytes = ByteArray(3)
                stream.readFully(lengthBytes)
                val blockLength = ((lengthBytes[0].toInt() and 0xFF) shl 16) or
                        ((lengthBytes[1].toInt() and 0xFF) shl 8) or
                        (lengthBytes[2].toInt() and 0xFF)

                when (blockType) {
                    0 -> { // STREAMINFO
                        val streamInfo = ByteArray(blockLength)
                        stream.readFully(streamInfo)
                        if (streamInfo.size == 34) {
                            hasStreamInfo = true
                            // Bytes 10-17 contain sample rate (20 bits), channels (3 bits), bits per sample (5 bits), total samples (36 bits)
                            val b10 = streamInfo[10].toInt() and 0xFF
                            val b11 = streamInfo[11].toInt() and 0xFF
                            val b12 = streamInfo[12].toInt() and 0xFF
                            val b13 = streamInfo[13].toInt() and 0xFF
                            val b14 = streamInfo[14].toInt() and 0xFF
                            val b15 = streamInfo[15].toInt() and 0xFF
                            val b16 = streamInfo[16].toInt() and 0xFF
                            val b17 = streamInfo[17].toInt() and 0xFF

                            sampleRate = (b10 shl 12) or (b11 shl 4) or (b12 ushr 4)
                            channels = ((b12 ushr 1) and 0x07) + 1
                            bitDepth = (((b12 and 0x01) shl 4) or (b13 ushr 4)) + 1

                            val top4Bits = (b13 and 0x0F).toLong()
                            val lower32Bits = ((b14.toLong() and 0xFF) shl 24) or
                                    ((b15.toLong() and 0xFF) shl 16) or
                                    ((b16.toLong() and 0xFF) shl 8) or
                                    (b17.toLong() and 0xFF)

                            totalSamples = (top4Bits shl 32) or (lower32Bits and 0xFFFFFFFFL)
                            if (sampleRate > 0) {
                                durationMs = (totalSamples * 1000L) / sampleRate
                            }
                        }
                    }
                    4 -> { // VORBIS_COMMENT
                        val commentData = ByteArray(blockLength)
                        stream.readFully(commentData)
                        val buffer = ByteBuffer.wrap(commentData).order(ByteOrder.LITTLE_ENDIAN)

                        if (buffer.remaining() >= 4) {
                            val vendorLength = buffer.int
                            if (vendorLength >= 0 && buffer.remaining() >= vendorLength) {
                                buffer.position(buffer.position() + vendorLength)
                                if (buffer.remaining() >= 4) {
                                    val userCommentCount = buffer.int
                                    for (i in 0 until userCommentCount) {
                                        if (buffer.remaining() < 4) break
                                        val len = buffer.int
                                        if (len < 0 || buffer.remaining() < len) break
                                        val strBytes = ByteArray(len)
                                        buffer.get(strBytes)
                                        val commentStr = String(strBytes, StandardCharsets.UTF_8)
                                        val eqIdx = commentStr.indexOf('=')
                                        if (eqIdx > 0) {
                                            val key = commentStr.substring(0, eqIdx).trim().uppercase()
                                            val value = commentStr.substring(eqIdx + 1).trim()
                                            tags[key] = value
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Skip other metadata blocks (PICTURE, PADDING, CUESHEET, etc.)
                        var remaining = blockLength
                        val buffer = ByteArray(8192)
                        while (remaining > 0) {
                            val count = stream.read(buffer, 0, minOf(remaining, buffer.size))
                            if (count < 0) throw EOFException()
                            remaining -= count
                        }
                    }
                }
            }

            if (!hasStreamInfo) return null
            return FlacInfo(
                sampleRate = sampleRate,
                bitDepth = bitDepth,
                channels = channels,
                totalSamples = totalSamples,
                durationMs = durationMs,
                tags = tags
            )
        } catch (_: EOFException) {
            return null
        }
    }

    /**
     * Recursively scan a local audio directory and convert FLAC files into domain Track models.
     */
    fun scanDirectory(rootDir: File): List<Track> {
        if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()

        val results = mutableListOf<Track>()
        val flacFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("flac", ignoreCase = true) }
            .toList()

        for ((index, file) in flacFiles.withIndex()) {
            try {
                val info = parseFlacFile(file)
                if (info != null) {
                    results.add(trackFromFlac(info, file.name, file.parentFile?.name, file.absolutePath, "local-flac-$index"))
                }
            } catch (e: Exception) {
                // Ignore corrupt individual file
            }
        }

        return results
    }

    /** Build the same library model for filesystem and document-provider FLAC files. */
    fun trackFromFlac(info: FlacInfo, name: String, parentName: String?, uri: String, id: String): Track {
        val title = info.tags["TITLE"] ?: name.substringBeforeLast('.', name)
        val artist = info.tags["ARTIST"] ?: parentName ?: "Unknown Artist"
        val album = info.tags["ALBUM"] ?: parentName ?: "Unknown Album"
        val bpm = info.tags["BPM"]?.toIntOrNull() ?: estimateBpmFromHash(name)
        val key = info.tags["INITIALKEY"] ?: estimateKeyFromHash(name)
        val minutes = (info.durationMs / 1000) / 60
        val seconds = (info.durationMs / 1000) % 60
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = String.format("%d:%02d", minutes, seconds),
            durationMs = info.durationMs,
            bpm = bpm,
            key = key,
            energy = estimateEnergy(bpm),
            danceability = estimateDanceability(bpm),
            crateId = "crate-local",
            format = "FLAC",
            sampleRate = String.format("%.1f kHz", info.sampleRate / 1000.0),
            bitDepth = "${info.bitDepth}-bit",
            localUri = uri,
            waveforms = generateDeterministicWaveform(name, 24)
        )
    }

    private fun estimateBpmFromHash(name: String): Int {
        val hash = abs(name.hashCode())
        return 80 + (hash % 65) // 80 - 145 bpm range
    }

    private fun estimateKeyFromHash(name: String): String {
        val keys = listOf("1A", "1B", "2A", "2B", "3A", "3B", "4A", "4B", "5A", "5B", "6A", "6B",
            "7A", "7B", "8A", "8B", "9A", "9B", "10A", "10B", "11A", "11B", "12A", "12B")
        val hash = abs(name.hashCode())
        return keys[hash % keys.size]
    }

    private fun estimateEnergy(bpm: Int): Double {
        return ((bpm - 60).toDouble() / 100.0).coerceIn(0.2, 0.98)
    }

    private fun estimateDanceability(bpm: Int): Double {
        return (1.0 - abs(bpm - 124) / 80.0).coerceIn(0.3, 0.95)
    }

    private fun generateDeterministicWaveform(seed: String, bars: Int): List<Float> {
        val hash = abs(seed.hashCode())
        return (0 until bars).map { i ->
            val v = (sin((hash + i * 17).toDouble()) + 1.0) / 2.0
            (0.15 + v * 0.85).toFloat().coerceIn(0.1f, 1.0f)
        }
    }

    private fun abs(value: Int): Int = if (value < 0) -value else value
}
