package com.vibeplay.core.wear

/**
 * Shared constants and serialization keys for Phone <-> Wear OS synchronization
 * via Google Play Services Wearable DataClient and MessageClient.
 */
object WearSyncProtocol {
    const val DATA_PATH_PLAYBACK = "/vibeplay/playback_state"
    const val DATA_PATH_CRATE_ITEMS = "/vibeplay/crate_items"
    const val MESSAGE_PATH_COMMAND = "/vibeplay/command"

    // Playback Data Map Keys
    const val KEY_TRACK_ID = "track_id"
    const val KEY_TITLE = "title"
    const val KEY_ARTIST = "artist"
    const val KEY_ALBUM = "album"
    const val KEY_DURATION_MS = "duration_ms"
    const val KEY_POSITION_MS = "position_ms"
    const val KEY_IS_PLAYING = "is_playing"
    const val KEY_SAMPLE_RATE = "sample_rate"
    const val KEY_BIT_DEPTH = "bit_depth"
    const val KEY_IS_BIT_PERFECT = "is_bit_perfect"
    const val KEY_BPM = "bpm"
    const val KEY_ENERGY = "energy"
    const val KEY_DAC_CONNECTED = "dac_connected"
    const val KEY_DAC_NAME = "dac_name"
    const val KEY_VOLUME = "volume"
    const val KEY_TIMESTAMP = "timestamp"

    // Command Actions
    const val CMD_PLAY_PAUSE = "PLAY_PAUSE"
    const val CMD_PLAY = "PLAY"
    const val CMD_PAUSE = "PAUSE"
    const val CMD_NEXT = "NEXT"
    const val CMD_PREVIOUS = "PREVIOUS"
    const val CMD_SEEK_PREFIX = "SEEK:"
    const val CMD_VOLUME_PREFIX = "VOLUME:"
    const val CMD_TRIGGER_VIBE_PREFIX = "TRIGGER_VIBE:"
    const val CMD_SELECT_TRACK_PREFIX = "SELECT_TRACK:"
    const val CMD_BPM_SYNC_PREFIX = "BPM_SYNC:"
    const val CMD_TOGGLE_BIT_PERFECT = "TOGGLE_BIT_PERFECT"

    fun buildSeekCommand(positionMs: Long): String = "$CMD_SEEK_PREFIX$positionMs"
    fun buildVolumeCommand(volume: Float): String = "$CMD_VOLUME_PREFIX$volume"
    fun buildVibePromptCommand(prompt: String): String = "$CMD_TRIGGER_VIBE_PREFIX$prompt"
    fun buildSelectTrackCommand(trackId: String): String = "$CMD_SELECT_TRACK_PREFIX$trackId"
    fun buildBpmSyncCommand(bpm: Int): String = "$CMD_BPM_SYNC_PREFIX$bpm"
}
