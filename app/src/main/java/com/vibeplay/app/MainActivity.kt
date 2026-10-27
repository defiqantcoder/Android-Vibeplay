package com.vibeplay.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.vibeplay.app.audio.UsbDacReceiver
import com.vibeplay.app.audio.VibePlaybackService
import com.vibeplay.app.ui.*
import com.vibeplay.app.ui.theme.*
import com.vibeplay.core.model.EndpointType
import com.vibeplay.core.model.UsbDacEndpoint

class MainActivity : ComponentActivity() {

    private val viewModel: VibePlayViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val app = application as VibePlayApplication
                val wearBridge = com.vibeplay.app.wear.PhoneWearBridge(applicationContext)
                return VibePlayViewModel(app.audioEngine, wearBridge = wearBridge) as T
            }
        }
    }

    private val usbDacReceiver = UsbDacReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Foreground MediaSessionService
        val serviceIntent = Intent(this, VibePlaybackService::class.java)
        startService(serviceIntent)

        // Setup USB DAC dynamic receivers
        usbDacReceiver.onDacAttached = { device ->
            handleDacAttached(device)
        }
        usbDacReceiver.onDacDetached = { device ->
            viewModel.onUsbDacDetached()
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        registerReceiver(usbDacReceiver, filter)

        // Check already connected USB devices on launch
        val usbManager = getSystemService(Context.USB_SERVICE) as? UsbManager
        usbManager?.deviceList?.values?.firstOrNull { UsbDacReceiver.isUsbAudioDevice(it) }?.let { device ->
            handleDacAttached(device)
        }

        setContent {
            val state by viewModel.uiState.collectAsState()
            var currentNavIndex by remember { mutableStateOf(0) }

            VibePlayTheme(mode = state.themeMode) {
                Scaffold(
                    bottomBar = {
                        Column {
                            // Mini-Player Pill
                            state.currentTrack?.let { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ContainerSurface)
                                        .border(1.dp, ContainerBorder, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setNowPlayingVisible(true) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TealPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text("${track.artist} • ${track.sampleRate}", color = TextMuted, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { viewModel.togglePlayPause() }) {
                                        Icon(
                                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = TealPrimary
                                        )
                                    }
                                }
                            }

                            // Bottom Navigation Bar
                            NavigationBar(containerColor = BackgroundDark) {
                                NavigationBarItem(
                                    selected = currentNavIndex == 0,
                                    onClick = { currentNavIndex = 0 },
                                    icon = { Icon(Icons.Default.Folder, contentDescription = "Crates") },
                                    label = { Text("Crates") }
                                )
                                NavigationBarItem(
                                    selected = currentNavIndex == 1,
                                    onClick = { currentNavIndex = 1 },
                                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Vibe Cooker") },
                                    label = { Text("Cooker") }
                                )
                                NavigationBarItem(
                                    selected = currentNavIndex == 2,
                                    onClick = { currentNavIndex = 2 },
                                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Soundbar") },
                                    label = { Text("Soundbar") }
                                )
                                NavigationBarItem(
                                    selected = currentNavIndex == 3,
                                    onClick = { currentNavIndex = 3 },
                                    icon = { Icon(Icons.Default.Extension, contentDescription = "Toybox") },
                                    label = { Text("Toybox") }
                                )
                                NavigationBarItem(
                                    selected = currentNavIndex == 4,
                                    onClick = { currentNavIndex = 4 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentNavIndex) {
                            0 -> CrateScreen(viewModel = viewModel)
                            1 -> VibeCookerScreen(viewModel = viewModel)
                            2 -> SoundbarScreen(viewModel = viewModel)
                            3 -> AddonsToyboxScreen(viewModel = viewModel)
                            4 -> SettingsScreen(viewModel = viewModel)
                        }

                        // Full Screen Overlays & Modals
                        if (state.showNowPlaying) {
                            NowPlayingOverlay(viewModel = viewModel)
                        }

                        if (state.showLyrics) {
                            SynchronizedLyricsOverlay(viewModel = viewModel)
                        }

                        if (state.earSafetyTriggered) {
                            DacDisconnectedModal(onDismiss = { viewModel.dismissSafetyGuard() })
                        }

                        if (state.showDacModal) {
                            DacOutputModal(viewModel = viewModel)
                        }

                        if (state.showTrackActions) {
                            TrackActionsSheet(viewModel = viewModel)
                        }

                        if (state.showWidgets) {
                            WidgetsShowcaseModal(onDismiss = { viewModel.setWidgetsVisible(false) })
                        }

                        if (state.showOnboarding) {
                            WelcomeOnboardingModal(onDismiss = { viewModel.setOnboardingVisible(false) })
                        }

                        if (state.showAssistant) {
                            PixelAssistantVoiceModal(viewModel = viewModel)
                        }

                        if (state.showDeduplicator) {
                            MusicDeduplicatorSheet(onDismiss = { viewModel.setDeduplicatorVisible(false) })
                        }

                        if (state.showDriveMode) {
                            DriveModeOverlay(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun handleDacAttached(device: UsbDevice) {
        val dacName = device.productName ?: "USB Audiophile DAC"
        val endpoint = UsbDacEndpoint(
            id = "usb-${device.vendorId}-${device.productId}",
            name = dacName,
            type = EndpointType.USB_DAC,
            maxSampleRate = "PCM 768kHz / 32-bit Direct",
            dsdSupport = true,
            vendorId = device.vendorId,
            productId = device.productId,
            isConnected = true
        )
        viewModel.onUsbDacAttached(endpoint)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbDacReceiver)
    }
}
