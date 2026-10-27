package com.vibeplay.core

import com.vibeplay.core.dsp.BiquadFilter
import com.vibeplay.core.dsp.BiquadPeqEngine
import com.vibeplay.core.dsp.BiquadType
import com.vibeplay.core.engine.VibeCookerEngine
import com.vibeplay.core.model.VibeSeedData
import com.vibeplay.core.scanner.LocalMusicScanner
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VibePlayCoreTest {

    @Test
    fun testPromptRegexParsing() {
        val rules1 = VibeCookerEngine.parsePromptToSqlBounds("fast cardio workout >130 bpm")
        assertTrue(rules1.bpmMin >= 130)
        assertTrue(rules1.energyMin >= 0.75)

        val rules2 = VibeCookerEngine.parsePromptToSqlBounds("chill lofi coffee vibes under 95 bpm")
        assertTrue(rules2.bpmMax <= 95)
        assertTrue(rules2.energyMax <= 0.60)

        val rules3 = VibeCookerEngine.parsePromptToSqlBounds("neon retro synthwave around 125 bpm in 8B")
        assertTrue(rules3.bpmMin in 115..120)
        assertTrue(rules3.bpmMax in 130..135)
        assertEquals("8B", rules3.targetKey)
    }

    @Test
    fun testCamelotHarmonicTransitions() {
        // Exact match
        val (scoreExact, descExact) = VibeCookerEngine.evaluateCamelotTransition("8B", "8B")
        assertEquals(1.0, scoreExact, 0.001)
        assertTrue(descExact.contains("Exact Key"))

        // Relative mode modulation (8B <-> 8A)
        val (scoreRel, descRel) = VibeCookerEngine.evaluateCamelotTransition("8B", "8A")
        assertEquals(0.95, scoreRel, 0.001)
        assertTrue(descRel.contains("Relative Mode"))

        // Energy step lift (8B -> 9B)
        val (scoreLift, descLift) = VibeCookerEngine.evaluateCamelotTransition("8B", "9B")
        assertEquals(0.90, scoreLift, 0.001)
        assertTrue(descLift.contains("+1"))

        // Key clash (8B -> 2A)
        val (scoreClash, _) = VibeCookerEngine.evaluateCamelotTransition("8B", "2A")
        assertEquals(0.40, scoreClash, 0.001)
    }

    @Test
    fun testBiquadDspEngine() {
        val peq = BiquadPeqEngine(48000f)

        // Flat response should yield ~0 dB across all bands
        val flatResponse = peq.evaluateCompositeCurve(listOf(100f, 1000f, 10000f))
        for (gain in flatResponse) {
            assertEquals(0f, gain, 0.1f)
        }
        assertEquals(0f, peq.calculateHeadroomDbFs(), 0.1f)

        // Boost 1kHz by 6 dB
        peq.setBandGain(5, 6.0f) // 1000Hz is band index 5
        val boostedResponse = peq.evaluateCompositeCurve(listOf(1000f))
        assertTrue(boostedResponse[0] > 5.0f)

        // Headroom should calculate negative attenuation to prevent clipping
        val headroom = peq.calculateHeadroomDbFs()
        assertTrue(headroom < -5.0f)

        // Test float buffer processing
        val samples = floatArrayOf(0.5f, -0.5f, 0.2f, -0.2f)
        peq.processInterleavedStereo(samples)
        for (s in samples) {
            assertTrue(s in -1.0f..1.0f)
        }
    }

    @Test
    fun testScanMusicTestDirectory() {
        val musicTestDir = File("/home/sinnoh/music-test")
        if (musicTestDir.exists()) {
            val tracks = LocalMusicScanner.scanDirectory(musicTestDir)
            println("Discovered ${tracks.size} lossless tracks from /home/sinnoh/music-test")
            assertTrue("Expected tracks in music-test", tracks.isNotEmpty())

            val first = tracks.first()
            assertNotNull(first.title)
            assertNotNull(first.artist)
            assertEquals("FLAC", first.format)
            assertTrue("Duration should be > 0", first.durationMs > 0)
        }
    }
}
