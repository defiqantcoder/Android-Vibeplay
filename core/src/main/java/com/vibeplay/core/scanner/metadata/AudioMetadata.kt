package com.vibeplay.core.scanner.metadata

data class AudioMetadata(
    val albumArtist: String?,
    val trackTitle: String?,
    val year: String?,
    val genre: String?,
    val bpm: Int?,
    val trackGainDb: Double?,
    val albumGainDb: Double?,
    val sampleRateHz: Int,
    val bitDepth: Int?,
    val channelCount: Int,
    val artworkCachePath: String?
)
