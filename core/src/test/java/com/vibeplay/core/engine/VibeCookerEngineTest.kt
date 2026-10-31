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
        assertTrue(scoredPerfect.score > scoredExtremelyFast.score)
    }

}
