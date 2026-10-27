# Universal Dynamic USB DAC Recognition & Kotlin / Jetpack Compose Implementation

Deliver universal, vendor-agnostic USB DAC recognition that dynamically identifies, configures, and routes bit-perfect audio to **any** connected USB-C DAC or external audio interface—implemented with native **Kotlin & Jetpack Compose** (`AudioManager`, `UsbManager`, Android 14+ `AudioMixerAttributes`, and Material 3 / Wear OS Compose) alongside synchronized live hardware detection in the interactive preview.

## User Review & Critical Decisions

> [!IMPORTANT]
> Because AI Studio's live preview window runs in a browser container on port 3000 while your target codebase is **Kotlin + Jetpack Compose**, this plan delivers **both** the complete native Android Kotlin/Jetpack Compose implementation (viewable and exportable directly inside the app and workspace) **and** the live WebUSB / `MediaDevices` / `setSinkId` hardware engine so the interactive preview dynamically recognizes any DAC you plug in or test.

- **Confirmed Decision 1 — Universal Any-DAC Hardware Recognition**: Remove any FiiO-only assumptions. Dynamically query USB Vendor/Product descriptors (`UsbDevice` + `AudioDeviceInfo` on Android; `navigator.usb` WebUSB + `navigator.mediaDevices.enumerateDevices` + `ondevicechange` in the live preview) to extract the exact manufacturer name, product model, UAC 1.0/2.0 interface class, supported PCM bit depths (`16/24/32-bit`), sample rates (`44.1kHz–768kHz`), and DSD capabilities for any DAC plugged in.
- **Confirmed Decision 2 — Kotlin & Jetpack Compose Architecture**: Write the Android phone and Pixel Watch 3 (44mm) companion modules in idiomatic Kotlin using Jetpack Compose (`androidx.compose.material3` and `androidx.wear.compose.material3`), exposing an in-app **Kotlin / Jetpack Compose Studio Inspector** button in the Audio Output & DAC sheet and Settings so you can inspect or copy the exact `.kt` Compose components and `UsbDacHardwareManager.kt` engine at any time.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Universal USB-C OTG & UAC 2.0 Hardware Discovery**: Automatically listens for physical USB-C hot-plug and unplug events for any DAC brand (e.g., Chord, Qudelix, iFi, Topping, AudioQuest, Moondrop, Apple USB-C, Samsung, Hidizs, Shanling, FiiO, or generic UAC 1.0/2.0 interfaces).
  2. **Dynamic Capability Negotiation**: Reads hardware descriptors to populate real-time bit-depth, maximum sample rate, clock mode, and chipset/interface telemetry across the Top Bar DAC Pill, Now Playing badge, The Soundbar, Audio Output & DAC Bottom Sheet, and Pixel Watch 3 Remote Output Tile.
  3. **Android 14+ Bit-Perfect Exclusive USB Routing & Dolby Atmos Interlock**: Uses Android `AudioManager.setPreferredMixerAttributes` (`MIXER_BEHAVIOR_BIT_PERFECT`) when Exclusive USB Access is enabled, and automatically switches to the spatial DSP graph when Dolby Atmos is toggled on.
- **Target Audience / Persona**: Audiophiles and Android / Wear OS developers using Pixel phones and Pixel Watch 3 (44mm) with diverse external USB-C DAC dongles and desktop DAC/amps.
- **Key Value**: Plugging in *any* USB DAC immediately surfaces its real hardware name and capabilities without manual configuration or vendor lock-in.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Automatic Hot-Plug & Unplug Flow**: When any USB DAC is connected (via physical USB/WebUSB pairing, `mediaDevices.ondevicechange`, or the interactive Hardware Plug Simulator), the app fires an `AudioDeviceCallback` event, mounts the device as the active endpoint, updates all telemetry badges to the detected DAC's model name, and optionally auto-enables Bit-Perfect Exclusive Mode. Unplugging triggers the instant safety auto-pause modal.
  2. **WebUSB & System Hardware Pairing**: Tapping **Pair Physical USB DAC (WebUSB / System)** in the Audio Output & DAC sheet opens the browser/OS USB device chooser, reads the connected hardware's real USB `manufacturerName`, `productName`, `vendorId`, `productId`, and USB Audio Class descriptors, and mounts it live.
  3. **Kotlin / Jetpack Compose Code View**: Tapping **Kotlin / Compose Source** inside the Audio Output & DAC sheet opens a dedicated syntax-highlighted modal displaying the complete Kotlin + Jetpack Compose implementation (`UsbDacHardwareManager.kt`, `DacOutputBottomSheet.kt`, `BitPerfectAudioEngine.kt`, and `PixelWatchDacTile.kt`) with one-tap copy.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Tactile dark audiophile console ("Obsidian & Electric Cyan") with high-contrast tabular telemetry and ergonomic thumb-zone bottom sheets.
  - *Color Palette & Mood*: Deep obsidian canvas (`#0e0e10`), elevated surface containers (`#16161a`, `#1e1e24`), electric cyan primary accents (`#06b6d4`), warm amber spatial highlights (`#f59e0b`), and emerald bit-perfect indicators (`#10b981`).
  - *Typography & Hierarchy*: `Plus Jakarta Sans` for display headers, `JetBrains Mono` (`tabular-nums`) for sample rate, bit depth, USB VID:PID, and latency metrics.
  - *Component Styling & Layout*: Material 3 `ModalBottomSheet` ergonomics with $48\times48\text{px}$ minimum touch targets, single-line controls (`whitespace-nowrap`), and clean unboxed metadata separators (`·`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Multi-Layer Hardware Recognition (`UsbManager` + `AudioDeviceInfo` + WebUSB)**:
  - *Chosen Approach*: Combine Android's `AudioManager.getDevices(GET_DEVICES_OUTPUTS)` and `UsbManager.deviceList` in Kotlin (and `navigator.usb` + `navigator.mediaDevices` + `setSinkId` in the live preview) with a USB Vendor ID (VID) lookup table and generic UAC 1.0/2.0 descriptor fallback.
  - *Why*: Standard audio output enumeration sometimes reports generic chipset strings (e.g., "USB Audio2.0"), whereas combining USB descriptor inspection (`productName`, `manufacturerName`, `VID:PID`) with audio sink enumeration guarantees accurate recognition of *any* DAC on the market.
  - *Alternatives Considered*: Relying solely on a static preset list—rejected because the user requires true dynamic recognition of any DAC used.
- **Decision 2 — Dual Native Kotlin/Compose + Live Preview Synchronization**:
  - *Chosen Approach*: Author the complete Kotlin + Jetpack Compose files (`UsbDacHardwareManager.kt`, `DacOutputModalSheet.kt`, `VibePlayViewModel.kt`, `WearDacTileScreen.kt`) and mirror their exact state machine in the live preview with an interactive Kotlin Compose Source viewer.
  - *Why*: Gives you authentic, copy-ready Kotlin/Jetpack Compose code while keeping the interactive phone + Pixel Watch 3 (44mm) preview 100% live and testable in AI Studio.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   USB-C OTG / UAC 1.0 & 2.0 HARDWARE LAYER                 │
│  • Android Kotlin: UsbManager + AudioManager + AudioDeviceCallback         │
│  • Android 14+: AudioMixerAttributes (MIXER_BEHAVIOR_BIT_PERFECT)          │
│  • Live Preview: WebUSB (navigator.usb) + MediaDevices + setSinkId()       │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Dynamic Descriptor & Sink Telemetry
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                UNIVERSAL DAC RECOGNITION STATE ENGINE                      │
│  • Parses Any Vendor/Model, VID:PID, UAC Class, PCM Bit Depth & Sample Rate│
│  • Enforces Bit-Perfect Exclusive USB vs. Dolby Atmos Spatial Interlock    │
│  • Fires Hot-Plug Auto-Switch & Unplug Safety Auto-Pause                   │
└───────────┬─────────────────────────────────┬──────────────────────────────┘
            │                                 │
            ▼                                 ▼
┌───────────────────────────────────┐ ┌──────────────────────────────────────┐
│   JETPACK COMPOSE PHONE UI        │ │  WEAR OS COMPOSE PIXEL WATCH 3 (44mm)│
│  • DacOutputBottomSheet (@Composable)│ │  • ScalingLazyColumn Remote DAC Tile │
│  • Live WebUSB / Any-DAC Prober   │ │  • Rotary Crown Hardware Vol Control │
│  • Kotlin/Compose Source Inspector│ │  • Instant Sink & Atmos Toggle       │
└───────────────────────────────────┘ └──────────────────────────────────────┘
```

- **Data Model & State**:
  - `UniversalDacDescriptor`: Stores `deviceId`, `vendorName`, `productName`, `usbVidPid` (e.g. `2972:0047`), `uacVersion` (`UAC 1.0` | `UAC 2.0` | `UAC 3.0`), `supportedSampleRates` (`44.1kHz` to `768kHz`), `supportedBitDepths` (`16-bit`, `24-bit`, `32-bit PCM`), `dsdSupport`, `sinkId`, and `connectionSource` (`WebUSB Hardware`, `System Audio Sink`, `Android UsbManager`, or `Custom UAC Profile`).
- **Interactive Component & State Mapping**:
  - **Hot-Plug Listener (`ondevicechange` / `usb.onconnect` / `AudioDeviceCallback`)**: Automatically detects newly connected audio outputs or USB devices, promotes the newly connected DAC to the active output when `autoSwapOnPlug` is enabled, and updates the active sink via `setSinkId()`.
  - **Universal WebUSB Hardware Pairing Button**: Calls `navigator.usb.requestDevice({ filters: [{ classCode: 1 }] })` (USB Audio Class `0x01` or unfiltered fallback) so any physical USB DAC plugged into the host machine can be inspected at the USB descriptor level.
  - **Kotlin / Jetpack Compose Source Drawer**: Interactive tab/drawer in the DAC modal allowing instant inspection and copying of the Kotlin Jetpack Compose source files.
