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
import com.vibeplay.core.model.AddonPackage
import com.vibeplay.core.model.VibeSeedData
import com.vibeplay.core.model.VibeThemeMode

@Composable
fun AddonsToyboxScreen(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    var addons by remember { mutableStateOf(VibeSeedData.INITIAL_ADDONS) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("EXTENSIONS & PLUGINS", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Addons Toybox", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
        }

        items(addons) { addon ->
            AddonCard(
                addon = addon,
                onToggle = {
                    addons = addons.map {
                        if (it.id == addon.id) it.copy(enabled = !it.enabled) else it
                    }
                }
            )
        }
    }
}

@Composable
fun AddonCard(addon: AddonPackage, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ContainerSurface)
            .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(addon.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text("v${addon.version} • ${addon.author}", color = TextMuted, fontSize = 12.sp)
            }
            Switch(
                checked = addon.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = TealPrimary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = ContainerBorder
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(addon.description, color = TextPrimary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Impact: ${addon.dspImpact}", color = AmberSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Latency: ${addon.latencyMs}ms", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("CONFIGURATION", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Audiophile Settings", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
        }

        // Theme Switcher
        item {
            Text("INTERFACE THEME", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VibeThemeMode.values().forEach { mode ->
                    val isSelected = state.themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ContainerSurface)
                            .border(1.dp, if (isSelected) TealPrimary else ContainerBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setThemeMode(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            mode.displayName.split(" ")[0],
                            color = if (isSelected) TealPrimary else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Audio Engine Toggles
        item {
            Text("AUDIO PIPELINE", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContainerSurface)
                    .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bit-Perfect Direct USB", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                        Text("Bypasses Android 48kHz SRC mixer", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = state.bitPerfectDirect,
                        onCheckedChange = { viewModel.setBitPerfect(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = TealPrimary
                        )
                    )
                }

                HorizontalDivider(color = ContainerBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dolby Atmos Spatializer", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                        Text("Mutually exclusive with Bit-Perfect", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = state.dolbyAtmosEnabled,
                        onCheckedChange = { viewModel.setDolbyAtmos(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AmberSecondary
                        )
                    )
                }
            }
        }

        // Showcase Modals & Launchers
        item {
            Text("MODES & INTEGRATIONS", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContainerSurface)
                    .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.setDriveModeVisible(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AmberSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch Drive Mode", color = TextPrimary)
                }

                Button(
                    onClick = { viewModel.setWidgetsVisible(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Home Screen Widgets Showcase", color = TextPrimary)
                }

                Button(
                    onClick = { viewModel.setDeduplicatorVisible(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = EmeraldTertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lossless Library Deduplicator", color = TextPrimary)
                }

                Button(
                    onClick = { viewModel.setOnboardingVisible(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review Welcome Onboarding", color = TextMuted)
                }
            }
        }
    }
}
