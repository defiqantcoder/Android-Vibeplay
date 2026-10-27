# Implementation Plan: Pixel Watch 3 Tactile Haptic Feedback & Wrist Search

Implement multi-sensory haptic feedback responses (Web vibration + synthesized mechanical audio detent click + visual bezel ripple) and a circular search engine (voice dictation + quick-filter tags + text query) for the Pixel Watch 3 (44mm) companion app.

## User Requirements & Selected Architecture
1. **Multi-Sensory Tactile Haptic Feedback**:
   - **Crown Rotation**: Discrete mechanical detent feedback on each rotary click (simulated via `navigator.vibrate(8)`, a micro-synthesized 1.8kHz transient click in Web Audio, and a glowing cyan accent ripple along the 1.4mm outer bezel and crown).
   - **Crate & Track Tap**: Solid tactile confirmation (`navigator.vibrate(15)` + mid-frequency mechanical tap tone + touch ripple).
   - **Track Cueing & Playback**: Affirmative double-tick (`navigator.vibrate([15, 30, 20])` + resonant cue chime).
   - **Audio Click Toggle**: Include a subtle haptic sound toggle in the watch options so users can silence the audio clicks while keeping vibration and visual ripples.
2. **Pixel Watch 3 Wrist Search Function**:
   - **Circular Search Bar**: Positioned at the top of the Wrist Crate & Collection Browser.
   - **Voice Search Dictation**: Tap the watch microphone to dictate track names, artists, or acoustic vibes directly on the watch.
   - **Quick-Filter Pills**: Circular-tailored chip tags (`Hi-Res FLAC`, `DSD128`, `>120 BPM`, `Camelot 8A`, `Chill`) for instant one-tap filtering without typing.
   - **Text Query Field**: Integrated input with instant live search matching across titles, artists, albums, and harmonic keys.
   - **Crown Scroll Navigation**: Rotary crown scrolls through search results with tactile detent clicks on each item.

---

## Proposed Changes

### 1. Create Haptic Feedback Utility (`src/utils/haptics.ts`)
- Implement `triggerWatchHaptic(type, options)`:
  - Supports types: `'crown_tick'`, `'tap'`, `'cue'`, `'swipe'`.
  - Fires `navigator.vibrate(ms)` when supported by the device.
  - Synthesizes ultra-short (3ms–8ms) mechanical watch escapement detent clicks via Web Audio API `AudioContext` (bandpassed transient burst), providing realistic tactile feedback even on desktop monitors.
  - Returns a visual haptic trigger event for rendering UI ripple rings.

### 2. Update Pixel Watch 3 Companion (`src/components/PixelWatchCompanion.tsx`)
- Add visual bezel ripple animation: a momentary glowing cyan halo pulse along the 1.4mm outer bezel whenever the crown is rotated or an item is tapped.
- In **Wrist Crate & Collection Browser**:
  - Add a dedicated **Search & Filter Drawer**:
    - Micro voice search button that captures spoken queries.
    - Quick-filter chips: `✨ Hi-Res`, `⚡ >120 BPM`, `🎹 Camelot`, `🎧 DSD`, `☕ Chill`.
    - Text search input with clear trigger.
    - Live result counter (e.g. *"5 tracks matching 'Light'"*).
  - Connect all crown rotations, track row taps, crate clicks, and transport triggers to `triggerWatchHaptic()`.

### 3. Verification & Testing
- Run `compile_applet` to ensure zero compilation or TypeScript errors.
- Test crown rotation on the watch: verify mechanical audio click, vibration, and bezel ripple.
- Test search on the watch: test voice query, quick-filter chips, and text search.
- Verify instant playback cueing from search results to phone and USB DAC.
