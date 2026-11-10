package com.vibeplay.core.engine

import com.vibeplay.core.model.Track
import com.vibeplay.core.model.VibeSqlBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

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
    fun `scoreAndRankTracks handles missing keys without crashing`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 130,
            energyMin = 0.6, energyMax = 0.8,
            danceMin = 0.5, danceMax = 1.0,
            targetKey = "8A"
        )

        // Empty string keys instead of proper camelot notation
        val weirdKeyTrack1 = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "")
        val weirdKeyTrack2 = createTrack("t2", bpm = 125, energy = 0.7, danceability = 0.8, key = "INVALID")

        val tracks = listOf(weirdKeyTrack1, weirdKeyTrack2)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules, anchorKey = null)

        // Assert
        assertEquals(2, scored.size)
        // Just asserting it processes without crashing. Score logic defaults handled internally.
        assertTrue(scored[0].score > 0.0)
    }

    @Test
    fun `scoreAndRankTracks handles null target and anchor keys`() {
        // Arrange
        val rules = VibeSqlBounds(
            bpmMin = 120, bpmMax = 130,
            energyMin = 0.6, energyMax = 0.8,
            danceMin = 0.5, danceMax = 1.0,
            targetKey = null
        )

        val track1 = createTrack("t1", bpm = 125, energy = 0.7, danceability = 0.8, key = "8A")
        val track2 = createTrack("t2", bpm = 125, energy = 0.7, danceability = 0.8, key = "5B")

        val tracks = listOf(track1, track2)

        // Act
        val scored = VibeCookerEngine.scoreAndRankTracks(tracks, rules, anchorKey = null)

        // Assert
        assertEquals(2, scored.size)
        assertEquals("General Transition", scored[0].harmonicCompatibility)
        assertEquals("General Transition", scored[1].harmonicCompatibility)
    }

    @Test
    fun `evaluateCamelotTransition handles adjacent steps properly`() {
        assertEquals(0.90, VibeCookerEngine.evaluateCamelotTransition("8A", "9A").first, 0.001)
        assertEquals(0.85, VibeCookerEngine.evaluateCamelotTransition("8A", "7A").first, 0.001)
        assertEquals(0.75, VibeCookerEngine.evaluateCamelotTransition("8A", "10A").first, 0.001)
    }

    @Test
    fun `evaluateCamelotTransition handles relative mode modulation`() {
        val (score, note) = VibeCookerEngine.evaluateCamelotTransition("8A", "8B")
        assertEquals(0.95, score, 0.001)
        assertTrue(note.contains("Relative Mode Modulation"))
    }

    @Test
    fun `evaluateCamelotTransition handles perfect match`() {
        val (score, note) = VibeCookerEngine.evaluateCamelotTransition("10B", "10B")
        assertEquals(1.0, score, 0.001)
        assertTrue(note.contains("Perfect Harmonic Lock"))
    }

    @Test
    fun `evaluateCamelotTransition handles dissonance`() {
        val (score, note) = VibeCookerEngine.evaluateCamelotTransition("1A", "6A")
        assertEquals(0.40, score, 0.001)
        assertTrue(note.contains("Dissonant"))
    }

    @Test
    fun `parsePromptToSqlBounds parses complex query successfully`() {
        val bounds = VibeCookerEngine.parsePromptToSqlBounds("chill lofi beats under 90 with some acoustic 8A")

        // chill/lofi gives bpmMax <= 105, under 90 gives bpmMax = 90
        assertEquals(90, bounds.bpmMax)
        // chill/lofi gives energyMax <= 0.60, acoustic gives energyMax <= 0.85 -> 0.60
        assertEquals(0.60, bounds.energyMax, 0.001)
        // 8A is the key
        assertEquals("8A", bounds.targetKey)
    }

    @Test
    fun `parsePromptToSqlBounds parses over 140 correctly`() {
        val bounds = VibeCookerEngine.parsePromptToSqlBounds("fast techno over 140 bpm")
        assertEquals(140, bounds.bpmMin)
    }

    @Test
    fun `parsePromptToSqlBounds parses exact bpm correctly`() {
        val bounds = VibeCookerEngine.parsePromptToSqlBounds("play some 128 bpm tracks")
        assertEquals(118, bounds.bpmMin)
        assertEquals(138, bounds.bpmMax)
    }

    @Test
    fun `parsePromptToSqlBounds parses around bpm correctly`() {
        val bounds = VibeCookerEngine.parsePromptToSqlBounds("play some tracks around 128")
        assertEquals(120, bounds.bpmMin)
        assertEquals(136, bounds.bpmMax)
    }

    @Test
    fun `generateMixtapeLinerNotes handles empty playlist`() {
        val note = VibeCookerEngine.generateMixtapeLinerNotes("test", emptyList())
        assertEquals("No tracks matched the requested vibe parameters.", note)
    }

    @Test
    fun `generateMixtapeLinerNotes generates correct notes for playlist`() {
        val t1 = createTrack("1", 120, 0.5, 0.5)
        val t2 = createTrack("2", 130, 0.5, 0.5)
        val note = VibeCookerEngine.generateMixtapeLinerNotes("gym workout", listOf(t1, t2))
        assertTrue(note.contains("gym workout"))
        assertTrue(note.contains("125 BPM")) // Average of 120 and 130
        assertTrue(note.contains("2 tracks"))
    }

    @Test
    fun `synthesizeWithGemini calls generateMixtapeLinerNotes on blank key`() = kotlinx.coroutines.test.runTest {
        val note = VibeCookerEngine.synthesizeWithGemini("test", listOf(createTrack("1", 120, 0.5, 0.5)), "")
        assertTrue(note.contains("Curated 'test'"))
    }
}
