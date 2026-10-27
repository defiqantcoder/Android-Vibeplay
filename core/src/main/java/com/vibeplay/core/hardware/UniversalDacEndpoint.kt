package com.vibeplay.core.hardware

/**
 * Universal USB DAC Hardware Descriptor representing any connected USB-C OTG
 * or external Audio Interface (UAC 1.0 / UAC 2.0 / UAC 3.0).
 */
data class UniversalDacEndpoint(
    val id: String,
    val androidDeviceId: Int?,
    val modelName: String,
    val vendorName: String,
    val usbVidPid: String,
    val interfaceType: String,
    val uacProtocol: String,
    val maxBitDepth: String,
    val maxSampleRate: String,
    val dsdCapability: String,
    val clockArchitecture: String,
    val chipset: String,
    val defaultHardwareVol: Int = 80,
    val latencyMs: Double = 3.8,
    val isBitPerfectCapable: Boolean = true,
    val detectionSource: String = "Android UsbManager + AudioDeviceInfo"
)
