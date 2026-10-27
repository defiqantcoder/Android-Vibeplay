package com.vibeplay.app.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.vibeplay.app.ui.VibePlayUiState
import com.vibeplay.core.wear.WearSyncProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Global event bus for incoming commands sent from connected Wear OS watches
 */
object PhoneWearBridgeEvents {
    private val _incomingCommands = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingCommands: SharedFlow<String> = _incomingCommands.asSharedFlow()

    fun dispatchCommand(command: String) {
        _incomingCommands.tryEmit(command)
    }
}

/**
 * Bridges phone playback state and crate collections to Wear OS via DataClient.
 */
class PhoneWearBridge(private val context: Context) {
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    fun syncPlaybackState(state: VibePlayUiState) {
        scope.launch {
            try {
                val track = state.currentTrack
                val putDataMapReq = PutDataMapRequest.create(WearSyncProtocol.DATA_PATH_PLAYBACK).apply {
                    dataMap.putString(WearSyncProtocol.KEY_TRACK_ID, track?.id ?: "")
                    dataMap.putString(WearSyncProtocol.KEY_TITLE, track?.title ?: "No Track")
                    dataMap.putString(WearSyncProtocol.KEY_ARTIST, track?.artist ?: "Unknown Artist")
                    dataMap.putString(WearSyncProtocol.KEY_ALBUM, track?.album ?: "Unknown Album")
                    dataMap.putLong(WearSyncProtocol.KEY_DURATION_MS, state.durationMs)
                    dataMap.putLong(WearSyncProtocol.KEY_POSITION_MS, state.positionMs)
                    dataMap.putBoolean(WearSyncProtocol.KEY_IS_PLAYING, state.isPlaying)
                    val sampleRateInt = track?.sampleRate?.filter { it.isDigit() }?.toIntOrNull() ?: 44100
                    val bitDepthInt = track?.bitDepth?.filter { it.isDigit() }?.toIntOrNull() ?: 16
                    dataMap.putInt(WearSyncProtocol.KEY_SAMPLE_RATE, sampleRateInt)
                    dataMap.putInt(WearSyncProtocol.KEY_BIT_DEPTH, bitDepthInt)
                    dataMap.putBoolean(WearSyncProtocol.KEY_IS_BIT_PERFECT, state.bitPerfectDirect)
                    dataMap.putDouble(WearSyncProtocol.KEY_BPM, (track?.bpm ?: 120).toDouble())
                    dataMap.putDouble(WearSyncProtocol.KEY_ENERGY, track?.energy ?: 0.7)
                    dataMap.putBoolean(WearSyncProtocol.KEY_DAC_CONNECTED, state.activeEndpoint.isConnected)
                    dataMap.putString(WearSyncProtocol.KEY_DAC_NAME, state.activeEndpoint.name)
                    dataMap.putLong(WearSyncProtocol.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataReq).await()
            } catch (_: Exception) {
                // Wear node might not be reachable or Google Play services unavailable
            }
        }
    }
}
