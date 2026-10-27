package com.vibeplay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeplay.app.ui.theme.*
import com.vibeplay.core.hardware.UniversalDacEndpoint

/**
 * Jetpack Compose Material 3 ModalBottomSheet for Universal Dynamic USB DAC Routing
 * and Dolby Atmos Spatial Audio Interlock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DacOutputBottomSheet(
    connectedDacs: List<UniversalDacEndpoint>,
    activeDac: UniversalDacEndpoint?,
    isUsbDacActive: Boolean,
    exclusiveUsbAccess: Boolean,
    dolbyAtmosEnabled: Boolean,
    dolbyAtmosProfile: String,
    onScanUsbBus: () -> Unit,
    onSelectDac: (UniversalDacEndpoint) -> Unit,
    onMountCustomDac: (modelName: String) -> Unit,
    onSelectPhoneSpeaker: () -> Unit,
    onToggleExclusiveUsb: (Boolean) -> Unit,
    onToggleDolbyAtmos: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customDacName by remember { mutableStateOf("") }
    var showAllDetectedModels by remember { mutableStateOf(false) }

    val isBitPerfect = isUsbDacActive && exclusiveUsbAccess && !dolbyAtmosEnabled

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ContainerSurface,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Audio Output & Universal DAC",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Android UsbManager + UAC 1.0/2.0/3.0 dynamic hardware discovery",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                StatusBadge(
                    label = when {
                        dolbyAtmosEnabled -> "DOLBY ATMOS 3D"
                        isBitPerfect -> "BIT-PERFECT"
                        else -> "OS MIXER 48kHz"
                    },
                    accentColor = when {
                        dolbyAtmosEnabled -> AmberSecondary
                        isBitPerfect -> EmeraldTertiary
                        else -> TextMuted
                    }
                )
            }

            HorizontalDivider(color = ContainerBorder)

            // Hardware Bus Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETECTED HARDWARE SINKS (${connectedDacs.size})",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onScanUsbBus) {
                        Text(
                            text = "Scan USB-C OTG",
                            color = TealPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    OutlinedButton(
                        onClick = { showAllDetectedModels = !showAllDetectedModels },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showAllDetectedModels) "Hide List" else "Switch DAC",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Expandable Multi-Vendor DAC Hardware List & Hot-Mount Input
            if (showAllDetectedModels) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BackgroundDark)
                        .border(1.dp, ContainerBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 170.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(connectedDacs, key = { it.id }) { dac ->
                            val isSelected = activeDac?.id == dac.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) TealPrimary.copy(alpha = 0.15f)
                                        else ContainerSurface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) TealPrimary else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectDac(dac) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dac.modelName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${dac.vendorName} · VID:PID ${dac.usbVidPid} · ${dac.maxBitDepth}/${dac.maxSampleRate}",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "ACTIVE",
                                        color = TealPrimary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Any-DAC Descriptor Hot-Mount Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customDacName,
                            onValueChange = { customDacName = it },
                            placeholder = {
                                Text("Mount any USB DAC (e.g. Topping G5, DragonFly)...", fontSize = 11.sp, color = TextMuted)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = ContainerBorder
                            )
                        )
                        Button(
                            onClick = {
                                if (customDacName.isNotBlank()) {
                                    onMountCustomDac(customDacName.trim())
                                    customDacName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Mount", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Primary Active USB DAC Endpoint Card
            if (activeDac != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isUsbDacActive) ContainerSurface else BackgroundDark)
                        .border(
                            width = if (isUsbDacActive) 2.dp else 1.dp,
                            color = if (isUsbDacActive) TealPrimary else ContainerBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectDac(activeDac) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeDac.modelName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "USB ${activeDac.usbVidPid} · ${activeDac.uacProtocol}",
                            color = TealPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${activeDac.interfaceType} · ${activeDac.maxBitDepth} / ${activeDac.maxSampleRate} · ${activeDac.dsdCapability}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Clock: ${activeDac.clockArchitecture} · Chipset: ${activeDac.chipset}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // Speaker fallback card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ContainerSurface)
                        .border(1.dp, ContainerBorder, RoundedCornerShape(14.dp))
                        .clickable { onSelectPhoneSpeaker() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Internal Phone Speaker", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Operating through Android OS AudioFlinger Mixer", color = TextMuted, fontSize = 11.sp)
                    }
                    Text("ACTIVE", color = AmberSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Exclusive Bit-Perfect USB vs Dolby Atmos Interlock Toggles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BackgroundDark)
                    .border(1.dp, ContainerBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exclusive USB Audio Access (Bit-Perfect)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Android 14+ MIXER_BEHAVIOR_BIT_PERFECT · Disables Dolby Atmos",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = exclusiveUsbAccess && !dolbyAtmosEnabled,
                        onCheckedChange = onToggleExclusiveUsb,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TealPrimary)
                    )
                }

                HorizontalDivider(color = ContainerBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dolby Atmos 3D Spatial Audio",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Profile: ${dolbyAtmosProfile.uppercase()} · Cancels Bit-Perfect USB",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = dolbyAtmosEnabled,
                        onCheckedChange = onToggleDolbyAtmos,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AmberSecondary)
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    contentColor = Color.Black
                )
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = accentColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
