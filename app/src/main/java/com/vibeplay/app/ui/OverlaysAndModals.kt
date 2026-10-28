package com.vibeplay.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeplay.app.ui.theme.*
import com.vibeplay.core.model.EndpointType
import com.vibeplay.core.model.Track
import com.vibeplay.core.hardware.UniversalDacEndpoint
import com.vibeplay.core.model.VibeSeedData
import kotlin.math.cos
import kotlin.math.sin

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

// ==========================================
// 3. EAR SAFETY GUARD MODAL
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
// 4. DAC OUTPUT ROUTING MODAL (Stitch Ref: 769150ee)
// ==========================================
@Composable
fun DacOutputModal(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.setDacModalVisible(false) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Usb, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AUDIO ENDPOINT ROUTING", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Direct hardware routing bypasses Android AudioFlinger 48kHz SRC mixer for bit-perfect output.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                // Endpoints List
                state.universalDacs.forEach { endpoint ->
                    val isSelected = state.activeUniversalDac?.id == endpoint.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ContainerBorder else BackgroundDark)
                            .border(1.dp, if (isSelected) TealPrimary else ContainerBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectEndpoint(endpoint) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectEndpoint(endpoint) },
                            colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(endpoint.modelName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text(endpoint.maxSampleRate, color = if (endpoint.dsdCapability?.isNotEmpty() == true) AmberSecondary else TextMuted, fontSize = 11.sp)
                        }
                        if (true) {
                            Badge(containerColor = TealPrimary.copy(alpha = 0.2f)) {
                                Text("DIRECT", color = TealPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = ContainerBorder)

                // Bit-Perfect switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bit-Perfect Direct USB", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                        Text("Zero SRC resampling", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = state.bitPerfectDirect,
                        onCheckedChange = { viewModel.setBitPerfect(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TealPrimary)
                    )
                }

                // Dolby Atmos spatializer switch (mutually exclusive)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dolby Atmos Spatializer", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                        Text("Disables bit-perfect mode", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = state.dolbyAtmosEnabled,
                        onCheckedChange = { viewModel.setDolbyAtmos(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AmberSecondary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.setDacModalVisible(false) },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ContainerSurface,
        modifier = modifier
    )
}

// ==========================================
// 5. TRACK ACTIONS & ACOUSTIC DETAILS (Stitch Ref: c2f0f68f)
// ==========================================
@Composable
fun TrackActionsSheet(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val track = state.selectedTrackForActions ?: state.currentTrack ?: return

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
            val context = androidx.compose.ui.platform.LocalContext.current
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
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Now spinning ${track.title} by ${track.artist} (${track.sampleRate} ${track.bitDepth}) on VibePlay Lossless Studio")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Track"))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

// ==========================================
// 6. ANDROID MATERIAL YOU WIDGETS (Stitch Ref: 25911bf2)
// ==========================================
@Composable
fun WidgetsShowcaseModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Widgets, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("HOME SCREEN WIDGETS", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "Pin Glance Material You widgets directly to your Android launcher.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Widget 1: Now Playing Waveform (4x2)
                item {
                    WidgetPreviewCard(
                        title = "Now Playing Waveform (4x2)",
                        desc = "Displays dynamic lossless album halo, 24-bar live waveform scrubber, and bit-perfect telemetry.",
                        onAdd = onDismiss
                    )
                }

                // Widget 2: Vibe Cooker Mood Launcher (2x2)
                item {
                    WidgetPreviewCard(
                        title = "Vibe Cooker Quick Launcher (2x2)",
                        desc = "One-tap mood synthesis shortcuts (Cardio, Night Drive, Lo-Fi Coffee) without opening the app.",
                        onAdd = onDismiss
                    )
                }

                // Widget 3: Soundbar PEQ Strip (4x1)
                item {
                    WidgetPreviewCard(
                        title = "Audiophile PEQ Strip (4x1)",
                        desc = "Compact 5-band slider equalizer strip with Flat vs Harman Target quick toggles.",
                        onAdd = onDismiss
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ContainerSurface,
        modifier = modifier
    )
}

@Composable
fun WidgetPreviewCard(title: String, desc: String, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundDark)
            .border(1.dp, ContainerBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = ContainerBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Add", color = TealPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, color = TextMuted, fontSize = 11.sp)
    }
}

// ==========================================
// 7. FIRST-LAUNCH ONBOARDING (Stitch Ref: 5c1c2e9e)
// ==========================================
@Composable
fun WelcomeOnboardingModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.2f))
                        .border(1.dp, TealPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("BIT-PERFECT AUDIO", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Welcome to VibePlay Android", color = TealPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingFeatureRow(
                    icon = Icons.Default.Usb,
                    title = "Direct Hardware DAC Routing",
                    desc = "Bypasses Android OS 48kHz SRC mixer for direct FLAC bitstream delivery to your DAC."
                )
                OnboardingFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Heuristic Vibe Cooker",
                    desc = "Describe any mood or workout tempo to synthesize Camelot harmonic sequenced mixtapes."
                )
                OnboardingFeatureRow(
                    icon = Icons.Default.Equalizer,
                    title = "Studio 10-Band Biquad PEQ",
                    desc = "Audiophile parametric filtering with live phase curve and zero-distortion bypass."
                )
                OnboardingFeatureRow(
                    icon = Icons.Default.Folder,
                    title = "Local Music Library Indexing",
                    desc = "Scans your lossless FLAC collection (/music-test) offline with zero cloud tracking."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Listening", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ContainerSurface,
        modifier = modifier
    )
}

@Composable
fun OnboardingFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
            Text(desc, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

// ==========================================
// 8. PIXEL ASSISTANT / VOICE VIBE SYNTHESIZER (Custom Crafted)
// ==========================================
@Composable
fun PixelAssistantVoiceModal(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var transcript by remember { mutableStateOf("“Play high-energy cardio tracks above 135 BPM with heavy bass”") }

    // Pulsing audio animation
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = { viewModel.setAssistantVisible(false) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PIXEL VOICE ASSISTANT", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text("Speech-to-Vibe Synthesizer", color = TextMuted, fontSize = 11.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pulsing circular waveform sphere
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(ContainerSurface)
                        .border(2.dp, TealPrimary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (size.minDimension / 2.5f) * pulseScale

                        drawCircle(
                            color = TealPrimary.copy(alpha = 0.15f),
                            radius = radius * 1.3f,
                            center = center
                        )
                        drawCircle(
                            color = AmberSecondary.copy(alpha = 0.3f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Listening",
                        tint = TealPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Real-time transcript box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundDark)
                        .border(1.dp, ContainerBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = transcript,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick suggestions
                Text("QUICK SUGGESTIONS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        label = { Text("Chill Lo-Fi", fontSize = 10.sp) },
                        onClick = {
                            transcript = "“Midnight chill lofi study coffee vibes”"
                            viewModel.cookMixtape("Midnight chill lofi study coffee vibes")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SuggestionChip(
                        label = { Text("Cardio 140+", fontSize = 10.sp) },
                        onClick = {
                            transcript = "“Fast cardio workout >140 bpm”"
                            viewModel.cookMixtape("Fast cardio workout >140 bpm")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.cookMixtape(transcript.trim('“', '”'))
                    viewModel.setAssistantVisible(false)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Cook Mixtape from Voice", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.setAssistantVisible(false) }) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = ContainerSurface,
        modifier = modifier
    )
}

// ==========================================
// 9. LOSSLESS DEDUPLICATOR (Stitch Ref)
// ==========================================
@Composable
fun MusicDeduplicatorSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOSSLESS DEDUPLICATOR", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Scanned crate files for identical audio tracks, prioritizing high-resolution lossless masters over compressed rips.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                items(VibeSeedData.INITIAL_DUPLICATE_GROUPS) { group ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .border(1.dp, ContainerBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(group.canonicalTitle, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Text(group.artist, color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        group.candidates.forEach { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${c.format} • ${c.sampleRate} (${c.bitDepth})",
                                    color = if (c.isBestQuality) EmeraldTertiary else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (c.isBestQuality) FontWeight.Bold else FontWeight.Normal
                                )
                                if (c.isBestQuality) {
                                    Badge(containerColor = EmeraldTertiary) {
                                        Text("KEEP MASTER", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Archive", color = AmberSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Archive Redundant Tracks", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = ContainerSurface,
        modifier = modifier
    )
}

// ==========================================
// 10. DRIVE MODE OVERLAY (Stitch Ref: d8447670)
// ==========================================
@Composable
fun DriveModeOverlay(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DRIVE MODE", color = AmberSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = { viewModel.setDriveModeVisible(false) }) {
                Icon(Icons.Default.Close, contentDescription = "Exit Drive Mode", tint = TextPrimary)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(track.title, fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 28.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(track.artist, color = TealPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("${track.bpm} BPM • ${state.activeUniversalDac?.modelName}", color = TextMuted, fontSize = 14.sp)
        }

        // Oversized Transport Buttons for driving safety
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ContainerSurface)
                    .clickable { viewModel.playPreviousTrack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = TextPrimary, modifier = Modifier.size(48.dp))
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(TealPrimary)
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(56.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ContainerSurface)
                    .clickable { viewModel.playNextTrack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
