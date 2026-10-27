import React, { useState } from 'react';
import {
  DuplicateGroup,
  Track,
  UsbDacEndpoint,
} from '../data/vibeplayData';

export function formatTime(sec: number): string {
  const s = Math.max(0, Math.floor(sec));
  const mins = Math.floor(s / 60);
  const rem = s % 60;
  return `${String(mins).padStart(2, '0')}:${String(rem).padStart(2, '0')}`;
}

interface NowPlayingProps {
  track: Track;
  isPlaying: boolean;
  currentTimeSec: number;
  shuffle: boolean;
  repeatMode: 'off' | 'all' | 'one';
  ambientGlow: boolean;
  activeEndpointType: 'usb-dac' | 'phone-speaker';
  activeDac: UsbDacEndpoint;
  exclusiveUsbAccess: boolean;
  dolbyAtmosEnabled: boolean;
  dolbyAtmosProfile: 'auto' | 'music' | 'movie' | 'voice';
  onClose: () => void;
  onTogglePlay: () => void;
  onPrev: () => void;
  onNext: () => void;
  onSeek: (sec: number) => void;
  onToggleLike: (id: string) => void;
  onToggleShuffle: () => void;
  onCycleRepeat: () => void;
  onOpenLyrics: () => void;
  onOpenDacSheet: () => void;
  onOpenDriveMode: () => void;
  onOpenTrackActions: (track: Track) => void;
}

export const NowPlayingModal: React.FC<NowPlayingProps> = ({
  track,
  isPlaying,
  currentTimeSec,
  shuffle,
  repeatMode,
  ambientGlow,
  activeEndpointType,
  activeDac,
  exclusiveUsbAccess,
  dolbyAtmosEnabled,
  dolbyAtmosProfile,
  onClose,
  onTogglePlay,
  onPrev,
  onNext,
  onSeek,
  onToggleLike,
  onToggleShuffle,
  onCycleRepeat,
  onOpenLyrics,
  onOpenDacSheet,
  onOpenDriveMode,
  onOpenTrackActions,
}) => {
  const progressPct = Math.min(
    100,
    Math.max(0, (currentTimeSec / Math.max(1, track.durationSec)) * 100)
  );
  const remainingSec = Math.max(0, track.durationSec - currentTimeSec);
  const isBitPerfect =
    activeEndpointType === 'usb-dac' && exclusiveUsbAccess && !dolbyAtmosEnabled;

  const activeBarCount = Math.round((progressPct / 100) * track.waveform.length);

  return (
    <div className="absolute inset-0 z-50 bg-[#0e0e10] overflow-y-auto no-scrollbar flex flex-col justify-between select-none">
      {/* Ambient Backlight Gradient Layer */}
      {ambientGlow && (
        <div
          aria-hidden="true"
          className="absolute -top-10 left-0 w-full h-[520px] pointer-events-none z-0"
        >
          <div className="absolute inset-0 artwork-aura-cyan"></div>
          <div className="absolute inset-0 artwork-aura-amber"></div>
        </div>
      )}

      {/* Top Status & App Bar */}
      <header className="relative z-10 pt-3 px-5 flex flex-col gap-2 shrink-0">
        <div className="flex justify-between items-center text-xs text-[#9ca3af]/80 font-mono tracking-tight pt-1">
          <span className="font-semibold text-[#e4e4e7] text-[13px]">10:42</span>
          <div className="flex items-center space-x-2 text-[11px]">
            <span className="text-primary font-bold tracking-wider">
              {dolbyAtmosEnabled ? 'Dolby Atmos' : 'Hi-Res'}
            </span>
            <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
              <path d="M12 3c-4.97 0-9 4.03-9 9 0 2.12.74 4.07 1.97 5.61L12 22l7.03-4.39C20.26 16.07 21 14.12 21 12c0-4.97-4.03-9-9-9z" />
            </svg>
            <div className="w-5 h-2.5 border border-[#9ca3af]/80 rounded-sm p-0.5 flex items-center">
              <div className="h-full w-4/5 bg-primary rounded-xs"></div>
            </div>
          </div>
        </div>

        <div className="h-12 flex items-center justify-between mt-1">
          <button
            aria-label="Dismiss to Mini-Player"
            onClick={onClose}
            className="w-10 h-10 rounded-full bg-[#16161a]/80 border border-[#23232c] flex items-center justify-center text-[#e4e4e7] hover:bg-[#1e1e24] active:scale-95 transition-all shadow-sm"
            type="button"
          >
            <span className="material-symbols-outlined text-[22px]">
              keyboard_arrow_down
            </span>
          </button>

          <button
            type="button"
            onClick={onOpenDriveMode}
            title="Tap to enter Car Cockpit Drive Mode"
            className="flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#16161a]/90 border border-[#23232c]/80 backdrop-blur-md shadow-inner hover:border-primary/40 transition-colors"
          >
            <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
            <span className="text-[11px] font-mono font-bold tracking-widest text-[#e4e4e7] uppercase">
              NOW SPINNING ☕
            </span>
          </button>

          <div className="flex items-center space-x-1">
            <button
              aria-label="Toggle Favorite"
              onClick={() => onToggleLike(track.id)}
              className="w-10 h-10 rounded-full bg-[#16161a]/60 border border-[#23232c]/60 flex items-center justify-center hover:bg-[#1e1e24] active:scale-95 transition-all"
              type="button"
            >
              <span
                className={`material-symbols-outlined text-[20px] ${
                  track.liked
                    ? 'text-secondary drop-shadow-[0_0_8px_rgba(245,158,11,0.5)]'
                    : 'text-[#9ca3af]'
                }`}
                style={{
                  fontVariationSettings: track.liked ? "'FILL' 1" : "'FILL' 0",
                }}
              >
                favorite
              </span>
            </button>

            <button
              aria-label="Toggle Synced Lyrics"
              onClick={onOpenLyrics}
              className="w-10 h-10 rounded-full bg-[#16161a]/60 border border-[#23232c]/60 flex items-center justify-center text-[#9ca3af] hover:text-[#e4e4e7] hover:bg-[#1e1e24] active:scale-95 transition-all"
              type="button"
            >
              <span className="material-symbols-outlined text-[20px]">
                lyrics
              </span>
            </button>
          </div>
        </div>
      </header>

      {/* Artwork Stage */}
      <section className="relative z-10 flex flex-col items-center justify-center px-6 mt-1">
        <div className="w-8 h-1 rounded-full bg-[#23232c]/80 mb-3 opacity-60"></div>
        <div
          onClick={() => onOpenTrackActions(track)}
          className="relative group cursor-pointer"
          title="Tap artwork for Acoustic Profile & Actions"
        >
          <div className="relative w-[308px] h-[308px] rounded-[24px] p-1 bg-gradient-to-b from-white/10 to-black/60 shadow-[0_20px_60px_-15px_rgba(6,182,212,0.3),0_15px_45px_-10px_rgba(245,158,11,0.25)]">
            <img
              alt={track.title}
              src={track.artworkUrl}
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover rounded-[22px] vinyl-sheen-border select-none"
            />
            <div className="absolute inset-1 rounded-[22px] bg-gradient-to-tr from-transparent via-white/5 to-white/15 pointer-events-none"></div>
          </div>
        </div>
      </section>

      {/* Metadata & Telemetry */}
      <section className="relative z-10 px-6 mt-3">
        <div className="flex items-start justify-between">
          <div className="flex-1 pr-3 min-w-0">
            <h1 className="text-[22px] leading-tight font-extrabold text-[#e4e4e7] tracking-tight truncate">
              {track.title}
            </h1>
            <p className="text-[14px] font-medium text-[#9ca3af] mt-1 truncate">
              {track.artist}{' '}
              <span className="text-[#9ca3af]/40 mx-1">•</span> {track.album}
            </p>
          </div>

          <div className="flex flex-col items-end shrink-0">
            <button
              type="button"
              onClick={onOpenDacSheet}
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-[#1e1e24] border border-primary/30 text-primary text-[11px] font-mono font-bold tracking-tight shadow-sm"
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${
                  dolbyAtmosEnabled
                    ? 'bg-secondary animate-pulse'
                    : 'bg-tertiary animate-pulse'
                }`}
              ></span>
              {dolbyAtmosEnabled
                ? `Atmos (${dolbyAtmosProfile.toUpperCase()})`
                : isBitPerfect
                ? `${track.shortFormat} Direct`
                : '48kHz Mixer'}
            </button>
            <span className="text-[10px] text-secondary font-mono mt-1 font-semibold flex items-center gap-0.5">
              {track.formatBadge} ✨
            </span>
          </div>
        </div>

        {/* Performance & Energy Telemetry Tags */}
        <div className="flex items-center gap-2 mt-3 font-mono text-[11px] flex-wrap">
          <div className="px-2.5 py-1 rounded-md bg-[#16161a]/90 border border-[#23232c] text-[#9ca3af] flex items-center gap-1.5">
            <span className="text-primary font-bold">{track.bpm}</span> BPM ⚡
          </div>
          <div className="px-2.5 py-1 rounded-md bg-[#16161a]/90 border border-[#23232c] text-[#9ca3af] flex items-center gap-1.5">
            <span className="text-secondary font-bold">
              {track.energy >= 0.7 ? 'High' : track.energy >= 0.45 ? 'Mid' : 'Chill'}
            </span>{' '}
            Energy 🔥
          </div>
          <div className="px-2.5 py-1 rounded-md bg-[#16161a]/90 border border-[#23232c] text-[#9ca3af] flex items-center gap-1">
            {dolbyAtmosEnabled ? (
              <span className="text-secondary">Dolby Atmos 3D</span>
            ) : isBitPerfect ? (
              <span className="text-tertiary">Bit-Perfect</span>
            ) : (
              <span className="text-primary">Key {track.camelotKey}</span>
            )}
          </div>
        </div>
      </section>

      {/* Audiophile Waveform Scrubber */}
      <section className="relative z-10 px-6 mt-2">
        <div
          className="h-10 w-full flex items-end justify-between gap-[3px] py-1 cursor-pointer"
          onClick={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const ratio = Math.min(
              1,
              Math.max(0, (e.clientX - rect.left) / rect.width)
            );
            onSeek(Math.round(ratio * track.durationSec));
          }}
        >
          {track.waveform.map((val, idx) => {
            const isPlayed = idx <= activeBarCount;
            const isPeakZone = idx >= 13 && idx <= 20;
            return (
              <div
                key={idx}
                style={{ height: `${val}%` }}
                className={`w-1.5 rounded-full transition-colors ${
                  isPlayed
                    ? isPeakZone
                      ? 'bg-secondary shadow-[0_0_8px_rgba(245,158,11,0.6)]'
                      : 'bg-primary'
                    : 'bg-[#272733]'
                }`}
              />
            );
          })}
        </div>

        {/* Linear Scrub Track & Handle Thumb */}
        <div
          className="relative w-full h-1.5 bg-[#23232c] rounded-full mt-2 flex items-center cursor-pointer group"
          onClick={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const ratio = Math.min(
              1,
              Math.max(0, (e.clientX - rect.left) / rect.width)
            );
            onSeek(Math.round(ratio * track.durationSec));
          }}
        >
          <div
            style={{ width: `${progressPct}%` }}
            className="h-full rounded-full bg-gradient-to-r from-primary via-cyan-400 to-secondary"
          ></div>
          <div
            style={{ left: `${progressPct}%` }}
            className="absolute -translate-x-1/2 w-4 h-4 rounded-full bg-white border-2 border-primary shadow-[0_0_25px_-4px_rgba(6,182,212,0.75)] group-hover:scale-125 transition-transform flex items-center justify-center"
          >
            <div className="w-1.5 h-1.5 rounded-full bg-primary"></div>
          </div>
        </div>

        {/* Timecode Telemetry Row */}
        <div className="flex justify-between items-center mt-2 font-mono text-[12px] font-semibold tracking-wider">
          <span className="text-primary tabular-nums">
            {formatTime(currentTimeSec)}
          </span>
          <span className="text-[#9ca3af]/70 tabular-nums">
            -{formatTime(remainingSec)}
          </span>
        </div>
      </section>

      {/* Playback Controls */}
      <section className="relative z-10 px-6 py-1">
        <div className="flex items-center justify-between">
          <button
            aria-label="Toggle Shuffle"
            onClick={onToggleShuffle}
            className={`w-12 h-12 rounded-full flex flex-col items-center justify-center hover:bg-[#16161a] active:scale-90 transition-all ${
              shuffle ? 'text-primary' : 'text-[#9ca3af]'
            }`}
            type="button"
          >
            <span className="material-symbols-outlined text-[22px]">shuffle</span>
            <span
              className={`w-1.5 h-1.5 rounded-full mt-0.5 ${
                shuffle ? 'bg-primary' : 'bg-transparent'
              }`}
            ></span>
          </button>

          <button
            aria-label="Previous Track"
            onClick={onPrev}
            className="w-12 h-12 rounded-full flex items-center justify-center text-[#e4e4e7] hover:bg-[#16161a] active:scale-90 transition-all"
            type="button"
          >
            <span className="material-symbols-outlined text-[30px]">
              skip_previous
            </span>
          </button>

          <button
            aria-label={isPlaying ? 'Pause' : 'Play'}
            onClick={onTogglePlay}
            className="w-16 h-16 rounded-full bg-gradient-to-tr from-primary-container to-primary flex items-center justify-center text-[#0e0e10] shadow-[0_10px_30px_-5px_rgba(6,182,212,0.55),0_0_15px_2px_rgba(6,182,212,0.3)] active:scale-95 transition-transform hover:brightness-110"
            type="button"
          >
            <span
              className="material-symbols-outlined text-[34px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              {isPlaying ? 'pause' : 'play_arrow'}
            </span>
          </button>

          <button
            aria-label="Next Track"
            onClick={onNext}
            className="w-12 h-12 rounded-full flex items-center justify-center text-[#e4e4e7] hover:bg-[#16161a] active:scale-90 transition-all"
            type="button"
          >
            <span className="material-symbols-outlined text-[30px]">
              skip_next
            </span>
          </button>

          <button
            aria-label="Repeat Mode"
            onClick={onCycleRepeat}
            className={`w-12 h-12 rounded-full flex flex-col items-center justify-center hover:bg-[#16161a] active:scale-90 transition-all ${
              repeatMode !== 'off' ? 'text-primary' : 'text-[#9ca3af]'
            }`}
            type="button"
          >
            <span className="material-symbols-outlined text-[22px]">
              {repeatMode === 'one' ? 'repeat_one' : 'repeat'}
            </span>
            <span
              className={`w-1.5 h-1.5 rounded-full mt-0.5 ${
                repeatMode !== 'off' ? 'bg-primary' : 'bg-transparent'
              }`}
            ></span>
          </button>
        </div>
      </section>

      {/* Hardware Output Routing & Footer */}
      <footer className="relative z-10 px-6 pb-3 flex flex-col items-center">
        <button
          aria-label="Audio Output Route"
          onClick={onOpenDacSheet}
          className="w-full h-11 px-4 rounded-xl bg-[#1e1e24]/90 border border-[#23232c]/80 flex items-center justify-between text-xs text-[#9ca3af] hover:border-primary/40 active:scale-[0.99] transition-all shadow-md"
          type="button"
        >
          <div className="flex items-center space-x-2.5 truncate">
            <span
              className={`w-2.5 h-2.5 rounded-full animate-pulse ${
                dolbyAtmosEnabled
                  ? 'bg-secondary shadow-[0_0_8px_#f59e0b]'
                  : 'bg-tertiary shadow-[0_0_8px_#10b981]'
              }`}
            ></span>
            <span className="text-[#e4e4e7] font-medium truncate">
              Playing to:{' '}
              <span className="text-white font-semibold">
                {activeEndpointType === 'usb-dac'
                  ? activeDac.modelName.replace(' USB DAC', '')
                  : 'Phone Speaker / BT'}
              </span>
            </span>
            <span
              className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-bold tracking-tight ${
                dolbyAtmosEnabled
                  ? 'bg-secondary/20 text-secondary'
                  : isBitPerfect
                  ? 'bg-primary/15 text-primary'
                  : 'bg-white/10 text-[#9ca3af]'
              }`}
            >
              {dolbyAtmosEnabled
                ? 'Dolby Atmos ✨'
                : isBitPerfect
                ? 'Bit-Perfect ✨'
                : '48kHz PCM'}
            </span>
          </div>
          <span className="material-symbols-outlined text-[18px] text-[#9ca3af] shrink-0">
            chevron_right
          </span>
        </button>

        <div className="w-20 h-1 bg-white/30 rounded-full mt-3 mb-0.5"></div>
      </footer>
    </div>
  );
};

interface LyricsModalProps {
  track: Track;
  isPlaying: boolean;
  currentTimeSec: number;
  onClose: () => void;
  onTogglePlay: () => void;
  onSeek: (sec: number) => void;
  onPrev: () => void;
  onNext: () => void;
  onToggleLike: (id: string) => void;
  onShowToast: (msg: string) => void;
}

export const SynchronizedLyricsModal: React.FC<LyricsModalProps> = ({
  track,
  isPlaying,
  currentTimeSec,
  onClose,
  onTogglePlay,
  onSeek,
  onPrev,
  onNext,
  onToggleLike,
  onShowToast,
}) => {
  const [syncOffsetMs, setSyncOffsetMs] = useState(0);
  const [largeFont, setLargeFont] = useState(false);
  const [vocalFocusMode, setVocalFocusMode] = useState(true);

  const progressPct = Math.min(
    100,
    Math.max(0, (currentTimeSec / Math.max(1, track.durationSec)) * 100)
  );
  const remainingSec = Math.max(0, track.durationSec - currentTimeSec);

  // Determine active lyric line based on currentTimeSec
  let activeIdx = 0;
  for (let i = 0; i < track.lyrics.length; i++) {
    if (currentTimeSec >= track.lyrics[i].timeSec) {
      activeIdx = i;
    }
  }

  return (
    <div className="absolute inset-0 z-50 bg-surface text-on-surface flex flex-col overflow-hidden select-none">
      {/* Header */}
      <header className="w-full z-40 bg-surface/90 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.4)] shrink-0">
        <div className="h-6 px-4 flex items-center justify-between text-on-surface-variant font-telemetry-sm">
          <span>9:41</span>
          <div className="flex items-center gap-1">
            <span className="material-symbols-outlined text-[14px]">
              signal_cellular_4_bar
            </span>
            <span className="font-telemetry-sm text-[10px]">5G</span>
            <span className="material-symbols-outlined text-[16px]">
              battery_full
            </span>
          </div>
        </div>
        <div className="h-14 px-4 flex items-center justify-between">
          <button
            onClick={onClose}
            className="w-11 h-11 flex items-center justify-center rounded-full text-on-surface-variant hover:text-on-surface transition-colors active:scale-95"
          >
            <span className="material-symbols-outlined text-[24px]">close</span>
          </button>
          <div className="flex flex-col items-center justify-center flex-1 px-2 text-center overflow-hidden">
            <span className="font-headline-sm text-on-surface truncate">
              Synchronized Lyrics
            </span>
            <span className="font-body-sm text-primary flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
              Live Synchronized{' '}
              {syncOffsetMs !== 0 &&
                `(${syncOffsetMs > 0 ? '+' : ''}${syncOffsetMs}ms)`}
            </span>
          </div>
          <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
            <span className="material-symbols-outlined text-on-primary text-[18px]">
              person
            </span>
          </div>
        </div>
      </header>

      {/* Scrollable Body */}
      <div className="flex-1 overflow-y-auto no-scrollbar relative pb-40">
        {/* Track Context Header */}
        <section className="relative z-10 px-4 pt-2 pb-2 flex flex-col gap-1.5">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-1.5 min-w-0">
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-surface-container-high text-primary font-telemetry-sm shadow-sm">
                <span
                  className="material-symbols-outlined text-[13px] text-tertiary"
                  style={{ fontVariationSettings: "'FILL' 1" }}
                >
                  auto_awesome
                </span>
                Hi-Res Direct
              </span>
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-surface-container text-on-surface-variant font-telemetry-sm truncate">
                <span className="w-1.5 h-1.5 rounded-full bg-tertiary"></span>
                LRC • 100% Match
              </span>
            </div>

            <div className="flex items-center gap-1 shrink-0">
              <button
                onClick={() => {
                  const next =
                    syncOffsetMs === 0 ? 150 : syncOffsetMs === 150 ? -150 : 0;
                  setSyncOffsetMs(next);
                  onShowToast(`LRC Offset: ${next > 0 ? '+' : ''}${next}ms`);
                }}
                title="Adjust LRC timing offset"
                className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-on-surface-variant hover:text-primary active:scale-90 transition-transform"
              >
                <span className="material-symbols-outlined text-[16px]">
                  tune
                </span>
              </button>
              <button
                onClick={() => setLargeFont((f) => !f)}
                title="Toggle lyric font size"
                className={`w-8 h-8 rounded-full flex items-center justify-center active:scale-90 transition-transform ${
                  largeFont
                    ? 'bg-primary/20 text-primary'
                    : 'bg-surface-container text-on-surface-variant'
                }`}
              >
                <span className="material-symbols-outlined text-[16px]">
                  format_size
                </span>
              </button>
            </div>
          </div>

          <div className="flex items-center gap-2 mt-1 p-2 rounded-2xl bg-surface-container-low shadow-sm">
            <div className="w-12 h-12 rounded-xl bg-surface-variant overflow-hidden shrink-0 shadow-md relative">
              <img
                src={track.artworkUrl}
                alt={track.title}
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
            </div>
            <div className="flex flex-col min-w-0 flex-1">
              <div className="flex items-center gap-1.5">
                <span className="font-headline-sm text-on-surface truncate">
                  {track.title}
                </span>
                <span
                  className="material-symbols-outlined text-primary text-[15px] shrink-0"
                  style={{ fontVariationSettings: "'FILL' 1" }}
                >
                  verified
                </span>
              </div>
              <span className="font-body-sm text-on-surface-variant truncate">
                {track.artist} • {track.album} ({track.formatBadge})
              </span>
            </div>
            <button
              onClick={() => onToggleLike(track.id)}
              className="w-9 h-9 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant hover:text-secondary active:scale-90 transition-transform"
            >
              <span
                className={`material-symbols-outlined text-[18px] ${
                  track.liked ? 'text-secondary' : ''
                }`}
                style={{
                  fontVariationSettings: track.liked ? "'FILL' 1" : "'FILL' 0",
                }}
              >
                favorite
              </span>
            </button>
          </div>
        </section>

        {/* Live Lyrics Synchronized Visual Stream */}
        <section className="relative z-10 px-4 flex flex-col gap-5 pt-4 pb-8">
          {track.lyrics.map((line, index) => {
            const isActive = index === activeIdx;
            const isPast = index < activeIdx;

            if (isActive) {
              return (
                <div
                  key={index}
                  onClick={() => {
                    onSeek(line.timeSec);
                    onShowToast(`Jumped to [${line.timeLabel}]`);
                  }}
                  className="group relative flex items-start gap-2 cursor-pointer transition-all duration-300 -mx-4 px-4 py-3 bg-surface-container/60 rounded-2xl shadow-lg"
                >
                  <div className="flex flex-col items-center justify-center shrink-0 w-12 pt-1 gap-1">
                    <span className="font-telemetry-sm text-primary font-semibold">
                      {line.timeLabel}
                    </span>
                    <div className="flex items-center gap-0.5 h-3">
                      <span className="w-1 h-3 rounded-full bg-primary animate-pulse"></span>
                      <span className="w-1 h-2 rounded-full bg-primary/70 animate-bounce"></span>
                      <span className="w-1 h-3.5 rounded-full bg-primary animate-pulse"></span>
                    </div>
                  </div>
                  <div className="flex-1 flex flex-col gap-1">
                    <p
                      className={`${
                        largeFont ? 'text-[24px]' : 'font-headline-md'
                      } text-primary font-extrabold leading-snug tracking-tight drop-shadow-[0_0_18px_rgba(76,215,246,0.55)]`}
                    >
                      {line.text}
                    </p>
                    {vocalFocusMode && (
                      <div className="flex items-center gap-2 mt-1">
                        <span className="inline-flex items-center gap-1 font-telemetry-sm text-primary/80 bg-primary/10 px-2 py-0.5 rounded-full">
                          <span
                            className="material-symbols-outlined text-[11px]"
                            style={{ fontVariationSettings: "'FILL' 1" }}
                          >
                            graphic_eq
                          </span>
                          {line.singer || 'Lead Vocals'}
                        </span>
                        {line.vocalNote && (
                          <span className="font-telemetry-sm text-on-surface-variant/60">
                            • {line.vocalNote}
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                  <div className="self-center shrink-0 w-8 h-8 rounded-full bg-primary text-on-primary flex items-center justify-center shadow-md active:scale-95 transition-transform">
                    <span
                      className="material-symbols-outlined text-[18px]"
                      style={{ fontVariationSettings: "'FILL' 1" }}
                    >
                      play_arrow
                    </span>
                  </div>
                </div>
              );
            }

            return (
              <div
                key={index}
                onClick={() => {
                  onSeek(line.timeSec);
                  onShowToast(`Jumped to [${line.timeLabel}]`);
                }}
                className={`group flex items-start gap-2 cursor-pointer transition-all duration-300 ${
                  isPast
                    ? 'opacity-45 hover:opacity-80'
                    : 'opacity-70 hover:opacity-100'
                }`}
              >
                <span className="font-telemetry-sm text-on-surface-variant/70 mt-1 w-12 shrink-0 group-hover:text-primary transition-colors">
                  {line.timeLabel}
                </span>
                <p
                  className={`${
                    largeFont ? 'text-[20px]' : 'font-headline-sm'
                  } text-on-surface font-medium leading-relaxed tracking-tight flex-1 ${
                    line.isInstrumental ? 'italic text-on-surface-variant' : ''
                  }`}
                >
                  {line.text}
                </p>
              </div>
            );
          })}
        </section>
      </div>

      {/* Floating Frosted Mini Control Deck */}
      <aside className="absolute bottom-5 left-4 right-4 z-40 flex flex-col gap-2 p-3.5 rounded-2xl bg-surface-container/95 backdrop-blur-xl shadow-2xl border border-outline-variant/30">
        <div className="flex flex-col gap-1.5 w-full">
          <div
            className="relative w-full h-2 rounded-full bg-surface-variant cursor-pointer group flex items-center"
            onClick={(e) => {
              const rect = e.currentTarget.getBoundingClientRect();
              const pct = Math.min(
                1,
                Math.max(0, (e.clientX - rect.left) / rect.width)
              );
              onSeek(Math.round(pct * track.durationSec));
            }}
          >
            <div
              className="h-full rounded-full bg-primary transition-all"
              style={{ width: `${progressPct}%` }}
            ></div>
            <div
              className="absolute w-3.5 h-3.5 rounded-full bg-on-surface shadow-[0_0_10px_rgba(76,215,246,0.9)] -translate-x-1/2"
              style={{ left: `${progressPct}%` }}
            ></div>
          </div>
          <div className="flex items-center justify-between font-telemetry-sm">
            <span className="text-primary font-semibold">
              {formatTime(currentTimeSec)}
            </span>
            <span className="text-on-surface-variant/70 uppercase tracking-widest text-[9px]">
              Master Stream
            </span>
            <span className="text-on-surface-variant">
              -{formatTime(remainingSec)}
            </span>
          </div>
        </div>

        <div className="flex items-center justify-between pt-1">
          <button
            onClick={() => {
              onSeek(Math.max(0, currentTimeSec - 10));
              onShowToast('Replayed -10s');
            }}
            className="w-10 h-10 rounded-full flex items-center justify-center text-on-surface hover:text-primary active:scale-90 transition-all"
          >
            <span className="material-symbols-outlined text-[20px]">
              replay_10
            </span>
          </button>

          <div className="flex items-center gap-3">
            <button
              onClick={onPrev}
              className="w-9 h-9 rounded-full flex items-center justify-center text-on-surface hover:text-primary active:scale-90 transition-all"
            >
              <span className="material-symbols-outlined text-[22px]">
                skip_previous
              </span>
            </button>
            <button
              onClick={onTogglePlay}
              className="w-12 h-12 rounded-full bg-primary text-on-primary flex items-center justify-center shadow-[0_0_16px_rgba(76,215,246,0.45)] active:scale-95 transition-all"
            >
              <span
                className="material-symbols-outlined text-[26px]"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                {isPlaying ? 'pause' : 'play_arrow'}
              </span>
            </button>
            <button
              onClick={onNext}
              className="w-9 h-9 rounded-full flex items-center justify-center text-on-surface hover:text-primary active:scale-90 transition-all"
            >
              <span className="material-symbols-outlined text-[22px]">
                skip_next
              </span>
            </button>
          </div>

          <button
            onClick={() => {
              onSeek(Math.min(track.durationSec, currentTimeSec + 10));
              onShowToast('Advanced +10s');
            }}
            className="w-10 h-10 rounded-full flex items-center justify-center text-on-surface hover:text-primary active:scale-90 transition-all"
          >
            <span className="material-symbols-outlined text-[20px]">
              forward_10
            </span>
          </button>

          <button
            onClick={() => {
              setVocalFocusMode((v) => !v);
              onShowToast(
                !vocalFocusMode
                  ? 'Vocal Pitch Telemetry Enabled'
                  : 'Vocal Pitch Telemetry Hidden'
              );
            }}
            className={`w-10 h-10 rounded-full flex items-center justify-center active:scale-90 transition-all ${
              vocalFocusMode
                ? 'bg-surface-container-high text-primary'
                : 'bg-surface-container text-on-surface-variant'
            }`}
          >
            <span className="material-symbols-outlined text-[20px]">mic</span>
          </button>
        </div>
      </aside>
    </div>
  );
};

interface DacModalProps {
  availableDacs: UsbDacEndpoint[];
  activeDac: UsbDacEndpoint;
  activeEndpointType: 'usb-dac' | 'phone-speaker';
  exclusiveUsbAccess: boolean;
  autoSwapOnPlug: boolean;
  dolbyAtmosEnabled: boolean;
  dolbyAtmosProfile: 'auto' | 'music' | 'movie' | 'voice';
  onSelectDacModel: (dacId: string) => void;
  onAddCustomDac: (modelName: string, specs: string) => void;
  onProbeHardware: () => void;
  onSelectEndpointType: (type: 'usb-dac' | 'phone-speaker') => void;
  onToggleExclusiveUsb: () => void;
  onToggleAutoSwap: () => void;
  onToggleDolbyAtmos: () => void;
  onSelectAtmosProfile: (profile: 'auto' | 'music' | 'movie' | 'voice') => void;
  onSimulateUnplug: () => void;
  onClose: () => void;
}

export const DacOutputModal: React.FC<DacModalProps> = ({
  availableDacs,
  activeDac,
  activeEndpointType,
  exclusiveUsbAccess,
  autoSwapOnPlug,
  dolbyAtmosEnabled,
  dolbyAtmosProfile,
  onSelectDacModel,
  onAddCustomDac,
  onProbeHardware,
  onSelectEndpointType,
  onToggleExclusiveUsb,
  onToggleAutoSwap,
  onToggleDolbyAtmos,
  onSelectAtmosProfile,
  onSimulateUnplug,
  onClose,
}) => {
  const [showModelSelector, setShowModelSelector] = useState(false);
  const [customModelInput, setCustomModelInput] = useState('');
  const isBitPerfect =
    activeEndpointType === 'usb-dac' && exclusiveUsbAccess && !dolbyAtmosEnabled;

  return (
    <div className="absolute inset-0 z-50 flex flex-col justify-end bg-black/70 backdrop-blur-[2px] select-none">
      <section className="relative z-20 w-full bg-[#1e1e24] rounded-t-[28px] border-t border-x border-[#2a2a35] shadow-[0_-12px_40px_rgba(0,0,0,0.85)] flex flex-col max-h-[870px]">
        {/* Drag Handle */}
        <div
          onClick={onClose}
          className="w-full flex items-center justify-center pt-3 pb-2 cursor-pointer"
        >
          <div className="w-10 h-1 bg-[#4a4a58] rounded-full"></div>
        </div>

        {/* Sheet Header */}
        <div className="px-5 pt-1 pb-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold tracking-tight text-[#e4e4e7] flex items-center gap-2">
              Audio Output &amp; DAC <span>🎧</span>
            </h2>
            {dolbyAtmosEnabled ? (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-secondary/15 border border-secondary/40 text-[11px] font-mono font-medium text-secondary">
                <span className="w-1.5 h-1.5 rounded-full bg-secondary animate-pulse"></span>
                DOLBY ATMOS 3D
              </span>
            ) : isBitPerfect ? (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-tertiary/10 border border-tertiary/30 text-[11px] font-mono font-medium text-tertiary">
                <span className="w-1.5 h-1.5 rounded-full bg-tertiary animate-pulse"></span>
                BIT-PERFECT
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/10 text-[11px] font-mono font-medium text-[#9ca3af]">
                OS MIXER 48kHz
              </span>
            )}
          </div>
          <p className="text-[12px] text-[#9ca3af] mt-1 leading-snug">
            Dynamic USB-C OTG &amp; hardware endpoint recognition for any DAC model.
          </p>
        </div>

        <div className="h-px bg-[#2a2a35]/80 mx-5"></div>

        {/* Scrollable Content */}
        <div className="px-5 py-3.5 space-y-3.5 overflow-y-auto no-scrollbar max-h-[580px]">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-mono tracking-wider text-[#9ca3af] uppercase font-medium">
              Detected Audio Endpoints
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onProbeHardware}
                className="text-[11px] font-mono text-primary hover:underline flex items-center gap-1"
              >
                <span className="material-symbols-outlined text-[13px]">
                  usb
                </span>
                Scan USB-C
              </button>
              <button
                type="button"
                onClick={() => setShowModelSelector((s) => !s)}
                className="text-[11px] font-mono px-2 py-0.5 rounded bg-[#25252e] border border-[#363645] text-[#e4e4e7] hover:border-primary"
              >
                {showModelSelector ? 'Hide Models' : 'Change DAC Model'}
              </button>
            </div>
          </div>

          {/* Dynamic USB DAC Model Picker Drawer */}
          {showModelSelector && (
            <div className="p-3 rounded-2xl bg-[#16161b] border border-[#2a2a35] space-y-2">
              <span className="text-[10px] font-mono uppercase tracking-wider text-primary block">
                Dynamic USB-C DAC Hardware Profiles ({availableDacs.length} Models)
              </span>
              <div className="grid grid-cols-1 gap-1.5 max-h-40 overflow-y-auto no-scrollbar">
                {availableDacs.map((dac) => (
                  <button
                    key={dac.id}
                    type="button"
                    onClick={() => {
                      onSelectDacModel(dac.id);
                      onSelectEndpointType('usb-dac');
                    }}
                    className={`w-full px-3 py-2 rounded-xl text-left flex items-center justify-between text-xs transition-colors ${
                      activeDac.id === dac.id
                        ? 'bg-primary/15 border border-primary/50 text-[#e4e4e7]'
                        : 'bg-[#1e1e24] text-[#9ca3af] hover:text-white'
                    }`}
                  >
                    <div className="truncate pr-2">
                      <div className="font-semibold text-[#e4e4e7] truncate">
                        {dac.modelName}
                      </div>
                      <div className="text-[10px] font-mono text-[#9ca3af] truncate">
                        {dac.chipset} • {dac.maxBitDepth}/{dac.maxSampleRate}
                      </div>
                    </div>
                    {activeDac.id === dac.id && (
                      <span className="text-[10px] font-mono text-primary font-bold shrink-0">
                        PLUGGED
                      </span>
                    )}
                  </button>
                ))}
              </div>
              {/* Custom DAC Model Hot-Probe Input */}
              <div className="flex items-center gap-1.5 pt-1">
                <input
                  type="text"
                  value={customModelInput}
                  onChange={(e) => setCustomModelInput(e.target.value)}
                  placeholder="Plug custom USB DAC model (e.g. DragonFly Cobalt)..."
                  className="flex-1 h-8 px-2.5 rounded-lg bg-[#121214] border border-[#2a2a35] text-xs text-white placeholder:text-[#9ca3af]/50 focus:outline-none focus:border-primary"
                />
                <button
                  type="button"
                  onClick={() => {
                    if (customModelInput.trim()) {
                      onAddCustomDac(customModelInput.trim(), '32-bit/384kHz');
                      setCustomModelInput('');
                    }
                  }}
                  className="h-8 px-3 rounded-lg bg-primary text-[#121214] text-xs font-bold shrink-0"
                >
                  Mount
                </button>
              </div>
            </div>
          )}

          {/* Device Cards List */}
          <div className="space-y-2.5">
            {/* Card 1: Active Dynamic USB DAC */}
            <div
              onClick={() => onSelectEndpointType('usb-dac')}
              className={`relative group p-3.5 rounded-2xl flex items-start justify-between cursor-pointer transition-all duration-200 ${
                activeEndpointType === 'usb-dac'
                  ? 'bg-[#25252e] border-2 border-primary/70 shadow-[0_0_20px_rgba(6,182,212,0.15)]'
                  : 'bg-[#16161b] border border-[#2a2a35] hover:border-[#363645]'
              }`}
            >
              <div className="flex items-start space-x-3 min-w-0">
                <div className="w-11 h-11 rounded-xl bg-primary/15 border border-primary/30 flex items-center justify-center shrink-0 mt-0.5">
                  <span className="text-xl">🎧</span>
                </div>
                <div className="flex flex-col min-w-0">
                  <div className="flex items-center space-x-2 flex-wrap">
                    <span className="text-[14px] font-bold text-[#e4e4e7] tracking-tight truncate">
                      {activeDac.modelName}
                    </span>
                    {activeEndpointType === 'usb-dac' && (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-primary/20 text-primary border border-primary/40">
                        ACTIVE
                      </span>
                    )}
                  </div>
                  <p className="text-[11px] font-mono text-primary/90 mt-1 flex items-center gap-1.5 flex-wrap">
                    <span>{activeDac.interfaceType}</span>
                    <span className="text-[#9ca3af]">•</span>
                    <span>
                      {activeDac.maxBitDepth} / {activeDac.maxSampleRate}
                    </span>
                    <span className="text-[#9ca3af]">•</span>
                    <span
                      className={
                        dolbyAtmosEnabled
                          ? 'text-secondary font-semibold'
                          : 'text-tertiary font-semibold'
                      }
                    >
                      {dolbyAtmosEnabled ? 'Atmos Spatial' : 'Bit-Perfect'}
                    </span>
                  </p>
                  <div className="flex items-center gap-2 mt-2 pt-2 border-t border-[#2a2a35]/50 text-[10px] font-mono text-[#9ca3af]">
                    <span>
                      Hardware Vol:{' '}
                      <b className="text-[#e4e4e7]">
                        {activeDac.defaultHardwareVol}%
                      </b>
                    </span>
                    <span>•</span>
                    <span className="truncate">
                      Clock:{' '}
                      <b className="text-[#e4e4e7]">
                        {activeDac.clockArchitecture}
                      </b>
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex flex-col items-end justify-between h-full space-y-3 shrink-0 ml-2">
                {activeEndpointType === 'usb-dac' ? (
                  <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center shadow-[0_0_8px_rgba(6,182,212,0.8)]">
                    <span className="material-symbols-outlined text-[#121214] text-[15px] font-bold">
                      check
                    </span>
                  </div>
                ) : (
                  <div className="w-6 h-6 rounded-full border-2 border-[#2a2a35]"></div>
                )}
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-tertiary/15 border border-tertiary/40 text-[10px] font-medium text-tertiary">
                  <span className="w-1.5 h-1.5 rounded-full bg-tertiary"></span>
                  Connected
                </span>
              </div>
            </div>

            {/* Card 2: Phone Speaker / Bluetooth */}
            <div
              onClick={() => onSelectEndpointType('phone-speaker')}
              className={`relative group p-3.5 rounded-2xl flex items-start justify-between cursor-pointer transition-all duration-200 ${
                activeEndpointType === 'phone-speaker'
                  ? 'bg-[#25252e] border-2 border-primary/70 shadow-[0_0_20px_rgba(6,182,212,0.15)]'
                  : 'bg-[#16161b] border border-[#2a2a35] hover:border-[#363645]'
              }`}
            >
              <div className="flex items-start space-x-3">
                <div className="w-11 h-11 rounded-xl bg-[#1e1e24] border border-[#2a2a35] flex items-center justify-center shrink-0 mt-0.5">
                  <span className="text-xl opacity-80">📱</span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-[#e4e4e7]">
                    Phone Speaker / Bluetooth
                  </span>
                  <p className="text-[11px] font-mono text-[#9ca3af] mt-1">
                    Android AudioTrack / AAudio Native
                  </p>
                  <div className="flex items-center gap-2 mt-2 pt-2 border-t border-[#2a2a35]/30 text-[10px] font-mono text-[#9ca3af]/80">
                    <span>
                      {dolbyAtmosEnabled
                        ? 'Dolby Atmos Renderer (Samsung One UI)'
                        : 'Resampled: 48kHz System Mixer'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex flex-col items-end justify-start shrink-0 pt-0.5">
                {activeEndpointType === 'phone-speaker' ? (
                  <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center">
                    <span className="material-symbols-outlined text-[#121214] text-[15px] font-bold">
                      check
                    </span>
                  </div>
                ) : (
                  <div className="w-6 h-6 rounded-full border-2 border-[#2a2a35]"></div>
                )}
                <span className="text-[10px] text-[#9ca3af]/70 mt-3 font-mono">
                  {activeEndpointType === 'phone-speaker' ? 'Active' : 'Standby'}
                </span>
              </div>
            </div>
          </div>

          {/* Audiophile Routing Controls + Samsung Dolby Atmos */}
          <div className="pt-1">
            <span className="text-[11px] font-mono tracking-wider text-[#9ca3af] uppercase font-medium block mb-2">
              Audiophile Routing &amp; Spatial Controls
            </span>

            <div className="bg-[#16161b] rounded-2xl border border-[#2a2a35] divide-y divide-[#2a2a35]/60">
              {/* Toggle 1: Exclusive USB Audio Access */}
              <div className="p-3.5 flex items-center justify-between gap-3">
                <div className="flex items-start space-x-3">
                  <div className="w-8 h-8 rounded-lg bg-[#1e1e24] border border-[#2a2a35] flex items-center justify-center shrink-0 mt-0.5 text-primary">
                    <span className="material-symbols-outlined text-[18px]">
                      lock
                    </span>
                  </div>
                  <div>
                    <p className="text-[13px] font-semibold text-[#e4e4e7] tracking-tight">
                      Exclusive USB Audio Access
                    </p>
                    <p className="text-[11px] text-[#9ca3af] mt-0.5 leading-snug">
                      Bypasses Android mixer &amp; disables Dolby Atmos for pure bit-perfect playback.
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={onToggleExclusiveUsb}
                  className={`relative w-12 h-7 rounded-full transition-colors shrink-0 ${
                    exclusiveUsbAccess && !dolbyAtmosEnabled
                      ? 'bg-primary'
                      : 'bg-[#2a2a35]'
                  }`}
                >
                  <span
                    className={`absolute left-1 top-1 w-5 h-5 rounded-full bg-[#121214] transition-transform ${
                      exclusiveUsbAccess && !dolbyAtmosEnabled
                        ? 'translate-x-5'
                        : 'translate-x-0'
                    }`}
                  ></span>
                </button>
              </div>

              {/* Toggle 2: Dolby Atmos Support (Samsung & Compatible Devices) */}
              <div className="p-3.5 flex flex-col gap-2.5">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-start space-x-3">
                    <div className="w-8 h-8 rounded-lg bg-[#1e1e24] border border-[#2a2a35] flex items-center justify-center shrink-0 mt-0.5 text-secondary">
                      <span className="material-symbols-outlined text-[18px]">
                        surround_sound
                      </span>
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <p className="text-[13px] font-semibold text-[#e4e4e7] tracking-tight">
                          Dolby Atmos Spatial Audio
                        </p>
                        <span className="px-1.5 py-0.2 rounded text-[9px] font-mono bg-secondary/20 text-secondary">
                          SAMSUNG / ANDROID
                        </span>
                      </div>
                      <p className="text-[11px] text-[#9ca3af] mt-0.5 leading-snug">
                        3D object spatializer. Automatically cancels Bit-Perfect mode when active.
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={onToggleDolbyAtmos}
                    className={`relative w-12 h-7 rounded-full transition-colors shrink-0 ${
                      dolbyAtmosEnabled ? 'bg-secondary' : 'bg-[#2a2a35]'
                    }`}
                  >
                    <span
                      className={`absolute left-1 top-1 w-5 h-5 rounded-full bg-[#121214] transition-transform ${
                        dolbyAtmosEnabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    ></span>
                  </button>
                </div>

                {dolbyAtmosEnabled && (
                  <div className="pl-11 flex items-center gap-1.5 flex-wrap">
                    {(['auto', 'music', 'movie', 'voice'] as const).map((mode) => (
                      <button
                        key={mode}
                        type="button"
                        onClick={() => onSelectAtmosProfile(mode)}
                        className={`px-2.5 py-1 rounded-full text-[10px] font-mono uppercase transition-colors ${
                          dolbyAtmosProfile === mode
                            ? 'bg-secondary text-[#121214] font-bold'
                            : 'bg-[#1e1e24] text-[#9ca3af]'
                        }`}
                      >
                        {mode}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Toggle 3: Dynamic Auto-Swap on Plug */}
              <div className="p-3.5 flex items-center justify-between gap-3">
                <div className="flex items-start space-x-3">
                  <div className="w-8 h-8 rounded-lg bg-[#1e1e24] border border-[#2a2a35] flex items-center justify-center shrink-0 mt-0.5 text-tertiary">
                    <span className="material-symbols-outlined text-[18px]">
                      bolt
                    </span>
                  </div>
                  <div>
                    <p className="text-[13px] font-semibold text-[#e4e4e7] tracking-tight">
                      Dynamic Auto-Swap on Plug
                    </p>
                    <p className="text-[11px] text-[#9ca3af] mt-0.5 leading-snug">
                      Instantly switches routing without playback stutter when USB DAC is inserted.
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={onToggleAutoSwap}
                  className={`relative w-12 h-7 rounded-full transition-colors shrink-0 ${
                    autoSwapOnPlug ? 'bg-primary' : 'bg-[#2a2a35]'
                  }`}
                >
                  <span
                    className={`absolute left-1 top-1 w-5 h-5 rounded-full bg-[#121214] transition-transform ${
                      autoSwapOnPlug ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  ></span>
                </button>
              </div>
            </div>
          </div>

          {/* DAC Real-Time Diagnostics Bar + Unplug Guard Trigger */}
          <div className="p-3 rounded-xl bg-[#16161b]/80 border border-[#2a2a35]/80 flex items-center justify-between text-xs">
            <div className="flex items-center space-x-2">
              <span className="w-2 h-2 rounded-full bg-tertiary"></span>
              <span className="text-[#9ca3af] font-mono text-[11px]">
                {dolbyAtmosEnabled
                  ? 'Stream: Dolby Atmos Binaural 3D'
                  : 'Stream: Direct PCM 2.0'}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-primary text-[11px] font-semibold">
                ~{activeDac.latencyMs} ms
              </span>
              <button
                type="button"
                onClick={onSimulateUnplug}
                className="px-2 py-0.5 rounded bg-error-container/60 text-error text-[10px] font-mono hover:bg-error-container"
                title="Test USB DAC Unplug Ear-Safety Guard"
              >
                Test Unplug
              </button>
            </div>
          </div>
        </div>

        {/* Footer Done Button */}
        <div className="p-5 pt-3 pb-6 bg-[#1e1e24] border-t border-[#2a2a35] flex flex-col space-y-2">
          <button
            type="button"
            onClick={onClose}
            className="w-full py-3 px-6 rounded-full bg-primary text-[#121214] font-bold text-[15px] tracking-wide shadow-[0_4px_20px_rgba(6,182,212,0.35)] active:scale-[0.98] transition-transform flex items-center justify-center"
          >
            Done
          </button>
        </div>
      </section>
    </div>
  );
};

interface DacDisconnectedProps {
  track: Track;
  unpluggedDac: UsbDacEndpoint;
  currentTimeSec: number;
  onSwitchToSpeakers: () => void;
  onReconnectDac: () => void;
}

export const DacDisconnectedModal: React.FC<DacDisconnectedProps> = ({
  track,
  unpluggedDac,
  currentTimeSec,
  onSwitchToSpeakers,
  onReconnectDac,
}) => {
  const [bannerDismissed, setBannerDismissed] = useState(false);

  return (
    <div className="absolute inset-0 z-50 bg-surface-container-lowest/85 backdrop-blur-md flex flex-col justify-between select-none">
      {/* Top Floating Android Heads-Up Notification Banner */}
      <div
        className={`w-full px-4 pt-14 transition-all duration-300 ${
          bannerDismissed ? '-translate-y-28 opacity-0 pointer-events-none' : ''
        }`}
      >
        <div className="w-full rounded-full bg-surface-container-high shadow-2xl px-4 py-2.5 flex items-center justify-between gap-2.5 border border-error/25">
          <div className="relative flex items-center justify-center shrink-0">
            <div className="absolute w-8 h-8 rounded-full bg-error/20 animate-ping"></div>
            <div className="w-8 h-8 rounded-full bg-error/30 text-error flex items-center justify-center relative z-10">
              <span className="material-symbols-outlined text-[18px]">
                usb_off
              </span>
            </div>
          </div>

          <div className="flex-1 min-w-0 pr-1">
            <div className="flex items-center gap-1">
              <span className="font-telemetry-sm text-error font-semibold uppercase tracking-wider">
                DAC Disconnected
              </span>
              <span className="w-1 h-1 rounded-full bg-error"></span>
              <span className="font-telemetry-sm text-on-surface-variant">
                Now
              </span>
            </div>
            <p className="font-body-sm text-on-surface truncate font-medium">
              {unpluggedDac.modelName.replace(' USB DAC', '')} unplugged. Playback paused.
            </p>
          </div>

          <button
            aria-label="Dismiss banner"
            onClick={() => setBannerDismissed(true)}
            className="w-7 h-7 rounded-full bg-surface-container-highest text-on-surface-variant hover:text-on-surface flex items-center justify-center shrink-0"
          >
            <span className="material-symbols-outlined text-[16px]">close</span>
          </button>
        </div>
      </div>

      {/* Bottom Sheet Dialog: Audiophile Safety Interruption */}
      <div className="w-full bg-surface-container-high rounded-t-[28px] shadow-2xl flex flex-col px-6 pt-4 pb-8 mt-auto border-t border-outline-variant/30">
        <div className="w-12 h-1 bg-surface-variant rounded-full mx-auto mb-4"></div>

        <div className="flex items-center gap-1.5 self-start mb-2 px-2.5 py-0.5 rounded-full bg-error-container text-on-error-container">
          <span className="material-symbols-outlined text-[14px]">shield</span>
          <span className="font-telemetry-sm font-semibold tracking-wide uppercase">
            Safety Fallback Triggered • Bit-Perfect Guard
          </span>
        </div>

        <h1 className="font-headline-md text-on-surface tracking-tight flex items-center gap-1.5">
          <span>Audio Output Changed</span>
          <span className="text-secondary">🔊</span>
        </h1>
        <p className="font-body-md text-on-surface-variant mt-1 leading-relaxed">
          Your USB DAC ({unpluggedDac.modelName}) was abruptly disconnected. Audio stream was halted instantly to protect ears &amp; prevent sudden speaker blast.
        </p>

        {/* Technical Stream Telemetry Card */}
        <div className="w-full bg-surface-container-lowest rounded-2xl p-4 my-4 flex items-center justify-between shadow-inner">
          <div className="flex items-center gap-3 min-w-0 pr-2">
            <div className="w-11 h-11 rounded-xl bg-surface-container-high overflow-hidden shrink-0 relative">
              <img
                src={track.artworkUrl}
                alt={track.title}
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary text-[18px]">
                  graphic_eq
                </span>
              </div>
            </div>
            <div className="min-w-0">
              <p className="font-label-lg text-on-surface truncate font-semibold">
                {track.title}
              </p>
              <div className="flex items-center gap-1 mt-0.5">
                <span className="font-telemetry-sm text-primary font-medium">
                  {track.sampleRate} • {track.bitDepth}
                </span>
                <span className="font-telemetry-sm text-outline">•</span>
                <span className="font-telemetry-sm text-on-surface-variant">
                  Paused at {formatTime(currentTimeSec)}
                </span>
              </div>
            </div>
          </div>

          <div className="shrink-0 flex flex-col items-end gap-1">
            <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-surface-container text-tertiary">
              <span className="material-symbols-outlined text-[13px]">
                health_and_safety
              </span>
              <span className="font-telemetry-sm font-semibold">Ear Safety</span>
            </div>
            <span className="font-telemetry-sm text-outline">
              0 dB Spike Blocked
            </span>
          </div>
        </div>

        {/* Ergonomic Action Cluster */}
        <div className="w-full flex flex-col gap-2.5">
          <button
            type="button"
            onClick={onSwitchToSpeakers}
            className="w-full h-14 rounded-full bg-primary-container text-on-primary-container font-label-lg font-bold flex items-center justify-center gap-2 hover:opacity-95 active:scale-[0.98] transition shadow-lg shadow-primary-container/20"
          >
            <span className="material-symbols-outlined text-[20px]">
              speaker_phone
            </span>
            <span>Switch to Phone Speakers</span>
          </button>

          <button
            type="button"
            onClick={onReconnectDac}
            className="w-full h-14 rounded-full bg-surface-container text-on-surface font-label-lg font-medium flex items-center justify-center gap-2 hover:bg-surface-container-highest active:scale-[0.98] transition"
          >
            <span className="material-symbols-outlined text-[20px]">
              hourglass_top
            </span>
            <span>Keep Paused • Reconnect {unpluggedDac.modelName.replace(' USB DAC', '')}</span>
          </button>
        </div>

        <div className="mt-4 flex items-center justify-center gap-1.5 text-center">
          <span className="material-symbols-outlined text-outline text-[14px]">
            cable
          </span>
          <p className="font-telemetry-sm text-on-surface-variant">
            Auto-resumes bit-perfect stream once{' '}
            <span className="text-primary font-semibold">
              {unpluggedDac.modelName.replace(' USB DAC', '')}
            </span>{' '}
            is reattached
          </p>
        </div>
      </div>
    </div>
  );
};

interface TrackActionsSheetProps {
  track: Track;
  onClose: () => void;
  onPlayNext: (track: Track) => void;
  onStageToCooker: (track: Track) => void;
  onToggleLike: (id: string) => void;
  onFindSimilarTempo: (track: Track) => void;
  onRemoveTrack: (id: string) => void;
}

export const TrackActionsSheet: React.FC<TrackActionsSheetProps> = ({
  track,
  onClose,
  onPlayNext,
  onStageToCooker,
  onToggleLike,
  onFindSimilarTempo,
  onRemoveTrack,
}) => {
  const [showTechSpecs, setShowTechSpecs] = useState(false);
  const [showLinerNotes, setShowLinerNotes] = useState(false);
  const energyPct = Math.round(track.energy * 100);
  const dancePct = Math.round(track.danceability * 100);
  const bpmDash = Math.min(100, Math.round((track.bpm / 180) * 100));

  return (
    <div className="absolute inset-0 z-50 flex flex-col justify-end bg-black/75 backdrop-blur-[3px] select-none">
      <section className="relative z-20 w-full bg-[#1e1e24] border-t border-x border-[#2a2a35] rounded-t-[28px] shadow-2xl flex flex-col max-h-[855px] overflow-hidden">
        <div
          onClick={onClose}
          className="w-full pt-3 pb-2 flex justify-center items-center cursor-pointer"
        >
          <div className="w-10 h-1 rounded-full bg-[#4b5563]/80"></div>
        </div>

        <div className="overflow-y-auto no-scrollbar px-5 pb-4">
          {/* Header Row */}
          <header className="flex items-center justify-between pb-4 pt-1 border-b border-[#2a2a35]">
            <div className="flex items-center space-x-3.5 min-w-0">
              <div className="relative w-[52px] h-[52px] shrink-0 rounded-[10px] overflow-hidden border border-white/10 shadow-md">
                <img
                  alt={track.title}
                  src={track.artworkUrl}
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="flex flex-col min-w-0">
                <h1 className="text-[17px] font-bold text-[#e4e4e7] tracking-tight truncate leading-snug">
                  {track.title}
                </h1>
                <p className="text-[13px] text-[#9ca3af] truncate leading-tight mt-0.5">
                  {track.artist} • {track.album}
                </p>
                <div className="flex items-center gap-1.5 mt-1.5">
                  <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-mono font-medium text-primary bg-cyan-950/40 border border-primary/30 tracking-tight">
                    <span className="w-1.5 h-1.5 rounded-full bg-primary mr-1 inline-block animate-pulse"></span>
                    {track.formatBadge}
                  </span>
                  <span className="inline-flex items-center text-[10px] font-mono text-secondary/90 font-medium">
                    Hi-Res Direct ✨
                  </span>
                </div>
              </div>
            </div>

            <button
              aria-label="Dismiss sheet"
              onClick={onClose}
              className="w-8 h-8 rounded-full bg-[#272730] hover:bg-[#32323e] active:scale-95 text-zinc-400 hover:text-white flex items-center justify-center transition border border-[#353545] shrink-0 ml-2"
              type="button"
            >
              <span className="material-symbols-outlined text-[18px]">close</span>
            </button>
          </header>

          {/* Acoustic Features Section */}
          <section className="py-3.5 border-b border-[#2a2a35]">
            <div className="flex items-center justify-between px-0.5 mb-2.5">
              <span className="text-[11px] font-mono uppercase tracking-wider text-zinc-400 font-semibold flex items-center gap-1">
                <span className="material-symbols-outlined text-[15px] text-primary">
                  music_note
                </span>
                Acoustic Profile
              </span>
              <span className="text-[10px] font-mono text-primary/80 bg-cyan-950/30 px-2 py-0.5 rounded-full border border-cyan-800/40">
                Local Heuristic Engine
              </span>
            </div>

            <div className="grid grid-cols-3 gap-2 text-center">
              {/* Dial 1: Tempo */}
              <div className="bg-[#17171d] rounded-xl p-2.5 border border-[#262631] flex flex-col items-center justify-center">
                <div className="relative w-14 h-14 flex items-center justify-center mb-1">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
                    <path
                      className="text-zinc-800"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="3"
                    />
                    <path
                      className="text-primary"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                      fill="none"
                      stroke="currentColor"
                      strokeDasharray={`${bpmDash}, 100`}
                      strokeLinecap="round"
                      strokeWidth="3"
                    />
                  </svg>
                  <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <span className="text-[12px] font-bold font-mono text-white leading-none">
                      {track.bpm}
                    </span>
                    <span className="text-[8px] font-mono text-primary font-semibold uppercase mt-0.5 leading-none">
                      BPM
                    </span>
                  </div>
                </div>
                <span className="text-[11px] font-medium text-zinc-300">
                  Tempo ⚡
                </span>
                <span className="text-[9px] font-mono text-zinc-400">
                  {track.tempoLabel}
                </span>
              </div>

              {/* Dial 2: Energy */}
              <div className="bg-[#17171d] rounded-xl p-2.5 border border-[#262631] flex flex-col items-center justify-center">
                <div className="relative w-14 h-14 flex items-center justify-center mb-1">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
                    <path
                      className="text-zinc-800"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="3"
                    />
                    <path
                      className="text-primary"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                      fill="none"
                      stroke="currentColor"
                      strokeDasharray={`${energyPct}, 100`}
                      strokeLinecap="round"
                      strokeWidth="3"
                    />
                  </svg>
                  <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <span className="text-[13px] font-extrabold font-mono text-white leading-none">
                      {energyPct}%
                    </span>
                  </div>
                </div>
                <span className="text-[11px] font-medium text-zinc-300">
                  Energy 🔥
                </span>
                <span className="text-[9px] font-mono text-primary font-medium">
                  {track.energyLabel}
                </span>
              </div>

              {/* Dial 3: Danceability */}
              <div className="bg-[#17171d] rounded-xl p-2.5 border border-[#262631] flex flex-col items-center justify-center">
                <div className="relative w-14 h-14 flex items-center justify-center mb-1">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
                    <path
                      className="text-zinc-800"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="3"
                    />
                    <path
                      className="text-secondary"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                      fill="none"
                      stroke="currentColor"
                      strokeDasharray={`${dancePct}, 100`}
                      strokeLinecap="round"
                      strokeWidth="3"
                    />
                  </svg>
                  <div className="absolute inset-0 flex flex-col items-center justify-center">
                    <span className="text-[13px] font-extrabold font-mono text-white leading-none">
                      {dancePct}%
                    </span>
                  </div>
                </div>
                <span className="text-[11px] font-medium text-zinc-300">
                  Dance 💃
                </span>
                <span className="text-[9px] font-mono text-secondary font-medium">
                  {track.danceLabel}
                </span>
              </div>
            </div>
          </section>

          {/* Action List Items */}
          <nav className="py-2 space-y-1.5">
            <button
              type="button"
              onClick={() => {
                onPlayNext(track);
                onClose();
              }}
              className="w-full min-h-[52px] px-3.5 py-2 rounded-xl flex items-center justify-between text-left hover:bg-[#25252e] transition group"
            >
              <div className="flex items-center space-x-3.5">
                <div className="w-9 h-9 rounded-lg bg-cyan-950/60 border border-cyan-500/30 flex items-center justify-center text-primary shrink-0">
                  <span className="material-symbols-outlined text-[18px]">
                    play_arrow
                  </span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-white group-hover:text-primary">
                    Play Next in Queue
                  </span>
                  <span className="text-[11px] text-[#9ca3af]">
                    Append right after current track
                  </span>
                </div>
              </div>
              <span className="material-symbols-outlined text-[18px] text-zinc-500">
                chevron_right
              </span>
            </button>

            <button
              type="button"
              onClick={() => {
                onStageToCooker(track);
                onClose();
              }}
              className="w-full min-h-[52px] px-3.5 py-2 rounded-xl flex items-center justify-between text-left hover:bg-[#25252e] transition group"
            >
              <div className="flex items-center space-x-3.5">
                <div className="w-9 h-9 rounded-lg bg-amber-950/40 border border-secondary/30 flex items-center justify-center text-secondary shrink-0">
                  <span className="text-[16px]">🍳</span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-white group-hover:text-secondary">
                    Add to Vibe Cooker Staging
                  </span>
                  <span className="text-[11px] text-[#9ca3af]">
                    Send acoustic vectors ({track.camelotKey} • {track.bpm} BPM) into prompt generator
                  </span>
                </div>
              </div>
              <span className="px-1.5 py-0.5 rounded text-[9px] font-mono uppercase bg-amber-500/10 text-secondary border border-secondary/20 font-semibold">
                Stage
              </span>
            </button>

            <button
              type="button"
              onClick={() => onToggleLike(track.id)}
              className="w-full min-h-[52px] px-3.5 py-2 rounded-xl flex items-center justify-between text-left hover:bg-[#25252e] transition group"
            >
              <div className="flex items-center space-x-3.5">
                <div className="w-9 h-9 rounded-lg bg-pink-950/40 border border-pink-500/30 flex items-center justify-center text-pink-400 shrink-0">
                  <span
                    className="material-symbols-outlined text-[18px]"
                    style={{
                      fontVariationSettings: track.liked
                        ? "'FILL' 1"
                        : "'FILL' 0",
                    }}
                  >
                    favorite
                  </span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-white">
                    {track.liked ? 'Favorited in Starred Crate' : 'Add to Favorites'}
                  </span>
                  <span className="text-[11px] text-[#9ca3af]">
                    Saved to Starred Crate
                  </span>
                </div>
              </div>
              <span className="text-[11px] font-mono text-zinc-400">
                {track.liked ? 'Starred ★' : 'Tap to Star'}
              </span>
            </button>

            <button
              type="button"
              onClick={() => {
                onFindSimilarTempo(track);
                onClose();
              }}
              className="w-full min-h-[52px] px-3.5 py-2 rounded-xl flex items-center justify-between text-left hover:bg-[#25252e] transition group"
            >
              <div className="flex items-center space-x-3.5">
                <div className="w-9 h-9 rounded-lg bg-teal-950/40 border border-teal-500/30 flex items-center justify-center text-teal-400 shrink-0">
                  <span className="material-symbols-outlined text-[18px]">
                    search
                  </span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-white">
                    Find Tracks with Similar Tempo
                  </span>
                  <span className="text-[11px] text-[#9ca3af]">
                    Harmonic energy match ({track.camelotKey})
                  </span>
                </div>
              </div>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-medium text-primary bg-cyan-950/50 border border-cyan-800/40">
                ±5 BPM ({track.bpm - 5}-{track.bpm + 5})
              </span>
            </button>

            <button
              type="button"
              onClick={() => setShowTechSpecs((s) => !s)}
              className="w-full min-h-[52px] px-3.5 py-2 rounded-xl flex items-center justify-between text-left hover:bg-[#25252e] transition group"
            >
              <div className="flex items-center space-x-3.5">
                <div className="w-9 h-9 rounded-lg bg-indigo-950/40 border border-indigo-500/30 flex items-center justify-center text-indigo-400 shrink-0">
                  <span className="material-symbols-outlined text-[18px]">
                    info
                  </span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-white">
                    View Technical Audio Specs
                  </span>
                  <span className="text-[11px] font-mono text-[#9ca3af]">
                    FLAC • {track.bitrateKbps.toLocaleString()} kbps •{' '}
                    {track.bitDepth}/{track.sampleRate} • 2ch
                  </span>
                </div>
              </div>
              <span className="material-symbols-outlined text-[18px] text-zinc-500">
                {showTechSpecs ? 'expand_less' : 'chevron_right'}
              </span>
            </button>

            {showTechSpecs && (
              <div className="mx-3.5 p-3 rounded-xl bg-[#141418] border border-[#2a2a35] font-mono text-[11px] space-y-1 text-[#9ca3af]">
                <div className="flex justify-between">
                  <span>AcoustID Fingerprint:</span>
                  <span className="text-primary">{track.acoustId.slice(0, 18)}...</span>
                </div>
                <div className="flex justify-between">
                  <span>Harmonic Key / Camelot:</span>
                  <span className="text-white">
                    {track.musicalKey} ({track.camelotKey})
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Integrated Loudness:</span>
                  <span className="text-tertiary">{track.lufs} LUFS (Bit-Verified)</span>
                </div>
                <div className="flex justify-between">
                  <span>Container Size:</span>
                  <span className="text-white">{track.fileSizeMb} MB</span>
                </div>
              </div>
            )}

            {/* AI Crate Historian & Audiophile Liner Notes Action */}
            <button
              type="button"
              onClick={() => setShowLinerNotes((s) => !s)}
              className="w-full min-h-[52px] px-3.5 py-2 rounded-xl flex items-center justify-between text-left hover:bg-[#25252e] transition group border border-primary/20 bg-primary/5"
            >
              <div className="flex items-center space-x-3.5">
                <div className="w-9 h-9 rounded-lg bg-primary/20 border border-primary/40 flex items-center justify-center text-primary shrink-0">
                  <span className="text-[16px]">📜</span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-white group-hover:text-primary transition-colors">
                    AI Crate Historian &amp; Liner Notes
                  </span>
                  <span className="text-[11px] text-[#9ca3af]">
                    Mastering trivia &amp; recommended DAC tuning
                  </span>
                </div>
              </div>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-medium text-tertiary bg-tertiary/10 border border-tertiary/30">
                AI Curator
              </span>
            </button>

            {showLinerNotes && (
              <div className="mx-3.5 p-3.5 rounded-xl bg-[#121216] border border-primary/30 text-xs space-y-2">
                <div className="flex items-center gap-1.5 text-primary font-mono text-[11px] font-bold">
                  <span>✨</span>
                  <span>AUDIOPHILE LINER NOTES</span>
                </div>
                <p className="text-[#e4e4e7] leading-relaxed text-[12px] italic">
                  "{track.title}" is a reference-grade {track.sampleRate} master cut anchored in {track.camelotKey} Camelot harmonic space. Preserves wide stereophonic transient response with uncompressed dynamic headroom.
                </p>
                <div className="p-2 rounded-lg bg-surface-container-high/60 font-mono text-[10px] space-y-1 text-[#9ca3af]">
                  <p className="text-tertiary font-bold">Recommended DAC Filter:</p>
                  <p>Fast Linear Phase • Direct ALSA bit-perfect pass-through (Bypasses Android mixer).</p>
                </div>
              </div>
            )}

            <button
              type="button"
              onClick={() => {
                onRemoveTrack(track.id);
                onClose();
              }}
              className="w-full min-h-[52px] px-3.5 py-2 rounded-xl flex items-center justify-between text-left hover:bg-red-950/20 transition group"
            >
              <div className="flex items-center space-x-3.5">
                <div className="w-9 h-9 rounded-lg bg-red-950/40 border border-red-500/30 flex items-center justify-center text-red-400 shrink-0">
                  <span className="material-symbols-outlined text-[18px]">
                    delete
                  </span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[14px] font-semibold text-red-400 tracking-tight">
                    Remove from Device
                  </span>
                  <span className="text-[11px] text-zinc-400">
                    Delete local FLAC file from storage ({track.fileSizeMb} MB)
                  </span>
                </div>
              </div>
              <span className="text-[10px] font-mono text-red-400/80 font-medium">
                Delete
              </span>
            </button>
          </nav>
        </div>
      </section>
    </div>
  );
};

interface DriveModeProps {
  track: Track;
  isPlaying: boolean;
  currentTimeSec: number;
  activeDac: UsbDacEndpoint;
  onClose: () => void;
  onTogglePlay: () => void;
  onPrev: () => void;
  onNext: () => void;
  onSeek: (sec: number) => void;
  onToggleLike: (id: string) => void;
  onApplyPresetPrompt: (prompt: string) => void;
  onShowToast: (msg: string) => void;
}

export const DriveModeModal: React.FC<DriveModeProps> = ({
  track,
  isPlaying,
  currentTimeSec,
  activeDac,
  onClose,
  onTogglePlay,
  onPrev,
  onNext,
  onSeek,
  onToggleLike,
  onApplyPresetPrompt,
  onShowToast,
}) => {
  const [voiceListening, setVoiceListening] = useState(false);
  const [activeCarPreset, setActiveCarPreset] = useState<'lofi' | 'roadtrip' | 'radio'>('roadtrip');
  const progressPct = Math.min(
    100,
    Math.max(0, (currentTimeSec / Math.max(1, track.durationSec)) * 100)
  );
  const remainingSec = Math.max(0, track.durationSec - currentTimeSec);

  return (
    <div className="absolute inset-0 z-50 bg-surface-container-lowest text-on-surface flex flex-col justify-between p-4 overflow-y-auto no-scrollbar select-none">
      {/* Top Drive Mode Telemetry Header Bar */}
      <header className="flex items-center justify-between w-full h-14 px-3 mb-2 bg-surface-container-low rounded-xl shrink-0">
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 bg-surface-container-high px-3 py-1.5 rounded-full">
            <span
              className="material-symbols-outlined text-primary text-[18px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              directions_car
            </span>
            <span className="font-label-lg text-on-surface tracking-wide font-bold">
              DRIVE MODE
            </span>
          </div>
          <div className="flex items-center gap-1 bg-surface-container px-2 py-1 rounded-full">
            <span className="w-2 h-2 rounded-full bg-tertiary"></span>
            <span className="font-telemetry-sm text-tertiary uppercase">
              24b/96k
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 text-on-surface-variant font-telemetry-sm">
            <span className="material-symbols-outlined text-[15px] text-primary">
              navigation
            </span>
            <span className="font-bold text-on-surface">62 MPH</span>
          </div>
          <button
            onClick={onClose}
            className="flex items-center justify-center min-w-[52px] h-10 px-3 bg-surface-container-highest hover:bg-surface-bright active:scale-95 transition-transform rounded-xl text-on-surface gap-1"
          >
            <span className="font-label-md font-bold">EXIT</span>
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>
      </header>

      {/* Track Identity Card */}
      <div className="flex flex-col justify-between p-4 bg-surface-container-low rounded-2xl">
        <div className="flex gap-3.5 items-center">
          <div className="relative w-28 h-28 shrink-0 rounded-xl overflow-hidden bg-surface-container-high shadow-xl">
            <img
              src={track.artworkUrl}
              alt={track.title}
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover"
            />
            <div className="absolute top-1.5 left-1.5 bg-surface-container-lowest/85 backdrop-blur-md px-2 py-0.5 rounded-full flex items-center gap-1">
              <span
                className="material-symbols-outlined text-secondary text-[12px]"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                album
              </span>
              <span className="font-telemetry-sm text-on-surface font-bold">
                HI-RES
              </span>
            </div>
          </div>

          <div className="flex flex-col justify-center min-w-0 flex-1">
            <div className="inline-flex items-center gap-1.5 mb-1 bg-surface-container px-2.5 py-0.5 rounded-md w-fit">
              <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
              <span className="font-telemetry-sm text-primary tracking-widest uppercase">
                Direct Stream
              </span>
            </div>
            <h1 className="font-headline-md font-extrabold text-on-surface truncate leading-tight">
              {track.title}
            </h1>
            <p className="font-label-lg text-on-surface-variant truncate mt-0.5">
              {track.artist}
            </p>
            <p className="font-body-sm text-outline truncate">{track.album}</p>
            <div className="mt-2 flex items-center gap-1.5">
              <span className="bg-surface-variant text-on-surface-variant font-telemetry-sm px-2 py-0.5 rounded">
                FLAC {track.sampleRate}
              </span>
              <span className="bg-surface-variant text-on-surface-variant font-telemetry-sm px-2 py-0.5 rounded">
                {track.bitDepth} Stereo
              </span>
            </div>
          </div>
        </div>

        {/* High-Visibility Driving Scrubber */}
        <div className="mt-4 w-full flex flex-col gap-1.5">
          <div
            onClick={(e) => {
              const rect = e.currentTarget.getBoundingClientRect();
              const pct = Math.min(
                1,
                Math.max(0, (e.clientX - rect.left) / rect.width)
              );
              onSeek(Math.round(pct * track.durationSec));
            }}
            className="relative w-full h-3.5 bg-surface-container-highest rounded-full overflow-hidden cursor-pointer"
          >
            <div
              style={{ width: `${progressPct}%` }}
              className="absolute left-0 top-0 bottom-0 bg-primary rounded-full shadow-[0_0_12px_rgba(76,215,246,0.6)]"
            ></div>
          </div>
          <div className="flex justify-between items-center px-1">
            <span className="font-telemetry-md font-bold text-primary">
              {formatTime(currentTimeSec)}
            </span>
            <span className="font-telemetry-md font-semibold text-on-surface-variant">
              -{formatTime(remainingSec)}
            </span>
          </div>
        </div>
      </div>

      {/* Massive Thumb-Friendly Transport Controls (80dp+ Hitboxes) */}
      <div className="flex flex-col justify-between gap-3.5 p-4 bg-surface-container-low rounded-2xl my-2">
        <div className="flex items-center justify-around w-full py-1">
          <button
            onClick={() => onToggleLike(track.id)}
            className="flex items-center justify-center w-14 h-14 rounded-full bg-surface-container hover:bg-surface-container-high active:scale-90 transition-transform text-secondary"
          >
            <span
              className="material-symbols-outlined text-[30px]"
              style={{
                fontVariationSettings: track.liked ? "'FILL' 1" : "'FILL' 0",
              }}
            >
              favorite
            </span>
          </button>

          <button
            aria-label="Previous Track"
            onClick={onPrev}
            className="flex items-center justify-center w-20 h-20 rounded-full bg-surface-container hover:bg-surface-bright active:scale-95 transition-transform text-on-surface shadow-md"
          >
            <span className="material-symbols-outlined text-[42px]">
              skip_previous
            </span>
          </button>

          <button
            aria-label="Play / Pause"
            onClick={onTogglePlay}
            className="flex items-center justify-center w-24 h-24 rounded-full bg-primary hover:bg-primary-fixed-dim text-on-primary shadow-[0_0_32px_rgba(76,215,246,0.45)] active:scale-95 transition-all"
          >
            <span
              className="material-symbols-outlined text-[54px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              {isPlaying ? 'pause' : 'play_arrow'}
            </span>
          </button>

          <button
            aria-label="Next Track"
            onClick={onNext}
            className="flex items-center justify-center w-20 h-20 rounded-full bg-surface-container hover:bg-surface-bright active:scale-95 transition-transform text-on-surface shadow-md"
          >
            <span className="material-symbols-outlined text-[42px]">
              skip_next
            </span>
          </button>

          <button
            aria-label="Forward 15 seconds"
            onClick={() => {
              onSeek(Math.min(track.durationSec, currentTimeSec + 15));
              onShowToast('Jumped +15s');
            }}
            className="flex items-center justify-center w-14 h-14 rounded-full bg-surface-container hover:bg-surface-container-high active:scale-90 transition-transform text-on-surface-variant"
          >
            <span className="material-symbols-outlined text-[30px]">
              fast_forward
            </span>
          </button>
        </div>

        {/* Voice Assistant Bar */}
        <button
          onClick={() => {
            setVoiceListening(true);
            setTimeout(() => {
              setVoiceListening(false);
              onApplyPresetPrompt(
                'Upbeat 2000s energetic pop-punk & roadtrip rock, 135+ BPM'
              );
              onShowToast('Voice AI: Loaded Roadtrip Rock Mixtape');
            }, 1400);
          }}
          className="flex items-center justify-between w-full h-14 px-4 bg-surface-container hover:bg-surface-container-high active:bg-surface-bright rounded-full transition-all group"
        >
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-full bg-primary-container/20 flex items-center justify-center text-primary shrink-0">
              <span
                className="material-symbols-outlined text-[20px]"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                mic
              </span>
            </div>
            <span
              className={`font-label-lg font-bold truncate ${
                voiceListening ? 'text-primary animate-pulse' : 'text-on-surface'
              }`}
            >
              {voiceListening
                ? 'Listening for driver voice command...'
                : '"Play Roadtrip Rock" or Tap to Speak'}
            </span>
          </div>
          <span className="flex items-center gap-1 font-telemetry-sm text-primary uppercase font-bold shrink-0">
            Voice AI
            <span className="material-symbols-outlined text-[14px]">
              arrow_forward_ios
            </span>
          </span>
        </button>

        {/* Quick Automotive Presets Strip */}
        <div className="grid grid-cols-3 gap-2 w-full">
          <button
            onClick={() => {
              setActiveCarPreset('lofi');
              onApplyPresetPrompt('Chilled lo-fi beats, warm Rhodes, 85 BPM');
            }}
            className={`flex items-center justify-center gap-1.5 h-14 px-2 rounded-xl transition ${
              activeCarPreset === 'lofi'
                ? 'bg-primary/15 text-primary'
                : 'bg-surface-container text-on-surface'
            }`}
          >
            <span className="material-symbols-outlined text-secondary text-[20px]">
              coffee
            </span>
            <div className="flex flex-col text-left truncate">
              <span className="font-label-md font-bold truncate">Daily Lo-Fi</span>
              <span className="font-telemetry-sm text-outline truncate">
                Commute
              </span>
            </div>
          </button>

          <button
            onClick={() => {
              setActiveCarPreset('roadtrip');
              onApplyPresetPrompt('Roadtrip energetic rock & electro >125 BPM');
            }}
            className={`relative flex items-center justify-center gap-1.5 h-14 px-2 rounded-xl transition ${
              activeCarPreset === 'roadtrip'
                ? 'bg-primary/15 text-primary'
                : 'bg-surface-container text-on-surface'
            }`}
          >
            <span
              className="material-symbols-outlined text-primary text-[20px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              bolt
            </span>
            <div className="flex flex-col text-left truncate">
              <span className="font-label-md font-bold truncate">
                Roadtrip Boost
              </span>
              <span className="font-telemetry-sm text-primary/80 truncate">
                Active Mix
              </span>
            </div>
          </button>

          <button
            onClick={() => {
              setActiveCarPreset('radio');
              onApplyPresetPrompt('Harmonic crate flow across all hi-res tracks');
            }}
            className={`flex items-center justify-center gap-1.5 h-14 px-2 rounded-xl transition ${
              activeCarPreset === 'radio'
                ? 'bg-primary/15 text-primary'
                : 'bg-surface-container text-on-surface'
            }`}
          >
            <span className="material-symbols-outlined text-tertiary text-[20px]">
              radio
            </span>
            <div className="flex flex-col text-left truncate">
              <span className="font-label-md font-bold truncate">Crate Radio</span>
              <span className="font-telemetry-sm text-outline truncate">
                Smart Flow
              </span>
            </div>
          </button>
        </div>
      </div>

      {/* Safety & Hardware Status Footer */}
      <footer className="px-2 py-1 flex items-center justify-between text-outline font-telemetry-sm">
        <div className="flex items-center gap-1.5 truncate">
          <span className="material-symbols-outlined text-[15px] text-secondary">
            verified_user
          </span>
          <span className="truncate">80dp+ Vehicular Safety Lock</span>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <span>{activeDac.modelName.replace(' USB DAC', '')}</span>
          <span className="text-tertiary font-bold">100% Vol Lock</span>
        </div>
      </footer>
    </div>
  );
};

interface WidgetsShowcaseProps {
  track: Track;
  isPlaying: boolean;
  currentTimeSec: number;
  activeDac: UsbDacEndpoint;
  activeTheme: 'teal' | 'amber' | 'lofi' | 'ink';
  onClose: () => void;
  onTogglePlay: () => void;
  onPrev: () => void;
  onNext: () => void;
  onToggleLike: (id: string) => void;
  onTriggerVibePrompt: (prompt: string) => void;
  onChangeTheme: (theme: 'teal' | 'amber' | 'lofi' | 'ink') => void;
  onShowToast: (msg: string) => void;
}

export const WidgetsShowcaseModal: React.FC<WidgetsShowcaseProps> = ({
  track,
  isPlaying,
  currentTimeSec,
  activeDac,
  activeTheme,
  onClose,
  onTogglePlay,
  onPrev,
  onNext,
  onToggleLike,
  onTriggerVibePrompt,
  onChangeTheme,
  onShowToast,
}) => {
  const [activeSegment, setActiveSegment] = useState<'home' | 'lock' | 'gallery'>('home');
  const [addedToHome, setAddedToHome] = useState(false);

  const progressPct = Math.min(
    100,
    Math.max(0, (currentTimeSec / Math.max(1, track.durationSec)) * 100)
  );

  return (
    <div className="absolute inset-0 z-50 bg-surface text-on-surface flex flex-col overflow-hidden select-none">
      {/* Header */}
      <header className="h-16 px-4 flex items-center justify-between bg-surface/90 backdrop-blur-xl border-b border-outline-variant/20 shrink-0">
        <div className="flex items-center gap-2.5">
          <button
            onClick={onClose}
            className="w-9 h-9 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant hover:text-on-surface"
          >
            <span className="material-symbols-outlined text-[20px]">
              arrow_back
            </span>
          </button>
          <div className="w-8 h-8 rounded-full bg-primary-container/20 flex items-center justify-center text-primary">
            <span className="material-symbols-outlined text-[18px]">
              graphic_eq
            </span>
          </div>
          <h1 className="font-headline-sm text-on-surface tracking-tight">
            Widget Showcase
          </h1>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
            <span className="material-symbols-outlined text-on-primary text-[18px]">
              person
            </span>
          </div>
        </div>
      </header>

      {/* Main Scrollable Content */}
      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pt-3 pb-24 space-y-5">
        <section className="flex flex-col space-y-2">
          <div className="flex items-center justify-between">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary font-telemetry-sm tracking-wide">
              <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
              DYNAMIC COLOR • MATERIAL 3
            </span>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-surface-container-highest text-on-surface-variant font-telemetry-sm">
              API 34+
            </span>
          </div>
          <div>
            <h2 className="font-headline-md text-on-surface tracking-tight">
              Android Widgets &amp; Complications
            </h2>
            <p className="font-body-sm text-on-surface-variant mt-1 leading-relaxed">
              Lossless playback control and instant prompt triggers right from your Android Home &amp; Lock screens.
            </p>
          </div>

          <div className="flex p-1 rounded-full bg-surface-container-lowest gap-1 mt-1">
            {(
              [
                ['home', 'Home Screen (Active)'],
                ['lock', 'Lock Screen Glance'],
                ['gallery', 'Widget Gallery'],
              ] as const
            ).map(([key, label]) => (
              <button
                key={key}
                onClick={() => setActiveSegment(key)}
                className={`flex-1 py-2 px-2 rounded-full font-label-md text-center transition-all ${
                  activeSegment === key
                    ? 'bg-surface-container text-primary font-semibold shadow-sm'
                    : 'text-on-surface-variant hover:text-on-surface'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </section>

        {/* Section 1: 4x2 Turntable Vinyl Widget */}
        <section className="flex flex-col space-y-1.5">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px] text-primary">
                view_quilt
              </span>
              <span className="font-label-lg text-on-surface">
                4×2 Turntable Vinyl Widget
              </span>
            </div>
            <span className="font-telemetry-sm text-on-surface-variant uppercase tracking-wider">
              Interactive
            </span>
          </div>

          <div className="relative w-full rounded-[24px] bg-surface-container p-4 shadow-xl overflow-hidden">
            <div className="absolute -right-10 -bottom-10 w-44 h-44 rounded-full bg-primary/10 blur-3xl pointer-events-none"></div>
            <div className="relative flex flex-col space-y-3">
              <div className="flex items-center gap-4">
                {/* Rotating Vinyl Record */}
                <div className="relative shrink-0 w-28 h-28 flex items-center justify-center">
                  <div
                    className={`relative w-28 h-28 rounded-full bg-surface-container-lowest flex items-center justify-center shadow-lg ${
                      isPlaying ? 'animate-[spin_10s_linear_infinite]' : ''
                    }`}
                  >
                    <div className="absolute inset-1.5 rounded-full opacity-40 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-surface-bright via-surface-container-low to-surface-container-lowest"></div>
                    <div className="relative w-12 h-12 rounded-full overflow-hidden shadow-inner flex items-center justify-center">
                      <img
                        src={track.artworkUrl}
                        alt={track.title}
                        referrerPolicy="no-referrer"
                        className="w-full h-full object-cover"
                      />
                      <div className="absolute w-2.5 h-2.5 rounded-full bg-surface-container-lowest shadow-md"></div>
                    </div>
                  </div>
                  {/* Chrome Tonearm */}
                  <div className="absolute -top-1 right-1 w-7 h-12 pointer-events-none flex flex-col items-end">
                    <div className="w-3 h-3 rounded-full bg-outline shadow"></div>
                    <div className="w-1 h-8 bg-surface-variant transform rotate-12 origin-top"></div>
                    <div className="w-2 h-1 bg-secondary rounded-sm shadow-sm -mt-0.5"></div>
                  </div>
                </div>

                {/* Track Info */}
                <div className="flex-1 min-w-0 flex flex-col justify-between h-28 py-0.5">
                  <div>
                    <div className="flex items-center justify-between gap-1 mb-1">
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-surface-container-highest text-primary font-telemetry-sm">
                        <span className="w-1.5 h-1.5 rounded-full bg-primary animate-ping"></span>
                        {track.formatBadge}
                      </span>
                      <span className="material-symbols-outlined text-[18px] text-secondary">
                        graphic_eq
                      </span>
                    </div>
                    <h3 className="font-headline-sm text-on-surface truncate tracking-tight">
                      {track.title}
                    </h3>
                    <p className="font-body-sm text-on-surface-variant truncate">
                      {track.artist} • {track.album}
                    </p>
                  </div>

                  <div className="w-full space-y-1">
                    <div className="w-full bg-surface-container-highest h-1.5 rounded-full overflow-hidden relative">
                      <div
                        style={{ width: `${progressPct}%` }}
                        className="bg-primary h-full rounded-full transition-all"
                      ></div>
                    </div>
                    <div className="flex items-center justify-between text-on-surface-variant font-telemetry-sm">
                      <span>{formatTime(currentTimeSec)}</span>
                      <span className="text-tertiary">Bit-Perfect Stream</span>
                      <span>{track.durationLabel}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Transport Dock */}
              <div className="flex items-center justify-between pt-1">
                <button
                  onClick={() => onShowToast('Widget Shuffle Toggled')}
                  className="w-11 h-11 rounded-full flex items-center justify-center text-tertiary active:scale-95"
                >
                  <span className="material-symbols-outlined text-[20px]">
                    shuffle
                  </span>
                </button>
                <div className="flex items-center gap-3">
                  <button
                    onClick={onPrev}
                    className="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface active:scale-95"
                  >
                    <span className="material-symbols-outlined text-[24px]">
                      skip_previous
                    </span>
                  </button>
                  <button
                    onClick={onTogglePlay}
                    className="w-14 h-14 rounded-full bg-primary-container text-on-primary flex items-center justify-center shadow-[0_0_24px_rgba(6,182,212,0.35)] active:scale-90 transition-transform"
                  >
                    <span
                      className="material-symbols-outlined text-[30px]"
                      style={{ fontVariationSettings: "'FILL' 1" }}
                    >
                      {isPlaying ? 'pause' : 'play_arrow'}
                    </span>
                  </button>
                  <button
                    onClick={onNext}
                    className="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface active:scale-95"
                  >
                    <span className="material-symbols-outlined text-[24px]">
                      skip_next
                    </span>
                  </button>
                </div>
                <button
                  onClick={() => onToggleLike(track.id)}
                  className="w-11 h-11 rounded-full flex items-center justify-center text-secondary active:scale-95"
                >
                  <span
                    className="material-symbols-outlined text-[20px]"
                    style={{
                      fontVariationSettings: track.liked
                        ? "'FILL' 1"
                        : "'FILL' 0",
                    }}
                  >
                    favorite
                  </span>
                </button>
              </div>

              {/* Dynamic DAC Status Bar */}
              <div className="pt-2 flex items-center justify-between text-on-surface-variant">
                <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-container-low text-on-surface">
                  <span className="w-2 h-2 rounded-full bg-tertiary shadow-[0_0_6px_rgba(78,222,163,0.8)]"></span>
                  <span className="font-telemetry-sm tracking-wide">
                    {activeDac.modelName} (Bit-Perfect)
                  </span>
                </div>
                <div className="flex items-center gap-1 text-on-surface-variant">
                  <span className="material-symbols-outlined text-[15px] text-primary">
                    sensors
                  </span>
                  <span className="font-telemetry-sm uppercase">
                    VibePlay Engine
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Section 2: 4x1 Quick Vibe Launcher Pill */}
        <section className="flex flex-col space-y-1.5">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px] text-secondary">
                bolt
              </span>
              <span className="font-label-lg text-on-surface">
                4×1 Quick Vibe Launcher Pill
              </span>
            </div>
            <span className="font-telemetry-sm text-on-surface-variant uppercase">
              Instant Prompt
            </span>
          </div>

          <div className="w-full rounded-full bg-surface-container p-2 shadow-lg flex items-center justify-between gap-2">
            <button
              onClick={() => {
                onTriggerVibePrompt(
                  'Late night downtempo with warm analog tape hiss, under 95 BPM'
                );
                onClose();
              }}
              className="flex-1 min-w-0 h-11 rounded-full bg-surface-container-high px-3 flex items-center gap-2 hover:bg-surface-bright transition-all text-left"
            >
              <div className="w-6 h-6 rounded-full bg-primary/20 text-primary flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-[15px]">
                  mic
                </span>
              </div>
              <span className="font-label-md text-on-surface truncate">
                What&apos;s the vibe?
              </span>
            </button>

            <div className="flex items-center gap-1 shrink-0">
              <button
                onClick={() => {
                  onTriggerVibePrompt('Morning Coffee Lo-Fi around 82 BPM');
                  onClose();
                }}
                className="h-10 px-2.5 rounded-full bg-secondary-container/20 text-secondary-fixed flex items-center gap-1 active:scale-95"
              >
                <span className="text-xs">☕</span>
                <span className="font-label-md font-semibold">Lo-Fi</span>
              </button>
              <button
                onClick={() => {
                  onTriggerVibePrompt('Fast Cardio industrial techno >135 BPM');
                  onClose();
                }}
                className="h-10 px-2.5 rounded-full bg-secondary/20 text-secondary flex items-center gap-1 active:scale-95"
              >
                <span className="text-xs">🔥</span>
                <span className="font-label-md font-semibold">Workout</span>
              </button>
              <button
                onClick={() => {
                  onTriggerVibePrompt('Rainy Ambient DSD under 90 BPM');
                  onClose();
                }}
                className="h-10 px-2.5 rounded-full bg-primary/20 text-primary flex items-center gap-1 active:scale-95"
              >
                <span className="text-xs">🌙</span>
                <span className="font-label-md font-semibold">Chill</span>
              </button>
            </div>
          </div>
        </section>

        {/* Section 3: Lock Screen & AOD Complications */}
        <section className="flex flex-col space-y-1.5">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px] text-tertiary">
                lock_clock
              </span>
              <span className="font-label-lg text-on-surface">
                Lock Screen &amp; AOD Complications
              </span>
            </div>
            <span className="font-telemetry-sm text-tertiary">
              Lossless Direct
            </span>
          </div>

          <div className="grid grid-cols-1 gap-2.5">
            <div className="rounded-2xl bg-surface-container p-3.5 shadow-md flex items-center justify-between gap-3">
              <div className="relative w-12 h-12 rounded-xl overflow-hidden shrink-0 bg-surface-container-highest">
                <img
                  src="https://lh3.googleusercontent.com/aida-public/AB6AXuCHxjwGriAI-vlMfOyGFuKG-fCVX97RyaXGot7N2n65R1ijNmHXg-GJr3BtBHgjTfCfNA_clfP6nsWi60KA5Hl505b6c4ro2L2YHBgqlJdPowd1eTWtvmMxQ7vEzngRDN-DtHV_R3fOMNRa-KOtMQrKHBpylcMnZk_XdD_vTHdTG3UzBrUUvBY6SFEmCseCb36FyhphRqSPK-TXvjY0MZg8RVomd3rgo1m5AisZ3eZLRytLtBDubj8"
                  alt="Midnight Pulse"
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-cover"
                />
                <div className="absolute bottom-0 inset-x-0 h-1.5 bg-primary"></div>
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-headline-sm text-on-surface truncate">
                    Midnight Pulse
                  </span>
                  <span className="px-1.5 py-0.5 rounded-full bg-tertiary/10 text-tertiary font-telemetry-sm shrink-0">
                    96kHz
                  </span>
                </div>
                <p className="font-body-sm text-on-surface-variant truncate">
                  Tycho • Simulcast
                </p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={onTogglePlay}
                  className="w-10 h-10 rounded-full bg-surface-container-high text-on-surface flex items-center justify-center active:scale-95"
                >
                  <span className="material-symbols-outlined text-[20px]">
                    {isPlaying ? 'pause' : 'play_arrow'}
                  </span>
                </button>
              </div>
            </div>

            <div className="rounded-xl bg-surface-container-lowest p-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-[18px] text-on-surface-variant">
                  usb
                </span>
                <span className="font-telemetry-sm text-on-surface">
                  Direct USB-C • Bit-Perfect Mode
                </span>
              </div>
              <div className="flex items-center gap-1.5 text-tertiary font-telemetry-sm">
                <span className="w-1.5 h-1.5 rounded-full bg-tertiary"></span>
                <span>DSD128 Native</span>
              </div>
            </div>
          </div>
        </section>

        {/* Section 4: Material You Dynamic Theme */}
        <section className="rounded-2xl bg-surface-container-low p-4 flex flex-col space-y-3 shadow-lg">
          <div className="flex items-start justify-between">
            <div>
              <h4 className="font-headline-sm text-on-surface">
                Material You Dynamic Theme
              </h4>
              <p className="font-body-sm text-on-surface-variant">
                Extract palette from current vinyl sleeve or hardware theme.
              </p>
            </div>
            <div className="flex -space-x-1.5 items-center">
              <div className="w-5 h-5 rounded-full bg-primary shadow-sm"></div>
              <div className="w-5 h-5 rounded-full bg-secondary shadow-sm"></div>
              <div className="w-5 h-5 rounded-full bg-tertiary shadow-sm"></div>
            </div>
          </div>

          <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
            {(
              [
                ['teal', '● Dynamic Cyan'],
                ['amber', 'Warm Amber'],
                ['ink', 'Deep OLED Pitch'],
              ] as const
            ).map(([tKey, label]) => (
              <button
                key={tKey}
                onClick={() => onChangeTheme(tKey)}
                className={`px-3.5 py-1.5 rounded-full font-label-md whitespace-nowrap transition-colors ${
                  activeTheme === tKey
                    ? 'bg-primary/20 text-primary font-semibold'
                    : 'bg-surface-container-high text-on-surface-variant'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          <button
            onClick={() => {
              setAddedToHome(true);
              onShowToast('Pinned 4×2 Vinyl & 4×1 Vibe Widgets to Android Launcher');
              setTimeout(() => setAddedToHome(false), 2200);
            }}
            className={`w-full h-13 rounded-full font-label-lg font-bold flex items-center justify-center gap-2 shadow-lg transition-all ${
              addedToHome
                ? 'bg-tertiary text-on-tertiary'
                : 'bg-primary text-on-primary'
            }`}
          >
            <span className="material-symbols-outlined text-[20px]">
              {addedToHome ? 'check_circle' : 'add_to_home_screen'}
            </span>
            {addedToHome
              ? 'Added to Launcher Pin Tray'
              : 'Add Widgets to Home Screen'}
          </button>
        </section>
      </div>
    </div>
  );
};

interface WelcomeProps {
  storageGranted: boolean;
  usbGranted: boolean;
  onToggleStorage: () => void;
  onToggleUsb: () => void;
  onCompleteOnboarding: () => void;
}

export const WelcomeOnboardingModal: React.FC<WelcomeProps> = ({
  storageGranted,
  usbGranted,
  onToggleStorage,
  onToggleUsb,
  onCompleteOnboarding,
}) => {
  const [scanningState, setScanningState] = useState<'idle' | 'scanning' | 'done'>('idle');

  const handleStartScan = () => {
    setScanningState('scanning');
    setTimeout(() => {
      setScanningState('done');
      setTimeout(() => {
        onCompleteOnboarding();
      }, 650);
    }, 900);
  };

  return (
    <div className="absolute inset-0 z-50 bg-surface text-on-surface flex flex-col justify-between overflow-y-auto no-scrollbar select-none">
      <header className="h-14 px-4 flex items-center justify-between shrink-0">
        <span className="font-telemetry-md text-on-surface tracking-wider">
          09:41
        </span>
        <div className="flex items-center gap-1.5">
          <div className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></div>
          <span className="font-label-md text-primary tracking-widest uppercase font-semibold">
            VibePlay
          </span>
        </div>
        <div className="flex items-center gap-1 text-on-surface">
          <span className="font-telemetry-sm font-semibold">5G</span>
          <span className="material-symbols-outlined text-[16px]">
            signal_cellular_4_bar
          </span>
          <span className="material-symbols-outlined text-[18px]">
            battery_charging_full
          </span>
        </div>
      </header>

      <div className="flex flex-col w-full px-4 pb-6">
        <div className="relative w-full rounded-[28px] bg-surface-container p-5 shadow-2xl overflow-hidden flex flex-col items-center">
          <div className="absolute -top-24 -left-24 w-56 h-56 rounded-full bg-primary/10 blur-3xl pointer-events-none"></div>
          <div className="absolute -bottom-24 -right-24 w-56 h-56 rounded-full bg-secondary/10 blur-3xl pointer-events-none"></div>

          {/* Spinning Vinyl Illustration */}
          <div className="relative w-32 h-32 flex items-center justify-center my-1">
            <div className="absolute inset-0 rounded-full bg-primary/20 blur-xl animate-pulse"></div>
            <div className="relative w-28 h-28 rounded-full bg-surface-container-lowest shadow-2xl flex items-center justify-center animate-[spin_8s_linear_infinite]">
              <svg
                className="absolute inset-0 w-full h-full opacity-40 text-on-surface-variant"
                fill="none"
                viewBox="0 0 128 128"
              >
                <circle
                  cx="64"
                  cy="64"
                  r="58"
                  stroke="currentColor"
                  strokeDasharray="3 3"
                  strokeWidth="0.75"
                />
                <circle
                  cx="64"
                  cy="64"
                  r="50"
                  stroke="currentColor"
                  strokeWidth="0.5"
                />
                <circle
                  cx="64"
                  cy="64"
                  r="42"
                  stroke="currentColor"
                  strokeDasharray="6 2"
                  strokeWidth="0.75"
                />
                <circle
                  cx="64"
                  cy="64"
                  r="34"
                  stroke="currentColor"
                  strokeWidth="0.5"
                />
              </svg>
              <div className="relative w-12 h-12 rounded-full bg-gradient-to-tr from-secondary-container via-primary to-primary-fixed flex items-center justify-center shadow-lg">
                <div className="w-3.5 h-3.5 rounded-full bg-surface-container-lowest flex items-center justify-center">
                  <div className="w-1.5 h-1.5 rounded-full bg-primary"></div>
                </div>
              </div>
            </div>
            <div className="absolute -top-1 -right-1 w-7 h-7 rounded-full bg-surface-container-high shadow-md flex items-center justify-center text-primary">
              <span className="material-symbols-outlined text-[16px]">
                graphic_eq
              </span>
            </div>
          </div>

          <div className="text-center mt-2 mb-4 max-w-xs">
            <h1 className="font-headline-lg text-on-surface tracking-tight flex items-center justify-center gap-1.5">
              <span>Welcome to VibePlay</span>
              <span className="text-secondary">🎧</span>
            </h1>
            <p className="font-body-md text-on-surface-variant mt-1 leading-relaxed">
              Bit-perfect local audio with conversational playlist cooking.
            </p>
          </div>

          <div className="w-full flex flex-col gap-2.5 mb-4">
            {/* Permission Card 1 */}
            <div className="w-full bg-surface-container-lowest rounded-2xl p-3.5 flex flex-col gap-1">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-primary text-[20px]">
                    folder_open
                  </span>
                  <span className="font-label-lg text-on-surface">
                    Music &amp; Audio Files Access
                  </span>
                </div>
                <span className="font-telemetry-sm bg-surface-container-high px-2 py-0.5 rounded-full text-secondary font-semibold uppercase">
                  Required
                </span>
              </div>
              <p className="font-body-sm text-on-surface-variant">
                Scans your lossless collection (
                <span className="font-telemetry-sm text-on-surface">/Music</span>
                ) directly with zero cloud telemetry.
              </p>
              <div className="flex items-center justify-between pt-1 mt-1">
                <div className="flex items-center gap-1.5 text-on-surface-variant font-telemetry-sm">
                  <span className="w-2 h-2 rounded-full bg-tertiary"></span>
                  <span>Local Storage Sandboxed</span>
                </div>
                <button
                  onClick={onToggleStorage}
                  className={`px-3.5 py-1.5 rounded-full font-label-md transition-all active:scale-95 flex items-center gap-1 ${
                    storageGranted
                      ? 'bg-tertiary/15 text-tertiary'
                      : 'bg-surface-container-high text-primary'
                  }`}
                >
                  <span>{storageGranted ? 'Granted' : 'Grant Access'}</span>
                  <span className="material-symbols-outlined text-[16px]">
                    {storageGranted ? 'check_circle' : 'arrow_forward'}
                  </span>
                </button>
              </div>
            </div>

            {/* Permission Card 2 */}
            <div className="w-full bg-surface-container-lowest rounded-2xl p-3.5 flex flex-col gap-1">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-secondary text-[20px]">
                    usb
                  </span>
                  <span className="font-label-lg text-on-surface">
                    USB DAC Direct Access
                  </span>
                </div>
                <span className="font-telemetry-sm bg-surface-container-high px-2 py-0.5 rounded-full text-tertiary font-semibold uppercase">
                  Bit-Perfect
                </span>
              </div>
              <p className="font-body-sm text-on-surface-variant">
                Direct hardware passthrough avoiding OS resampling. Up to 768kHz DSD/DXD streams.
              </p>
              <div className="flex items-center justify-between pt-1 mt-1">
                <div className="flex items-center gap-1.5 text-on-surface-variant font-telemetry-sm">
                  <span className="material-symbols-outlined text-tertiary text-[14px]">
                    bolt
                  </span>
                  <span>Bypasses Android Mixer</span>
                </div>
                <button
                  onClick={onToggleUsb}
                  className={`px-3.5 py-1.5 rounded-full font-label-md transition-all active:scale-95 flex items-center gap-1 ${
                    usbGranted
                      ? 'bg-secondary/15 text-secondary'
                      : 'bg-surface-container-high text-on-surface'
                  }`}
                >
                  <span>{usbGranted ? 'Active' : 'Plug & Authorize'}</span>
                  <span className="material-symbols-outlined text-[16px]">
                    {usbGranted ? 'toggle_on' : 'toggle_off'}
                  </span>
                </button>
              </div>
            </div>
          </div>

          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-surface-container-high mb-4">
            <span className="material-symbols-outlined text-tertiary text-[14px]">
              verified_user
            </span>
            <span className="font-label-md text-on-surface-variant">
              100% Offline • Zero Analytics • DSP Verified
            </span>
          </div>

          <div className="w-full flex flex-col items-center gap-2">
            <button
              onClick={handleStartScan}
              className={`w-full h-14 rounded-full font-headline-sm tracking-wide shadow-lg flex items-center justify-center gap-2 active:scale-98 transition-all ${
                scanningState === 'done'
                  ? 'bg-tertiary-container text-on-tertiary'
                  : 'bg-primary-container text-on-primary-container shadow-primary-container/25'
              }`}
            >
              {scanningState === 'scanning' ? (
                <>
                  <span className="material-symbols-outlined animate-spin text-[20px]">
                    sync
                  </span>
                  <span>Indexing FLAC / DSD...</span>
                </>
              ) : scanningState === 'done' ? (
                <>
                  <span className="material-symbols-outlined text-[20px]">
                    done_all
                  </span>
                  <span>1,248 Tracks Found</span>
                </>
              ) : (
                <>
                  <span>Scan Device &amp; Start Jamming</span>
                  <span className="material-symbols-outlined text-[20px]">
                    rocket_launch
                  </span>
                </>
              )}
            </button>
            <button
              onClick={onCompleteOnboarding}
              className="py-1.5 px-4 rounded-full text-on-surface-variant hover:text-on-surface font-label-md flex items-center gap-1"
            >
              <span>Manual setup &amp; folder selector</span>
              <span className="material-symbols-outlined text-[14px]">
                chevron_right
              </span>
            </button>
          </div>
        </div>

        <div className="w-full flex items-center justify-between px-1 mt-3">
          <div className="flex items-center gap-1.5 font-telemetry-sm text-outline">
            <span className="w-1.5 h-1.5 rounded-full bg-tertiary"></span>
            <span>Engine: Native Hi-Fi v4.8</span>
          </div>
          <span className="font-telemetry-sm text-outline">v2.4.0-OLED</span>
        </div>
      </div>
    </div>
  );
};

interface DeduplicatorModalProps {
  groups: DuplicateGroup[];
  onResolveGroup: (groupId: string) => void;
  onPurgeAll: () => void;
  onClose: () => void;
}

export const MusicDeduplicatorModal: React.FC<DeduplicatorModalProps> = ({
  groups,
  onResolveGroup,
  onPurgeAll,
  onClose,
}) => {
  const [isScanning, setIsScanning] = useState(false);
  const unresolved = groups.filter((g) => !g.resolved);
  const totalReclaimableMb = unresolved.reduce(
    (sum, g) =>
      sum + g.candidates.reduce((cSum, c) => cSum + c.sizeMb, 0),
    0
  );

  return (
    <div className="absolute inset-0 z-50 flex flex-col justify-end bg-black/75 backdrop-blur-xs select-none">
      <section className="relative z-20 w-full bg-surface-container rounded-t-[28px] border-t border-outline-variant/40 shadow-2xl flex flex-col max-h-[860px] overflow-hidden">
        <div
          onClick={onClose}
          className="w-full pt-3 pb-2 flex justify-center items-center cursor-pointer"
        >
          <div className="w-10 h-1 rounded-full bg-outline/60"></div>
        </div>

        <div className="px-5 pb-3 border-b border-outline-variant/30 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl">🧹</span>
              <h2 className="font-headline-sm text-on-surface">
                Music Deduplicator &amp; Crate Cleaner
              </h2>
            </div>
            <p className="font-telemetry-sm text-on-surface-variant mt-0.5">
              Chromaprint AcoustID + SHA-256 Audio Stream Fingerprint Engine
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant"
          >
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>

        {/* Summary Telemetry Strip */}
        <div className="px-5 py-3 bg-surface-container-low flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-secondary animate-pulse"></span>
            <span className="font-telemetry-md text-on-surface">
              {unresolved.length} Duplicate Groups •{' '}
              <span className="text-secondary">
                {totalReclaimableMb.toFixed(1)} MB Reclaimable
              </span>
            </span>
          </div>
          <button
            onClick={() => {
              setIsScanning(true);
              setTimeout(() => setIsScanning(false), 800);
            }}
            className="px-2.5 py-1 rounded-full bg-surface-container-high text-primary font-telemetry-sm flex items-center gap-1"
          >
            <span
              className={`material-symbols-outlined text-[14px] ${
                isScanning ? 'animate-spin' : ''
              }`}
            >
              sync
            </span>
            {isScanning ? 'Scanning...' : 'Deep Scan'}
          </button>
        </div>

        <div className="p-5 space-y-3 overflow-y-auto no-scrollbar max-h-[520px]">
          {groups.map((group) => (
            <div
              key={group.id}
              className={`p-3.5 rounded-2xl border transition-all ${
                group.resolved
                  ? 'bg-surface-container-lowest/60 border-tertiary/30 opacity-65'
                  : 'bg-surface-container-low border-outline-variant/40'
              }`}
            >
              <div className="flex items-start justify-between gap-2 mb-2">
                <div>
                  <div className="flex items-center gap-1.5">
                    <span className="font-label-lg text-on-surface font-bold">
                      {group.title}
                    </span>
                    <span className="font-telemetry-sm px-1.5 py-0.5 rounded bg-surface-container-highest text-primary">
                      AcoustID {group.acoustIdHash}
                    </span>
                  </div>
                  <p className="font-body-sm text-on-surface-variant">
                    {group.artist}
                  </p>
                </div>
                {group.resolved ? (
                  <span className="px-2 py-0.5 rounded-full bg-tertiary/15 text-tertiary font-telemetry-sm font-bold">
                    Cleaned ✓
                  </span>
                ) : (
                  <button
                    onClick={() => onResolveGroup(group.id)}
                    className="px-2.5 py-1 rounded-lg bg-secondary/20 text-secondary font-telemetry-sm font-semibold hover:bg-secondary hover:text-on-secondary transition-colors"
                  >
                    Purge Dupes
                  </button>
                )}
              </div>

              {/* Master Retained File */}
              <div className="p-2 rounded-xl bg-surface-container-lowest mb-1.5 flex items-center justify-between text-xs">
                <div className="flex items-center gap-2 min-w-0 pr-2">
                  <span className="px-1.5 py-0.5 rounded bg-tertiary/20 text-tertiary font-telemetry-sm font-bold shrink-0">
                    KEEP MASTER
                  </span>
                  <span className="font-telemetry-sm text-on-surface truncate">
                    {group.masterFormat}
                  </span>
                </div>
                <span className="font-telemetry-sm text-on-surface-variant shrink-0">
                  {group.masterSizeMb} MB
                </span>
              </div>

              {/* Redundant Duplicate Candidates */}
              {group.candidates.map((cand) => (
                <div
                  key={cand.id}
                  className="p-2 rounded-xl bg-error-container/15 border border-error/20 mt-1 flex items-center justify-between text-xs"
                >
                  <div className="min-w-0 pr-2">
                    <div className="flex items-center gap-1.5">
                      <span className="px-1.5 py-0.2 rounded bg-error/20 text-error font-telemetry-sm font-bold">
                        DUPLICATE ({cand.acoustIdSimilarity}%)
                      </span>
                      <span className="font-telemetry-sm text-on-surface">
                        {cand.format}
                      </span>
                    </div>
                    <p className="font-telemetry-sm text-on-surface-variant truncate mt-0.5">
                      {cand.reason}
                    </p>
                  </div>
                  <span className="font-telemetry-sm text-error font-semibold shrink-0">
                    -{cand.sizeMb} MB
                  </span>
                </div>
              ))}
            </div>
          ))}
        </div>

        <div className="p-5 pt-3 bg-surface-container border-t border-outline-variant/30 flex items-center gap-2.5">
          <button
            onClick={onPurgeAll}
            disabled={unresolved.length === 0}
            className={`flex-1 h-12 rounded-full font-label-lg font-bold flex items-center justify-center gap-2 transition-all ${
              unresolved.length === 0
                ? 'bg-surface-container-high text-on-surface-variant'
                : 'bg-primary-container text-on-primary-container shadow-[0_0_20px_rgba(6,182,212,0.4)] active:scale-98'
            }`}
          >
            <span className="material-symbols-outlined text-[20px]">
              cleaning_services
            </span>
            <span>
              {unresolved.length === 0
                ? 'All Duplicates Purged (0 Dupes)'
                : `Purge All Duplicates (${totalReclaimableMb.toFixed(1)} MB)`}
            </span>
          </button>
        </div>
      </section>
    </div>
  );
};

/* ============================================================================
 * 10. PIXEL ASSISTANT VOICE OVERLAY ("Hey Google" / In-App Voice AI)
 * ========================================================================== */

interface PixelAssistantProps {
  isListening: boolean;
  transcript: string;
  onClose: () => void;
  onExecuteCommand: (command: string) => void;
}

export const PixelAssistantVoiceModal: React.FC<PixelAssistantProps> = ({
  isListening,
  transcript,
  onClose,
  onExecuteCommand,
}) => {
  return (
    <div className="absolute inset-0 z-50 flex flex-col justify-end bg-black/70 backdrop-blur-xs select-none">
      <section className="relative z-20 w-full bg-surface-container-high rounded-t-[28px] border-t border-outline-variant/30 shadow-2xl flex flex-col p-5 pb-7 animate-slide-up">
        {/* Pixel Gemini / Assistant Multi-Color Glowing Horizon */}
        <div className="w-full h-1 rounded-full mb-3 bg-gradient-to-r from-[#4285F4] via-[#EA4335] via-[#FBBC05] to-[#34A853] shadow-[0_0_12px_rgba(66,133,244,0.8)] animate-pulse" />

        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-primary/20 text-primary flex items-center justify-center">
              <span className="material-symbols-outlined text-[20px]">
                mic
              </span>
            </div>
            <div>
              <span className="font-label-lg text-white font-bold block">
                Pixel Voice Assistant
              </span>
              <span className="font-telemetry-sm text-primary">
                {isListening ? 'Listening on device...' : 'Voice Command Ready'}
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-on-surface-variant hover:text-white"
          >
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>

        {/* Live / Simulated Speech Waveform */}
        <div className="my-3 p-3.5 rounded-2xl bg-surface-container-lowest border border-outline-variant/30 flex flex-col items-center justify-center gap-2 text-center min-h-[80px]">
          {isListening ? (
            <div className="flex items-center gap-1 h-8">
              {[4, 10, 16, 24, 14, 20, 8, 18, 12, 6].map((h, i) => (
                <span
                  key={i}
                  style={{
                    height: `${h}px`,
                    animationDelay: `${i * 100}ms`,
                  }}
                  className="w-1 bg-gradient-to-t from-primary to-secondary rounded-full animate-pulse"
                />
              ))}
            </div>
          ) : (
            <span className="material-symbols-outlined text-primary text-[24px]">
              graphic_eq
            </span>
          )}
          <p className="font-headline-sm text-white text-[15px] font-semibold leading-snug">
            {transcript || '"Hey Google, cook a late night mix"'}
          </p>
        </div>

        {/* Quick Voice Command Action Chips */}
        <div className="space-y-1.5 pt-1">
          <span className="text-[10px] font-mono uppercase text-[#9ca3af] block px-1">
            Tap to test voice intents:
          </span>
          <div className="grid grid-cols-2 gap-1.5">
            {[
              'Cook late night lo-fi in the rain',
              'Tame 6kHz treble sibilance',
              'Turn on Dolby Atmos',
              'Exclusive Bit-Perfect USB',
              'Fast cardio >135 BPM',
              'Skip to next track',
            ].map((cmd) => (
              <button
                key={cmd}
                onClick={() => onExecuteCommand(cmd)}
                className="py-1.5 px-2.5 rounded-xl bg-surface-container hover:bg-surface-bright text-white text-[11px] font-mono text-left truncate active:scale-95 transition-all border border-outline-variant/30"
              >
                "{cmd}"
              </button>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
};

