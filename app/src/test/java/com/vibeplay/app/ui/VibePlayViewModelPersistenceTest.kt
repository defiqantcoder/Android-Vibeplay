package com.vibeplay.app.ui

import android.content.Context
import android.content.ContentResolver
import android.database.Cursor
import com.vibeplay.core.db.CrateFolderEntity
import com.vibeplay.core.db.EqBandEntity
import com.vibeplay.core.db.TrackEntity
import com.vibeplay.core.db.VibePlayDao
import com.vibeplay.core.db.VibePlayDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

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
        var getAllTracksCallCount = 0
        var pendingRead: CompletableDeferred<List<TrackEntity>>? = null

        override suspend fun getAllTracks(): List<TrackEntity> {
            getAllTracksCallCount++
            return pendingRead?.await() ?: tracks.toList()
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

        override suspend fun deleteAllTracks() { tracks.clear() }

        override suspend fun getAllCrates(): List<CrateFolderEntity> = emptyList()
        override suspend fun insertCrates(crates: List<CrateFolderEntity>) {}
        override suspend fun getAllEqBands(): List<EqBandEntity> = emptyList()
        override suspend fun insertEqBands(bands: List<EqBandEntity>) {}
        override suspend fun updateBandGain(id: String, gain: Double) {}
    }

    @Test
    fun loadCachedTracksOnLaunch_whenDatabaseHasTracks_populatesUiStateInstantlyWithoutScanning() = runTest {
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
        val fakeDb = mock<VibePlayDatabase> {
            on { dao() } doReturn fakeDao
        }

        val viewModel = VibePlayViewModel(database = fakeDb, ioDispatcher = testDispatcher)

        advanceUntilIdle()

        assertEquals(1, fakeDao.getAllTracksCallCount)
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.tracks.size)
        assertEquals("cached-01", uiState.tracks.first().id)
        assertEquals("Cached Track", uiState.tracks.first().title)
        assertEquals("Loaded 1 cached tracks", uiState.scanSummary)
        assertFalse(uiState.isScanning)
    }

    @Test
    fun loadCachedTracksOnLaunch_whenDatabaseIsEmpty_leavesTracksEmpty() = runTest {
        val fakeDao = FakeDao(emptyList())
        val fakeDb = mock<VibePlayDatabase> {
            on { dao() } doReturn fakeDao
        }

        val viewModel = VibePlayViewModel(database = fakeDb, ioDispatcher = testDispatcher)

        advanceUntilIdle()

        assertEquals(1, fakeDao.getAllTracksCallCount)
        val uiState = viewModel.uiState.value
        assertTrue(uiState.tracks.isEmpty())
        assertFalse(uiState.isScanning)
    }
    private fun cachedTrack(id: String = "cached") = TrackEntity(
        id, "Title", "Artist", "Album", "1:00", 60000L, 120, "8A", 0.5, 0.5,
        "crate", "FLAC", "48 kHz", "24-bit"
    )

    private fun mediaStoreContext(fails: Boolean = false): Context {
        val cursor = mock<Cursor>()
        val resolver = mock<ContentResolver>()
        whenever(resolver.query(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(if (fails) null else cursor)
        return mock { on { contentResolver } doReturn resolver }
    }

    @Test
    fun completedEmptyScan_supersedesPendingCacheRead() = runTest {
        val stale = listOf(cachedTrack())
        val fakeDao = FakeDao(stale)
        val pendingRead = CompletableDeferred<List<TrackEntity>>()
        fakeDao.pendingRead = pendingRead
        val db = mock<VibePlayDatabase> { on { dao() } doReturn fakeDao }
        val viewModel = VibePlayViewModel(database = db, ioDispatcher = testDispatcher)
        runCurrent()
        assertEquals(1, fakeDao.getAllTracksCallCount)

        viewModel.scanMediaStoreAudio(mediaStoreContext())
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isScanning)
        assertTrue(fakeDao.tracks.isEmpty())
        assertEquals(listOf(emptyList<TrackEntity>()), fakeDao.insertedTracks)

        pendingRead.complete(stale)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.tracks.isEmpty())
        assertEquals("Indexed 0 audio tracks from MediaStore", viewModel.uiState.value.scanSummary)
    }

    @Test
    fun failedScan_preservesCachedTracks() = runTest {
        val stale = listOf(cachedTrack())
        val fakeDao = FakeDao(stale)
        val db = mock<VibePlayDatabase> { on { dao() } doReturn fakeDao }
        val viewModel = VibePlayViewModel(database = db, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.scanMediaStoreAudio(mediaStoreContext(fails = true))
        advanceUntilIdle()

        assertEquals(stale, fakeDao.tracks)
        assertEquals("cached", viewModel.uiState.value.tracks.single().id)
        assertTrue(fakeDao.insertedTracks.isEmpty())
        assertTrue(viewModel.uiState.value.scanSummary.startsWith("Could not scan"))
    }

    @Test
    fun replacement_removesTracksAbsentFromSuccessfulScan() = runTest {
        val fakeDao = FakeDao(listOf(cachedTrack("removed"), cachedTrack("retained")))
        val replacement = listOf(cachedTrack("retained"), cachedTrack("new"))
        fakeDao.replaceTracks(replacement)
        assertEquals(replacement, fakeDao.getAllTracks())
    }

}
