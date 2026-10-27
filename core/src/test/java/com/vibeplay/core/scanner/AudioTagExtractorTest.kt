package com.vibeplay.core.scanner

import com.vibeplay.core.scanner.metadata.ReplayGainNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class AudioTagExtractorTest {

    @Test
    fun testReplayGainNormalizer_ScaleFactor() {
        // Reference LUFS = -18.0
        // Target LUFS = -14.0
        // Target Shift = (-14.0) - (-18.0) = +4.0 dB

        // Scenario 1: Track has ReplayGain of -5.0 dB
        // Total desired gain = -5.0 + 4.0 = -1.0 dB
        // 10^(-1.0/20) ≈ 0.89125
        val scaleFactor1 = ReplayGainNormalizer.computeScaleFactor(-5.0)
        assertEquals(0.89125, scaleFactor1, 0.0001)

        // Scenario 2: Track has ReplayGain of 0.0 dB
        // Total desired gain = 0.0 + 4.0 = +4.0 dB
        // This exceeds safety headroom (3.0 dB), so it gets capped at 3.0 dB
        // 10^(3.0/20) ≈ 1.4125
        val scaleFactor2 = ReplayGainNormalizer.computeScaleFactor(0.0)
        assertEquals(1.412537, scaleFactor2, 0.0001)

        // Scenario 3: Track has ReplayGain of +1.0 dB
        // Total desired gain = 1.0 + 4.0 = +5.0 dB -> Capped at 3.0 dB
        val scaleFactor3 = ReplayGainNormalizer.computeScaleFactor(1.0)
        assertEquals(1.412537, scaleFactor3, 0.0001)

        // Scenario 4: Track has ReplayGain of -10.0 dB
        // Total desired gain = -10.0 + 4.0 = -6.0 dB
        // 10^(-6.0/20) ≈ 0.501187
        val scaleFactor4 = ReplayGainNormalizer.computeScaleFactor(-10.0)
        assertEquals(0.501187, scaleFactor4, 0.0001)
    }
}
