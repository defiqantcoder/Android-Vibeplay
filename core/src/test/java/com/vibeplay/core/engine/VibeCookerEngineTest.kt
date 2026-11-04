package com.vibeplay.core.engine

import com.vibeplay.core.model.Track
import com.vibeplay.core.model.VibeSqlBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibeCookerEngineTest {

    private fun createTrack(
        id: String = "1",
        title: String = "Test Track",
        artist: String = "Test Artist",
        album: String = "Test Album",
        duration: String = "3:00",
        durationMs: Long = 180000,
        bpm: Int = 120,
        key: String = "8A",
        energy: Double = 0.5,
        danceability: Double = 0.5,
        crateId: String = "crate1",
        format: String = "FLAC",
        sampleRate: String = "44100",
        bitDepth: String = "16"
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        durationMs = durationMs,
        bpm = bpm,
        key = key,
        energy = energy,
        danceability = danceability,
        crateId = crateId,
        format = format,
        sampleRate = sampleRate,
        bitDepth = bitDepth
    )

    @Test
    fun `scoreAndRankTracks returns empty list when given empty track list`() {
        val rules = VibeSqlBounds(100, 120, 0.4, 0.6, 0.4, 0.6)
        val result = VibeCookerEngine.scoreAndRankTracks(emptyList(), rules)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `scoreAndRankTracks ranks exact match highest`() {
        val rules = VibeSqlBounds(
            bpmMin = 110, bpmMax = 130, // target bpm = 120
            energyMin = 0.4, energyMax = 0.6, // target energy = 0.5
            danceMin = 0.4, danceMax = 1.0,
            targetKey = "8A"
        )
        val perfectTrack = createTrack(id = "1", bpm = 120, energy = 0.5, key = "8A", danceability = 0.8)
        val imperfectTrack = createTrack(id = "2", bpm = 140, energy = 0.8, key = "9A", danceability = 0.3)

        val result = VibeCookerEngine.scoreAndRankTracks(listOf(imperfectTrack, perfectTrack), rules)

        assertEquals(2, result.size)
        assertEquals("1", result[0].track.id) // Perfect track should be first
        assertEquals("2", result[1].track.id)
    }

    @Test
    fun `scoreAndRankTracks applies BPM penalty correctly`() {
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 120, // target = 120
            energyMin = 0.5, energyMax = 0.5, // target = 0.5
            danceMin = 0.0, danceMax = 1.0
        )
        // BPM delta > 40 should result in 0 BPM score.
        // If energy score is 1.0, dance score 1.0, harmonic score 0.7
        // Total score = (1.0 * 0.35) + (0.0 * 0.30) + (1.0 * 0.15) + (0.7 * 0.20) = 0.35 + 0 + 0.15 + 0.14 = 0.64
        val trackLargeBpmDelta = createTrack(bpm = 165, energy = 0.5, danceability = 0.5, key = "8A")
        val result = VibeCookerEngine.scoreAndRankTracks(listOf(trackLargeBpmDelta), rules)

        assertEquals(1, result.size)
        // 0.35 (energy) + 0.0 (bpm) + 0.15 (dance) + 0.14 (harmonic) = 0.64
        assertEquals(0.64, result[0].score, 0.01)
    }

    @Test
    fun `scoreAndRankTracks applies energy penalty correctly`() {
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 120, // target = 120
            energyMin = 0.0, energyMax = 0.0, // target = 0.0
            danceMin = 0.0, danceMax = 1.0
        )

        // Energy delta is 0.8. score = maxOf(0.0, 1.0 - 0.8) = 0.2
        val track = createTrack(bpm = 120, energy = 0.8, danceability = 0.5, key = "8A")
        val result = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules)

        assertEquals(1, result.size)
        // 0.2 * 0.35 = 0.07 (energy)
        // 1.0 * 0.30 = 0.30 (bpm)
        // 1.0 * 0.15 = 0.15 (dance)
        // 0.7 * 0.20 = 0.14 (harmonic)
        // Total = 0.07 + 0.30 + 0.15 + 0.14 = 0.66
        assertEquals(0.66, result[0].score, 0.01)
    }

    @Test
    fun `scoreAndRankTracks applies danceability penalty when under min`() {
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 120, // target = 120
            energyMin = 0.5, energyMax = 0.5, // target = 0.5
            danceMin = 0.8, danceMax = 1.0
        )

        // danceability 0.5 < 0.8, so danceScore is 0.5
        val track = createTrack(bpm = 120, energy = 0.5, danceability = 0.5, key = "8A")
        val result = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules)

        assertEquals(1, result.size)
        // 1.0 * 0.35 = 0.35 (energy)
        // 1.0 * 0.30 = 0.30 (bpm)
        // 0.5 * 0.15 = 0.075 (dance)
        // 0.7 * 0.20 = 0.14 (harmonic)
        // Total = 0.35 + 0.30 + 0.075 + 0.14 = 0.865
        assertEquals(0.865, result[0].score, 0.01)
    }

    @Test
    fun `scoreAndRankTracks evaluates harmonic compatibility with anchor key`() {
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 120,
            energyMin = 0.5, energyMax = 0.5,
            danceMin = 0.0, danceMax = 1.0
        )
        val track = createTrack(bpm = 120, energy = 0.5, danceability = 0.5, key = "8A")

        // Exact match with anchor key -> 1.0 harmonic score
        val result = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules, anchorKey = "8A")

        // 1.0 * 0.35 = 0.35 (energy)
        // 1.0 * 0.30 = 0.30 (bpm)
        // 1.0 * 0.15 = 0.15 (dance)
        // 1.0 * 0.20 = 0.20 (harmonic)
        // Total = 1.0
        assertEquals(1.0, result[0].score, 0.01)
        assertEquals("Perfect Harmonic Lock (Exact Key)", result[0].harmonicCompatibility)
    }

    @Test
    fun `scoreAndRankTracks evaluates harmonic compatibility with target key when anchor key is null`() {
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 120,
            energyMin = 0.5, energyMax = 0.5,
            danceMin = 0.0, danceMax = 1.0,
            targetKey = "8A"
        )
        val track = createTrack(bpm = 120, energy = 0.5, danceability = 0.5, key = "8B") // Relative mode

        // Relative Major/Minor with target key -> 0.95 harmonic score
        val result = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules, anchorKey = null)

        // 1.0 * 0.35 = 0.35 (energy)
        // 1.0 * 0.30 = 0.30 (bpm)
        // 1.0 * 0.15 = 0.15 (dance)
        // 0.95 * 0.20 = 0.19 (harmonic)
        // Total = 0.35 + 0.30 + 0.15 + 0.19 = 0.99
        assertEquals(0.99, result[0].score, 0.01)
        assertEquals("Relative Mode Modulation (A to B)", result[0].harmonicCompatibility)
    }

    @Test
    fun `scoreAndRankTracks applies default harmonic score when no keys provided`() {
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 120,
            energyMin = 0.5, energyMax = 0.5,
            danceMin = 0.0, danceMax = 1.0,
            targetKey = null
        )
        val track = createTrack(bpm = 120, energy = 0.5, danceability = 0.5, key = "8A")

        val result = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules, anchorKey = null)

        // 1.0 * 0.35 = 0.35 (energy)
        // 1.0 * 0.30 = 0.30 (bpm)
        // 1.0 * 0.15 = 0.15 (dance)
        // 0.7 * 0.20 = 0.14 (harmonic default)
        // Total = 0.94
        assertEquals(0.94, result[0].score, 0.01)
        assertEquals("General Transition", result[0].harmonicCompatibility)
    }
}
