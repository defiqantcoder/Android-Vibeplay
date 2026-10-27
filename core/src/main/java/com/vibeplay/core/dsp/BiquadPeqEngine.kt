package com.vibeplay.core.dsp

import kotlin.math.*

enum class BiquadType {
    LOW_SHELF,
    BELL_PEAK,
    HIGH_SHELF,
    NOTCH
}

data class BiquadCoefficients(
    val b0: Float,
    val b1: Float,
    val b2: Float,
    val a1: Float,
    val a2: Float
)

class BiquadFilter(var type: BiquadType, var frequency: Float, var gainDb: Float, var q: Float, var sampleRate: Float = 48000f) {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    // Direct Form II transposed states (left & right)
    private var s1L = 0f
    private var s2L = 0f
    private var s1R = 0f
    private var s2R = 0f

    init {
        updateCoefficients()
    }

    fun update(frequency: Float, gainDb: Float, q: Float, sampleRate: Float = this.sampleRate) {
        this.frequency = frequency
        this.gainDb = gainDb
        this.q = max(0.1f, q)
        this.sampleRate = max(8000f, sampleRate)
        updateCoefficients()
    }

    fun resetState() {
        s1L = 0f
        s2L = 0f
        s1R = 0f
        s2R = 0f
    }

    private fun updateCoefficients() {
        // Robert Bristow-Johnson (RBJ) Audio EQ Cookbook formulae
        val omega = (2.0 * Math.PI * (frequency / sampleRate)).toFloat()
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)
        val alpha = sinOmega / (2.0f * q)
        val a = 10.0.pow((gainDb / 40.0)).toFloat()

        var a0 = 1.0f
        var rawB0 = 1.0f
        var rawB1 = 0.0f
        var rawB2 = 0.0f
        var rawA1 = 0.0f
        var rawA2 = 0.0f

        when (type) {
            BiquadType.BELL_PEAK -> {
                rawB0 = 1.0f + alpha * a
                rawB1 = -2.0f * cosOmega
                rawB2 = 1.0f - alpha * a
                a0 = 1.0f + alpha / a
                rawA1 = -2.0f * cosOmega
                rawA2 = 1.0f - alpha / a
            }
            BiquadType.LOW_SHELF -> {
                val sqrtA = sqrt(a)
                val twoSqrtAAlpha = 2.0f * sqrtA * alpha
                rawB0 = a * ((a + 1.0f) - (a - 1.0f) * cosOmega + twoSqrtAAlpha)
                rawB1 = 2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosOmega)
                rawB2 = a * ((a + 1.0f) - (a - 1.0f) * cosOmega - twoSqrtAAlpha)
                a0 = (a + 1.0f) + (a - 1.0f) * cosOmega + twoSqrtAAlpha
                rawA1 = -2.0f * ((a - 1.0f) + (a + 1.0f) * cosOmega)
                rawA2 = (a + 1.0f) + (a - 1.0f) * cosOmega - twoSqrtAAlpha
            }
            BiquadType.HIGH_SHELF -> {
                val sqrtA = sqrt(a)
                val twoSqrtAAlpha = 2.0f * sqrtA * alpha
                rawB0 = a * ((a + 1.0f) + (a - 1.0f) * cosOmega + twoSqrtAAlpha)
                rawB1 = -2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosOmega)
                rawB2 = a * ((a + 1.0f) + (a - 1.0f) * cosOmega - twoSqrtAAlpha)
                a0 = (a + 1.0f) - (a - 1.0f) * cosOmega + twoSqrtAAlpha
                rawA1 = 2.0f * ((a - 1.0f) - (a + 1.0f) * cosOmega)
                rawA2 = (a + 1.0f) - (a - 1.0f) * cosOmega - twoSqrtAAlpha
            }
            BiquadType.NOTCH -> {
                rawB0 = 1.0f
                rawB1 = -2.0f * cosOmega
                rawB2 = 1.0f
                a0 = 1.0f + alpha
                rawA1 = -2.0f * cosOmega
                rawA2 = 1.0f - alpha
            }
        }

        // Normalize by a0
        val invA0 = 1.0f / a0
        b0 = rawB0 * invA0
        b1 = rawB1 * invA0
        b2 = rawB2 * invA0
        a1 = rawA1 * invA0
        a2 = rawA2 * invA0
    }

    /**
     * Process a single stereo sample pair in-place using Direct Form II Transposed structure
     */
    fun processSample(inputL: Float, inputR: Float): Pair<Float, Float> {
        val outL = b0 * inputL + s1L
        s1L = b1 * inputL - a1 * outL + s2L
        s2L = b2 * inputL - a2 * outL

        val outR = b0 * inputR + s1R
        s1R = b1 * inputR - a1 * outR + s2R
        s2R = b2 * inputR - a2 * outR

        return Pair(outL, outR)
    }

    /**
     * Evaluate complex frequency response magnitude in dB at target frequency f
     */
    fun getMagnitudeResponseDb(f: Float): Float {
        val omega = (2.0 * Math.PI * (f / sampleRate)).toFloat()
        val phi = 4.0 * (sin(omega / 2.0).pow(2.0))
        val num = (b0 + b1 + b2).toDouble().pow(2.0) +
                (b0 * b2 * phi - (b1 * (b0 + b2) + 4.0 * b0 * b2)) * phi
        val den = (1.0 + a1 + a2).toDouble().pow(2.0) +
                (a2 * phi - (a1 * (1.0 + a2) + 4.0 * a2)) * phi

        if (den <= 0.0 || num <= 0.0) return 0f
        val hSquared = num / den
        return (10.0 * log10(max(1e-12, hSquared))).toFloat()
    }
}

class BiquadPeqEngine(sampleRate: Float = 48000f) {
    private val filters = mutableListOf<BiquadFilter>()
    var sampleRate: Float = sampleRate
        set(value) {
            field = value
            filters.forEach { it.update(it.frequency, it.gainDb, it.q, value) }
        }

    init {
        // Standard 10-band ISO frequencies
        val bands = listOf(
            Triple(31f, BiquadType.LOW_SHELF, 1.41f),
            Triple(63f, BiquadType.BELL_PEAK, 1.41f),
            Triple(125f, BiquadType.BELL_PEAK, 1.41f),
            Triple(250f, BiquadType.BELL_PEAK, 1.41f),
            Triple(500f, BiquadType.BELL_PEAK, 1.41f),
            Triple(1000f, BiquadType.BELL_PEAK, 1.41f),
            Triple(2500f, BiquadType.BELL_PEAK, 1.41f),
            Triple(5000f, BiquadType.BELL_PEAK, 1.41f),
            Triple(8000f, BiquadType.BELL_PEAK, 1.41f),
            Triple(16000f, BiquadType.HIGH_SHELF, 1.41f)
        )
        bands.forEach { (freq, type, q) ->
            filters.add(BiquadFilter(type, freq, 0f, q, sampleRate))
        }
    }

    fun setGains(gains: List<Double>) {
        for (i in 0 until min(gains.size, filters.size)) {
            val filter = filters[i]
            filter.update(filter.frequency, gains[i].toFloat(), filter.q, sampleRate)
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in filters.indices) {
            val filter = filters[bandIndex]
            filter.update(filter.frequency, gainDb, filter.q, sampleRate)
        }
    }

    fun reset() {
        filters.forEach {
            it.update(it.frequency, 0f, it.q, sampleRate)
            it.resetState()
        }
    }

    /**
     * Compute composite frequency response curve in dB across target points
     */
    fun evaluateCompositeCurve(frequencies: List<Float>): List<Float> {
        return frequencies.map { f ->
            var totalDb = 0f
            for (filter in filters) {
                totalDb += filter.getMagnitudeResponseDb(f)
            }
            totalDb
        }
    }

    /**
     * Calculate headroom and potential clipping prevention in dBFS
     */
    fun calculateHeadroomDbFs(): Float {
        // Sample standard frequencies from 20Hz to 20kHz
        val testFreqs = listOf(
            20f, 31f, 45f, 63f, 90f, 125f, 180f, 250f, 350f, 500f,
            750f, 1000f, 1500f, 2500f, 3500f, 5000f, 7000f, 8000f,
            10000f, 12000f, 16000f, 20000f
        )
        val curve = evaluateCompositeCurve(testFreqs)
        val maxGain = curve.maxOrNull() ?: 0f
        return if (maxGain > 0f) -maxGain else 0f
    }

    /**
     * Process an array of interleaved stereo float samples [-1.0f .. 1.0f]
     */
    fun processInterleavedStereo(samples: FloatArray, offset: Int = 0, length: Int = samples.size) {
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            var l = samples[i]
            var r = samples[i + 1]
            for (f in filters) {
                val res = f.processSample(l, r)
                l = res.first
                r = res.second
            }
            samples[i] = l.coerceIn(-1.0f, 1.0f)
            samples[i + 1] = r.coerceIn(-1.0f, 1.0f)
            i += 2
        }
    }
}
