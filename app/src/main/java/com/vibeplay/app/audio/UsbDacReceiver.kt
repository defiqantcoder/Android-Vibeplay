package com.vibeplay.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.os.Build

class UsbDacReceiver(
    var onDacAttached: ((UsbDevice) -> Unit)? = null,
    var onDacDetached: ((UsbDevice?) -> Unit)? = null
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                if (device != null && isUsbAudioDevice(device)) {
                    onDacAttached?.invoke(device)
                }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                onDacDetached?.invoke(device)
            }
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                // Immediate ear-safety guard pause when headphones or DAC are unplugged
                onDacDetached?.invoke(null)
            }
        }
    }

    companion object {
        fun isUsbAudioDevice(device: UsbDevice): Boolean {
            // Class 1 = USB Audio Class (UAC1/UAC2/UAC3)
            if (device.deviceClass == 1) return true
            for (i in 0 until device.interfaceCount) {
                if (device.getInterface(i).interfaceClass == 1) {
                    return true
                }
            }
            return false
        }
    }
}
