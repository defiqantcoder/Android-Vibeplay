import React, { useState, useEffect, useRef } from 'react';
import { CrateFolder, Track, UsbDacEndpoint } from '../data/vibeplayData';
import { formatTime } from './OverlaysAndModals';

/**
 * Pixel Watch 3 (44mm Edition) Companion Component
 * Optimized specifically for the Pixel Watch 3 44mm variation:
 * - 456×456px Circular Actua AMOLED Display (320 ppi, 2,000 nits, ultra-slim 1.4mm bezel)
 * - Physical Rotating Digital Crown Knob with scroll inertia and rotary volume/seeking
 * - 5 Wear OS 5 Tiles:
 *   1. Circular Now Playing (radial SVG arc scrubber, center vinyl artwork, rotary crown volume)
 *   2. Wrist Crate & Music Collection Browser (explore crates, playlists, and all tracks on wrist)
 *   3. Biometric Heart-Rate & Cadence BPM Sync (live pulse graph + tempo auto-sync)
 *   4. Quick Vibe Cooker Voice & One-Touch Tiles (Pixel voice mic + vibe capsules)
 *   5. USB DAC & Dolby Atmos Remote Output Tile (remote hardware switch & Bit-Perfect toggle)
 */

interface PixelWatchCompanionProps {
  track: Track;
  isPlaying: boolean;
  currentTimeSec: number;
  activeDac: UsbDacEndpoint;
  activeEndpointType: 'usb-dac' | 'phone-speaker';
  exclusiveUsbAccess: boolean;
  dolbyAtmosEnabled: boolean;
  dolbyAtmosProfile: 'auto' | 'music' | 'movie' | 'voice';
  crates: CrateFolder[];
  tracks: Track[];
  onTogglePlay: () => void;
  onPrev: () => void;
  onNext: () => void;
  onSeek: (sec: number) => void;
  onSelectTrack: (track: Track) => void;
  onToggleDolbyAtmos: () => void;
  onToggleBitPerfect: () => void;
  onTriggerVoicePrompt: (prompt: string) => void;
  onSyncBiometricBpm: (targetBpm: number) => void;
  onShowToast: (msg: string) => void;
}

type WatchScreen = 'now-playing' | 'crate-browser' | 'biometrics' | 'vibe-voice' | 'dac-remote';

export const PixelWatchCompanion: React.FC<PixelWatchCompanionProps> = ({
  track,
  isPlaying,
  currentTimeSec,
  activeDac,
  activeEndpointType,
  exclusiveUsbAccess,
  dolbyAtmosEnabled,
  dolbyAtmosProfile,
  crates,
  tracks,
  onTogglePlay,
  onPrev,
  onNext,
  onSeek,
  onSelectTrack,
  onToggleDolbyAtmos,
  onToggleBitPerfect,
  onTriggerVoicePrompt,
  onSyncBiometricBpm,
  onShowToast,
}) => {
  const [activeScreen, setActiveScreen] = useState<WatchScreen>('now-playing');
  const [watchVolume, setWatchVolume] = useState<number>(78);
  const [browserTab, setBrowserTab] = useState<'crates' | 'tracks'>('tracks');
  const [selectedCrateId, setSelectedCrateId] = useState<string | null>(null);

  // Biometrics simulation (Pixel Watch 3 Cardio & Heart Rate Sensor)
  const [simulatedHeartRate, setSimulatedHeartRate] = useState<number>(134);
  const [simulatedCadence, setSimulatedCadence] = useState<number>(168);
  const [isMeasuringPulse, setIsMeasuringPulse] = useState<boolean>(true);

  // Crown rotation visual angle
  const [crownAngle, setCrownAngle] = useState<number>(0);
  const [voiceListening, setVoiceListening] = useState<boolean>(false);

  // Heart-rate subtle oscillation
  useEffect(() => {
    if (!isMeasuringPulse) return;
    const interval = setInterval(() => {
      setSimulatedHeartRate((prev) => {
        const delta = Math.floor(Math.random() * 5) - 2;
        return Math.min(175, Math.max(72, prev + delta));
      });
    }, 2500);
    return () => clearInterval(interval);
  }, [isMeasuringPulse]);

  // Compute progress angle for circular arc (0 to 360 deg)
  const progressFraction = Math.min(
    1,
    Math.max(0, currentTimeSec / Math.max(1, track.durationSec))
  );
  const progressDegrees = progressFraction * 360;

  // Arc stroke dash for r=216px (Circumference = 2 * PI * 216 ≈ 1357px)
  const radius = 216;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (progressFraction * circumference);

  const rotateCrown = (delta: number) => {
    setCrownAngle((prev) => (prev + delta) % 360);
    if (activeScreen === 'now-playing') {
      setWatchVolume((prev) => {
        const next = Math.min(100, Math.max(0, prev + (delta > 0 ? 4 : -4)));
        onShowToast(`Watch Crown: Volume ${next}%`);
        return next;
      });
    }
  };

  const isBitPerfect =
    activeEndpointType === 'usb-dac' && exclusiveUsbAccess && !dolbyAtmosEnabled;

  const filteredCollectionTracks = tracks.filter((t) => {
    if (selectedCrateId) return t.crateId === selectedCrateId;
    return true;
  });

  return (
    <div className="flex flex-col items-center justify-center p-2 select-none">
      {/* Hardware Chassis Badge */}
      <div className="flex items-center gap-2 mb-2 px-3 py-1 rounded-full bg-[#16161a] border border-[#23232c] text-[11px] font-mono text-[#9ca3af]">
        <span className="w-2 h-2 rounded-full bg-primary shadow-[0_0_8px_rgba(6,182,212,0.8)] animate-pulse"></span>
        <span className="text-white font-semibold">Pixel Watch 3 (44mm)</span>
        <span className="text-primary font-bold">456×456 AMOLED</span>
        <span>•</span>
        <span className="text-tertiary">Actua Display</span>
      </div>

      {/* 44mm Watch Case Outer Frame */}
      <div className="relative flex items-center justify-center">
        {/* Watch Straps (Matte Obsidian Silicone Straps) */}
        <div className="absolute -top-12 w-44 h-16 rounded-t-2xl bg-gradient-to-b from-[#1b1b22] to-[#141418] border-t border-x border-[#2c2c36]/60 shadow-lg"></div>
        <div className="absolute -bottom-12 w-44 h-16 rounded-b-2xl bg-gradient-to-t from-[#1b1b22] to-[#141418] border-b border-x border-[#2c2c36]/60 shadow-lg"></div>

        {/* Physical Matte Aluminum Bezel (478px outer case) */}
        <div className="relative w-[476px] h-[476px] rounded-full bg-gradient-to-tr from-[#16161b] via-[#23232c] to-[#111114] p-[10px] shadow-[0_20px_60px_rgba(0,0,0,0.9),0_0_0_2px_rgba(255,255,255,0.06)] flex items-center justify-center">
          
          {/* Rotating Digital Crown (Right side hardware affordance with 3D rotation) */}
          <div
            onClick={() => rotateCrown(30)}
            title="Click to rotate Digital Crown (Volume & Scroll)"
            className="absolute -right-3.5 top-1/2 -translate-y-1/2 w-6 h-16 rounded-r-lg bg-gradient-to-r from-[#202026] via-[#353542] to-[#18181f] border-r border-y border-[#4a4a58] shadow-[0_4px_12px_rgba(0,0,0,0.6)] cursor-pointer flex flex-col justify-around py-1.5 active:scale-95 group z-40"
          >
            {[...Array(6)].map((_, i) => (
              <div
                key={i}
                className="w-full h-1 bg-[#4a4a58] rounded-full group-hover:bg-primary transition-colors"
                style={{
                  transform: `translateX(${Math.sin((crownAngle + i * 40) * (Math.PI / 180)) * 1.5}px)`,
                }}
              />
            ))}
          </div>

          {/* Physical Side Assistant / Recents Button (Top right side of bezel) */}
          <button
            onClick={() => {
              setActiveScreen((prev) =>
                prev === 'crate-browser' ? 'now-playing' : 'crate-browser'
              );
              onShowToast('Side Button: Toggled Wrist Crate Explorer');
            }}
            title="Physical Side Button: Quick Jump to Crate & Collection"
            className="absolute right-3 top-20 w-3 h-9 rounded-r bg-[#2a2a35] hover:bg-primary active:scale-90 transition-all shadow-md z-30"
          />

          {/* 456x456 Circular AMOLED Display Surface */}
          <div className="relative w-[456px] h-[456px] rounded-full bg-black overflow-hidden flex flex-col items-center justify-between p-4 shadow-inner border border-white/5">
            
            {/* Top Status Curved Header */}
            <div className="w-full flex items-center justify-between px-7 pt-2 z-20 text-[11px] font-mono text-[#9ca3af]">
              <span className="font-bold text-white tracking-wider">10:42</span>
              <div className="flex items-center gap-1.5">
                {dolbyAtmosEnabled ? (
                  <span className="px-1.5 py-0.2 rounded-full bg-secondary/20 text-secondary text-[10px] font-bold">
                    ATMOS
                  </span>
                ) : isBitPerfect ? (
                  <span className="px-1.5 py-0.2 rounded-full bg-tertiary/20 text-tertiary text-[10px] font-bold">
                    DAC 24/96
                  </span>
                ) : (
                  <span className="text-[#9ca3af] text-[10px]">PHONE</span>
                )}
                <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
              </div>
            </div>

            {/* SCREEN 1: Circular Now Playing Tile */}
            {activeScreen === 'now-playing' && (
              <div className="relative w-full flex-1 flex flex-col items-center justify-between py-1 z-10">
                {/* Full Circular Radial SVG Progress Track */}
                <svg
                  className="absolute inset-0 w-full h-full -rotate-90 pointer-events-none"
                  viewBox="0 0 456 456"
                >
                  {/* Outer track background */}
                  <circle
                    cx="228"
                    cy="228"
                    r={radius}
                    fill="none"
                    stroke="#1e1e24"
                    strokeWidth="5"
                  />
                  {/* Dynamic Arc Progress with Cyan Glow */}
                  <circle
                    cx="228"
                    cy="228"
                    r={radius}
                    fill="none"
                    stroke="#06b6d4"
                    strokeWidth="5"
                    strokeDasharray={circumference}
                    strokeDashoffset={strokeDashoffset}
                    strokeLinecap="round"
                    style={{ transition: 'stroke-dashoffset 0.8s ease' }}
                  />
                </svg>

                {/* Center Spinning Vinyl Disc Artwork */}
                <div className="relative w-36 h-36 rounded-full bg-[#121214] p-1 shadow-[0_0_30px_rgba(6,182,212,0.25)] flex items-center justify-center my-auto">
                  <div
                    className={`relative w-full h-full rounded-full overflow-hidden flex items-center justify-center ${
                      isPlaying ? 'animate-[spin_12s_linear_infinite]' : ''
                    }`}
                  >
                    <img
                      src={track.artworkUrl}
                      alt={track.title}
                      referrerPolicy="no-referrer"
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute w-8 h-8 rounded-full bg-[#0a0a0c] border border-white/20 flex items-center justify-center shadow-inner">
                      <div className="w-2.5 h-2.5 rounded-full bg-primary"></div>
                    </div>
                  </div>
                  {/* Format telemetry badge */}
                  <span className="absolute -bottom-2 px-2 py-0.5 rounded-full bg-[#1e1e24]/90 border border-primary/40 text-[9px] font-mono text-primary font-bold shadow-md">
                    {track.shortFormat}
                  </span>
                </div>

                {/* Metadata & Volume */}
                <div className="text-center px-6 max-w-[280px]">
                  <h3 className="font-headline-sm text-white text-[15px] truncate font-bold">
                    {track.title}
                  </h3>
                  <p className="font-body-sm text-[#9ca3af] text-[12px] truncate">
                    {track.artist}
                  </p>
                  <div className="flex items-center justify-center gap-2 mt-0.5 font-telemetry-sm text-[10px] text-[#9ca3af]">
                    <span>{formatTime(currentTimeSec)}</span>
                    <span>•</span>
                    <span className="text-primary font-semibold">
                      Crown Vol: {watchVolume}%
                    </span>
                    <span>•</span>
                    <span>{track.durationLabel}</span>
                  </div>
                </div>

                {/* Transport Controls */}
                <div className="flex items-center justify-center gap-4 pt-1 pb-1">
                  <button
                    onClick={onPrev}
                    aria-label="Previous"
                    className="w-10 h-10 rounded-full bg-[#1e1e24] text-white flex items-center justify-center active:scale-90 hover:bg-[#2a2a35] transition-transform"
                  >
                    <span className="material-symbols-outlined text-[20px]">
                      skip_previous
                    </span>
                  </button>

                  <button
                    onClick={onTogglePlay}
                    aria-label="Play or Pause"
                    className="w-14 h-14 rounded-full bg-primary text-[#0e0e10] flex items-center justify-center shadow-[0_0_20px_rgba(6,182,212,0.45)] active:scale-95 transition-transform"
                  >
                    <span
                      className="material-symbols-outlined text-[28px]"
                      style={{ fontVariationSettings: "'FILL' 1" }}
                    >
                      {isPlaying ? 'pause' : 'play_arrow'}
                    </span>
                  </button>

                  <button
                    onClick={onNext}
                    aria-label="Next"
                    className="w-10 h-10 rounded-full bg-[#1e1e24] text-white flex items-center justify-center active:scale-90 hover:bg-[#2a2a35] transition-transform"
                  >
                    <span className="material-symbols-outlined text-[20px]">
                      skip_next
                    </span>
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN 2: Wrist Crate & Music Collection Browser (Optimized for Pixel Watch 3 44mm) */}
            {activeScreen === 'crate-browser' && (
              <div className="relative w-full flex-1 flex flex-col justify-between py-1 z-10">
                {/* Header Subtabs (Crates vs All Tracks) */}
                <div className="flex items-center justify-center gap-1.5 mb-1 px-8">
                  <button
                    onClick={() => {
                      setBrowserTab('tracks');
                      setSelectedCrateId(null);
                    }}
                    className={`px-3 py-1 rounded-full text-[11px] font-mono transition-colors ${
                      browserTab === 'tracks' && !selectedCrateId
                        ? 'bg-primary text-[#0e0e10] font-bold shadow-sm'
                        : 'bg-[#1e1e24] text-[#9ca3af]'
                    }`}
                  >
                    Audio Stash ({tracks.length})
                  </button>
                  <button
                    onClick={() => setBrowserTab('crates')}
                    className={`px-3 py-1 rounded-full text-[11px] font-mono transition-colors ${
                      browserTab === 'crates'
                        ? 'bg-primary text-[#0e0e10] font-bold shadow-sm'
                        : 'bg-[#1e1e24] text-[#9ca3af]'
                    }`}
                  >
                    Crates ({crates.length})
                  </button>
                </div>

                {/* Scrollable Container with Rotary Crown Support */}
                <div
                  onWheel={(e) => {
                    rotateCrown(e.deltaY > 0 ? 20 : -20);
                  }}
                  className="flex-1 overflow-y-auto no-scrollbar px-6 space-y-1.5 max-h-[290px]"
                >
                  {browserTab === 'crates' && !selectedCrateId && (
                    <div className="space-y-1.5">
                      <span className="text-[10px] font-mono uppercase text-primary block text-center">
                        Active Crates &amp; Folders
                      </span>
                      {crates.map((crate) => (
                        <div
                          key={crate.id}
                          onClick={() => {
                            setSelectedCrateId(crate.id);
                            setBrowserTab('tracks');
                            onShowToast(`Wrist Filter: ${crate.name}`);
                          }}
                          className="p-2.5 rounded-2xl bg-[#1a1a22] hover:bg-[#252530] border border-[#2a2a35] flex items-center justify-between cursor-pointer active:scale-98 transition-transform"
                        >
                          <div className="flex items-center gap-2 min-w-0 pr-1">
                            <div className="w-8 h-8 rounded-xl bg-primary/15 text-primary flex items-center justify-center text-sm shrink-0">
                              <span className="material-symbols-outlined text-[18px]">
                                {crate.icon}
                              </span>
                            </div>
                            <div className="min-w-0">
                              <p className="font-label-md text-white text-[12px] font-bold truncate">
                                {crate.name}
                              </p>
                              <p className="font-telemetry-sm text-[#9ca3af] text-[9px] truncate">
                                {crate.subtitle}
                              </p>
                            </div>
                          </div>
                          <span className="font-telemetry-sm text-[10px] px-1.5 py-0.5 rounded bg-black/40 text-secondary font-semibold shrink-0">
                            {crate.badge}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  {browserTab === 'tracks' && (
                    <div className="space-y-1.5">
                      {selectedCrateId && (
                        <div className="flex items-center justify-between px-1 mb-1">
                          <button
                            onClick={() => setSelectedCrateId(null)}
                            className="text-[10px] font-mono text-primary flex items-center gap-0.5"
                          >
                            <span className="material-symbols-outlined text-[13px]">
                              arrow_back
                            </span>
                            All Stash
                          </button>
                          <span className="text-[10px] font-mono text-[#9ca3af]">
                            {filteredCollectionTracks.length} tracks
                          </span>
                        </div>
                      )}

                      {filteredCollectionTracks.map((t) => {
                        const isCurrent = t.id === track.id;
                        return (
                          <div
                            key={t.id}
                            onClick={() => {
                              onSelectTrack(t);
                              onShowToast(`Watch cued: ${t.title}`);
                            }}
                            className={`p-2 rounded-2xl flex items-center justify-between cursor-pointer active:scale-98 transition-all ${
                              isCurrent
                                ? 'bg-primary/20 border border-primary/60 shadow-[0_0_12px_rgba(6,182,212,0.3)]'
                                : 'bg-[#181820] hover:bg-[#22222c] border border-[#2a2a35]'
                            }`}
                          >
                            <div className="flex items-center gap-2 min-w-0 pr-1">
                              <div className="w-8 h-8 rounded-xl overflow-hidden bg-black shrink-0 relative">
                                <img
                                  src={t.artworkUrl}
                                  alt={t.title}
                                  referrerPolicy="no-referrer"
                                  className="w-full h-full object-cover"
                                />
                                {isCurrent && (
                                  <div className="absolute inset-0 bg-primary/30 flex items-center justify-center">
                                    <span className="material-symbols-outlined text-[14px] text-primary">
                                      equalizer
                                    </span>
                                  </div>
                                )}
                              </div>
                              <div className="min-w-0">
                                <p
                                  className={`font-label-md text-[11px] font-bold truncate ${
                                    isCurrent ? 'text-primary' : 'text-white'
                                  }`}
                                >
                                  {t.title}
                                </p>
                                <p className="font-telemetry-sm text-[#9ca3af] text-[9px] truncate">
                                  {t.artist} • {t.bpm} BPM • {t.camelotKey}
                                </p>
                              </div>
                            </div>
                            <span
                              className={`font-telemetry-sm text-[9px] px-1.5 py-0.5 rounded font-bold shrink-0 ${
                                t.shortFormat.includes('DSD')
                                  ? 'bg-secondary/20 text-secondary'
                                  : 'bg-primary/15 text-primary'
                              }`}
                            >
                              {t.shortFormat}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                <div className="text-center pt-0.5 text-[9px] font-mono text-[#9ca3af]">
                  Rotate Crown to fast-scroll collection
                </div>
              </div>
            )}

            {/* SCREEN 3: Biometric Heart-Rate & Cadence BPM Sync Tile */}
            {activeScreen === 'biometrics' && (
              <div className="relative w-full flex-1 flex flex-col items-center justify-between py-2 z-10 px-6">
                <div className="text-center">
                  <div className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-error/15 text-error text-[10px] font-mono font-bold">
                    <span className="w-1.5 h-1.5 rounded-full bg-error animate-ping"></span>
                    CARDIOPULSE SENSOR
                  </div>
                  <h4 className="font-headline-sm text-white text-[15px] font-bold mt-1">
                    Biometric BPM Sync
                  </h4>
                </div>

                {/* Animated Heart Rate Pulse Display */}
                <div className="flex flex-col items-center justify-center my-1">
                  <div className="flex items-baseline gap-1">
                    <span className="text-4xl font-extrabold text-white font-mono tracking-tight animate-pulse">
                      {simulatedHeartRate}
                    </span>
                    <span className="text-xs font-mono text-error font-bold">
                      BPM
                    </span>
                  </div>
                  <p className="text-[11px] font-mono text-[#9ca3af] mt-0.5">
                    Step Cadence: <b className="text-white">{simulatedCadence} SPM</b>
                  </p>
                </div>

                {/* Simulated Real-Time ECG Waveform */}
                <div className="w-full h-10 flex items-center justify-center px-4">
                  <svg
                    className="w-full h-8 text-error"
                    viewBox="0 0 160 32"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M 0 16 L 30 16 L 40 8 L 50 24 L 60 4 L 70 28 L 80 16 L 110 16 L 120 10 L 130 22 L 140 16 L 160 16" />
                  </svg>
                </div>

                {/* Sync Action Button */}
                <button
                  onClick={() => {
                    onSyncBiometricBpm(simulatedHeartRate);
                    onShowToast(`Steered Crate to Heart Rate: ${simulatedHeartRate} BPM`);
                  }}
                  className="w-full py-2.5 px-4 rounded-full bg-gradient-to-r from-error/80 to-secondary text-white font-label-md text-xs font-bold shadow-[0_0_16px_rgba(239,68,68,0.35)] active:scale-95 transition-transform flex items-center justify-center gap-1.5"
                >
                  <span className="material-symbols-outlined text-[16px]">
                    favorite
                  </span>
                  <span>Sync Crate to {simulatedHeartRate} BPM</span>
                </button>
              </div>
            )}

            {/* SCREEN 4: Quick Vibe Cooker & Pixel Voice Mic Tile */}
            {activeScreen === 'vibe-voice' && (
              <div className="relative w-full flex-1 flex flex-col items-center justify-between py-2 z-10 px-6">
                <div className="text-center">
                  <span className="text-[10px] font-mono text-primary uppercase font-bold tracking-wider">
                    Pixel Voice Assistant
                  </span>
                  <h4 className="font-headline-sm text-white text-[15px] font-bold mt-0.5">
                    "Hey Google, cook a vibe"
                  </h4>
                </div>

                {/* Giant Voice Mic Button */}
                <button
                  onClick={() => {
                    setVoiceListening(true);
                    setTimeout(() => {
                      setVoiceListening(false);
                      onTriggerVoicePrompt('Late night downtempo with warm analog tape hiss, under 95 BPM');
                      onShowToast('Wrist AI: Cooking Late Night Downtempo');
                    }, 1400);
                  }}
                  className={`w-20 h-20 rounded-full flex items-center justify-center shadow-lg transition-transform active:scale-95 ${
                    voiceListening
                      ? 'bg-error text-white animate-pulse shadow-[0_0_24px_rgba(239,68,68,0.6)]'
                      : 'bg-primary text-[#0e0e10] shadow-[0_0_24px_rgba(6,182,212,0.4)]'
                  }`}
                >
                  <span
                    className="material-symbols-outlined text-[36px]"
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    mic
                  </span>
                </button>

                {/* One-Tap Wrist Vibe Capsules */}
                <div className="grid grid-cols-2 gap-1.5 w-full">
                  {[
                    { label: 'Morning Lo-Fi', prompt: 'Chilled lo-fi beats, warm Rhodes piano, 80 BPM', emoji: '☕' },
                    { label: 'Fast Cardio', prompt: 'High cadence industrial techno & drum-and-bass, >135 BPM', emoji: '🔥' },
                    { label: 'Rainy Ambient', prompt: 'Pure generative ambient field recordings of gentle rain, DSD', emoji: '🌧️' },
                    { label: 'Pop-Punk Blast', prompt: 'Upbeat 2000s energetic pop-punk drums, 140 BPM', emoji: '⚡' },
                  ].map((capsule) => (
                    <button
                      key={capsule.label}
                      onClick={() => {
                        onTriggerVoicePrompt(capsule.prompt);
                        onShowToast(`Cooked: ${capsule.label}`);
                      }}
                      className="py-1.5 px-2 rounded-xl bg-[#1e1e24] hover:bg-primary/20 text-white font-label-md text-[10px] flex items-center gap-1 active:scale-95 transition-colors"
                    >
                      <span>{capsule.emoji}</span>
                      <span className="truncate">{capsule.label}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* SCREEN 5: USB DAC & Dolby Atmos Remote Output Tile */}
            {activeScreen === 'dac-remote' && (
              <div className="relative w-full flex-1 flex flex-col items-center justify-between py-2 z-10 px-6">
                <div className="text-center">
                  <span className="text-[10px] font-mono text-tertiary uppercase font-bold tracking-wider">
                    Audio Hardware Remote
                  </span>
                  <h4 className="font-headline-sm text-white text-[15px] font-bold mt-0.5">
                    {activeDac.modelName}
                  </h4>
                </div>

                {/* DAC Telemetry Capsule */}
                <div className="p-3 rounded-2xl bg-[#181820] border border-[#2a2a35] w-full text-center space-y-1">
                  <div className="flex items-center justify-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-tertiary shadow-[0_0_6px_rgba(78,222,163,0.8)]"></span>
                    <span className="font-mono text-xs text-white font-bold">
                      {activeDac.interfaceType}
                    </span>
                  </div>
                  <p className="font-mono text-[10px] text-primary">
                    {activeDac.maxBitDepth} / {activeDac.maxSampleRate} • {activeDac.clockArchitecture}
                  </p>
                </div>

                {/* Interactive Watch Toggles */}
                <div className="space-y-1.5 w-full">
                  <button
                    onClick={onToggleBitPerfect}
                    className={`w-full py-2 px-3 rounded-xl font-mono text-xs flex items-center justify-between transition-colors ${
                      isBitPerfect
                        ? 'bg-tertiary/20 text-tertiary border border-tertiary/50 font-bold'
                        : 'bg-[#1e1e24] text-[#9ca3af]'
                    }`}
                  >
                    <span>Bit-Perfect Direct</span>
                    <span>{isBitPerfect ? 'ON ✓' : 'OFF'}</span>
                  </button>

                  <button
                    onClick={onToggleDolbyAtmos}
                    className={`w-full py-2 px-3 rounded-xl font-mono text-xs flex items-center justify-between transition-colors ${
                      dolbyAtmosEnabled
                        ? 'bg-secondary/20 text-secondary border border-secondary/50 font-bold'
                        : 'bg-[#1e1e24] text-[#9ca3af]'
                    }`}
                  >
                    <span>Dolby Atmos ({dolbyAtmosProfile.toUpperCase()})</span>
                    <span>{dolbyAtmosEnabled ? 'ACTIVE 🌐' : 'OFF'}</span>
                  </button>
                </div>

                <div className="text-[9px] font-mono text-[#9ca3af] text-center">
                  Hardware Vol Locked at 0 dB (100%)
                </div>
              </div>
            )}

            {/* Bottom Tile Navigation Dots (Material You Watch Indicator) */}
            <div className="w-full flex items-center justify-center gap-2 pb-1 z-20">
              {(
                [
                  ['now-playing', 'play_circle', 'Now Playing'],
                  ['crate-browser', 'inventory_2', 'Crates & Stash'],
                  ['biometrics', 'favorite', 'Cardio BPM'],
                  ['vibe-voice', 'mic', 'Voice Assistant'],
                  ['dac-remote', 'headphones', 'DAC Output'],
                ] as const
              ).map(([screenKey, icon, title]) => (
                <button
                  key={screenKey}
                  onClick={() => {
                    setActiveScreen(screenKey);
                    onShowToast(`Watch Screen: ${title}`);
                  }}
                  title={title}
                  className={`w-7 h-7 rounded-full flex items-center justify-center transition-all ${
                    activeScreen === screenKey
                      ? 'bg-primary text-[#0e0e10] scale-110 shadow-[0_0_10px_rgba(6,182,212,0.6)] font-bold'
                      : 'bg-[#1e1e24] text-[#9ca3af] hover:text-white'
                  }`}
                >
                  <span className="material-symbols-outlined text-[15px]">
                    {icon}
                  </span>
                </button>
              ))}
            </div>

          </div>
        </div>
      </div>
    </div>
  );
};
