package com.vibeplay.core.audio.dsp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class FftSpectrumAnalyzer(
    private val sampleRate: Int = 44100,
    private val numBands: Int = 64,
    private var attack: Float = 0.85f,
    private var decay: Float = 0.15f
) {
    private val _bandAmplitudes = MutableStateFlow(FloatArray(numBands))
    val bandAmplitudes: StateFlow<FloatArray> = _bandAmplitudes.asStateFlow()

    private var previousBands = FloatArray(numBands)
    private val minFreq = 20.0f
    private val maxFreq = 20000.0f

    fun setSmoothing(newAttack: Float, newDecay: Float) {
        attack = newAttack
        decay = newDecay
    }

    fun processAudio(buffer: ShortArray) {
        val floatBuffer = FloatArray(buffer.size)
        for (i in buffer.indices) {
            floatBuffer[i] = buffer[i] / 32768.0f
        }
        processAudio(floatBuffer)
    }

    fun processAudio(buffer: FloatArray) {
        val n = buffer.size
        // Ensure n is a power of 2
        if (n and (n - 1) != 0) {
            throw IllegalArgumentException("Buffer size must be a power of 2")
        }

        // Apply Hann window
        val windowed = FloatArray(n)
        for (i in 0 until n) {
            val multiplier = 0.5f * (1.0f - cos(2.0 * Math.PI * i / (n - 1))).toFloat()
            windowed[i] = buffer[i] * multiplier
        }

        // Perform FFT
        val (real, imag) = fft(windowed)

        // Calculate magnitude spectrum
        val magnitudes = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        // Bin into logarithmic bands
        val bands = binFrequencies(magnitudes, n)

        // Apply attack/decay smoothing
        for (i in 0 until numBands) {
            val current = bands[i]
            val prev = previousBands[i]
            if (current > prev) {
                previousBands[i] = prev + attack * (current - prev)
            } else {
                previousBands[i] = prev - decay * (prev - current)
            }
            // Add a small safety floor
            if (previousBands[i] < 0f) previousBands[i] = 0f
        }

        _bandAmplitudes.value = previousBands.clone()
    }

    private fun fft(input: FloatArray): Pair<FloatArray, FloatArray> {
        val n = input.size
        val real = input.clone()
        val imag = FloatArray(n)

        // Bit reversal sorting
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }
            var m = n / 2
            while (m <= j) {
                j -= m
                m /= 2
            }
            j += m
        }

        // Cooley-Tukey FFT
        var step = 2
        while (step <= n) {
            val halfStep = step / 2
            val angle = -2.0 * Math.PI / step
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()

            for (i in 0 until n step step) {
                var currentWReal = 1.0f
                var currentWImag = 0.0f

                for (k in 0 until halfStep) {
                    val idx = i + k
                    val halfIdx = idx + halfStep

                    val tReal = currentWReal * real[halfIdx] - currentWImag * imag[halfIdx]
                    val tImag = currentWReal * imag[halfIdx] + currentWImag * real[halfIdx]

                    real[halfIdx] = real[idx] - tReal
                    imag[halfIdx] = imag[idx] - tImag
                    real[idx] += tReal
                    imag[idx] += tImag

                    val nextWReal = currentWReal * wReal - currentWImag * wImag
                    val nextWImag = currentWReal * wImag + currentWImag * wReal
                    currentWReal = nextWReal
                    currentWImag = nextWImag
                }
            }
            step *= 2
        }

        return Pair(real, imag)
    }

    private fun binFrequencies(magnitudes: FloatArray, fftSize: Int): FloatArray {
        val bands = FloatArray(numBands)
        val logMinFreq = log10(minFreq)
        val logMaxFreq = log10(maxFreq)
        val logRange = logMaxFreq - logMinFreq

        val freqPerBin = sampleRate.toFloat() / fftSize

        val bandCounts = IntArray(numBands)

        for (i in 1 until magnitudes.size) { // Skip DC component
            val freq = i * freqPerBin
            if (freq < minFreq || freq > maxFreq) continue

            val logFreq = log10(freq)
            val bandFraction = (logFreq - logMinFreq) / logRange
            var bandIndex = (bandFraction * numBands).toInt()

            if (bandIndex >= numBands) bandIndex = numBands - 1
            if (bandIndex < 0) bandIndex = 0

            bands[bandIndex] += magnitudes[i]
            bandCounts[bandIndex]++
        }

        // Average and normalize
        var maxAmplitude = 0f
        for (i in 0 until numBands) {
            if (bandCounts[i] > 0) {
                bands[i] /= bandCounts[i].toFloat()
            }
            // Normalize with respect to fftSize to some degree,
            // though absolute normalisation depends on expected signal levels
            bands[i] = bands[i] / (fftSize / 2f)

            // To make visualizer look better, we can apply log scale to amplitude
            if (bands[i] > 0) {
                bands[i] = (20 * log10(bands[i] + 1e-6f) + 120f) / 120f
            } else {
                bands[i] = 0f
            }

            if (bands[i] < 0f) bands[i] = 0f
            if (bands[i] > 1f) bands[i] = 1f
        }

        return bands
    }
}
