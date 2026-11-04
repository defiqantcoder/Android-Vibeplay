package com.vibeplay.core.engine

import com.vibeplay.core.model.Track
import com.vibeplay.core.model.VibeSqlBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibeCookerEngineTest {

    private fun createTrack(
        id: String,
        bpm: Int,
        energy: Double,
        danceability: Double,
        key: String = "8A"
    ): Track {
        return Track(
            id = id,
            title = "Title",
            artist = "Artist",
            album = "Album",
            duration = "3:30",
            durationMs = 210000,
            bpm = bpm,
            key = key,
            energy = energy,
            danceability = danceability,
            crateId = "crate_1",
            format = "FLAC",
            sampleRate = "44.1kHz",
            bitDepth = "16bit"
        )
    }

    @Test
    fun `scoreAndRankTracks prioritizes tracks closest to target BPM and Energy`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130, // Target BPM will be 125
            energyMin = 0.6,
            energyMax = 0.8, // Target Energy will be 0.7
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = null
        )

        val perfectTrack = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "8A")
        val okayTrack = createTrack("t2", bpm = 120, energy = 0.6, danceability = 0.6, key = "5B")
        val badTrack = createTrack("t3", bpm = 80, energy = 0.2, danceability = 0.2, key = "2A")

        val tracks = listOf(badTrack, perfectTrack, okayTrack)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules)

        // Assert
        assertEquals(3, scored.size)
        assertEquals("t1", scored[0].track.id)
        assertEquals("t2", scored[1].track.id)
        assertEquals("t3", scored[2].track.id)

        assertTrue(scored[0].score > scored[1].score)
        assertTrue(scored[1].score > scored[2].score)

        // Equal-BPM pair with different energy (loser first)
        val lowEnergy = createTrack("e_low", bpm = 125, energy = 0.2, danceability = 0.8)
        val targetEnergy = createTrack("e_target", bpm = 125, energy = 0.7, danceability = 0.8)
        val scoredByEnergy = VibeCookerEngine.scoreAndRankTracks(listOf(lowEnergy, targetEnergy), rules)
        assertEquals("e_target", scoredByEnergy[0].track.id)
        assertEquals("e_low", scoredByEnergy[1].track.id)
        assertTrue(scoredByEnergy[0].score > scoredByEnergy[1].score)

        // Equal-energy pair with different BPM (loser first)
        val farBpm = createTrack("bpm_far", bpm = 100, energy = 0.7, danceability = 0.8)
        val targetBpm = createTrack("bpm_target", bpm = 125, energy = 0.7, danceability = 0.8)
        val scoredByBpm = VibeCookerEngine.scoreAndRankTracks(listOf(farBpm, targetBpm), rules)
        assertEquals("bpm_target", scoredByBpm[0].track.id)
        assertEquals("bpm_far", scoredByBpm[1].track.id)
        assertTrue(scoredByBpm[0].score > scoredByBpm[1].score)
    }

    @Test
    fun `scoreAndRankTracks applies harmonic bonus for anchor key`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130,
            energyMin = 0.6,
            energyMax = 0.8,
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = null
        )

        // Both tracks have the exact same stats, so harmonic score will break the tie
        val anchorKey = "8A"
        val harmonicMatchTrack = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "8A") // Perfect match
        val harmonicClashTrack = createTrack("t2", bpm = 125, energy = 0.7, danceability = 0.8, key = "3B") // Clash

        val tracks = listOf(harmonicClashTrack, harmonicMatchTrack)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules, anchorKey = anchorKey)

        // Assert
        assertEquals(2, scored.size)
        assertEquals("t1", scored[0].track.id)
        assertEquals("t2", scored[1].track.id)
        assertTrue(scored[0].score > scored[1].score)
    }

    @Test
    fun `scoreAndRankTracks applies harmonic bonus for target key when anchor key is null`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130,
            energyMin = 0.6,
            energyMax = 0.8,
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = "8A"
        )

        val harmonicMatchTrack = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "8A")
        val harmonicClashTrack = createTrack("t2", bpm = 125, energy = 0.7, danceability = 0.8, key = "3B")

        val tracks = listOf(harmonicClashTrack, harmonicMatchTrack)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules, anchorKey = null)

        // Assert
        assertEquals(2, scored.size)
        assertEquals("t1", scored[0].track.id)
        assertEquals("t2", scored[1].track.id)
        assertTrue(scored[0].score > scored[1].score)
    }

    @Test
    fun `scoreAndRankTracks uses default harmonic score when neither anchor nor target key provided`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130,
            energyMin = 0.6,
            energyMax = 0.8,
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = null
        )

        val track = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "8A")
        val tracks = listOf(track)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules, anchorKey = null)

        // Assert
        assertEquals(1, scored.size)
        assertEquals("General Transition", scored[0].harmonicCompatibility)
        // totalScore = (energyScore * 0.35) + (bpmScore * 0.30) + (danceScore * 0.15) + (harmonicScore * 0.20)
        // energyDelta = 0, energyScore = 1.0 -> 0.35
        // bpmDelta = 0, bpmScore = 1.0 -> 0.30
        // danceScore = 1.0 -> 0.15
        // harmonicScore = 0.7 -> 0.14
        // totalScore = 0.35 + 0.30 + 0.15 + 0.14 = 0.94
        assertEquals(0.94, scored[0].score, 0.001)
    }

    @Test
    fun `scoreAndRankTracks calculates dance score correctly based on danceMin`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130, // Target BPM will be 125
            energyMin = 0.6,
            energyMax = 0.8, // Target Energy will be 0.7
            danceMin = 0.7,
            danceMax = 1.0,
            targetKey = null
        )

        val highDanceTrack = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8) // >= 0.7 -> score 1.0
        val lowDanceTrack = createTrack("t2", bpm = 125, energy = 0.7, danceability = 0.6)  // < 0.7 -> score 0.5

        val tracks = listOf(lowDanceTrack, highDanceTrack)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules)

        // Assert
        assertEquals(2, scored.size)
        val scoredHigh = scored.find { it.track.id == "t1" }!!
        val scoredLow = scored.find { it.track.id == "t2" }!!

        assertTrue(scoredHigh.score > scoredLow.score)

        // expected score difference is just the dance component difference (1.0 vs 0.5) * 0.15 weight = 0.075
        assertEquals(0.075, scoredHigh.score - scoredLow.score, 0.001)
    }

    @Test
    fun `scoreAndRankTracks returns empty list for empty track list`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130,
            energyMin = 0.6,
            energyMax = 0.8,
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = null
        )

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(emptyList(), rules)

        // Assert
        assertEquals(0, scored.size)
    }

    @Test
    fun `scoreAndRankTracks handles BPM penalty and normalizes`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130, // Target BPM will be 125
            energyMin = 0.6,
            energyMax = 0.8, // Target Energy will be 0.7
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = null
        )

        val perfectTrack = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8)
        val extremelyFastTrack = createTrack("t2", bpm = 200, energy = 0.7, danceability = 0.8) // > 40 delta gives 0 BPM score

        val tracks = listOf(perfectTrack, extremelyFastTrack)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules)

        // Assert
        val scoredPerfect = scored.find { it.track.id == "t1" }!!
        val scoredExtremelyFast = scored.find { it.track.id == "t2" }!!

        // The extremely fast track should have a BPM delta of 75, which means BPM score is 0
        assertEquals(75.0, scoredExtremelyFast.bpmDelta, 0.01)
        assertEquals(0.64, scoredExtremelyFast.score, 0.0001)
        assertTrue(scoredPerfect.score > scoredExtremelyFast.score)
    }
}
