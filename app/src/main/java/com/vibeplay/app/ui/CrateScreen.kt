package com.vibeplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeplay.app.ui.theme.*
import com.vibeplay.core.model.CrateFolder
import com.vibeplay.core.model.Track

@Composable
fun CrateScreen(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Indexed Stream

    Column(modifier = modifier.fillMaxSize().background(BackgroundDark).padding(horizontal = 16.dp)) {
        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ContainerSurface)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 0) TealPrimary else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Overview",
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == 0) Color.Black else TextMuted,
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 1) TealPrimary else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Indexed Stream (${state.tracks.size})",
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == 1) Color.Black else TextMuted,
                    fontSize = 14.sp
                )
            }
        }

        if (selectedTab == 0) {
            // Overview Bento Grid
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text("ACTIVE CRATES", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.crates.forEach { crate ->
                            CrateCard(
                                crate = crate,
                                onClick = {
                                    viewModel.selectCrate(crate.id)
                                    selectedTab = 1
                                }
                            )
                        }
                    }
                }

                item {
                    Text("HEAVY ROTATION", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.tracks.take(4).forEach { track ->
                            TrackRow(
                                track = track,
                                isCurrent = state.currentTrack?.id == track.id,
                                onPlay = { viewModel.playTrack(track) },
                                onMoreClick = { viewModel.setTrackActionsVisible(true, track) }
                            )
                        }
                    }
                }
            }
        } else {
            // Full Indexed Stream (filtered if crate is selected)
            val filteredTracks = if (state.activeCrateId != null) {
                state.tracks.filter { it.crateId == state.activeCrateId }
            } else {
                state.tracks
            }
            val activeCrateName = state.crates.find { it.id == state.activeCrateId }?.name

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.activeCrateId != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filtered by: ${activeCrateName ?: "Crate"}",
                                color = TealPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { viewModel.selectCrate(null) }) {
                                Text("Clear Filter", color = AmberSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                items(filteredTracks) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = state.currentTrack?.id == track.id,
                        onPlay = { viewModel.playTrack(track) },
                        onMoreClick = { viewModel.setTrackActionsVisible(true, track) }
                    )
                }
            }
        }
    }
}

@Composable
fun CrateCard(crate: CrateFolder, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ContainerSurface)
            .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TealPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = TealPrimary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(crate.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
            Text("${crate.trackCount} tracks • ${crate.energyArc}", color = TextMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) ContainerSurface.copy(alpha = 0.8f) else ContainerSurface)
            .border(1.dp, if (isCurrent) TealPrimary.copy(alpha = 0.5f) else ContainerBorder, RoundedCornerShape(12.dp))
            .clickable { onPlay() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrent) TealPrimary else ContainerBorder),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isCurrent) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrent) Color.Black else TextMuted
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.SemiBold, color = if (isCurrent) TealPrimary else TextPrimary, fontSize = 14.sp)
            Text("${track.artist} • ${track.format} ${track.sampleRate}", color = TextMuted, fontSize = 12.sp)
        }
        Text("${track.bpm} BPM", color = AmberSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
