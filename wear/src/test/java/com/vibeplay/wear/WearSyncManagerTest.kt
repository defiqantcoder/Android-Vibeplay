package com.vibeplay.wear

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.vibeplay.core.wear.WearSyncProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class WearSyncManagerTest {

    @Test
    fun testParseDataMap() {
        val mockContext = mock(Context::class.java)
        val syncManager = WearSyncManager(mockContext)

        // Create a DataMap with test values
        val dataMap = DataMap().apply {
            putString(WearSyncProtocol.KEY_TRACK_ID, "test-track-1")
            putString(WearSyncProtocol.KEY_TITLE, "Test Title")
            putString(WearSyncProtocol.KEY_ARTIST, "Test Artist")
            putString(WearSyncProtocol.KEY_ALBUM, "Test Album")
            putLong(WearSyncProtocol.KEY_DURATION_MS, 120000L)
            putLong(WearSyncProtocol.KEY_POSITION_MS, 60000L)
            putBoolean(WearSyncProtocol.KEY_IS_PLAYING, true)
            putInt(WearSyncProtocol.KEY_SAMPLE_RATE, 44100)
            putInt(WearSyncProtocol.KEY_BIT_DEPTH, 16)
            putBoolean(WearSyncProtocol.KEY_IS_BIT_PERFECT, false)
            putDouble(WearSyncProtocol.KEY_BPM, 128.0)
            putDouble(WearSyncProtocol.KEY_ENERGY, 0.9)
            putBoolean(WearSyncProtocol.KEY_DAC_CONNECTED, false)
            putString(WearSyncProtocol.KEY_DAC_NAME, "None")
            putFloat(WearSyncProtocol.KEY_VOLUME, 0.5f)
        }



        syncManager.parseDataMap(dataMap)

        // Verify the internal playbackState was updated correctly
        val state = syncManager.playbackState.value

        assertEquals("test-track-1", state.trackId)
        assertEquals("Test Title", state.title)
        assertEquals("Test Artist", state.artist)
        assertEquals("Test Album", state.album)
        assertEquals(120000L, state.durationMs)
        assertEquals(60000L, state.positionMs)
        assertTrue(state.isPlaying)
        assertEquals(44100, state.sampleRate)
        assertEquals(16, state.bitDepth)
        assertFalse(state.isBitPerfect)
        assertEquals(128.0, state.bpm, 0.01)
        assertEquals(0.9, state.energy, 0.01)
        assertFalse(state.dacConnected)
        assertEquals("None", state.dacName)
        assertEquals(0.5f, state.volume, 0.01f)
    }

    @Test
    fun testParseDataMap_partialUpdate_keepsExistingValues() {
        val mockContext = mock(Context::class.java)
        val syncManager = WearSyncManager(mockContext)




        val initialDataMap = DataMap().apply {
            putString(WearSyncProtocol.KEY_TRACK_ID, "initial-track")
            putString(WearSyncProtocol.KEY_ARTIST, "Initial Artist")
            putString(WearSyncProtocol.KEY_ALBUM, "Initial Album")
            putLong(WearSyncProtocol.KEY_DURATION_MS, 180000L)
        }
        syncManager.parseDataMap(initialDataMap)
        val initialState = syncManager.playbackState.value

        val dataMap = DataMap().apply {
            putString(WearSyncProtocol.KEY_TITLE, "New Title Only")
            // Not setting anything else
        }

        syncManager.parseDataMap(dataMap)

        val state = syncManager.playbackState.value

        // Updated value
        assertEquals("New Title Only", state.title)

        // Kept existing values
        assertEquals(initialState.trackId, state.trackId)
        assertEquals(initialState.artist, state.artist)
        assertEquals(initialState.album, state.album)
        assertEquals(initialState.durationMs, state.durationMs)
    }


    @Test
    fun testParseCrateItems() {
        val mockContext = mock(Context::class.java)
        val syncManager = WearSyncManager(mockContext)

        val trackDataMap = DataMap().apply {
            putString(WearSyncProtocol.KEY_TRACK_ID, "track-123")
            putString(WearSyncProtocol.KEY_TITLE, "Crate Track")
            putString(WearSyncProtocol.KEY_ARTIST, "Crate Artist")
            putString(WearSyncProtocol.KEY_ALBUM, "Crate Album")
            putDouble(WearSyncProtocol.KEY_BPM, 130.0)
            putString("format", "WAV")
        }

        val dataMap = DataMap().apply {
            putDataMapArrayList("tracks", arrayListOf(trackDataMap))
        }

        syncManager.parseCrateItems(dataMap)

        val tracks = syncManager.tracks.value
        assertEquals(1, tracks.size)

        val track = tracks[0]
        assertEquals("track-123", track.id)
        assertEquals("Crate Track", track.title)
        assertEquals("Crate Artist", track.artist)
        assertEquals("Crate Album", track.album)
        assertEquals(130, track.bpm)
        assertEquals("WAV", track.format)

        // Assert defaults
        assertEquals("0:00", track.duration)
        assertEquals(0L, track.durationMs)
        assertEquals("1A", track.key)
        assertEquals(0.5, track.energy, 0.01)
        assertEquals(0.5, track.danceability, 0.01)
        assertEquals("crate", track.crateId)
        assertEquals("44.1 kHz", track.sampleRate)
        assertEquals("16-bit", track.bitDepth)
    }
}
