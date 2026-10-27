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
import java.io.File

data class VibePlayUiState(
    val currentTrack: Track? = VibeSeedData.INITIAL_TRACKS.first(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 342000L,
    val tracks: List<Track> = VibeSeedData.INITIAL_TRACKS,
    val crates: List<CrateFolder> = VibeSeedData.INITIAL_CRATES,
    val activeCrateId: String? = null,
    val activeEndpoint: UsbDacEndpoint = VibeSeedData.INITIAL_USB_DAC_MODELS.first(),
    val availableEndpoints: List<UsbDacEndpoint> = VibeSeedData.INITIAL_USB_DAC_MODELS,
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
                }
            }
        }

        // Listen to commands dispatched from connected Wear OS watch
        viewModelScope.launch {
            PhoneWearBridgeEvents.incomingCommands.collect { cmd ->
                handleWearCommand(cmd)
            }
        }

        // Trigger initial local lossless scan of /home/sinnoh/music-test
        scanLocalMusicFolder(File("/home/sinnoh/music-test"))
    }

    fun scanLocalMusicFolder(directory: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val scannedTracks = withContext(Dispatchers.IO) {
                if (directory.exists() && directory.isDirectory) {
                    LocalMusicScanner.scanDirectory(directory)
                } else {
                    emptyList()
                }
            }

            if (scannedTracks.isNotEmpty()) {
                val combinedTracks = scannedTracks + VibeSeedData.INITIAL_TRACKS
                val losslessCrate = CrateFolder(
                    id = "crate-lossless-local",
                    name = "Local Lossless Master Vault",
                    trackCount = scannedTracks.size,
                    energyArc = "Dynamic High-Res",
                    lastCooked = "Live Indexed"
                )
                val updatedCrates = listOf(losslessCrate) + VibeSeedData.INITIAL_CRATES

                _uiState.value = _uiState.value.copy(
                    tracks = combinedTracks,
                    crates = updatedCrates,
                    isScanning = false,
                    scanSummary = "Indexed ${scannedTracks.size} lossless FLAC tracks from ${directory.name}"
                )
            } else {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }

            // Re-cook initial queue with updated library
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

    fun onUsbDacAttached(endpoint: UsbDacEndpoint) {
        val updatedEndpoints = (_uiState.value.availableEndpoints.filter { it.id != endpoint.id } + endpoint)
        _uiState.value = _uiState.value.copy(
            activeEndpoint = endpoint,
            availableEndpoints = updatedEndpoints,
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

    fun selectEndpoint(endpoint: UsbDacEndpoint) {
        _uiState.value = _uiState.value.copy(
            activeEndpoint = endpoint,
            bitPerfectDirect = endpoint.type == EndpointType.USB_DAC
        )
        audioEngine?.setBitPerfectMode(endpoint.type == EndpointType.USB_DAC, _uiState.value.exclusiveUsbAccess)
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
