package com.vibeplay.core.dedup

import com.vibeplay.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackDeduplicatorTest {

    private val deduplicator = TrackDeduplicator()

    private fun createTrack(
        id: String,
        title: String,
        artist: String,
        durationMs: Long,
        format: String = "MP3",
        sampleRate: String = "44.1kHz",
        bitDepth: String = "16-bit",
        localUri: String? = null
    ): Track {
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = "Test Album",
            duration = "0:00",
            durationMs = durationMs,
            bpm = 120,
            key = "C",
            energy = 0.5,
            danceability = 0.5,
            crateId = "crate1",
            format = format,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            localUri = localUri
        )
    }

    @Test
    fun testExactMatches() {
        val tracks = listOf(
            createTrack("1", "Song A", "Artist A", 180000),
            createTrack("2", "Song A", "Artist A", 180000)
        )

        val report = deduplicator.deduplicate(tracks)
        assertEquals(1, report.groups.size)
        assertEquals(2, report.groups[0].candidates.size)
    }

    @Test
    fun testFuzzyMatches() {
        // Levenshtein distance 2
        val tracks = listOf(
            createTrack("1", "Song ABC", "Artist", 180000),
            createTrack("2", "Song ADC", "Artist", 180000) // Dist 1
        )

        val report = deduplicator.deduplicate(tracks)
        assertEquals(1, report.groups.size)
    }

    @Test
    fun testFuzzyMatches_TooDifferent() {
        // Levenshtein distance 3
        val tracks = listOf(
            createTrack("1", "Song ABC", "Artist", 180000),
            createTrack("2", "Song XYZ", "Artist", 180000)
        )

        val report = deduplicator.deduplicate(tracks)
        assertEquals(0, report.groups.size)
    }

    @Test
    fun testDurationTolerance() {
        val tracks = listOf(
            createTrack("1", "Song A", "Artist A", 180000),
            createTrack("2", "Song A", "Artist A", 182000), // Within 3s
            createTrack("3", "Song A", "Artist A", 184000)  // Outside 3s
        )

        val report = deduplicator.deduplicate(tracks)
        assertEquals(1, report.groups.size)
        assertEquals(2, report.groups[0].candidates.size)
        assertTrue(report.groups[0].candidates.any { it.trackId == "1" })
        assertTrue(report.groups[0].candidates.any { it.trackId == "2" })
    }

    @Test
    fun testAudiophileRanking_Format() {
        val tracks = listOf(
            createTrack("1", "Song", "Artist", 180000, format = "MP3"),
            createTrack("2", "Song", "Artist", 180000, format = "FLAC")
        )

        val report = deduplicator.deduplicate(tracks)
        val best = report.groups[0].candidates.first { it.isBestQuality }
        assertEquals("2", best.trackId)
    }

    @Test
    fun testAudiophileRanking_SampleRate() {
        val tracks = listOf(
            createTrack("1", "Song", "Artist", 180000, format = "FLAC", sampleRate = "44.1kHz"),
            createTrack("2", "Song", "Artist", 180000, format = "FLAC", sampleRate = "192kHz")
        )

        val report = deduplicator.deduplicate(tracks)
        val best = report.groups[0].candidates.first { it.isBestQuality }
        assertEquals("2", best.trackId)
    }

    @Test
    fun testAudiophileRanking_BitDepth() {
        val tracks = listOf(
            createTrack("1", "Song", "Artist", 180000, format = "FLAC", sampleRate = "192kHz", bitDepth = "16-bit"),
            createTrack("2", "Song", "Artist", 180000, format = "FLAC", sampleRate = "192kHz", bitDepth = "24-bit")
        )

        val report = deduplicator.deduplicate(tracks)
        val best = report.groups[0].candidates.first { it.isBestQuality }
        assertEquals("2", best.trackId)
    }

    @Test
    fun testTieBreaker_ShorterPath() {
        val tracks = listOf(
            createTrack("1", "Song", "Artist", 180000, format = "FLAC", sampleRate = "192kHz", bitDepth = "24-bit", localUri = "/a/b/c/long_path.flac"),
            createTrack("2", "Song", "Artist", 180000, format = "FLAC", sampleRate = "192kHz", bitDepth = "24-bit", localUri = "/a/short.flac")
        )

        val report = deduplicator.deduplicate(tracks)
        val best = report.groups[0].candidates.first { it.isBestQuality }
        assertEquals("2", best.trackId)
    }

    @Test
    fun testTieBreaker_AlphabeticalPath() {
        val tracks = listOf(
            createTrack("1", "Song", "Artist", 180000, format = "FLAC", sampleRate = "192kHz", bitDepth = "24-bit", localUri = "/a/b/c.flac"),
            createTrack("2", "Song", "Artist", 180000, format = "FLAC", sampleRate = "192kHz", bitDepth = "24-bit", localUri = "/a/b/a.flac")
        )

        val report = deduplicator.deduplicate(tracks)
        val best = report.groups[0].candidates.first { it.isBestQuality }
        assertEquals("2", best.trackId)
    }
}
