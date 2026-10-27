package com.vibeplay.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioEngineTest {

    enum class PlaybackState {
        IDLE, BUFFERING, PLAYING, PAUSED, STOPPED
    }

    class MockAudioEngine {
        var state = PlaybackState.IDLE
        var currentPosition = 0L

        fun play() {
            if (state == PlaybackState.IDLE || state == PlaybackState.STOPPED) {
                state = PlaybackState.BUFFERING
                // Mock transition to playing after buffer
                state = PlaybackState.PLAYING
            } else if (state == PlaybackState.PAUSED) {
                state = PlaybackState.PLAYING
            }
        }

        fun pause() {
            if (state == PlaybackState.PLAYING || state == PlaybackState.BUFFERING) {
                state = PlaybackState.PAUSED
            }
        }

        fun stop() {
            state = PlaybackState.STOPPED
            currentPosition = 0L
        }

        fun seekTo(position: Long) {
            currentPosition = position
        }
    }

    @Test
    fun `test state transitions`() {
        val engine = MockAudioEngine()
        assertEquals(PlaybackState.IDLE, engine.state)

        engine.play()
        assertEquals(PlaybackState.PLAYING, engine.state)

        engine.pause()
        assertEquals(PlaybackState.PAUSED, engine.state)

        engine.play()
        assertEquals(PlaybackState.PLAYING, engine.state)

        engine.stop()
        assertEquals(PlaybackState.STOPPED, engine.state)
    }

    @Test
    fun `test seek functionality`() {
        val engine = MockAudioEngine()
        engine.play()
        engine.seekTo(1000L)
        assertEquals(1000L, engine.currentPosition)
    }
}
