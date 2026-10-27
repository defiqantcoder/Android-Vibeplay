package com.defiqantcoder.vibeplay.ui.sheets

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
import com.defiqantcoder.vibeplay.hardware.UniversalDacEndpoint

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
    autoSwapOnPlug: Boolean,
    onScanUsbBus: () -> Unit,
    onSelectDac: (UniversalDacEndpoint) -> Unit,
    onMountCustomDac: (modelName: String) -> Unit,
    onSelectPhoneSpeaker: () -> Unit,
    onToggleExclusiveUsb: (Boolean) -> Unit,
    onToggleDolbyAtmos: (Boolean) -> Unit,
    onSelectAtmosProfile: (String) -> Unit,
    onToggleAutoSwap: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customDacName by remember { mutableStateOf("") }
    var showAllDetectedModels by remember { mutableStateOf(false) }

    val isBitPerfect = isUsbDacActive && exclusiveUsbAccess && !dolbyAtmosEnabled

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E24),
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
                        color = Color(0xFFE4E4E7),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Android UsbManager + UAC 1.0/2.0/3.0 dynamic hardware discovery",
                        color = Color(0xFF9CA3AF),
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
                        dolbyAtmosEnabled -> Color(0xFFF59E0B)
                        isBitPerfect -> Color(0xFF10B981)
                        else -> Color(0xFF9CA3AF)
                    }
                )
            }

            HorizontalDivider(color = Color(0xFF2A2A35))

            // Hardware Bus Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETECTED HARDWARE SINKS (${connectedDacs.size})",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onScanUsbBus) {
                        Text(
                            text = "Scan USB-C OTG",
                            color = Color(0xFF06B6D4),
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
                            color = Color(0xFFE4E4E7),
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
                        .background(Color(0xFF16161B))
                        .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(16.dp))
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
                                        if (isSelected) Color(0xFF06B6D4).copy(alpha = 0.15f)
                                        else Color(0xFF1E1E24)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF06B6D4) else Color.Transparent,
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
                                        color = Color(0xFFE4E4E7),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${dac.vendorName} · VID:PID ${dac.usbVidPid} · ${dac.maxBitDepth}/${dac.maxSampleRate}",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color(0xFF06B6D4),
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
                                Text("Mount any USB DAC (e.g. Topping G5, DragonFly)...", fontSize = 11.sp)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (customDacName.isNotBlank()) {
                                    onMountCustomDac(customDacName.trim())
                                    customDacName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF06B6D4),
                                contentColor = Color(0xFF121214)
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
                        .background(if (isUsbDacActive) Color(0xFF25252E) else Color(0xFF16161B))
                        .border(
                            width = if (isUsbDacActive) 2.dp else 1.dp,
                            color = if (isUsbDacActive) Color(0xFF06B6D4) else Color(0xFF2A2A35),
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
                            color = Color(0xFFE4E4E7),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "USB ${activeDac.usbVidPid} · ${activeDac.uacProtocol}",
                            color = Color(0xFF06B6D4),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${activeDac.interfaceType} · ${activeDac.maxBitDepth} / ${activeDac.maxSampleRate} · ${activeDac.dsdCapability}",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Clock: ${activeDac.clockArchitecture} · Chipset: ${activeDac.chipset}",
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Exclusive Bit-Perfect USB vs Dolby Atmos Interlock Toggles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16161B))
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(18.dp))
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
                            color = Color(0xFFE4E4E7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Android 14+ MIXER_BEHAVIOR_BIT_PERFECT · Disables Dolby Atmos",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = exclusiveUsbAccess && !dolbyAtmosEnabled,
                        onCheckedChange = onToggleExclusiveUsb
                    )
                }

                HorizontalDivider(color = Color(0xFF2A2A35))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dolby Atmos 3D Spatial Audio",
                            color = Color(0xFFE4E4E7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Profile: ${dolbyAtmosProfile.uppercase()} · Cancels Bit-Perfect USB",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = dolbyAtmosEnabled,
                        onCheckedChange = onToggleDolbyAtmos
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF06B6D4),
                    contentColor = Color(0xFF121214)
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
