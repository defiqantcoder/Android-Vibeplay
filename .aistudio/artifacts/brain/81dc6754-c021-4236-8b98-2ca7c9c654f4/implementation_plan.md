# Implementation Plan: AI Engine, Pixel Assistant & Pixel Watch 3 (44mm) Companion for VibePlay

Implement the requested AI expansion, Pixel Assistant voice control, and a dedicated **Pixel Watch 3 (44mm edition, 456×456 AMOLED)** companion app featuring an interactive **Wrist Crate & Collection Browser** alongside the 412×915 Android phone viewport.

## User Requirements & Revised Scope

1. **Pixel Watch 3 (44mm Variation) Optimization**:
   - Hardware target: 44mm chassis with **456×456 circular Actua AMOLED** (1.4" display, 320 ppi, ultra-thin 1.4mm bezel, 2,000 nits peak brightness, 60Hz fluid refresh).
   - Precision **Rotary Digital Crown**: crown rotation drives smooth inertial scrolling through long crate tracklists and fast-scrubs the circular audio timeline with tactile haptic tick indicators.
2. **Wrist Crate & Music Collection Browser (Directly on Watch)**:
   - Dedicated **Crate & Collection Explorer Tile / App Mode** on the Pixel Watch 3:
     - **Active Crates & Playlists**: Browse "Studio Monitor Ref", "Tube Amp Velvet", "Modular Techno Live", "Heavy Rotation", and cooked mixtapes with folder artwork and track counts.
     - **Curated Audio Stash**: Fast scrollable track library with rotary indexer (BPM, Title, Camelot Key, Hi-Res format badges).
     - **Instant Wrist Cue**: Tapping any track cues it immediately on the phone and sends lossless stream to the active USB DAC or Dolby Atmos output.
3. **Optional AI Plain-Text Playlist Generation ("Vibe Cooker AI")**:
   - Dual-engine architecture: local heuristic Camelot Wheel + BPM engine runs 100% offline by default.
   - Optional Cloud/Gemini API integration (`gemini-2.5-flash` via `@google/genai` or user-supplied key) providing rich semantic understanding, multi-rule extraction, and acoustic storytelling.
4. **Pixel Assistant Voice Commands ("Hey Google" / In-App Voice AI)**:
   - Voice intent parser supporting standard playback commands (*"play"*, *"pause"*, *"skip"*, *"previous"*), acoustic adjustments (*"volume to 80"*, *"turn on Bit-Perfect"*, *"enable Dolby Atmos"*), and generative prompts (*"cook me a late night lo-fi mix"*).
   - Powered by Web Speech API (`webkitSpeechRecognition`) with one-tap simulated fallback for noisy environments or browsers without microphone permission.
   - Visual Pixel Assistant / Gemini floating overlay with voice wave animation.
5. **Additional AI Features**:
   - **Soundbar AI Auto-EQ Curve Generator**: Users can type or speak headphone targets or acoustic fixes (e.g. *"Tame sibilant 6kHz highs on my planar IEMs, boost sub-bass warmth for synthwave"*), automatically computing all 10 parametric PEQ bands (`Hz`, `gainDb`, `Q-factor`) and applying them to the Web Audio engine.
   - **AI Harmonic DJ Queue Sequencer**: Re-orders active crate/mixtape tracks for optimal Camelot Wheel harmonic transitions (`1A–12B`) and smooth BPM ramping.
   - **AI Crate Historian & Audiophile Liner Notes**: Generates deep acoustic tags, instrumentation breakdowns, and mastering trivia for any track in the Audio Stash.
   - **Biometric Heart-Rate & Cadence BPM Sync**: Syncs real-time simulated or sensor heart-rate from the Pixel Watch 3 to adapt music tempo and energy in real-time.
6. **Desktop Layout**:
   - Side-by-side display on desktop: `412×915` Android Phone on the left + `456×456` Circular Pixel Watch 3 (44mm) on the right, plus viewport mode toggles (`Side-by-Side`, `Phone Only`, `Pixel Watch 3 Only`).

---

## Architecture & Implementation Steps

### Step 1: Create AI Engine (`src/utils/aiEngine.ts`)
- Implement `@google/genai` and local heuristic fallback routines for:
  - `generateAiMixtape(prompt, tracks, apiKey)`: Structured extraction of playlist title, target BPM, energy trajectory, and ranked track IDs.
  - `generateAiEqCurve(prompt, currentBands, apiKey)`: Maps plain-text EQ requests into 10 parametric gain/Q offsets with headroom protection.
  - `sequenceHarmonicDjQueue(tracks)`: Evaluates Camelot wheel adjacency (`±1 step` or relative major/minor) to sequence seamless DJ transitions.
  - `generateCrateLinerNotes(track, apiKey)`: Generates audiophile mastering context and acoustic breakdown.

### Step 2: Create Voice Assistant Engine (`src/utils/voiceAssistant.ts`)
- Implement `VoiceAssistantService` using browser `SpeechRecognition` / `webkitSpeechRecognition`.
- Match intents for playback (`play`, `pause`, `next`, `back`), volume/DAC routing (`dac`, `atmos`, `volume`), and Vibe Cooker prompts (`cook`, `make a playlist`, `vibe`).
- Return intent payloads for immediate UI and audio graph dispatch.

### Step 3: Build Wear OS 5 Pixel Watch 3 (44mm) Companion (`src/components/PixelWatchCompanion.tsx`)
- Render a realistic **44mm Pixel Watch 3 chassis** (`456×456px` circular display, 1.4mm slim bezel, dark titanium/aluminum finish, digital rotary crown knob, physical side button).
- Implement 5 Wear OS Screens & Tiles:
  1. **Circular Now Playing**: Curved SVG progress track, vinyl center disc, transport buttons, and crown rotary volume/DAC gain control.
  2. **Wrist Crate & Collection Browser (NEW)**:
     - Tabbed view: `Crates & Folders` vs. `All Lossless Tracks`.
     - Smooth scrolling powered by mouse wheel, touch drag, or clicking the physical rotary crown.
     - Track cards with format tags (`FLAC 24/96`, `DSD128`), BPM, and one-tap playback cueing.
  3. **Biometric Heart-Rate & Cadence Sync Tile**: Live animated pulse meter, cadence tracker, and "Sync Crate to Heart Rate" trigger.
  4. **Quick Vibe Cooker & Pixel Voice Mic Tile**: Instant voice query trigger and one-tap vibe capsules.
  5. **USB DAC & Dolby Atmos Remote Output Tile**: Remote DAC switcher, volume lock status, and Atmos 3D toggle.

### Step 4: Integrate AI & Voice Controls into Mobile Views
- **SoundbarView (`src/components/TabViews.tsx`)**: Add "AI Auto-EQ Prompt" pill and generator drawer with presets (*"Harman Bass Boost"*, *"Tame IEM Treble Sibilance"*, *"Warm Vinyl Re-amp"*).
- **CrateView (`src/components/TabViews.tsx`)**: Add "AI Harmonic DJ Sort" action pill on the indexed track stream.
- **TrackActionsSheet (`src/components/OverlaysAndModals.tsx`)**: Add "AI Crate Historian & Audiophile Liner Notes" drawer.
- **Pixel Assistant Voice Overlay**: Add animated bottom microphone drawer on the phone screen with waveform listening animation.

### Step 5: Update Main App Shell (`src/App.tsx`)
- Add side-by-side layout supporting both the `412×915` Android phone viewport and the `456×456` circular Pixel Watch 3 (44mm).
- Add desktop top bar controls to toggle between `Side-by-Side (Phone + Watch 3)`, `Phone Viewport Only`, and `Pixel Watch 3 Only`.
- Wire live bi-directional sync between Watch and Phone (cues, scrubbing, volume, playback state, DAC routing, and heart-rate BPM sync).

---

## Verification & Testing
1. **Compilation Check**: Run `compile_applet` to verify zero TypeScript/JSX errors.
2. **Pixel Watch 3 44mm Verification**:
   - Check `456×456` circular viewport proportions, bezel thinness, and rotary crown scrolling.
   - Test browsing crates/playlists and the full audio collection on the watch.
   - Tap a track on the watch and verify instant playback cueing on the phone and Web Audio engine.
3. **AI Engine Verification**:
   - Test Vibe Cooker with plain-text prompts (local heuristic and optional Gemini API).
   - Test Soundbar AI Auto-EQ with custom prompts and preset chips.
   - Test AI Harmonic DJ re-sequencing on the Crate stream.
   - Test AI Crate Historian modal on track details.
4. **Voice Assistant Verification**:
   - Test mic voice activation and simulated one-tap voice command shortcuts.
