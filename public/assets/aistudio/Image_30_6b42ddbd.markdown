---
name: VibePlay Mobile Experience
colors:
  surface: '#131315'
  surface-dim: '#131315'
  surface-bright: '#39393b'
  surface-container-lowest: '#0e0e10'
  surface-container-low: '#1b1b1d'
  surface-container: '#201f21'
  surface-container-high: '#2a2a2c'
  surface-container-highest: '#353437'
  on-surface: '#e5e1e4'
  on-surface-variant: '#bcc9cd'
  inverse-surface: '#e5e1e4'
  inverse-on-surface: '#303032'
  outline: '#869397'
  outline-variant: '#3d494c'
  surface-tint: '#4cd7f6'
  primary: '#4cd7f6'
  on-primary: '#003640'
  primary-container: '#06b6d4'
  on-primary-container: '#00424f'
  inverse-primary: '#00687a'
  secondary: '#ffb95f'
  on-secondary: '#472a00'
  secondary-container: '#ee9800'
  on-secondary-container: '#5b3800'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#1bbd85'
  on-tertiary-container: '#00452e'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#acedff'
  primary-fixed-dim: '#4cd7f6'
  on-primary-fixed: '#001f26'
  on-primary-fixed-variant: '#004e5c'
  secondary-fixed: '#ffddb8'
  secondary-fixed-dim: '#ffb95f'
  on-secondary-fixed: '#2a1700'
  on-secondary-fixed-variant: '#653e00'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#131315'
  on-background: '#e5e1e4'
  surface-variant: '#353437'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  telemetry-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.04em
  telemetry-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1.25rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an immersive, ergonomic mobile audio environment tailored for deep OLED displays, foldables, and edge-to-edge handheld media consumption.

### Brand Personality & Emotional Impact
- **Nocturnal & Immersive:** A pitch-black baseline that retreats into hardware bezels, keeping visual focus squarely on album art, telemetry, and waveforms.
- **Precision & Reliability:** Unapologetic fidelity where performance signals, bitrate indicators, and scrubbers communicate precision and responsiveness.
- **Tactile Ergonomics:** Controls are engineered for one-handed operation, thumb-reach arcs, and satisfying kinetic micro-interactions.

### Design Movement
The visual language merges **Modern Ergonomics** with **Ambient Dark Glass and Tonal Elevation**. Deep charcoal surfaces (#121214) layer seamlessly above hardware black. Radiant cyan (#06b6d4) provides high-energy luminous focal points, contrasted by warm amber (#f59e0b) for active playback states and emerald (#10b981) for pristine audio stream verification.

## Colors

The palette operates on strict contrast tiers designed for zero-light and low-light environments, eliminating eye fatigue while ensuring WCAG AAA legibility.

### Surface Architecture
- **Canvas Base (`#121214`):** The ground level for the full display canvas. Absorbs reflections on modern AMOLED panels.
- **Surface Elevation 1 (`#1a1a20`):** App bar backings, lower navigation drawers, and bottom sheet scaffolds.
- **Surface Elevation 2 (`#1e1e24`):** Interactive cards, deck trays, and floating media docks.
- **Surface Hairline / Structural Border (`#2a2a35`):** 1px structural outlines defining depth without heavy shadows.

### Accents & Indicators
- **Primary Accent (`#06b6d4`):** The signature vibrant cyan. Used for scrub heads, primary play triggers, volume fills, and active track highlights.
- **Secondary Accent (`#f59e0b`):** Warm amber used for live queues, favorited stems, dynamic EQ modes, and warning telemetry.
- **Status / Audio Route (`#10b981`):** Direct hardware routing, Lossless/FLAC verification badges, and connection integrity.

### Text Contrast
- **Text Primary (`#e4e4e7`):** Headers, active tracks, and primary values.
- **Text Muted / Secondary (`#9ca3af`):** Metadata, artist names, duration stamps, and dormant tabs.
- **Text Inactive (`#4b5563`):** Disabled actions and placeholder fields.

## Typography

The typographic hierarchy pairs the soft geometric geometry of Plus Jakarta Sans for titles with the systematic legibility of Inter for body text and interfaces, accompanied by JetBrains Mono for technical audio statistics.

### Typographic Roles
- **Headlines & Display (Plus Jakarta Sans):** Provides an organic, open contour to track headers and playlist banners, softening visual fatigue in dark mode.
- **Interface & Metadata (Inter):** High x-height ensures pristine legibility at small sizes, in dense track listings, and while the user is in motion.
- **Telemetry (JetBrains Mono):** Dedicated to technical readouts including dynamic bitrates (e.g., `24-bit / 96kHz FLAC`), buffer speeds, timestamps, and decibel meters.

## Layout & Spacing

Designed for the 412x915 baseline screen dimension, the spacing model prioritizes the physical anatomy of the human hand, concentrating high-frequency controls inside the lower 40% vertical screen zone.

### Grid & Ergonomic Zones
- **Vertical Reach Regions:**
  - *Top Zone (0–180dp):* Ambient art headers, album visuals, collapsed status indicators. Passive observation.
  - *Mid Zone (180–520dp):* Waveform scrubbers, contextual track information, lyric lines.
  - *Thumb Action Zone (520–915dp):* Main playback clusters, track skips, volume sliders, device selectors, and bottom navigation bar.
- **Horizontal Framing:** 16dp (`margin-mobile`) safe margins protect edge-swiping gestures and Android back swipe triggers.
- **Density:** 8dp modular increment baseline. Dense track listings use a compact 56dp row height; featured card carousels use 16dp spacing.

## Elevation & Depth

This system avoids heavy drop shadows, instead using subtle surface tone shifts with fine outlines to define layers cleanly against OLED dark backgrounds.

### Elevation Hierarchy
- **Level 0 (Canvas):** Pure `#121214`. Deepest layer, entirely flush.
- **Level 1 (Dock & Bottom Sheet Backdrops):** `#1a1a20` with a 1px top border of `#2a2a35`.
- **Level 2 (Active Cards & Trays):** `#1e1e24` with a complete 1px border of `#2a2a35`. Features an ultra-subtle ambient shadow: `0 8px 24px rgba(0, 0, 0, 0.45)`.
- **Level 3 (Floating Mini-Player & Overlays):** `#1e1e24` accented with a 1px border of `#2a2a35` and an outer ambient blur: `0 12px 32px rgba(0, 0, 0, 0.6)`.

### Accent Luminescence
Active playback indicators and selected play buttons leverage a luminous cyan aura (`0 0 20px rgba(6, 182, 212, 0.35)`) to signal live audio states through soft glowing light.

## Shapes

The interface embraces generous radii that echo modern Android hardware outlines, providing an approachable and tactile surface profile.

### Shape Language
- **Pill Containers (Level 3 Roundedness):** Core buttons, category chips, duration badges, and scrub heads feature full pill radiuses (`9999px` / `rounded-full`).
- **Interactive Deck Cards:** Use a dedicated 24dp to 28dp corner radius (`rounded-lg` through `rounded-xl`) matching handheld chassis corners.
- **Nested Album Art:** Kept in harmony with enclosing cards through a proportional 16dp radius.

## Components

### Buttons & Transport Controls
- **Primary Play Action:** 64x64dp minimum circular target. Solid `#06b6d4` fill, `#121214` icon color, subtle cyan aura on active state.
- **Secondary Actions (Skip, Shuffle, Repeat):** Minimum 48x48dp target bounding box. Ghost surface with `#e4e4e7` icons. Active states illuminate in `#06b6d4` or `#f59e0b`.
- **Micro-Pills:** 32dp height, padding `8px 16px`, background `#1e1e24`, border 1px `#2a2a35`.

### Track Cards & Carousels
- Card body filled with `#1e1e24`, framed with 1px `#2a2a35`, and 24dp corner radii.
- Contains 1:1 ratio square artwork with 16dp rounded corners.
- Telemetry badge (`JetBrains Mono`, 10px) docked inside card top-right corner with 40% translucent black background.

### Audio Scrubbers & Sliders
- Track rail: 4dp height, `#2a2a35` background.
- Progress fill: Solid `#06b6d4` with rounded cap.
- Scrub thumb: 16x16dp pill with 2dp solid `#e4e4e7` fill and active press scale factor of 1.25x with cyan glow. Touch target expanded to 48x48dp transparent boundary.

### Lists & Track Rows
- Row height: 64dp.
- Left-aligned 48x48dp artwork or track index.
- Central stack: Title (`Inter` 14dp, `#e4e4e7`) stacked over Artist (`Inter` 12dp, `#9ca3af`).
- Right-aligned trailing slot: Telemetry duration tag and vertical overflow action (min 48x48dp tap target).
- Row separation: Visual separation achieved through whitespace or subtle 1px divider in `#1a1a20`.

### Chips & Filters
- 36dp height pill geometry.
- Inactive: Background `#1a1a20`, border 1px `#2a2a35`, text `#9ca3af`.
- Active: Background `#06b6d4` at 15% opacity, border 1px `#06b6d4`, text `#06b6d4`.

### Input Fields & Search
- 52dp height, 26dp pill border radius.
- Background `#1e1e24`, 1px border `#2a2a35`.
- Focus state activates `#06b6d4` 1px outline with soft cyan ambient outer spread.