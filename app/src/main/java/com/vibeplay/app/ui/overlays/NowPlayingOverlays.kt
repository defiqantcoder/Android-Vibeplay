package com.vibeplay.app.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeplay.app.ui.VibePlayViewModel
import com.vibeplay.app.ui.theme.*
import com.vibeplay.core.model.Track

// ==========================================
// 1. NOW PLAYING OVERLAY
// ==========================================
@Composable
fun NowPlayingOverlay(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val track = state.currentTrack ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setNowPlayingVisible(false) }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = TextPrimary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PLAYING FROM CRATE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(track.album, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row {
                IconButton(onClick = { viewModel.setTrackActionsVisible(true, track) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = TextPrimary)
                }
                IconButton(onClick = { viewModel.setLyricsVisible(true) }) {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Lyrics", tint = TealPrimary)
                }
            }
        }

        // Cover Art & Ambient Halo
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ContainerSurface)
                .border(2.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(96.dp))
        }

        // Title & Track Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(track.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(track.artist, color = TextMuted, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(
                    containerColor = ContainerSurface,
                    modifier = Modifier.clickable { viewModel.setDacModalVisible(true) }
                ) {
                    Text("${track.format} ${track.sampleRate}", color = TealPrimary, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                }
                Badge(containerColor = ContainerSurface) {
                    Text("${track.bpm} BPM / ${track.key}", color = AmberSecondary, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                }
            }
        }

        // Waveform Scrubber
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val bars = track.waveforms.ifEmpty { List(24) { 0.5f } }
            bars.forEachIndexed { i, amp ->
                val progress = (state.positionMs.toFloat() / state.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                val barProgress = i.toFloat() / bars.size
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(amp.coerceIn(0.1f, 1.0f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (barProgress <= progress) TealPrimary else ContainerBorder)
                        .clickable {
                            val targetMs = (barProgress * state.durationMs).toLong()
                            viewModel.seekTo(targetMs)
                        }
                )
            }
        }

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.playPreviousTrack() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(TealPrimary)
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = { viewModel.playNextTrack() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }
        }
    }
}

// ==========================================
// 2. SYNCHRONIZED LYRICS OVERLAY
// ==========================================
@Composable
fun SynchronizedLyricsOverlay(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val track = state.currentTrack ?: return
    val currentSec = state.positionMs / 1000.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setLyricsVisible(false) }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
            }
            Text("SYNCHRONIZED LYRICS", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            itemsIndexed(track.lyrics) { index, line ->
                val isActive = currentSec >= line.time && (index == track.lyrics.lastIndex || currentSec < track.lyrics[index + 1].time)
                Text(
                    text = line.text,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isActive) 22.sp else 16.sp,
                    color = if (isActive) TealPrimary else TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.seekTo((line.time * 1000).toLong()) }
                )
            }
        }
    }
}
