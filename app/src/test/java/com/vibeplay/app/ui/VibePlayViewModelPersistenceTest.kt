package com.vibeplay.app.ui

import com.vibeplay.core.db.CrateFolderEntity
import com.vibeplay.core.db.EqBandEntity
import com.vibeplay.core.db.TrackEntity
import com.vibeplay.core.db.VibePlayDao
import com.vibeplay.core.db.VibePlayDatabase
import com.vibeplay.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class VibePlayViewModelPersistenceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class FakeDao(initialTracks: List<TrackEntity> = emptyList()) : VibePlayDao {
        val tracks = initialTracks.toMutableList()
        val insertedTracks = mutableListOf<List<TrackEntity>>()
        val getAllTracksCallCount = AtomicInteger(0)
        var clearTracksCallCount = 0

        override suspend fun clearTracks() {
            clearTracksCallCount++
            tracks.clear()
        }

        override suspend fun replaceTracks(tracks: List<TrackEntity>) {
            clearTracks()
            insertTracks(tracks)
        }

        override suspend fun getAllTracks(): List<TrackEntity> {
            getAllTracksCallCount.incrementAndGet()
            return tracks
        }

        override suspend fun getTrackById(id: String): TrackEntity? = tracks.find { it.id == id }

        override suspend fun queryTracksByBounds(
            bpmMin: Int,
            bpmMax: Int,
            energyMin: Double,
            energyMax: Double,
            danceMin: Double,
            danceMax: Double
        ): List<TrackEntity> = tracks

        override suspend fun insertTracks(tracks: List<TrackEntity>) {
            insertedTracks.add(tracks)
            this.tracks.addAll(tracks)
        }

        override suspend fun getAllCrates(): List<CrateFolderEntity> = emptyList()
        override suspend fun insertCrates(crates: List<CrateFolderEntity>) {}
        override suspend fun getAllEqBands(): List<EqBandEntity> = emptyList()
        override suspend fun insertEqBands(bands: List<EqBandEntity>) {}
        override suspend fun updateBandGain(id: String, gain: Double) {}
    }

    @Test
    fun loadCachedTracksOnLaunch_whenDatabaseHasTracks_populatesUiStateInstantlyWithoutScanning() = runTest(testDispatcher) {
        val cachedTrackEntity = TrackEntity(
            id = "cached-01",
            title = "Cached Track",
            artist = "Cached Artist",
            album = "Cached Album",
            duration = "03:45",
            durationMs = 225000L,
            bpm = 120,
            key = "8A",
            energy = 0.75,
            danceability = 0.8,
            crateId = "crate-cached",
            format = "FLAC",
            sampleRate = "96.0 kHz",
            bitDepth = "24-bit",
            waveformsJson = "0.5,0.8,0.3",
            lyricsJson = ""
        )
        val fakeDao = FakeDao(listOf(cachedTrackEntity))

        // Pass null for database to bypass Room initialization and supply a test specific dao provider
        val viewModel = VibePlayViewModel(database = null).apply {
            testDaoProvider = { fakeDao }
        }

        advanceUntilIdle()
        viewModel.loadCachedTracks()
        advanceUntilIdle()

        assertTrue("getAllTracksCallCount should be > 0", fakeDao.getAllTracksCallCount.get() > 0)
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.tracks.size)
        assertEquals("cached-01", uiState.tracks.first().id)
        assertEquals("Cached Track", uiState.tracks.first().title)
        assertEquals("Loaded 1 cached tracks", uiState.scanSummary)
        assertFalse(uiState.isScanning)
    }

    @Test
    fun loadCachedTracksOnLaunch_whenDatabaseIsEmpty_leavesTracksEmpty() = runTest(testDispatcher) {
        val fakeDao = FakeDao(emptyList())

        val viewModel = VibePlayViewModel(database = null).apply {
            testDaoProvider = { fakeDao }
        }

        advanceUntilIdle()
        viewModel.loadCachedTracks()
        advanceUntilIdle()

        assertTrue("getAllTracksCallCount should be > 0", fakeDao.getAllTracksCallCount.get() > 0)
        val uiState = viewModel.uiState.value
        assertTrue(uiState.tracks.isEmpty())
        assertFalse(uiState.isScanning)
    }
}
