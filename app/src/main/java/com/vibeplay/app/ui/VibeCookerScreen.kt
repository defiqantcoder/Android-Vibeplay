package com.vibeplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun VibeCookerScreen(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var promptInput by remember { mutableStateOf(state.activeCookPrompt) }

    val quickPrompts = listOf(
        "Fast cardio workout >130 bpm",
        "Midnight chill lofi study coffee",
        "Neon Tokyo synthwave 125 bpm",
        "Acoustic audiophile DSD masters"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("VIBE COOKER", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Harmonic Mixtape Synthesizer", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
        }

        // Prompt Input
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContainerSurface)
                    .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("Describe the vibe, BPM, or mood...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = ContainerBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setAssistantVisible(true) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", tint = TealPrimary)
                    }

                    Button(
                        onClick = { viewModel.cookMixtape(promptInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cook Local Mixtape", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Influx Presets
        item {
            Text("QUICK INFLUX PRESETS", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPrompts.forEach { qp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ContainerSurface)
                            .border(1.dp, ContainerBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                promptInput = qp
                                viewModel.cookMixtape(qp)
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(qp, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Mixtape Liner Notes
        if (state.cookLinerNotes.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldTertiary.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldTertiary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HARMONIC DJ LINER NOTES", color = EmeraldTertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(state.cookLinerNotes, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Ranked Tracks Queue
        item {
            Text("COOKED HARMONIC QUEUE (${state.cookedQueue.size})", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
        }

        items(state.cookedQueue) { scored ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContainerSurface)
                    .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
                    .clickable { viewModel.playTrack(scored.track) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(scored.track.title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                    Text("${scored.track.artist} • ${scored.harmonicCompatibility}", color = EmeraldTertiary, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(scored.score * 100).toInt()}% Match", fontWeight = FontWeight.Bold, color = TealPrimary, fontSize = 13.sp)
                    Text("${scored.track.bpm} BPM / ${scored.track.key}", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
