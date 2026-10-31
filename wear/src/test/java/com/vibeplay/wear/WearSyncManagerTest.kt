package com.vibeplay.wear

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.vibeplay.core.wear.WearSyncProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.lang.reflect.Method

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
        }

        // Use reflection to access the private parseDataMap method
        val parseDataMapMethod: Method = WearSyncManager::class.java.getDeclaredMethod("parseDataMap", DataMap::class.java)
        parseDataMapMethod.isAccessible = true
        parseDataMapMethod.invoke(syncManager, dataMap)

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
    }

    @Test
    fun testParseDataMap_partialUpdate_keepsExistingValues() {
        val mockContext = mock(Context::class.java)
        val syncManager = WearSyncManager(mockContext)

        // Initial state is the default one

        val dataMap = DataMap().apply {
            putString(WearSyncProtocol.KEY_TITLE, "New Title Only")
            // Not setting anything else
        }

        val parseDataMapMethod: Method = WearSyncManager::class.java.getDeclaredMethod("parseDataMap", DataMap::class.java)
        parseDataMapMethod.isAccessible = true
        parseDataMapMethod.invoke(syncManager, dataMap)

        val state = syncManager.playbackState.value

        // Updated value
        assertEquals("New Title Only", state.title)

        // Kept existing values
        val defaultState = WearPlaybackState()
        assertEquals(defaultState.trackId, state.trackId)
        assertEquals(defaultState.artist, state.artist)
        assertEquals(defaultState.album, state.album)
        assertEquals(defaultState.durationMs, state.durationMs)
    }
}
