package com.vibeplay.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.vibeplay.app.audio.VibeAudioEngine
import com.vibeplay.app.scanner.SafMusicScanner
import androidx.room.Room
import com.vibeplay.core.db.TrackEntity
import com.vibeplay.core.db.VibePlayDatabase
import com.vibeplay.core.engine.VibeCookerEngine
import com.vibeplay.core.model.*
import com.vibeplay.core.scanner.LocalMusicScanner
import com.vibeplay.app.hardware.UsbDacHardwareManager
import com.vibeplay.app.wear.PhoneWearBridge
import com.vibeplay.app.wear.PhoneWearBridgeEvents
import com.vibeplay.core.hardware.UniversalDacEndpoint
import com.vibeplay.core.wear.WearSyncProtocol
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal fun resolveLibraryDirectory(configuredPath: String?, defaultDirectory: () -> File?): File? =
    configuredPath?.let(::File) ?: defaultDirectory()

data class VibePlayUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 342000L,
    val tracks: List<Track> = emptyList(),
    val crates: List<CrateFolder> = VibeSeedData.INITIAL_CRATES,
    val activeCrateId: String? = null,


    val universalDacs: List<UniversalDacEndpoint> = emptyList(),
    val activeUniversalDac: UniversalDacEndpoint? = null,
    val eqBands: List<EqBand> = VibeSeedData.INITIAL_EQ_BANDS,
    val selectedPresetId: String = "flat",
    val bitPerfectDirect: Boolean = true,
    val exclusiveUsbAccess: Boolean = true,
    val dolbyAtmosEnabled: Boolean = false,
    val atmosProfile: AtmosProfile = AtmosProfile.MUSIC,
    val forceDspOnDac: Boolean = false,
    val themeMode: VibeThemeMode = VibeThemeMode.TEAL,
    val activeCookPrompt: String = "Fast cardio workout >130 bpm",
    val cookedQueue: List<ScoredTrack> = emptyList(),
    val cookLinerNotes: String = "",
    val isScanning: Boolean = false,
    val scanSummary: String = "",
    val earSafetyTriggered: Boolean = false,
    val showNowPlaying: Boolean = false,
    val showLyrics: Boolean = false,
    val showDacModal: Boolean = false,
    val showDeduplicator: Boolean = false,
    val showDriveMode: Boolean = false,
    val showAssistant: Boolean = false,
    val showTrackActions: Boolean = false,
    val selectedTrackForActions: Track? = null,
    val showWidgets: Boolean = false,
    val showOnboarding: Boolean = false,
    val geminiApiKey: String? = null
)

class VibePlayViewModel(
    private val context: android.content.Context? = null,
    private val audioEngine: VibeAudioEngine? = null,
    private val database: VibePlayDatabase? = null,
    private val wearBridge: PhoneWearBridge? = null,
    private val hardwareManager: UsbDacHardwareManager? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(VibePlayUiState())
    val uiState: StateFlow<VibePlayUiState> = _uiState.asStateFlow()
    private var scanJob: Job? = null
    private var scanStarted = false

    init {
        // Observe audioEngine playback state
        audioEngine?.let { engine ->
            viewModelScope.launch {
                engine.isPlaying.collect { playing ->
                    _uiState.value = _uiState.value.copy(isPlaying = playing)
                }
            }
            viewModelScope.launch {
                engine.playbackPositionMs.collect { pos ->
                    _uiState.value = _uiState.value.copy(positionMs = pos)
                }
            }
            viewModelScope.launch {
                engine.durationMs.collect { dur ->
                    if (dur > 0) _uiState.value = _uiState.value.copy(durationMs = dur)
                }
            }
        }

        // Observe Universal USB DAC hardware discovery
        hardwareManager?.let { hw ->
            viewModelScope.launch {
                hw.connectedDacs.collect { dacs ->
                    _uiState.value = _uiState.value.copy(universalDacs = dacs)
                }
            }
            viewModelScope.launch {
                hw.activeDac.collect { active ->
                    _uiState.value = _uiState.value.copy(activeUniversalDac = active)
                }
            }
        }

        // Bridge state updates to connected Wear OS watch
        wearBridge?.let { bridge ->
            viewModelScope.launch {
                _uiState.collect { state ->
                    bridge.syncPlaybackState(state)
                    bridge.syncTracks(state.tracks)
                }
            }
        }

        // Listen to commands dispatched from connected Wear OS watch
        viewModelScope.launch {
            PhoneWearBridgeEvents.incomingCommands.collect { cmd ->
                handleWearCommand(cmd)
            }
        }

        // Populate instantly from the Room cache. Only when nothing is cached yet (first run)
        // do we fall back to scanning: saved tree key, then a configured path, then the app
        // music directory. Afterwards, rescans happen only on demand (Scan Music Folder).
        viewModelScope.launch {
            if (loadCachedTracks()) return@launch
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("vibeplay_prefs", Context.MODE_PRIVATE)
                val treeUri = prefs.getString("library_tree_uri", null) ?: prefs.getString("saf_tree_uri", null)
                if (treeUri != null) {
                    scanSelectedMusicFolder(treeUri)
                } else {
                    val configuredPath = prefs.getString("library_path", null)
                    val directory = resolveLibraryDirectory(configuredPath) { ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC) }
                    if (directory != null) scanLocalMusicFolder(directory) else scanMediaStoreAudio(ctx)
                }
            }
        }
    }

    fun scanSelectedMusicFolder(treeUri: String) {
        val ctx = context ?: return
        scanLibrary("selected folder", fallbackToMediaStore = true) { SafMusicScanner.scanTree(ctx, Uri.parse(treeUri)) }
    }

    fun scanLocalMusicFolder(directory: File) {
        scanLibrary(directory.name, fallbackToMediaStore = context != null) {
            if (directory.exists() && directory.isDirectory) LocalMusicScanner.scanDirectory(directory)
            else emptyList()
        }
    }

    fun scanMediaStoreAudio(context: Context) {
        scanLibrary("MediaStore", clearOnEmpty = true) { mediaStoreTracks(context) }
    }

    private fun scanLibrary(
        source: String,
        clearOnEmpty: Boolean = false,
        fallbackToMediaStore: Boolean = false,
        scan: suspend () -> List<Track>
    ) {
        scanStarted = true
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanSummary = "")
            try {
                val (scannedTracks, actualSource) = withContext(ioDispatcher) {
                    val found = scan()
                    if (found.isEmpty() && fallbackToMediaStore && context != null) {
                        mediaStoreTracks(context) to "MediaStore"
                    } else found to source
                }
                if (scannedTracks.isEmpty() && !clearOnEmpty && !fallbackToMediaStore) {
                    _uiState.value = _uiState.value.copy(isScanning = false)
                    cookMixtape(_uiState.value.activeCookPrompt)
                    return@launch
                }
                val losslessCrate = CrateFolder(
                    id = "crate-lossless-local",
                    name = "Local Music Vault",
                    trackCount = scannedTracks.size,
                    energyArc = "Dynamic High-Res",
                    lastCooked = "Live Indexed"
                )
                _uiState.value = _uiState.value.copy(
                    tracks = scannedTracks,
                    crates = if (scannedTracks.isEmpty()) VibeSeedData.INITIAL_CRATES
                        else listOf(losslessCrate) + VibeSeedData.INITIAL_CRATES,
                    isScanning = false,
                    scanSummary = "Indexed ${scannedTracks.size} audio tracks from $actualSource"
                )
                persistTracks(scannedTracks)
                cookMixtape(_uiState.value.activeCookPrompt)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanSummary = "Could not scan $source. Check folder access and try again."
                )
            }
        }
    }

    /** Uses the injected database, or lazily opens the on-device library cache. */
    private fun libraryDao() = (database ?: context?.let { cacheDatabase(it) })?.dao()

    /** Loads the cache; returns true when startup scanning is unnecessary or superseded. */
    private suspend fun loadCachedTracks(): Boolean {
        val cached = try {
            withContext(ioDispatcher) { libraryDao()?.getAllTracks().orEmpty().map { it.toTrack() } }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        // Even a completed empty scan supersedes startup cache loading and fallback.
        if (scanStarted) return true
        if (cached.isEmpty()) return false
        val losslessCrate = CrateFolder(
            id = "crate-lossless-local",
            name = "Local Music Vault",
            trackCount = cached.size,
            energyArc = "Dynamic High-Res",
            lastCooked = "Cached"
        )
        _uiState.value = _uiState.value.copy(
            tracks = cached,
            crates = listOf(losslessCrate) + VibeSeedData.INITIAL_CRATES,
            scanSummary = "Loaded ${cached.size} cached tracks"
        )
        cookMixtape(_uiState.value.activeCookPrompt)
        return true
    }

    private suspend fun persistTracks(tracks: List<Track>) {
        try {
            withContext(ioDispatcher) {
                libraryDao()?.replaceTracks(tracks.map { it.toEntity() })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Persistence is best-effort; the in-memory library is already populated.
        }
    }

    private fun Track.toEntity() = TrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        durationMs = durationMs,
        bpm = bpm,
        key = key,
        energy = energy,
        danceability = danceability,
        crateId = crateId,
        format = format,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        coverArt = coverArt,
        localUri = localUri,
        waveformsJson = waveforms.joinToString(","),
        lyricsJson = lyrics.joinToString("|||") { "${it.time}:::${it.text}" }
    )

    private fun TrackEntity.toTrack() = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        durationMs = durationMs,
        bpm = bpm,
        key = key,
        energy = energy,
        danceability = danceability,
        crateId = crateId,
        format = format,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        coverArt = coverArt,
        localUri = localUri,
        waveforms = if (waveformsJson.isBlank()) emptyList() else waveformsJson.split(",").mapNotNull { it.toFloatOrNull() },
        lyrics = if (lyricsJson.isBlank()) emptyList() else lyricsJson.split("|||").mapNotNull {
            val parts = it.split(":::", limit = 2)
            if (parts.size == 2) {
                val time = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                LyricLine(time, parts[1])
            } else null
        }
    )

    private companion object {
        @Volatile private var cacheDb: VibePlayDatabase? = null

        fun cacheDatabase(context: Context): VibePlayDatabase =
            cacheDb ?: synchronized(this) {
                cacheDb ?: Room.databaseBuilder(
                    context.applicationContext,
                    VibePlayDatabase::class.java,
                    "vibeplay_library.db"
                ).build().also { cacheDb = it }
            }
    }

    private fun mediaStoreTracks(context: Context): List<Track> {
        val results = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                var index = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val durationMs = cursor.getLong(durationCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val minutes = (durationMs / 1000) / 60
                    val seconds = (durationMs / 1000) % 60
                    val durationStr = String.format("%d:%02d", minutes, seconds)

                    val hash = kotlin.math.abs(title.hashCode())
                    val bpm = 80 + (hash % 65)
                    val keys = listOf("1A", "1B", "2A", "2B", "3A", "3B", "4A", "4B", "5A", "5B", "6A", "6B",
                        "7A", "7B", "8A", "8B", "9A", "9B", "10A", "10B", "11A", "11B", "12A", "12B")
                    val key = keys[hash % keys.size]
                    val energy = ((bpm - 60).toDouble() / 100.0).coerceIn(0.2, 0.98)
                    val danceability = (1.0 - kotlin.math.abs(bpm - 124) / 80.0).coerceIn(0.3, 0.95)
                    val waveforms = (0 until 24).map { i ->
                        val v = (kotlin.math.sin((hash + i * 17).toDouble()) + 1.0) / 2.0
                        (0.15 + v * 0.85).toFloat().coerceIn(0.1f, 1.0f)
                    }

                    results.add(
                        Track(
                            id = "mediastore-audio-$index",
                            title = title,
                            artist = artist,
                            album = album,
                            duration = durationStr,
                            durationMs = durationMs,
                            bpm = bpm,
                            key = key,
                            energy = energy,
                            danceability = danceability,
                            crateId = "crate-mediastore",
                            format = "AUDIO",
                            sampleRate = "48.0 kHz",
                            bitDepth = "24-bit",
                            localUri = contentUri.toString(),
                            waveforms = waveforms
                        )
                    )
                    index++
                }
            } ?: throw java.io.IOException("Could not read MediaStore")
        return results
    }

    fun playTrack(track: Track) {
        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            isPlaying = true,
            positionMs = 0L
        )
        audioEngine?.playTrack(track)
    }

    fun togglePlayPause() {
        val nextState = !_uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(isPlaying = nextState)
        audioEngine?.togglePlayPause()
    }

    fun seekTo(posMs: Long) {
        _uiState.value = _uiState.value.copy(positionMs = posMs)
        audioEngine?.seekTo(posMs)
    }

    fun setEqBandGain(index: Int, gainDb: Double) {
        val updatedBands = _uiState.value.eqBands.toMutableList()
        if (index in updatedBands.indices) {
            updatedBands[index] = updatedBands[index].copy(gain = gainDb)
            _uiState.value = _uiState.value.copy(eqBands = updatedBands, selectedPresetId = "custom")
            audioEngine?.updatePeqGains(updatedBands.map { it.gain })
        }
    }

    fun applyPreset(preset: EqPreset) {
        val updatedBands = _uiState.value.eqBands.mapIndexed { index, band ->
            val g = preset.gains.getOrElse(index) { 0.0 }
            band.copy(gain = g)
        }
        _uiState.value = _uiState.value.copy(
            eqBands = updatedBands,
            selectedPresetId = preset.id
        )
        audioEngine?.updatePeqGains(preset.gains)
    }

    fun cookMixtape(prompt: String) {
        viewModelScope.launch {
            val rules = VibeCookerEngine.parsePromptToSqlBounds(prompt)
            val scored = VibeCookerEngine.scoreAndRankTracks(_uiState.value.tracks, rules)
            val linerNotes = VibeCookerEngine.synthesizeWithGemini(
                prompt,
                scored.map { it.track },
                _uiState.value.geminiApiKey
            )
            _uiState.value = _uiState.value.copy(
                activeCookPrompt = prompt,
                cookedQueue = scored,
                cookLinerNotes = linerNotes
            )
        }
    }

    fun setGeminiApiKey(key: String?) {
        _uiState.value = _uiState.value.copy(geminiApiKey = key)
    }

    fun setBitPerfect(enabled: Boolean) {
        val newAtmos = if (enabled) false else _uiState.value.dolbyAtmosEnabled
        _uiState.value = _uiState.value.copy(
            bitPerfectDirect = enabled,
            dolbyAtmosEnabled = newAtmos
        )
        audioEngine?.setBitPerfectMode(enabled, _uiState.value.exclusiveUsbAccess)
        hardwareManager?.configureBitPerfectRouting(enabled)
    }

    fun setDolbyAtmos(enabled: Boolean) {
        val newBitPerfect = if (enabled) false else _uiState.value.bitPerfectDirect
        _uiState.value = _uiState.value.copy(
            dolbyAtmosEnabled = enabled,
            bitPerfectDirect = newBitPerfect
        )
        hardwareManager?.toggleDolbyAtmosSpatial(enabled)
    }

    fun setExclusiveUsb(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(exclusiveUsbAccess = enabled)
        audioEngine?.setBitPerfectMode(_uiState.value.bitPerfectDirect, enabled)
    }

    fun setAtmosProfile(profile: AtmosProfile) {
        _uiState.value = _uiState.value.copy(atmosProfile = profile)
    }

    fun scanUsbBus() {
        hardwareManager?.scanAllConnectedAudioHardware()
    }

    fun selectUniversalDac(dac: UniversalDacEndpoint) {
        hardwareManager?.selectDacEndpoint(dac)
    }

    fun mountCustomDac(name: String) {
        hardwareManager?.mountCustomDac(name)
    }

    fun selectPhoneSpeaker() {
        hardwareManager?.selectPhoneSpeaker()
    }

    fun setForceDspOnDac(force: Boolean) {
        _uiState.value = _uiState.value.copy(forceDspOnDac = force)
    }

    fun onUsbDacAttached(endpoint: UniversalDacEndpoint) {
        val updatedEndpoints = (_uiState.value.universalDacs.filter { it.id != endpoint.id } + endpoint)
        _uiState.value = _uiState.value.copy(
            activeUniversalDac = endpoint,
            universalDacs = updatedEndpoints,
            bitPerfectDirect = true,
            dolbyAtmosEnabled = false
        )
        audioEngine?.setBitPerfectMode(true, _uiState.value.exclusiveUsbAccess)
    }

    fun onUsbDacDetached() {
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            earSafetyTriggered = true,
            bitPerfectDirect = false
        )
        audioEngine?.onDacDetachedSafetyGuard()
    }

    fun dismissSafetyGuard() {
        _uiState.value = _uiState.value.copy(earSafetyTriggered = false)
        audioEngine?.dismissSafetyGuard()
    }

    fun setThemeMode(mode: VibeThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun setNowPlayingVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showNowPlaying = visible)
    }

    fun setLyricsVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showLyrics = visible)
    }

    fun setDacModalVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showDacModal = visible)
    }

    fun setDeduplicatorVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showDeduplicator = visible)
    }

    fun setDriveModeVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showDriveMode = visible)
    }

    fun setAssistantVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showAssistant = visible)
    }

    fun setTrackActionsVisible(visible: Boolean, track: Track? = null) {
        _uiState.value = _uiState.value.copy(
            showTrackActions = visible,
            selectedTrackForActions = track ?: _uiState.value.currentTrack
        )
    }

    fun setWidgetsVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showWidgets = visible)
    }

    fun setOnboardingVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showOnboarding = visible)
    }

    fun selectEndpoint(endpoint: UniversalDacEndpoint) {
        _uiState.value = _uiState.value.copy(
            activeUniversalDac = endpoint,
            bitPerfectDirect = true
        )
        audioEngine?.setBitPerfectMode(true, _uiState.value.exclusiveUsbAccess)
    }

    fun selectCrate(crateId: String?) {
        _uiState.value = _uiState.value.copy(activeCrateId = crateId)
    }

    fun playNextTrack() {
        val current = _uiState.value.currentTrack
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            val currentIndex = tracks.indexOfFirst { it.id == current?.id }
            val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % tracks.size else 0
            playTrack(tracks[nextIndex])
        }
    }

    fun playPreviousTrack() {
        val current = _uiState.value.currentTrack
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            val currentIndex = tracks.indexOfFirst { it.id == current?.id }
            val prevIndex = if (currentIndex <= 0) tracks.size - 1 else currentIndex - 1
            playTrack(tracks[prevIndex])
        }
    }

    fun selectTrackById(trackId: String) {
        val track = _uiState.value.tracks.find { it.id == trackId }
        if (track != null) {
            playTrack(track)
        }
    }

    fun setVolume(volume: Float) {
        audioEngine?.setVolume(volume)
    }

    private fun handleWearCommand(cmd: String) {
        when {
            cmd == WearSyncProtocol.CMD_PLAY_PAUSE -> togglePlayPause()
            cmd == WearSyncProtocol.CMD_PLAY -> if (!_uiState.value.isPlaying) togglePlayPause()
            cmd == WearSyncProtocol.CMD_PAUSE -> if (_uiState.value.isPlaying) togglePlayPause()
            cmd == WearSyncProtocol.CMD_NEXT -> playNextTrack()
            cmd == WearSyncProtocol.CMD_PREVIOUS -> playPreviousTrack()
            cmd == WearSyncProtocol.CMD_TOGGLE_BIT_PERFECT -> setBitPerfect(!_uiState.value.bitPerfectDirect)
            cmd.startsWith(WearSyncProtocol.CMD_SEEK_PREFIX) -> {
                val ms = cmd.removePrefix(WearSyncProtocol.CMD_SEEK_PREFIX).toLongOrNull() ?: 0L
                seekTo(ms)
            }
            cmd.startsWith(WearSyncProtocol.CMD_VOLUME_PREFIX) -> {
                val vol = cmd.removePrefix(WearSyncProtocol.CMD_VOLUME_PREFIX).toFloatOrNull() ?: 1f
                setVolume(vol)
            }
            cmd.startsWith(WearSyncProtocol.CMD_TRIGGER_VIBE_PREFIX) -> {
                val prompt = cmd.removePrefix(WearSyncProtocol.CMD_TRIGGER_VIBE_PREFIX)
                cookMixtape(prompt)
            }
            cmd.startsWith(WearSyncProtocol.CMD_SELECT_TRACK_PREFIX) -> {
                val trackId = cmd.removePrefix(WearSyncProtocol.CMD_SELECT_TRACK_PREFIX)
                selectTrackById(trackId)
            }
            cmd.startsWith(WearSyncProtocol.CMD_BPM_SYNC_PREFIX) -> {
                val bpm = cmd.removePrefix(WearSyncProtocol.CMD_BPM_SYNC_PREFIX).toIntOrNull() ?: 120
                cookMixtape("Workout cardio match BPM ~$bpm")
            }
        }
    }
}
