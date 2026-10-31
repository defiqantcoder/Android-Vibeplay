package com.vibeplay.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeplay.app.ui.theme.*
import com.vibeplay.core.model.VibeSeedData

@Composable
fun SoundbarScreen(
    viewModel: VibePlayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isBypassed = state.bitPerfectDirect && !state.forceDspOnDac

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SoundbarHeader() }

        // Bit-Perfect Hardware Intercept Banner
        if (state.bitPerfectDirect) {
            item { BitPerfectBanner(state, isBypassed, viewModel) }
        }

        // Live Frequency Response Canvas
        item { FrequencyResponseCanvas(state, isBypassed) }

        // Preset Chips
        item { PresetChips(state, viewModel) }

        // 10-Band Precision Sliders
        item {
            Text("FREQUENCY BANDS", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
        }

        itemsIndexed(state.eqBands) { index, band ->
            EqBandSliderRow(band, index, isBypassed, viewModel)
        }
    }
}




@Composable
private fun SoundbarHeader() {
    Text("SOUNDBAR DSP", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Text("10-Band Parametric Equalizer", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
}


@Composable
private fun BitPerfectBanner(
    state: VibePlayUiState,
    isBypassed: Boolean,
    viewModel: VibePlayViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isBypassed) AmberSecondary.copy(alpha = 0.15f) else EmeraldTertiary.copy(alpha = 0.15f))
            .border(1.dp, if (isBypassed) AmberSecondary.copy(alpha = 0.5f) else EmeraldTertiary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isBypassed) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isBypassed) AmberSecondary else EmeraldTertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isBypassed) "BIT-PERFECT DIRECT ACTIVE — DSP BYPASSED" else "BIT-PERFECT WITH FORCED DSP",
                    color = if (isBypassed) AmberSecondary else EmeraldTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (isBypassed)
                    "Audio is streaming directly to ${state.activeUniversalDac?.modelName ?: "Unknown DAC"} without SRC or software EQ coloration."
                else
                    "Software RBJ Biquad filters are active while streaming to USB DAC.",
                color = TextMuted,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.setForceDspOnDac(!state.forceDspOnDac) },
                colors = ButtonDefaults.buttonColors(containerColor = if (isBypassed) AmberSecondary else EmeraldTertiary)
            ) {
                Text(if (isBypassed) "Force Software DSP" else "Restore Bit-Perfect Bypass", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}


@Composable
private fun FrequencyResponseCanvas(
    state: VibePlayUiState,
    isBypassed: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ContainerSurface)
            .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // Center 0dB line
            drawLine(
                color = ContainerBorder,
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.dp.toPx()
            )

            // Spline curve of gains
            val path = Path()
            val bands = state.eqBands
            if (bands.isNotEmpty()) {
                val stepX = width / (bands.size - 1)
                bands.forEachIndexed { i, band ->
                    val x = i * stepX
                    val y = midY - (band.gain.toFloat() / 12f) * (midY * 0.8f)
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevY = midY - (bands[i - 1].gain.toFloat() / 12f) * (midY * 0.8f)
                        val cX = (prevX + x) / 2f
                        path.cubicTo(cX, prevY, cX, y, x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = if (isBypassed) TextMuted else TealPrimary,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}


@Composable
private fun PresetChips(
    state: VibePlayUiState,
    viewModel: VibePlayViewModel
) {
    Text("TARGET PRESETS", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(VibeSeedData.EQ_PRESETS) { preset ->
            val isSelected = state.selectedPresetId == preset.id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) TealPrimary else ContainerSurface)
                    .border(1.dp, if (isSelected) TealPrimary else ContainerBorder, RoundedCornerShape(8.dp))
                    .clickable { viewModel.applyPreset(preset) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    preset.name,
                    color = if (isSelected) Color.Black else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}


@Composable
private fun EqBandSliderRow(
    band: com.vibeplay.core.model.EqBand,
    index: Int,
    isBypassed: Boolean,
    viewModel: VibePlayViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ContainerSurface)
            .border(1.dp, ContainerBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            band.label,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.width(60.dp)
        )

        Slider(
            value = band.gain.toFloat(),
            onValueChange = { viewModel.setEqBandGain(index, it.toDouble()) },
            valueRange = -12f..12f,
            steps = 47,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = TealPrimary,
                activeTrackColor = TealPrimary,
                inactiveTrackColor = ContainerBorder
            ),
            enabled = !isBypassed
        )

        Text(
            String.format("%+.1f dB", band.gain),
            fontWeight = FontWeight.SemiBold,
            color = if (band.gain > 0) TealPrimary else if (band.gain < 0) AmberSecondary else TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
