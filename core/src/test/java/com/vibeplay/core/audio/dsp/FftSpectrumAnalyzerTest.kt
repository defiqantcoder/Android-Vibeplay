package com.vibeplay.core.audio.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

class FftSpectrumAnalyzerTest {

    @Test
    fun test1kHzSineWaveBinning() {
        val sampleRate = 44100
        val numBands = 64
        val analyzer = FftSpectrumAnalyzer(sampleRate = sampleRate, numBands = numBands)

        // Generate 1kHz sine wave
        val frequency = 1000.0f
        val fftSize = 1024
        val buffer = FloatArray(fftSize)
        for (i in 0 until fftSize) {
            buffer[i] = sin(2.0 * PI * frequency * i / sampleRate).toFloat()
        }

        // Process audio twice to overcome initial smoothing attack
        analyzer.processAudio(buffer)
        analyzer.processAudio(buffer)

        val bands = analyzer.bandAmplitudes.value

        // Calculate expected band index for 1kHz
        val minFreq = 20.0f
        val maxFreq = 20000.0f
        val logMinFreq = log10(minFreq)
        val logMaxFreq = log10(maxFreq)
        val logRange = logMaxFreq - logMinFreq
        val logFreq = log10(frequency)
        val expectedBandIndex = ((logFreq - logMinFreq) / logRange * numBands).toInt()

        // Verify that the expected band has a significant amplitude
        assertTrue("Amplitude in expected 1kHz band ($expectedBandIndex) should be greater than 0", bands[expectedBandIndex] > 0.0f)

        // Find peak band
        var maxIndex = -1
        var maxValue = -1f
        for (i in bands.indices) {
            if (bands[i] > maxValue) {
                maxValue = bands[i]
                maxIndex = i
            }
        }

        // The peak should be exactly at or very close to the expected band index
        val diff = Math.abs(expectedBandIndex - maxIndex)
        assertTrue("Peak band ($maxIndex) should be close to expected band ($expectedBandIndex)", diff <= 1)
    }

    @Test
    fun testImpulseSignalBroadband() {
        val analyzer = FftSpectrumAnalyzer(sampleRate = 44100, numBands = 64)

        val fftSize = 1024
        val buffer = FloatArray(fftSize)
        buffer[fftSize / 2] = 1.0f // Impulse

        analyzer.processAudio(buffer)
        analyzer.processAudio(buffer)

        val bands = analyzer.bandAmplitudes.value

        // An impulse signal contains all frequencies, so many bands should be active
        var activeBands = 0
        for (amp in bands) {
            if (amp > 0.0f) {
                activeBands++
            }
        }

        assertTrue("Many bands should be active for an impulse signal", activeBands > 30)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNonPowerOfTwoBuffer() {
        val analyzer = FftSpectrumAnalyzer()
        val buffer = FloatArray(1000)
        analyzer.processAudio(buffer)
    }
}
