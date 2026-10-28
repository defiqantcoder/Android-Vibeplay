package com.vibeplay.app.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeplay.app.ui.VibePlayViewModel
import com.vibeplay.app.ui.theme.*
import com.vibeplay.core.model.Track

// ==========================================
// 1. EAR SAFETY GUARD MODAL
// ==========================================
@Composable
fun DacDisconnectedModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hearing, contentDescription = null, tint = AmberSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EAR-SAFETY GUARD ENGAGED", color = AmberSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Text(
                "An external USB DAC endpoint was unplugged during playback. Audio was muted instantly to prevent sudden loud speaker blast.",
                color = TextPrimary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary)
            ) {
                Text("Acknowledge & Resume", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ContainerSurface,
        modifier = modifier
    )
}

// ==========================================
// 2. TRACK ACTIONS & ACOUSTIC DETAILS (Stitch Ref: c2f0f68f)
// ==========================================
@Composable
fun TrackActionsSheet(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val track: Track = state.selectedTrackForActions ?: state.currentTrack ?: return

    AlertDialog(
        onDismissRequest = { viewModel.setTrackActionsVisible(false) },
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ContainerSurface)
                            .border(1.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = TealPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp, maxLines = 1)
                        Text(track.artist, color = TextMuted, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge(containerColor = BackgroundDark) {
                        Text("${track.format} ${track.sampleRate} (${track.bitDepth})", color = TealPrimary, fontSize = 10.sp)
                    }
                    Badge(containerColor = BackgroundDark) {
                        Text("${track.bpm} BPM • Key ${track.key}", color = AmberSecondary, fontSize = 10.sp)
                    }
                }
            }
        },
        text = {
            val context = LocalContext.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrackActionRow(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Play Now in Lossless Direct",
                    onClick = {
                        viewModel.playTrack(track)
                        viewModel.setTrackActionsVisible(false)
                    }
                )
                TrackActionRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Cook Vibe from this Track",
                    onClick = {
                        viewModel.setTrackActionsVisible(false)
                        viewModel.cookMixtape("Similar to ${track.title} at ${track.bpm} BPM in ${track.key}")
                    }
                )
                TrackActionRow(
                    icon = Icons.Default.CleaningServices,
                    title = "Scan for Better Quality Master Rip",
                    onClick = {
                        viewModel.setTrackActionsVisible(false)
                        viewModel.setDeduplicatorVisible(true)
                    }
                )
                TrackActionRow(
                    icon = Icons.Default.Share,
                    title = "Share Lossless Audio Meta",
                    onClick = {
                        viewModel.setTrackActionsVisible(false)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Now spinning ${track.title} by ${track.artist} (${track.sampleRate} ${track.bitDepth}) on VibePlay Lossless Studio")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.setTrackActionsVisible(false) }) {
                Text("Close", color = TextMuted)
            }
        },
        containerColor = ContainerSurface,
        modifier = modifier
    )
}

@Composable
fun TrackActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
