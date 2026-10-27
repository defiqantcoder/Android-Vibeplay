package com.vibeplay.app.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.vibeplay.core.dsp.BiquadPeqEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AndroidX Media3 AudioProcessor bridge that routes real-time 16-bit PCM stereo audio
 * through the Robert Bristow-Johnson (RBJ) 10-band Biquad PEQ DSP Engine.
 *
 * Automatically bypasses processing when isBypassed is set to true
 * (e.g. When bit-perfect hardware output mode is active).
 */
class BiquadPeqAudioProcessor(val peqEngine: BiquadPeqEngine = BiquadPeqEngine(44100f)) : BaseAudioProcessor() {

    @Volatile
    var isBypassed: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // We handle 16-bit PCM signed integer audio, typical for decoded FLAC and Android AudioTrack
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        if (inputAudioFormat.channelCount != 2 && inputAudioFormat.channelCount != 1) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        // Dynamically update sample rate in DSP engine
        peqEngine.sampleRate = inputAudioFormat.sampleRate.toFloat()

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val buffer = replaceOutputBuffer(remaining)

        if (isBypassed || inputAudioFormat.channelCount != 2) {
            // Passthrough mode (Bit-Perfect direct bypass or mono audio)
            buffer.put(inputBuffer)
            buffer.flip()
            return
        }

        // Process stereo 16-bit PCM samples [-32768 .. 32767]
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val sampleCount = remaining / 2
        val floatSamples = FloatArray(sampleCount)

        var idx = 0
        while (inputBuffer.hasRemaining()) {
            val shortVal = inputBuffer.short
            floatSamples[idx++] = shortVal / 32768.0f
        }

        // Apply 10-Band PEQ filter cascade
        peqEngine.processInterleavedStereo(floatSamples)

        // Write processed float samples back into 16-bit PCM output buffer
        for (f in floatSamples) {
            val clamped = f.coerceIn(-1.0f, 1.0f)
            val shortOut = (clamped * 32767.0f).toInt().toShort()
            buffer.putShort(shortOut)
        }

        buffer.flip()
    }
}
