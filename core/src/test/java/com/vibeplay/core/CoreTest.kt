package com.vibeplay.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreTest {

    enum class LoopMode {
        OFF, TRACK, ALL
    }

    class PlaylistManager {
        var queue = mutableListOf<String>()
        var currentIndex = -1
        var loopMode = LoopMode.OFF
        var isShuffle = false

        fun addTrack(trackId: String) {
            queue.add(trackId)
            if (currentIndex == -1) currentIndex = 0
        }

        fun nextTrack() {
            if (queue.isEmpty()) return

            if (isShuffle) {
                currentIndex = (queue.indices).random()
            } else {
                if (loopMode == LoopMode.TRACK) {
                    // Do not change index
                } else if (loopMode == LoopMode.ALL) {
                    currentIndex = (currentIndex + 1) % queue.size
                } else {
                    if (currentIndex < queue.size - 1) {
                        currentIndex++
                    } else {
                        // Reached end, do what we want
                    }
                }
            }
        }
    }

    @Test
    fun `test sequential queue progression`() {
        val manager = PlaylistManager()
        manager.addTrack("Track 1")
        manager.addTrack("Track 2")
        manager.addTrack("Track 3")

        assertEquals(0, manager.currentIndex)
        manager.nextTrack()
        assertEquals(1, manager.currentIndex)
        manager.nextTrack()
        assertEquals(2, manager.currentIndex)
    }

    @Test
    fun `test loop all mode`() {
        val manager = PlaylistManager()
        manager.loopMode = LoopMode.ALL
        manager.addTrack("Track 1")
        manager.addTrack("Track 2")

        assertEquals(0, manager.currentIndex)
        manager.nextTrack()
        assertEquals(1, manager.currentIndex)
        manager.nextTrack()
        assertEquals(0, manager.currentIndex) // should loop back to 0
    }

    @Test
    fun `test loop single track mode`() {
        val manager = PlaylistManager()
        manager.loopMode = LoopMode.TRACK
        manager.addTrack("Track 1")
        manager.addTrack("Track 2")

        assertEquals(0, manager.currentIndex)
        manager.nextTrack()
        assertEquals(0, manager.currentIndex) // should stay on same track
    }
}
