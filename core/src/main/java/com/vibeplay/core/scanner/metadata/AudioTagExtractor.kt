package com.vibeplay.core.scanner.metadata

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AudioTagExtractor {

    fun extractMetadata(audioFile: File, cacheDir: File?): AudioMetadata {
        val audioFileObj = AudioFileIO.read(audioFile)
        val tag = audioFileObj.tag
        val header = audioFileObj.audioHeader

        val albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST).takeUnless { it.isNullOrBlank() }
            ?: tag?.getFirst(FieldKey.ARTIST).takeUnless { it.isNullOrBlank() }
        val trackTitle = tag?.getFirst(FieldKey.TITLE).takeUnless { it.isNullOrBlank() }
        val year = tag?.getFirst(FieldKey.YEAR).takeUnless { it.isNullOrBlank() }
        val genre = tag?.getFirst(FieldKey.GENRE).takeUnless { it.isNullOrBlank() }
        val bpm = tag?.getFirst(FieldKey.BPM)?.toIntOrNull()

        // Extract ReplayGain
        val trackGainDb = extractReplayGain(tag?.getFirst("REPLAYGAIN_TRACK_GAIN"))
        val albumGainDb = extractReplayGain(tag?.getFirst("REPLAYGAIN_ALBUM_GAIN"))

        // Extract format attributes
        val sampleRateHz = header.sampleRateAsNumber

        // Handle bit depth nicely (fallback to 16 for lossy if jaudiotagger returns 0 or empty)
        val headerBitDepth = header.bitsPerSample
        val bitDepth = if (headerBitDepth > 0) headerBitDepth else 16

        val channelCount = header.channels.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 2 // Default to stereo if not readable

        // Extract Artwork
        var artworkCachePath: String? = null
        if (cacheDir != null && tag != null) {
            val artworkList = tag.artworkList
            if (artworkList.isNotEmpty()) {
                val artwork = artworkList.first()
                val binaryData = artwork.binaryData
                if (binaryData != null && binaryData.isNotEmpty()) {
                    val mimeType = artwork.mimeType ?: ""
                    val extension = when {
                        mimeType.contains("png") -> "png"
                        mimeType.contains("webp") -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "${UUID.randomUUID()}.$extension"
                    val cachedFile = File(cacheDir, fileName)
                    try {
                        FileOutputStream(cachedFile).use { fos ->
                            fos.write(binaryData)
                        }
                        artworkCachePath = cachedFile.absolutePath
                    } catch (e: Exception) {
                        // Failed to save artwork, proceed without it
                    }
                }
            }
        }

        return AudioMetadata(
            albumArtist = albumArtist,
            trackTitle = trackTitle,
            year = year,
            genre = genre,
            bpm = bpm,
            trackGainDb = trackGainDb,
            albumGainDb = albumGainDb,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            channelCount = channelCount,
            artworkCachePath = artworkCachePath
        )
    }

    private fun extractReplayGain(gainString: String?): Double? {
        if (gainString.isNullOrBlank()) return null
        // Standard format is something like "-6.45 dB" or "+1.23 dB"
        val regex = Regex("([-+]?\\d*\\.?\\d+)")
        val matchResult = regex.find(gainString)
        return matchResult?.value?.toDoubleOrNull()
    }
}
