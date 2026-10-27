package com.vibeplay.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.vibeplay.core.model.Track
import com.vibeplay.core.model.VibeSeedData
import kotlinx.coroutines.launch
import kotlin.math.sin

val WearBackground = Color(0xFF121214)
val WearPrimary = Color(0xFF06B6D4)
val WearSecondary = Color(0xFFF59E0B)
val WearSurface = Color(0xFF1E1E24)

@Composable
fun PixelWatchApp() {
    val context = LocalContext.current
    val haptics = remember { WatchHapticsController(context) }
    val syncManager = remember { WearSyncManager(context) }

    DisposableEffect(Unit) {
        syncManager.register()
        onDispose {
            syncManager.unregister()
        }
    }

    val playbackState by syncManager.playbackState.collectAsState()
    var selectedScreen by remember { mutableStateOf(0) }

    Scaffold(
        timeText = { TimeText() },
        pageIndicator = {
            HorizontalPageIndicator(
                pageIndicatorState = object : PageIndicatorState {
                    override val pageCount: Int = 5
                    override val pageOffset: Float = 0f
                    override val selectedPage: Int = selectedScreen
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WearBackground),
            contentAlignment = Alignment.Center
        ) {
            when (selectedScreen) {
                0 -> NowPlayingWatchScreen(
                    state = playbackState,
                    haptics = haptics,
                    onVolumeChange = { syncManager.setVolume(it) },
                    onTogglePlay = {
                        syncManager.togglePlayPause()
                        haptics.performClick()
                    },
                    onNextTrack = {
                        syncManager.nextTrack()
                        haptics.performClick()
                    },
                    onPrevTrack = {
                        syncManager.prevTrack()
                        haptics.performClick()
                    },
                    onNextScreen = {
                        selectedScreen = (selectedScreen + 1) % 5
                        haptics.performClick()
                    }
                )
                1 -> WristCrateExplorerScreen(
                    haptics = haptics,
                    onSelectTrack = { trackId ->
                        syncManager.selectTrack(trackId)
                        selectedScreen = 0
                        haptics.performClick()
                    }
                )
                2 -> BiometricBpmSyncScreen(
                    haptics = haptics,
                    onSyncBpm = { bpm ->
                        syncManager.syncBpm(bpm)
                    }
                )
                3 -> QuickVibeVoiceScreen(
                    haptics = haptics,
                    onPromptSelected = { prompt ->
                        syncManager.triggerVibePrompt(prompt)
                        selectedScreen = 0
                    }
                )
                4 -> DacRemoteWatchScreen(
                    state = playbackState,
                    haptics = haptics,
                    onToggleBitPerfect = {
                        syncManager.toggleBitPerfect()
                    }
                )
            }
        }
    }
}

@Composable
fun NowPlayingWatchScreen(
    state: WearPlaybackState,
    haptics: WatchHapticsController,
    onVolumeChange: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onNextScreen: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                // Rotate crown adjusts volume attenuation with haptic ticks
                val delta = event.verticalScrollPixels / 1000f
                val newVol = (state.volume + delta).coerceIn(0f, 1f)
                if (newVol != state.volume) {
                    onVolumeChange(newVol)
                    haptics.performCrownTick()
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        // Circular Radial Scrubber Arc showing Volume & Playback state
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background arc (270 degrees)
            drawArc(
                color = WearSurface,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active progress arc reflecting volume level
            drawArc(
                color = WearPrimary,
                startAngle = 135f,
                sweepAngle = 270f * state.volume,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center Content & Transport Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(state.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
            Text(state.artist, color = WearSecondary, fontSize = 9.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Vol ${(state.volume * 100).toInt()}% • ${(state.sampleRate / 1000)}k", color = Color.Gray, fontSize = 8.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPrevTrack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = WearSurface),
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onTogglePlay,
                    colors = ButtonDefaults.buttonColors(backgroundColor = WearPrimary),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onNextTrack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = WearSurface),
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${state.dacName.take(12)} • ${if (state.isBitPerfect) "Bit-Perfect" else "DSP Active"}",
                color = WearPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNextScreen() }
            )
        }
    }
}

@Composable
fun WristCrateExplorerScreen(
    haptics: WatchHapticsController,
    onSelectTrack: (String) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                listState.dispatchRawDelta(event.verticalScrollPixels)
                haptics.performCrownTick()
                true
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text("Wrist Crates", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WearPrimary)
        }
        items(VibeSeedData.INITIAL_TRACKS) { track ->
            Chip(
                label = { Text(track.title, fontSize = 11.sp, maxLines = 1) },
                secondaryLabel = { Text("${track.bpm.toInt()} BPM • ${track.format}", fontSize = 9.sp) },
                onClick = { onSelectTrack(track.id) },
                colors = ChipDefaults.chipColors(backgroundColor = WearSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BiometricBpmSyncScreen(
    haptics: WatchHapticsController,
    onSyncBpm: (Int) -> Unit
) {
    var isLocked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(if (isLocked) "134 BPM [LOCKED]" else "134 BPM", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text("Cardiopulse Live Sync", fontSize = 9.sp, color = WearSecondary)

        // Real-time ECG waveform canvas
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            for (i in 0 until (width / 4).toInt()) {
                val x = i * 4f
                val y = midY + (sin((i * 0.4).toDouble()) * 7f).toFloat()
                drawCircle(color = WearPrimary.copy(alpha = 0.8f), radius = 1.5f, center = Offset(x, y))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        CompactChip(
            label = { Text(if (isLocked) "Cadence Locked: 134" else "Match Mixtape to 134", fontSize = 9.sp) },
            onClick = {
                isLocked = !isLocked
                onSyncBpm(134)
                haptics.performHeavyCue()
            },
            colors = ChipDefaults.chipColors(backgroundColor = if (isLocked) WearSecondary else WearPrimary)
        )
    }
}

@Composable
fun QuickVibeVoiceScreen(
    haptics: WatchHapticsController,
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Mic, contentDescription = null, tint = WearPrimary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Quick Vibe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactChip(
                label = { Text("Chill", fontSize = 9.sp) },
                onClick = {
                    haptics.performClick()
                    onPromptSelected("Chill ambient study vibe")
                }
            )
            CompactChip(
                label = { Text("Cardio", fontSize = 9.sp) },
                onClick = {
                    haptics.performClick()
                    onPromptSelected("Fast cardio workout >130 bpm")
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactChip(
                label = { Text("Synth", fontSize = 9.sp) },
                onClick = {
                    haptics.performClick()
                    onPromptSelected("Cyberpunk neon French synthwave")
                }
            )
            CompactChip(
                label = { Text("DSD HiFi", fontSize = 9.sp) },
                onClick = {
                    haptics.performClick()
                    onPromptSelected("Audiophile master acoustic DSD")
                }
            )
        }
    }
}

@Composable
fun DacRemoteWatchScreen(
    state: WearPlaybackState,
    haptics: WatchHapticsController,
    onToggleBitPerfect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(state.dacName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = WearPrimary, textAlign = TextAlign.Center)
        Text("${state.sampleRate / 1000} kHz • ${state.bitDepth} bit Direct", fontSize = 9.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(6.dp))
        CompactChip(
            label = { Text(if (state.isBitPerfect) "Bit-Perfect: ON" else "Bit-Perfect: OFF", fontSize = 9.sp) },
            onClick = {
                onToggleBitPerfect()
                haptics.performClick()
            },
            colors = ChipDefaults.chipColors(backgroundColor = if (state.isBitPerfect) WearPrimary else WearSurface)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Safety Guard: Active", fontSize = 8.sp, color = WearSecondary)
    }
}
