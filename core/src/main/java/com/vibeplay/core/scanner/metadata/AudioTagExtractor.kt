package com.vibeplay.core.scanner.metadata

import android.media.MediaMetadataRetriever
import com.vibeplay.core.scanner.LocalMusicScanner
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AudioTagExtractor {

    fun extractMetadata(audioFile: File, cacheDir: File?): AudioMetadata {
        if (!audioFile.exists() || !audioFile.isFile) {
            return AudioMetadata(
                albumArtist = null,
                trackTitle = null,
                year = null,
                genre = null,
                bpm = null,
                trackGainDb = null,
                albumGainDb = null,
                sampleRateHz = 44100,
                bitDepth = 16,
                channelCount = 2,
                artworkCachePath = null
            )
        }

        var albumArtist: String? = null
        var trackTitle: String? = null
        var year: String? = null
        var genre: String? = null
        var bpm: Int? = null
        var trackGainDb: Double? = null
        var albumGainDb: Double? = null
        var sampleRateHz: Int = 44100
        var bitDepth: Int = 16
        var channelCount: Int = 2
        var rawArtworkBytes: ByteArray? = null

        // 1. If it's a FLAC file, extract streaminfo and vorbis comments using our native FLAC parser
        if (audioFile.extension.equals("flac", ignoreCase = true)) {
            val flacInfo = LocalMusicScanner.parseFlacFile(audioFile)
            if (flacInfo != null) {
                sampleRateHz = flacInfo.sampleRate
                bitDepth = flacInfo.bitDepth
                channelCount = flacInfo.channels

                val tags = flacInfo.tags
                trackTitle = tags["TITLE"]
                albumArtist = tags["ALBUMARTIST"] ?: tags["ALBUM_ARTIST"] ?: tags["ARTIST"]
                year = tags["DATE"] ?: tags["YEAR"]
                genre = tags["GENRE"]
                bpm = tags["BPM"]?.toIntOrNull()
                trackGainDb = extractReplayGain(tags["REPLAYGAIN_TRACK_GAIN"])
                albumGainDb = extractReplayGain(tags["REPLAYGAIN_ALBUM_GAIN"])
            }
        }

        // 2. Use Android framework MediaMetadataRetriever to supplement tags and extract artwork
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFile.absolutePath)

            if (trackTitle.isNullOrBlank()) {
                trackTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            }
            if (albumArtist.isNullOrBlank()) {
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            }
            if (year.isNullOrBlank()) {
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            }
            if (genre.isNullOrBlank()) {
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            }

            // Extract sample rate if not already parsed
            if (sampleRateHz == 44100) {
                val srStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                val parsedSr = srStr?.toIntOrNull()
                if (parsedSr != null && parsedSr > 0) {
                    sampleRateHz = parsedSr
                }
            }

            rawArtworkBytes = retriever.embeddedPicture
            retriever.release()
        } catch (_: Exception) {
            // Non-critical: metadata retriever fallback
        }

        // 3. Cache extracted embedded album artwork
        var artworkCachePath: String? = null
        if (cacheDir != null && rawArtworkBytes != null && rawArtworkBytes.isNotEmpty()) {
            val fileName = "${UUID.randomUUID()}.jpg"
            val cachedFile = File(cacheDir, fileName)
            try {
                FileOutputStream(cachedFile).use { fos ->
                    fos.write(rawArtworkBytes)
                }
                artworkCachePath = cachedFile.absolutePath
            } catch (_: Exception) {
                // Ignore caching errors
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
        val regex = Regex("([-+]?\\d*\\.?\\d+)")
        val matchResult = regex.find(gainString)
        return matchResult?.value?.toDoubleOrNull()
    }
}
