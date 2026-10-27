package com.vibeplay.wear

import android.content.Context
import com.google.android.gms.wearable.*
import com.vibeplay.core.wear.WearSyncProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WearPlaybackState(
    val trackId: String = "seed-01",
    val title: String = "Midnight City Lights",
    val artist: String = "Kavinsky & Daft Sound",
    val album: String = "Outrun Resonance 2026",
    val durationMs: Long = 342000L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val sampleRate: Int = 96000,
    val bitDepth: Int = 24,
    val isBitPerfect: Boolean = true,
    val bpm: Double = 124.0,
    val energy: Double = 0.82,
    val dacConnected: Boolean = true,
    val dacName: String = "Qudelix-5K USB Direct",
    val volume: Float = 0.75f
)

class WearSyncManager(private val context: Context) : DataClient.OnDataChangedListener {

    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _playbackState = MutableStateFlow(WearPlaybackState())
    val playbackState: StateFlow<WearPlaybackState> = _playbackState.asStateFlow()

    fun register() {
        dataClient.addListener(this)
        fetchInitialData()
    }

    fun unregister() {
        dataClient.removeListener(this)
    }

    private fun fetchInitialData() {
        scope.launch {
            try {
                val dataItemBuffer = dataClient.dataItems.await()
                for (item in dataItemBuffer) {
                    if (item.uri.path == WearSyncProtocol.DATA_PATH_PLAYBACK) {
                        parseDataMap(DataMapItem.fromDataItem(item).dataMap)
                    }
                }
                dataItemBuffer.release()
            } catch (_: Exception) {}
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearSyncProtocol.DATA_PATH_PLAYBACK
            ) {
                parseDataMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
    }

    private fun parseDataMap(dataMap: DataMap) {
        _playbackState.value = _playbackState.value.copy(
            trackId = dataMap.getString(WearSyncProtocol.KEY_TRACK_ID, _playbackState.value.trackId),
            title = dataMap.getString(WearSyncProtocol.KEY_TITLE, _playbackState.value.title),
            artist = dataMap.getString(WearSyncProtocol.KEY_ARTIST, _playbackState.value.artist),
            album = dataMap.getString(WearSyncProtocol.KEY_ALBUM, _playbackState.value.album),
            durationMs = dataMap.getLong(WearSyncProtocol.KEY_DURATION_MS, _playbackState.value.durationMs),
            positionMs = dataMap.getLong(WearSyncProtocol.KEY_POSITION_MS, _playbackState.value.positionMs),
            isPlaying = dataMap.getBoolean(WearSyncProtocol.KEY_IS_PLAYING, _playbackState.value.isPlaying),
            sampleRate = dataMap.getInt(WearSyncProtocol.KEY_SAMPLE_RATE, _playbackState.value.sampleRate),
            bitDepth = dataMap.getInt(WearSyncProtocol.KEY_BIT_DEPTH, _playbackState.value.bitDepth),
            isBitPerfect = dataMap.getBoolean(WearSyncProtocol.KEY_IS_BIT_PERFECT, _playbackState.value.isBitPerfect),
            bpm = dataMap.getDouble(WearSyncProtocol.KEY_BPM, _playbackState.value.bpm),
            energy = dataMap.getDouble(WearSyncProtocol.KEY_ENERGY, _playbackState.value.energy),
            dacConnected = dataMap.getBoolean(WearSyncProtocol.KEY_DAC_CONNECTED, _playbackState.value.dacConnected),
            dacName = dataMap.getString(WearSyncProtocol.KEY_DAC_NAME, _playbackState.value.dacName)
        )
    }

    fun sendCommand(command: String) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val payload = command.toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearSyncProtocol.MESSAGE_PATH_COMMAND, payload).await()
                }
            } catch (_: Exception) {}
        }
    }

    fun togglePlayPause() {
        val nextPlay = !_playbackState.value.isPlaying
        _playbackState.value = _playbackState.value.copy(isPlaying = nextPlay)
        sendCommand(WearSyncProtocol.CMD_PLAY_PAUSE)
    }

    fun nextTrack() {
        sendCommand(WearSyncProtocol.CMD_NEXT)
    }

    fun prevTrack() {
        sendCommand(WearSyncProtocol.CMD_PREVIOUS)
    }

    fun setVolume(volume: Float) {
        val coerced = volume.coerceIn(0f, 1f)
        _playbackState.value = _playbackState.value.copy(volume = coerced)
        sendCommand(WearSyncProtocol.buildVolumeCommand(coerced))
    }

    fun seekTo(positionMs: Long) {
        _playbackState.value = _playbackState.value.copy(positionMs = positionMs)
        sendCommand(WearSyncProtocol.buildSeekCommand(positionMs))
    }

    fun triggerVibePrompt(prompt: String) {
        sendCommand(WearSyncProtocol.buildVibePromptCommand(prompt))
    }

    fun selectTrack(trackId: String) {
        sendCommand(WearSyncProtocol.buildSelectTrackCommand(trackId))
    }

    fun syncBpm(bpm: Int) {
        sendCommand(WearSyncProtocol.buildBpmSyncCommand(bpm))
    }

    fun toggleBitPerfect() {
        sendCommand(WearSyncProtocol.CMD_TOGGLE_BIT_PERFECT)
    }
}
