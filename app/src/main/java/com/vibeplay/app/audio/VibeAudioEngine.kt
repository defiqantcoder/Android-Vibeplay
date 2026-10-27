package com.vibeplay.app.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.vibeplay.core.dsp.BiquadPeqEngine
import com.vibeplay.core.model.Track
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VibeAudioEngine(private val context: Context) {

    var exoPlayer: ExoPlayer? = null
        private set

    val biquadAudioProcessor = BiquadPeqAudioProcessor()
    val peqEngine: BiquadPeqEngine get() = biquadAudioProcessor.peqEngine

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Audiophile & Safety State
    val isBitPerfectBypass = MutableStateFlow(false)
    val dspActive = MutableStateFlow(true)
    val earSafetyGuardTriggered = MutableStateFlow(false)

    init {
        initializePlayer()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf<AudioProcessor>(biquadAudioProcessor))
                    .build()
            }
        }

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _durationMs.value = duration
                        }
                    }
                })
            }
    }

    fun playTrack(track: Track) {
        _currentTrack.value = track
        val uri = track.localUri
        if (uri != null) {
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.play()
        } else {
            // Procedural preview mode when no local FLAC exists
            _isPlaying.value = true
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        } ?: run {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackPositionMs.value = positionMs
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    /**
     * Ear-Safety Guard: Triggers immediate mute and pause when external DAC is detached
     */
    fun onDacDetachedSafetyGuard() {
        exoPlayer?.pause()
        _isPlaying.value = false
        earSafetyGuardTriggered.value = true
        isBitPerfectBypass.value = false
        biquadAudioProcessor.isBypassed = false
    }

    fun dismissSafetyGuard() {
        earSafetyGuardTriggered.value = false
    }

    fun setBitPerfectMode(enabled: Boolean, exclusiveUsb: Boolean) {
        val bypass = enabled && exclusiveUsb
        isBitPerfectBypass.value = bypass
        dspActive.value = !bypass
        biquadAudioProcessor.isBypassed = bypass
    }

    fun updatePeqGains(gains: List<Double>) {
        peqEngine.setGains(gains)
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
