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
    fun `scoreAndRankTracks applies harmonic bonus for target key in rules`() {
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

        // Both tracks have the exact same stats, so harmonic score will break the tie
        val harmonicMatchTrack = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "8A") // Perfect match
        val harmonicClashTrack = createTrack("t2", bpm = 125, energy = 0.7, danceability = 0.8, key = "3B") // Clash

        val tracks = listOf(harmonicClashTrack, harmonicMatchTrack)

        // Act - anchorKey is null, but targetKey is in rules
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules, anchorKey = null)

        // Assert
        assertEquals(2, scored.size)
        assertEquals("t1", scored[0].track.id)
        assertEquals("t2", scored[1].track.id)
        assertTrue(scored[0].score > scored[1].score)
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

    @Test
    fun `scoreAndRankTracks handles empty track list`() {
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

        val tracks = emptyList<Track>()

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules)

        // Assert
        assertTrue(scored.isEmpty())
    }

    @Test
    fun `scoreAndRankTracks penalizes low danceability`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130, // Target BPM 125
            energyMin = 0.6,
            energyMax = 0.8, // Target Energy 0.7
            danceMin = 0.8, // High danceability required
            danceMax = 1.0,
            targetKey = null
        )

        // Same everything, but t1 meets danceMin and t2 does not
        val highDanceTrack = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.9)
        val lowDanceTrack = createTrack("t2", bpm = 125, energy = 0.7, danceability = 0.5)

        val tracks = listOf(lowDanceTrack, highDanceTrack)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules)

        // Assert
        assertEquals(2, scored.size)
        assertEquals("t1", scored[0].track.id)
        assertEquals("t2", scored[1].track.id)
        assertTrue(scored[0].score > scored[1].score)

        // High dance gives 1.0 (weighted 0.15), low gives 0.5 (weighted 0.075)
        assertEquals(0.075, scored[0].score - scored[1].score, 0.0001)
    }

    @Test
    fun `scoreAndRankTracks calculates exact expected composite score`() {
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130, // Target BPM: 125
            energyMin = 0.6,
            energyMax = 0.8, // Target Energy: 0.7
            danceMin = 0.8,
            danceMax = 1.0,
            targetKey = null
        )

        val track = createTrack("t1", bpm = 115, energy = 0.6, danceability = 0.9, key = "8B")

        val scored = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules, anchorKey = "8A")

        assertEquals(1, scored.size)
        assertEquals(0.88, scored[0].score, 0.0001)
    }

    @Test
    fun `scoreAndRankTracks defaults to General Transition when keys are absent`() {
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130,
            energyMin = 0.6,
            energyMax = 0.8,
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = null
        )

        val track = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8)

        val scored = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules, anchorKey = null)

        assertEquals(1, scored.size)
        assertEquals("General Transition", scored[0].harmonicCompatibility)
        assertEquals(0.94, scored[0].score, 0.0001)
    }

    @Test
    fun `scoreAndRankTracks prefers anchorKey over rules targetKey`() {
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130,
            energyMin = 0.6,
            energyMax = 0.8,
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = "3B" // This would clash with 8A
        )

        // anchorKey will perfectly match the track key
        val track = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "8A")

        val scored = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules, anchorKey = "8A")

        assertEquals(1, scored.size)
        assertEquals("Perfect Harmonic Lock (Exact Key)", scored[0].harmonicCompatibility)
        assertEquals(1.0, scored[0].score, 0.0001)
    }

    @Test
    fun `scoreAndRankTracks handles extreme values yielding zero sub-scores`() {
        val rules = VibeSqlBounds(
            bpmMin = 120,
            bpmMax = 130, // Target BPM: 125
            energyMin = 0.6,
            energyMax = 0.8, // Target Energy: 0.7
            danceMin = 0.5,
            danceMax = 1.0,
            targetKey = null
        )

        // Extreme BPM delta: 125 - 200 = 75 (bpmScore 0)
        // Extreme Energy delta: 0.7 - 2.0 = 1.3 (energyScore 0)
        val track = createTrack("extreme", bpm = 200, energy = 2.0, danceability = 0.1, key = "8A")

        val scored = VibeCookerEngine.scoreAndRankTracks(listOf(track), rules, anchorKey = null)

        assertEquals(1, scored.size)
        assertEquals(0.215, scored[0].score, 0.0001)
        assertEquals(75.0, scored[0].bpmDelta, 0.0001)
        assertEquals(1.3, scored[0].energyDelta, 0.0001)
    }
}
