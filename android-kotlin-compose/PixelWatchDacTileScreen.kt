package com.defiqantcoder.vibeplay.wear.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.defiqantcoder.vibeplay.hardware.UniversalDacEndpoint
import kotlinx.coroutines.launch

/**
 * Wear OS Jetpack Compose Remote DAC & Spatial Audio Tile for Pixel Watch 3 (44mm, 456×456px).
 *
 * Displays any dynamically recognized USB DAC connected to the companion Pixel phone,
 * supports Rotary Crown hardware volume attenuation with haptic detents, and toggles
 * Bit-Perfect Exclusive USB vs. Dolby Atmos 3D Spatial Audio from the wrist.
 */
@Composable
fun PixelWatchDacTileScreen(
    activeDac: UniversalDacEndpoint?,
    exclusiveBitPerfect: Boolean,
    dolbyAtmosEnabled: Boolean,
    hardwareVolumePct: Int,
    onAdjustHardwareVolume: (Int) -> Unit,
    onToggleBitPerfect: () -> Unit,
    onToggleDolbyAtmos: () -> Unit,
    onCycleConnectedDac: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val localView = LocalView.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    val step = if (event.verticalScrollPixels > 0) 2 else -2
                    val nextVol = (hardwareVolumePct + step).coerceIn(0, 100)
                    onAdjustHardwareVolume(nextVol)
                    // Trigger Wear OS Pixel Watch 3 mechanical crown detent haptic
                    localView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "REMOTE DAC & ATMOS",
                    color = Color(0xFF06B6D4),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Active Universal USB DAC Card (Tap to cycle detected DACs)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF141419))
                        .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .clickable {
                            localView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onCycleConnectedDac()
                        }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeDac?.modelName ?: "No USB DAC Plugged",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeDac?.let {
                            "${it.uacProtocol} · ${it.maxBitDepth}/${it.maxSampleRate} · Vol $hardwareVolumePct%"
                        } ?: "Tap to scan USB-C OTG bus",
                        color = Color(0xFF9CA3AF),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bit-Perfect Exclusive USB Pill Button
            item {
                WearToggleChip(
                    title = "Bit-Perfect USB",
                    subtitle = if (exclusiveBitPerfect && !dolbyAtmosEnabled) "Direct PCM/DSD Active" else "Bypassed (48kHz Mixer)",
                    checked = exclusiveBitPerfect && !dolbyAtmosEnabled,
                    accentColor = Color(0xFF10B981),
                    onClick = {
                        localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onToggleBitPerfect()
                    }
                )
            }

            // Dolby Atmos 3D Spatial Pill Button
            item {
                WearToggleChip(
                    title = "Dolby Atmos 3D",
                    subtitle = if (dolbyAtmosEnabled) "Binaural Renderer ON" else "Tap to enable Spatial",
                    checked = dolbyAtmosEnabled,
                    accentColor = Color(0xFFF59E0B),
                    onClick = {
                        localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onToggleDolbyAtmos()
                    }
                )
            }
        }
    }
}

@Composable
private fun WearToggleChip(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) accentColor.copy(alpha = 0.18f) else Color(0xFF141419))
            .border(
                1.dp,
                if (checked) accentColor else Color(0xFF272732),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF9CA3AF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = if (checked) "ON" else "OFF",
            color = if (checked) accentColor else Color(0xFF9CA3AF),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
