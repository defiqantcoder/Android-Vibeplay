package com.vibeplay.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeplay.app.audio.VibeAudioEngine
import com.vibeplay.core.db.VibePlayDatabase
import com.vibeplay.core.engine.VibeCookerEngine
import com.vibeplay.core.model.*
import com.vibeplay.core.scanner.LocalMusicScanner
import com.vibeplay.app.hardware.UsbDacHardwareManager
import com.vibeplay.app.wear.PhoneWearBridge
import com.vibeplay.app.wear.PhoneWearBridgeEvents
import com.vibeplay.core.hardware.UniversalDacEndpoint
import com.vibeplay.core.wear.WearSyncProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile

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
    private val hardwareManager: UsbDacHardwareManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VibePlayUiState())
    val uiState: StateFlow<VibePlayUiState> = _uiState.asStateFlow()

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

        // Trigger initial audio library scan via SAF or MediaStore fallback
        context?.let { ctx ->
            val sharedPrefs = ctx.getSharedPreferences("vibeplay_prefs", Context.MODE_PRIVATE)
            val safUriString = sharedPrefs.getString("saf_tree_uri", null)
            if (!safUriString.isNullOrEmpty()) {
                scanSafDirectory(Uri.parse(safUriString), ctx)
            } else {
                scanMediaStoreAudio(ctx)
            }
        }
    }

    fun scanSafDirectory(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val scannedTracks = withContext(Dispatchers.IO) {
                val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext emptyList<Track>()
                val audioFiles = mutableListOf<DocumentFile>()
                collectAudioFiles(rootDoc, audioFiles)

                val results = mutableListOf<Track>()
                for ((index, docFile) in audioFiles.withIndex()) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        var title: String? = null
                        var artist: String? = null
                        var album: String? = null
                        var durationMs: Long = 0L
                        var sampleRateStr = "44.1 kHz"

                        try {
                            retriever.setDataSource(context, docFile.uri)
                            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            val sr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                            if (sr != null && sr > 0) {
                                sampleRateStr = String.format("%.1f kHz", sr / 1000.0)
                            }
                        } catch (_: Exception) {
                        } finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }

                        val fileName = docFile.name ?: "Track"
                        val finalTitle = if (!title.isNullOrBlank()) title else fileName.substringBeforeLast(".")
                        val finalArtist = if (!artist.isNullOrBlank()) artist else "Unknown Artist"
                        val finalAlbum = if (!album.isNullOrBlank()) album else "Unknown Album"
                        val ext = fileName.substringAfterLast(".", "FLAC").uppercase()

                        val minutes = (durationMs / 1000) / 60
                        val seconds = (durationMs / 1000) % 60
                        val durationStr = String.format("%d:%02d", minutes, seconds)

                        val hash = kotlin.math.abs(fileName.hashCode())
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
                                id = "saf-track-$index",
                                title = finalTitle,
                                artist = finalArtist,
                                album = finalAlbum,
                                duration = durationStr,
                                durationMs = durationMs,
                                bpm = bpm,
                                key = key,
                                energy = energy,
                                danceability = danceability,
                                crateId = "crate-saf",
                                format = ext,
                                sampleRate = sampleRateStr,
                                bitDepth = "24-bit",
                                localUri = docFile.uri.toString(),
                                waveforms = waveforms
                            )
                        )
                    } catch (_: Exception) {}
                }
                results
            }

            if (scannedTracks.isNotEmpty()) {
                val folderName = DocumentFile.fromTreeUri(context, uri)?.name ?: "SAF Vault"
                val losslessCrate = CrateFolder(
                    id = "crate-saf-local",
                    name = "$folderName Vault",
                    trackCount = scannedTracks.size,
                    energyArc = "Dynamic High-Res",
                    lastCooked = "Live Indexed"
                )
                val updatedCrates = listOf(losslessCrate) + VibeSeedData.INITIAL_CRATES

                _uiState.value = _uiState.value.copy(
                    tracks = scannedTracks,
                    crates = updatedCrates,
                    isScanning = false,
                    scanSummary = "Indexed ${scannedTracks.size} lossless audio tracks from $folderName"
                )
                cookMixtape(_uiState.value.activeCookPrompt)
            } else {
                scanMediaStoreAudio(context)
            }
        }
    }

    private fun collectAudioFiles(dir: DocumentFile, list: MutableList<DocumentFile>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                collectAudioFiles(file, list)
            } else if (file.isFile) {
                val name = file.name ?: ""
                val type = file.type
                if (type?.startsWith("audio/") == true ||
                    name.endsWith(".flac", ignoreCase = true) ||
                    name.endsWith(".mp3", ignoreCase = true) ||
                    name.endsWith(".wav", ignoreCase = true) ||
                    name.endsWith(".m4a", ignoreCase = true) ||
                    name.endsWith(".ogg", ignoreCase = true) ||
                    name.endsWith(".aac", ignoreCase = true)
                ) {
                    list.add(file)
                }
            }
        }
    }

    fun scanMediaStoreAudio(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val scannedTracks = withContext(Dispatchers.IO) {
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

                try {
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
                    }
                } catch (_: Exception) {}
                results
            }

            if (scannedTracks.isNotEmpty()) {
                val mediaStoreCrate = CrateFolder(
                    id = "crate-mediastore-local",
                    name = "MediaStore Vault",
                    trackCount = scannedTracks.size,
                    energyArc = "Dynamic High-Res",
                    lastCooked = "Live Indexed"
                )
                val updatedCrates = listOf(mediaStoreCrate) + VibeSeedData.INITIAL_CRATES

                _uiState.value = _uiState.value.copy(
                    tracks = scannedTracks,
                    crates = updatedCrates,
                    isScanning = false,
                    scanSummary = "Indexed ${scannedTracks.size} audio tracks from MediaStore"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanSummary = "MediaStore scan complete (0 tracks found, default crates active)"
                )
            }

            cookMixtape(_uiState.value.activeCookPrompt)
        }
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
