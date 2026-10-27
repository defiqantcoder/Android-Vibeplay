# VibePlay Lossless AI Studio (Android & Wear OS)

An audiophile-grade, Bit-Perfect direct-hardware Android audio studio and Wear OS companion built with Kotlin, Jetpack Compose Material 3, AndroidX Media3 (ExoPlayer), Room SQLite, and Robert Bristow-Johnson (RBJ) Biquad DSP filtering.

[![GitHub](https://img.shields.io/badge/GitHub-defiqantcoder%2FAndroid--Vibeplay-06B6D4?logo=github)](https://github.com/defiqantcoder/Android-Vibeplay)
[![Android](https://img.shields.io/badge/Android-14%20(API%2034)-3DDC84?logo=android)](https://developer.android.com)
[![Wear OS](https://img.shields.io/badge/Wear%20OS-by%20Google-4285F4?logo=wearos)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.00-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

---

## 🎧 Architecture & Multi-Module Structure

```
Android-Vibeplay/
├── core/                        # Domain models, database, DSP, and scanner
│   ├── dsp/                     # 10-band RBJ Biquad Parametric Equalizer engine
│   ├── engine/                  # Heuristic VibeCooker & Camelot Harmonic Wheel scoring
│   ├── scanner/                 # Pure-Kotlin binary FLAC STREAMINFO & Vorbis comment parser
│   ├── db/                      # Room SQLite database, entity schemas, DAOs
│   ├── wear/                    # Shared Phone <-> Watch bidirectional sync protocol
│   └── model/                   # Domain entities (Tracks, Crates, EQ Presets, DAC endpoints)
├── app/                         # Phone application (Compose Material 3 + Media3)
│   ├── audio/                   # ExoPlayer AudioSink, BiquadPeqAudioProcessor, UsbDacReceiver
│   ├── wear/                    # PhoneWearBridge & PhoneWearMessageListenerService
│   ├── ui/                      # CrateScreen, VibeCookerScreen, SoundbarScreen, Toybox, Overlays
│   └── MainActivity.kt          # Top-level navigation shell & mini-player pill
└── wear/                        # Pixel Watch / Wear OS companion app
    ├── PixelWatchScreens.kt     # Circular Wear OS UI (Now Playing, Crates, BPM Sync, DAC Remote)
    ├── WatchHapticsController.kt# Physical crown rotary ticks & heavy cues
    ├── WearSyncManager.kt       # Google Play Services Wearable DataClient & MessageClient sync
    └── WearMainActivity.kt     # Standalone Wear OS entrypoint
```

---

## ⚡ Key Highlights

### 1. Bit-Perfect Hardware Direct Routing & Audiophile AudioSink
* Injects a custom `BaseAudioProcessor` directly into AndroidX Media3's `DefaultAudioSink`.
* Bypasses the default Android OS 48 kHz AudioFlinger Sample Rate Converter (SRC) for pure uncolored hardware bitstreaming to external USB DACs (e.g. FiiO KA15, Qudelix-5K, Chord Mojo 2).
* Seamlessly toggles between Bit-Perfect direct delivery and 10-band software DSP with anti-clipping headroom compensation.

### 2. Ear-Safety Guard
* Dynamic broadcast receiver intercepting `ACTION_USB_DEVICE_ATTACHED`, `ACTION_USB_DEVICE_DETACHED`, and `ACTION_AUDIO_BECOMING_NOISY`.
* If a high-output USB DAC is suddenly unplugged, playback mutes and pauses in under 10 milliseconds to protect hearing against sudden phone speaker blast.

### 3. Pure Kotlin Binary Lossless FLAC Scanner
* Parses raw `.flac` binary headers directly (`STREAMINFO` and `METADATA_BLOCK_VORBIS_COMMENT`) without native C/C++ dependencies.
* Successfully indexes local master libraries (e.g., 204 FLAC tracks across 18 artists) with real-time waveform envelope synthesis.

### 4. Camelot Harmonic Wheel & Vibe Cooker Engine
* Natural language prompt parsing (`"Cardio >130 bpm"`, `"Midnight chill lofi study"`, `"French synthwave"`).
* Ranks tracks based on energy deltas, tempo curves, and harmonic key compatibility (+1/-1 semitones, relative major/minor).
* Auto-synthesizes DJ liner notes using local heuristics with optional Gemini API cloud expansion.

### 5. Pixel Watch Wear OS Companion
* 5 circular Wear OS screens:
  1. **Now Playing**: 270° radial volume/progress arc with transport controls.
  2. **Rotary Crown Volume**: Physical haptic tick feedback on crown rotation.
  3. **Wrist Crates**: Crown-scrollable track list with tap-to-play.
  4. **Biometric BPM Sync**: Real-time ECG waveform canvas matching mixtape BPM to heart rate.
  5. **DAC Remote**: Bit-Perfect hardware toggle & DAC endpoint status.

---

## 🛠️ Building & Developing

### Requirements
* **JDK**: 17+ (Adoptium Temurin 17 recommended)
* **Android SDK**: API 34 (`platforms;android-34`, `build-tools;34.0.0`)
* **Gradle**: 8.9 (included via `./gradlew`)

### Commands
```bash
# Set Java 17 environment
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# Run core unit tests
./gradlew :core:testDebugUnitTest

# Assemble debug APKs (app & wear)
./gradlew assembleDebug
```

---

## 📄 License
MIT License. Built for audiophiles and local lossless music enthusiasts.
