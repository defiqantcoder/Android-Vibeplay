package com.defiqantcoder.vibeplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defiqantcoder.vibeplay.hardware.UniversalDacEndpoint
import com.defiqantcoder.vibeplay.hardware.UsbDacHardwareManager
import com.defiqantcoder.vibeplay.ui.sheets.DacOutputBottomSheet

class MainActivity : ComponentActivity() {
    private lateinit var usbDacHardwareManager: UsbDacHardwareManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbDacHardwareManager = UsbDacHardwareManager(
            context = applicationContext,
            onDacUnpluggedSafetyPause = { unpluggedDac ->
                // Pause playback immediately when active USB DAC is physically unplugged
            }
        )
        usbDacHardwareManager.startMonitoring()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                VibePlayMainScreen(hardwareManager = usbDacHardwareManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        usbDacHardwareManager.stopMonitoring()
    }
}

@Composable
fun VibePlayMainScreen(hardwareManager: UsbDacHardwareManager) {
    val connectedDacs by hardwareManager.connectedDacs.collectAsState()
    val activeDac by hardwareManager.activeDac.collectAsState()
    val exclusiveBitPerfect by hardwareManager.exclusiveBitPerfect.collectAsState()
    val dolbyAtmosEnabled by hardwareManager.dolbyAtmosEnabled.collectAsState()
    val autoSwapOnPlug by hardwareManager.autoSwapOnPlug.collectAsState()

    var showDacSheet by remember { mutableStateOf(false) }
    var isUsbDacActive by remember { mutableStateOf(true) }
    var dolbyAtmosProfile by remember { mutableStateOf("music") }
    var customDacs by remember { mutableStateOf<List<UniversalDacEndpoint>>(emptyList()) }

    val allDacs = remember(connectedDacs, customDacs) {
        customDacs + connectedDacs
    }

    Scaffold(
        containerColor = Color(0xFF0E0E10),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "vibeplay",
                    color = Color(0xFFE4E4E7),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Dynamic Any-DAC Status Pill in Top App Bar
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E24))
                        .clickable { showDacSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when {
                            dolbyAtmosEnabled -> "Atmos 3D"
                            isUsbDacActive && activeDac != null -> activeDac!!.modelName
                            else -> "Speaker"
                        },
                        color = Color(0xFF06B6D4),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showDacSheet) {
                DacOutputBottomSheet(
                    connectedDacs = allDacs,
                    activeDac = activeDac ?: allDacs.firstOrNull(),
                    isUsbDacActive = isUsbDacActive,
                    exclusiveUsbAccess = exclusiveBitPerfect,
                    dolbyAtmosEnabled = dolbyAtmosEnabled,
                    dolbyAtmosProfile = dolbyAtmosProfile,
                    autoSwapOnPlug = autoSwapOnPlug,
                    onScanUsbBus = { hardwareManager.scanAllConnectedAudioHardware() },
                    onSelectDac = { selected ->
                        isUsbDacActive = true
                        hardwareManager.selectDacEndpoint(selected)
                    },
                    onMountCustomDac = { modelName ->
                        val mounted = UniversalDacEndpoint(
                            id = "custom-${System.currentTimeMillis()}",
                            androidDeviceId = null,
                            modelName = modelName,
                            vendorName = modelName.split(" ").firstOrNull() ?: "Universal USB Audio",
                            usbVidPid = "20B1:UAC2",
                            interfaceType = "Direct USB Host (UAC 2.0)",
                            uacProtocol = "UAC 2.0",
                            maxBitDepth = "32-bit",
                            maxSampleRate = "384kHz",
                            dsdCapability = "DSD256 Native / DoP128",
                            clockArchitecture = "Async Isochronous USB Clock",
                            chipset = "Hardware UAC 2.0 Bridge"
                        )
                        customDacs = listOf(mounted) + customDacs
                        isUsbDacActive = true
                        hardwareManager.selectDacEndpoint(mounted)
                    },
                    onSelectPhoneSpeaker = { isUsbDacActive = false },
                    onToggleExclusiveUsb = { enabled ->
                        hardwareManager.configureBitPerfectRouting(enabled)
                    },
                    onToggleDolbyAtmos = { enabled ->
                        hardwareManager.toggleDolbyAtmosSpatial(enabled)
                    },
                    onSelectAtmosProfile = { profile -> dolbyAtmosProfile = profile },
                    onToggleAutoSwap = {},
                    onDismiss = { showDacSheet = false }
                )
            }
        }
    }
}
