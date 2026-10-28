# Agent Directives & Architecture Rules: Android-Vibeplay

## 1. Project Overview & Architecture
- **Package Hierarchy:** `com.vibeplay.app.*` (application layer) and `com.vibeplay.core.*` (shared domain/DSP layer).
- **Target Platform:** Android 14+ (API 34+) and Wear OS Companion.
- **UI Stack:** Kotlin 2.0+, Jetpack Compose, Material 3 Dark Palette (`VibePlayTheme`).
- **Audio Stack:** AndroidX Media3 (`ExoPlayer`, `MediaSessionService`), 10-band RBJ Biquad PEQ (`BiquadPeqAudioProcessor`), and bit-perfect USB routing (`MIXER_BEHAVIOR_BIT_PERFECT`).
- **Companion Sync:** Wearable Data Layer (`PhoneWearBridge`, `WearSyncProtocol`).

---

## 2. Canonical Data Models & Single Sources of Truth
- **Universal USB DAC Model:** `com.vibeplay.core.hardware.UniversalDacEndpoint` is the ONLY valid endpoint model across the entire project. The legacy `com.vibeplay.core.model.UsbDacEndpoint` is deprecated and forbidden.
- **Hardware Monitoring:** `UsbDacHardwareManager` is the sole hardware discovery and event monitoring system. Do NOT register or instantiate secondary broadcast listeners like `UsbDacReceiver`.
- **UI State Source:** `VibePlayViewModel` exposes state strictly through Kotlin `StateFlow`. Composable screens must remain stateless and consume state from `viewModel.uiState`.
"Do not push directly to main. Create a new feature branch named fe/modularize-overlays, push that branch to GitHub, and open a Pull Request against main."

---

## 3. Agent Ownership Boundaries

### Antigravity (Frontend Agent)
- **Allowed Directories:**
  - `app/src/main/java/com/vibeplay/app/ui/`
  - `app/src/main/res/`
- **Primary Responsibilities:**
  - Jetpack Compose layouts, screens, navigation bars, bottom sheets, dialogs, and animations.
  - UI modularity: Never place multiple large overlays into single multi-screen files. Keep screens, modals, and sheets in separate dedicated files.
- **Forbidden Actions:**
  - Never modify files in `app/src/main/java/com/vibeplay/app/audio/`, `app/src/main/java/com/vibeplay/app/hardware/`, or `app/src/main/java/com/vibeplay/app/wear/`.
  - Never alter audio routing, playback controllers, or DSP math.
  - Never create duplicate endpoint or track data classes in the UI package.

### Jules (Backend, Audio & Core Agent)
- **Allowed Directories:**
  - `app/src/main/java/com/vibeplay/app/audio/`
  - `app/src/main/java/com/vibeplay/app/hardware/`
  - `app/src/main/java/com/vibeplay/app/wear/`
  - `app/src/main/java/com/vibeplay/app/VibePlayApplication.kt`
  - All core library code under `com.vibeplay.core.*`
- **Primary Responsibilities:**
  - ExoPlayer session lifecycle, foreground `MediaSessionService`, USB DAC device discovery, RBJ Biquad DSP filtering, and Wear OS DataClient synchronization.
- **Forbidden Actions:**
  - Never edit Jetpack Compose views, layouts, colors, typography, or UI themes.
  - Never run file scanning, audio decoding, or hardware queries on the Main UI thread (`Dispatchers.Main`).

---

## 4. Strict Engineering Constraints
1. **Ear-Safety Guard Protocol:** The automatic pause/mute safeguard on USB unplug or `AudioManager.ACTION_AUDIO_BECOMING_NOISY` must NEVER be bypassed, delayed, or removed.
2. **Bit-Perfect vs. DSP Interlock:** Android 14+ bit-perfect exclusive mode (`MIXER_BEHAVIOR_BIT_PERFECT`) and software DSP (Biquad PEQ or Dolby Atmos) are mutually exclusive. Activating one must automatically disengage the other unless explicit forced DSP bypass is set.
3. **No Hardcoded Filesystem Paths:** Never hardcode desktop paths (such as `/home/...`). Library scanning must dynamically resolve paths using Android `MediaStore` or `context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)`.
4. **Preserve Existing Implementations:** Do not overwrite functional working methods. Append new modular functions or extend interfaces instead.
