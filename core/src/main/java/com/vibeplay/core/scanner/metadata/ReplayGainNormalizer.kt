package com.vibeplay.core.scanner.metadata

import kotlin.math.pow

object ReplayGainNormalizer {
    /**
     * Target loudness in LUFS. -14 LUFS is a common standard for streaming platforms.
     */
    const val TARGET_LUFS = -14.0

    /**
     * Safety headroom in dB to avoid clipping when applying gain.
     */
    const val SAFETY_HEADROOM_DB = 3.0

    /**
     * Reference level for ReplayGain.
     * ReplayGain 1.0 was referenced to 83 dB SPL (-18 LUFS).
     * ReplayGain 2.0 uses -18 LUFS as reference.
     */
    const val REPLAY_GAIN_REFERENCE_LUFS = -18.0

    /**
     * Computes the target linear volume scale factor from a ReplayGain dB value.
     *
     * @param replayGainDb The ReplayGain value in dB (from tags).
     * @return A linear scalar representing the volume multiplier to apply.
     */
    fun computeScaleFactor(replayGainDb: Double): Double {
        // We calculate the required shift to reach our target LUFS from the reference LUFS.
        val targetShiftDb = TARGET_LUFS - REPLAY_GAIN_REFERENCE_LUFS

        // The total desired gain in dB is the tag's gain plus the shift to our target loudness.
        var totalGainDb = replayGainDb + targetShiftDb

        // To avoid clipping, we limit the maximum positive gain based on safety headroom.
        if (totalGainDb > SAFETY_HEADROOM_DB) {
            totalGainDb = SAFETY_HEADROOM_DB
        }

        // Convert dB to linear scale multiplier (10^(dB/20))
        return 10.0.pow(totalGainDb / 20.0)
    }
}
