import React, { useState } from 'react';
import {
  AddonPackage,
  CrateFolder,
  EQ_PRESETS,
  EqBand,
  ParsedCookRules,
  Track,
  UsbDacEndpoint,
} from '../data/vibeplayData';

/* ============================================================================
 * 1. CRATE VIEW (Overview Screen 1 + Full Indexed Track Stream Screen 2)
 * ========================================================================== */

interface CrateViewProps {
  tracks: Track[];
  crates: CrateFolder[];
  activeTrackId: string;
  isPlaying: boolean;
  crateSubMode: 'overview' | 'indexed';
  onChangeSubMode: (mode: 'overview' | 'indexed') => void;
  onSelectTrack: (track: Track) => void;
  onToggleLike: (id: string) => void;
  onOpenTrackActions: (track: Track) => void;
  onCreateCrate: (name: string, badge: string) => void;
  onShufflePlay: () => void;
  onAiHarmonicSort?: () => void;
  onShowToast: (msg: string) => void;
}

export const CrateView: React.FC<CrateViewProps> = ({
  tracks,
  crates,
  activeTrackId,
  isPlaying,
  crateSubMode,
  onChangeSubMode,
  onSelectTrack,
  onToggleLike,
  onOpenTrackActions,
  onCreateCrate,
  onShufflePlay,
  onAiHarmonicSort,
  onShowToast,
}) => {
  const [selectedVibePill, setSelectedVibePill] = useState<
    'all' | 'hires' | 'recent' | 'focus' | 'night'
  >('all');
  const [selectedCrateId, setSelectedCrateId] = useState<string | null>(null);
  const [sortMode, setSortMode] = useState<
    'energy' | 'title' | 'bpm' | 'hires' | 'recent' | 'camelot'
  >('energy');
  const [showNewCrateModal, setShowNewCrateModal] = useState(false);
  const [newCrateName, setNewCrateName] = useState('');
  const [multiSelectActive, setMultiSelectActive] = useState(false);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  // Filter tracks
  const filteredTracks = tracks.filter((t) => {
    if (selectedCrateId && t.crateId !== selectedCrateId) return false;
    if (selectedVibePill === 'all') return true;
    return t.vibeCategory.includes(selectedVibePill);
  });

  // Sort tracks
  const sortedTracks = [...filteredTracks].sort((a, b) => {
    if (sortMode === 'title') return a.title.localeCompare(b.title);
    if (sortMode === 'bpm') return b.bpm - a.bpm;
    if (sortMode === 'energy') return b.energy - a.energy;
    if (sortMode === 'hires') return b.bitrateKbps - a.bitrateKbps;
    if (sortMode === 'camelot') return a.camelotKey.localeCompare(b.camelotKey);
    return 0;
  });

  const heavyRotationTracks = tracks.filter((t) =>
    ['hyperion-nocturne', 'analog-horizons', 'vessel-in-the-deep', 'dead-on-arrival'].includes(
      t.id
    )
  );

  if (crateSubMode === 'indexed') {
    return (
      <div className="flex flex-col w-full select-none">
        {/* Sticky Quick-Sort Pill Carousel (Screen 2 / Image 3) */}
        <div className="sticky top-0 z-30 -mx-4 px-4 py-2 bg-surface/90 backdrop-blur-md">
          <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-0.5">
            <button
              onClick={() => onChangeSubMode('overview')}
              className="h-9 px-3 rounded-full flex items-center gap-1 bg-surface-container-high text-primary font-label-md shrink-0 active:scale-95"
            >
              <span className="material-symbols-outlined text-[16px]">
                grid_view
              </span>
              <span>Overview</span>
            </button>
            {(
              [
                ['title', 'Sort: Title', '🔤'],
                ['bpm', 'BPM', '⚡'],
                ['energy', 'Energy', '🔥'],
                ['hires', 'Hi-Res FLAC', '✨'],
                ['recent', 'Recent', ''],
                ['camelot', 'Keys / Camelot', '🎹'],
              ] as const
            ).map(([key, label, emoji]) => {
              const active = sortMode === key;
              return (
                <button
                  key={key}
                  onClick={() => {
                    setSortMode(key);
                    onShowToast(`Sorted by ${label}`);
                  }}
                  className={`h-9 px-3.5 rounded-full flex items-center gap-1.5 transition-colors shrink-0 active:scale-95 ${
                    active
                      ? 'bg-primary-container/20 text-primary-fixed shadow-[0_0_12px_rgba(6,182,212,0.3)] font-semibold'
                      : 'bg-surface-container text-on-surface hover:bg-surface-container-high'
                  }`}
                >
                  <span className="font-label-md">{label}</span>
                  {emoji && <span className="text-xs">{emoji}</span>}
                </button>
              );
            })}
          </div>
        </div>

        {/* Library Header & Telemetry Stats Bar */}
        <div className="flex items-center justify-between py-2.5 mt-1 mb-2 px-1">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-tertiary shadow-[0_0_8px_rgba(78,222,163,0.8)]"></div>
            <span className="font-telemetry-sm text-on-surface-variant uppercase tracking-wider font-semibold">
              428 Tracks in Crate • Lossless Synced
            </span>
          </div>
          <div className="flex items-center gap-1">
            <button
              aria-label="Shuffle Crate"
              onClick={onShufflePlay}
              className="w-9 h-9 rounded-full bg-surface-container flex items-center justify-center text-primary hover:bg-surface-container-high transition-transform active:scale-90 shadow-sm"
            >
              <span className="material-symbols-outlined text-[18px]">
                shuffle
              </span>
            </button>
            <button
              aria-label="Multi-select"
              onClick={() => {
                setMultiSelectActive((m) => !m);
                setSelectedIds([]);
              }}
              className={`w-9 h-9 rounded-full flex items-center justify-center transition-colors active:scale-90 ${
                multiSelectActive
                  ? 'bg-primary text-on-primary'
                  : 'bg-surface-container text-on-surface-variant hover:text-on-surface'
              }`}
            >
              <span className="material-symbols-outlined text-[18px]">
                checklist
              </span>
            </button>
          </div>
        </div>

        {/* Track Cards Stream */}
        <div className="flex flex-col gap-2 w-full pb-6">
          {sortedTracks.map((track) => {
            const isCurrent = track.id === activeTrackId;
            const isChecked = selectedIds.includes(track.id);

            return (
              <div
                key={track.id}
                onClick={() => {
                  if (multiSelectActive) {
                    setSelectedIds((prev) =>
                      prev.includes(track.id)
                        ? prev.filter((i) => i !== track.id)
                        : [...prev, track.id]
                    );
                  } else {
                    onSelectTrack(track);
                  }
                }}
                className={`relative w-full rounded-2xl overflow-hidden transition-all group active:scale-[0.99] cursor-pointer ${
                  isCurrent
                    ? 'bg-surface-container/90 shadow-lg'
                    : 'bg-surface-container/60 hover:bg-surface-container'
                }`}
              >
                {isCurrent && (
                  <div className="absolute left-0 top-0 bottom-0 w-1 bg-primary shadow-[0_0_12px_rgba(6,182,212,0.9)]"></div>
                )}
                <div
                  className={`h-[72px] px-3.5 py-2.5 flex items-center justify-between gap-3 ${
                    isCurrent ? 'bg-primary-container/10' : ''
                  }`}
                >
                  {/* Thumbnail + Equalizer Animation */}
                  <div className="relative w-[52px] h-[52px] rounded-xl overflow-hidden shrink-0 bg-surface-container-lowest shadow-inner">
                    <img
                      src={track.artworkUrl}
                      alt={track.title}
                      referrerPolicy="no-referrer"
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    {isCurrent && (
                      <div className="absolute inset-0 bg-surface-dim/60 backdrop-blur-[1px] flex items-center justify-center gap-[3px]">
                        <span
                          className={`w-[3px] h-3.5 bg-primary rounded-full ${
                            isPlaying ? 'animate-bounce' : ''
                          }`}
                          style={{ animationDelay: '0.1s' }}
                        ></span>
                        <span
                          className={`w-[3px] h-5 bg-primary rounded-full ${
                            isPlaying ? 'animate-bounce' : ''
                          }`}
                          style={{ animationDelay: '0.25s' }}
                        ></span>
                        <span
                          className={`w-[3px] h-2.5 bg-primary rounded-full ${
                            isPlaying ? 'animate-bounce' : ''
                          }`}
                          style={{ animationDelay: '0.15s' }}
                        ></span>
                        <span
                          className={`w-[3px] h-4 bg-primary rounded-full ${
                            isPlaying ? 'animate-bounce' : ''
                          }`}
                          style={{ animationDelay: '0.3s' }}
                        ></span>
                      </div>
                    )}
                  </div>

                  {/* Center Metadata */}
                  <div className="flex flex-col min-w-0 flex-1 justify-center">
                    <div className="flex items-center gap-1.5">
                      <span
                        className={`font-headline-sm text-body-lg truncate ${
                          isCurrent
                            ? 'text-primary font-bold'
                            : 'text-on-surface font-semibold'
                        }`}
                      >
                        {track.title}
                      </span>
                      {isCurrent && (
                        <span className="shrink-0 w-1.5 h-1.5 rounded-full bg-primary animate-ping"></span>
                      )}
                    </div>
                    <span className="font-body-sm text-on-surface-variant truncate">
                      {track.artist}
                    </span>
                    <div className="flex items-center gap-1.5 mt-1">
                      <span
                        className={`font-telemetry-sm px-1.5 py-0.5 rounded font-medium tracking-tight ${
                          isCurrent
                            ? 'bg-secondary/15 text-secondary'
                            : 'bg-surface-container-high text-secondary-fixed-dim'
                        }`}
                      >
                        {track.bpm} BPM ⚡
                      </span>
                      <span className="font-telemetry-sm px-1.5 py-0.5 rounded bg-surface-container-lowest text-primary tracking-wide font-medium">
                        {track.formatBadge} ✨
                      </span>
                      {sortMode === 'camelot' && (
                        <span className="font-telemetry-sm px-1.5 py-0.5 rounded bg-tertiary/15 text-tertiary font-bold">
                          {track.camelotKey}
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Right Side: 5-Segment Energy Mini-Bar & More Action */}
                  <div className="flex items-center gap-1 shrink-0">
                    {multiSelectActive ? (
                      <div
                        className={`w-6 h-6 rounded-full flex items-center justify-center border ${
                          isChecked
                            ? 'bg-primary border-primary text-on-primary'
                            : 'border-outline'
                        }`}
                      >
                        {isChecked && (
                          <span className="material-symbols-outlined text-[15px]">
                            check
                          </span>
                        )}
                      </div>
                    ) : (
                      <>
                        <div
                          className="flex flex-col gap-[2px] items-center px-1"
                          title={`Energy Level ${track.energyBars}/5`}
                        >
                          {[5, 4, 3, 2, 1].map((seg) => (
                            <div
                              key={seg}
                              className={`w-3.5 h-[3px] rounded-full ${
                                seg <= track.energyBars
                                  ? 'bg-primary shadow-[0_0_6px_rgba(6,182,212,0.8)]'
                                  : 'bg-surface-bright/40'
                              }`}
                            />
                          ))}
                        </div>
                        <button
                          aria-label="Track options"
                          onClick={(e) => {
                            e.stopPropagation();
                            onOpenTrackActions(track);
                          }}
                          className="w-10 h-10 rounded-full flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high transition-colors"
                        >
                          <span className="material-symbols-outlined text-[20px]">
                            more_vert
                          </span>
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="flex flex-col items-center justify-center py-4 gap-2 opacity-60">
          <div className="w-1.5 h-1.5 rounded-full bg-surface-bright"></div>
          <span className="font-telemetry-sm text-on-surface-variant tracking-widest uppercase">
            ALL 428 TRACKS INDEXED
          </span>
        </div>
      </div>
    );
  }

  // Default Overview Mode (Screen 1 / Image 1)
  return (
    <div className="flex flex-col w-full pb-4 select-none">
      {/* Pull-to-refresh subtle visual indicator */}
      <div className="flex items-center justify-between py-1 text-on-surface-variant/80">
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined text-[16px] animate-spin text-primary">
            sync
          </span>
          <span className="font-telemetry-sm tracking-widest uppercase">
            Crate sync: verified 4.2 MB/s
          </span>
        </div>
        <button
          onClick={() => onChangeSubMode('indexed')}
          className="font-telemetry-sm text-primary hover:underline flex items-center gap-0.5"
        >
          <span>Full Stream (428)</span>
          <span className="material-symbols-outlined text-[14px]">
            chevron_right
          </span>
        </button>
      </div>

      {/* Hero Quick Vibe Switcher */}
      <div className="w-full mt-2 mb-4 overflow-x-auto no-scrollbar py-1">
        <div className="flex items-center gap-2 px-0.5" role="tablist">
          {(
            [
              ['all', 'All Tracks', 'library_music', ''],
              ['hires', 'Hi-Res (FLAC)', 'graphic_eq', 'text-tertiary'],
              ['recent', 'Recent Crates', 'inventory_2', 'text-secondary'],
              ['focus', 'Deep Focus', 'psychology', ''],
              ['night', 'Late Night Drive', 'nightlife', ''],
            ] as const
          ).map(([id, label, icon, iconTint]) => {
            const active = selectedVibePill === id && !selectedCrateId;
            return (
              <button
                key={id}
                role="tab"
                aria-selected={active}
                onClick={() => {
                  setSelectedVibePill(id);
                  setSelectedCrateId(null);
                  onShowToast(`Filter set: ${label}`);
                }}
                className={`shrink-0 h-11 px-4 rounded-full font-label-lg flex items-center gap-1.5 transition-all ${
                  active
                    ? 'bg-primary-container text-on-primary-container shadow-[0_0_16px_rgba(6,182,212,0.35)]'
                    : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
                }`}
              >
                <span
                  className={`material-symbols-outlined text-[18px] ${
                    active ? '' : iconTint
                  }`}
                >
                  {icon}
                </span>
                <span>{label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Section: Heavy Rotation */}
      <section className="flex flex-col w-full mb-6">
        <div className="flex items-center justify-between px-0.5 mb-2">
          <div className="flex items-center gap-2">
            <span className="font-headline-sm text-on-surface tracking-tight">
              Heavy Rotation
            </span>
            <span className="font-telemetry-sm px-2 py-0.5 rounded-full bg-surface-container-high text-primary tracking-wide">
              32 READY
            </span>
          </div>
          <button
            onClick={onShufflePlay}
            className="h-9 px-3 rounded-full bg-surface-container hover:bg-surface-container-high flex items-center gap-1 text-on-surface-variant hover:text-on-surface transition-all"
          >
            <span className="material-symbols-outlined text-[18px] text-secondary">
              shuffle
            </span>
            <span className="font-label-md">Mix</span>
          </button>
        </div>

        <div className="w-full overflow-x-auto no-scrollbar py-1 -mx-4 px-4">
          <div className="flex gap-3.5 w-max">
            {heavyRotationTracks.map((track, idx) => (
              <article
                key={track.id}
                onClick={() => onSelectTrack(track)}
                className="w-68 rounded-[20px] bg-surface-container p-3.5 shadow-[0_8px_24px_rgba(0,0,0,0.45)] flex flex-col relative group transition-transform active:scale-[0.98] cursor-pointer"
              >
                <div className="relative w-full aspect-square rounded-[16px] overflow-hidden bg-surface-container-low mb-3">
                  <img
                    src={track.artworkUrl}
                    alt={track.title}
                    referrerPolicy="no-referrer"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-surface-container-lowest/90 via-transparent to-transparent"></div>
                  <div
                    className={`absolute top-2.5 right-2.5 flex items-center gap-1 px-2 py-1 rounded-full bg-surface-container-lowest/80 backdrop-blur-md shadow-sm ${
                      idx === 1 ? 'text-secondary' : 'text-tertiary'
                    }`}
                  >
                    <span
                      className={`w-1.5 h-1.5 rounded-full ${
                        idx === 1
                          ? 'bg-secondary'
                          : 'bg-tertiary animate-pulse'
                      }`}
                    ></span>
                    <span className="font-telemetry-sm font-semibold">
                      {track.formatBadge}
                    </span>
                  </div>
                  <div className="absolute bottom-2.5 left-2.5 px-2 py-0.5 rounded-full bg-surface-container-lowest/70 backdrop-blur-md text-on-surface-variant font-telemetry-sm">
                    {track.bpm} BPM • {track.camelotKey}
                  </div>
                  <button
                    aria-label="Play Track"
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectTrack(track);
                    }}
                    className={`absolute bottom-2.5 right-2.5 w-12 h-12 rounded-full flex items-center justify-center transition-transform hover:scale-105 active:scale-95 ${
                      idx === 0 || track.id === activeTrackId
                        ? 'bg-primary-container text-on-primary-container shadow-[0_0_16px_rgba(6,182,212,0.45)]'
                        : 'bg-surface-container-high text-on-surface hover:bg-primary-container hover:text-on-primary-container'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[24px]">
                      {track.id === activeTrackId && isPlaying
                        ? 'pause'
                        : 'play_arrow'}
                    </span>
                  </button>
                </div>

                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <h3 className="font-headline-sm text-on-surface truncate">
                      {track.title}
                    </h3>
                    <p className="font-body-sm text-on-surface-variant truncate">
                      {track.artist}
                    </p>
                  </div>
                  <button
                    aria-label="Favorite"
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleLike(track.id);
                    }}
                    className={`w-10 h-10 -mr-1 flex items-center justify-center rounded-full transition-colors ${
                      track.liked
                        ? 'text-secondary'
                        : 'text-on-surface-variant hover:text-secondary'
                    }`}
                  >
                    <span
                      className="material-symbols-outlined text-[20px]"
                      style={{
                        fontVariationSettings: track.liked
                          ? "'FILL' 1"
                          : "'FILL' 0",
                      }}
                    >
                      {track.liked ? 'favorite' : 'favorite_border'}
                    </span>
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* Section: Active Crates (2-Column Bento Grid) */}
      <section className="flex flex-col w-full mb-6">
        <div className="flex items-center justify-between px-0.5 mb-2">
          <div className="flex items-center gap-2">
            <span className="font-headline-sm text-on-surface tracking-tight">
              Active Crates
            </span>
            <span className="font-telemetry-sm text-on-surface-variant">
              {crates.length} FOLDERS
            </span>
          </div>
          <button
            aria-label="New Crate"
            onClick={() => setShowNewCrateModal((s) => !s)}
            className="w-10 h-10 rounded-full bg-surface-container hover:bg-surface-container-high flex items-center justify-center text-primary transition-colors"
          >
            <span className="material-symbols-outlined text-[20px]">
              create_new_folder
            </span>
          </button>
        </div>

        {showNewCrateModal && (
          <div className="mb-3 p-3 rounded-2xl bg-surface-container-high flex items-center gap-2">
            <input
              type="text"
              value={newCrateName}
              onChange={(e) => setNewCrateName(e.target.value)}
              placeholder="New Crate Name (e.g. Late Night Synth)..."
              className="flex-1 h-9 px-3 rounded-xl bg-surface-container-lowest text-on-surface font-body-sm focus:outline-none"
            />
            <button
              onClick={() => {
                if (newCrateName.trim()) {
                  onCreateCrate(newCrateName.trim(), '24/96');
                  setNewCrateName('');
                  setShowNewCrateModal(false);
                }
              }}
              className="h-9 px-3.5 rounded-xl bg-primary-container text-on-primary-container font-label-md font-bold"
            >
              Add
            </button>
          </div>
        )}

        <div className="grid grid-cols-2 gap-2.5 w-full">
          {crates.map((crate) => {
            const isSelected = selectedCrateId === crate.id;
            return (
              <div
                key={crate.id}
                onClick={() => {
                  setSelectedCrateId(isSelected ? null : crate.id);
                  onShowToast(
                    isSelected
                      ? 'Showing all crates'
                      : `Opened crate: ${crate.name}`
                  );
                }}
                className={`rounded-[20px] p-3 flex flex-col justify-between transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-surface-container-high ring-1 ring-primary'
                    : 'bg-surface-container hover:bg-surface-container-high/70'
                }`}
              >
                <div className="flex items-start justify-between mb-2">
                  <div
                    className={`w-10 h-10 rounded-[14px] flex items-center justify-center ${crate.iconBg}`}
                  >
                    <span className="material-symbols-outlined text-[22px]">
                      {crate.icon}
                    </span>
                  </div>
                  <span
                    className={`font-telemetry-sm px-1.5 py-0.5 rounded bg-surface-container-lowest ${
                      crate.badgeColor === 'tertiary'
                        ? 'text-tertiary'
                        : crate.badgeColor === 'secondary'
                        ? 'text-secondary'
                        : crate.badgeColor === 'primary'
                        ? 'text-primary'
                        : 'text-on-surface-variant'
                    }`}
                  >
                    {crate.badge}
                  </span>
                </div>
                <div>
                  <h4 className="font-label-lg text-on-surface truncate font-semibold">
                    {crate.name}
                  </h4>
                  <p className="font-body-sm text-on-surface-variant mt-0.5">
                    {crate.subtitle}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Section: Audio Stash */}
      <section className="flex flex-col w-full">
        <div className="flex items-center justify-between px-0.5 mb-2">
          <div className="flex items-center gap-2">
            <span className="font-headline-sm text-on-surface tracking-tight">
              Audio Stash
            </span>
            <span className="font-telemetry-sm text-on-surface-variant">
              1,248 TRACKS
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              onClick={() => {
                const next =
                  sortMode === 'hires'
                    ? 'bpm'
                    : sortMode === 'bpm'
                    ? 'title'
                    : 'hires';
                setSortMode(next);
                onShowToast(
                  `Sort: ${
                    next === 'hires'
                      ? 'Bitrate'
                      : next === 'bpm'
                      ? 'BPM'
                      : 'Title'
                  }`
                );
              }}
              className="h-9 px-3 rounded-full bg-surface-container hover:bg-surface-container-high flex items-center gap-1 text-on-surface-variant transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">
                swap_vert
              </span>
              <span className="font-label-md">
                {sortMode === 'bpm'
                  ? 'BPM'
                  : sortMode === 'title'
                  ? 'Title'
                  : 'Bitrate'}
              </span>
            </button>
          </div>
        </div>

        <div className="flex flex-col gap-1.5 w-full">
          {sortedTracks.map((track) => {
            const isCurrent = track.id === activeTrackId;
            return (
              <div
                key={track.id}
                onClick={() => onSelectTrack(track)}
                className={`flex items-center justify-between p-2 rounded-[20px] transition-colors group cursor-pointer ${
                  isCurrent
                    ? 'bg-surface-container'
                    : 'bg-surface-container/60 hover:bg-surface-container-high'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0 flex-1">
                  <div className="relative w-12 h-12 rounded-[14px] overflow-hidden bg-surface-container-lowest shrink-0 flex items-center justify-center">
                    {isCurrent ? (
                      <>
                        <div className="w-full h-full bg-primary/20 flex items-center justify-center">
                          <span className="material-symbols-outlined text-primary text-[24px]">
                            graphic_eq
                          </span>
                        </div>
                        <div className="absolute bottom-1 right-1 w-2 h-2 rounded-full bg-primary animate-ping"></div>
                      </>
                    ) : (
                      <img
                        src={track.artworkUrl}
                        alt={track.title}
                        referrerPolicy="no-referrer"
                        className="w-full h-full object-cover opacity-90"
                      />
                    )}
                  </div>

                  <div className="flex flex-col min-w-0 pr-2">
                    <div className="flex items-center gap-1.5">
                      <span
                        className={`font-label-lg truncate ${
                          isCurrent
                            ? 'text-primary font-bold'
                            : 'text-on-surface font-semibold'
                        }`}
                      >
                        {track.title}
                      </span>
                      <span
                        className={`font-telemetry-sm px-1 rounded bg-surface-container-lowest shrink-0 ${
                          track.shortFormat.includes('DSD')
                            ? 'text-secondary'
                            : 'text-tertiary'
                        }`}
                      >
                        {track.shortFormat}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-body-sm text-on-surface-variant truncate">
                        {track.artist}
                      </span>
                      <span className="font-telemetry-sm text-on-surface-variant/60">
                        {track.durationLabel}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-0.5 shrink-0">
                  <button
                    aria-label={`Favorite ${track.title}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleLike(track.id);
                    }}
                    className={`w-11 h-11 flex items-center justify-center rounded-full transition-colors ${
                      track.liked
                        ? 'text-secondary'
                        : 'text-on-surface-variant hover:text-secondary'
                    }`}
                  >
                    <span
                      className="material-symbols-outlined text-[20px]"
                      style={{
                        fontVariationSettings: track.liked
                          ? "'FILL' 1"
                          : "'FILL' 0",
                      }}
                    >
                      {track.liked ? 'favorite' : 'favorite_border'}
                    </span>
                  </button>
                  <button
                    aria-label="Track Context Menu"
                    onClick={(e) => {
                      e.stopPropagation();
                      onOpenTrackActions(track);
                    }}
                    className="w-11 h-11 flex items-center justify-center rounded-full text-on-surface-variant hover:text-on-surface transition-colors"
                  >
                    <span className="material-symbols-outlined text-[20px]">
                      more_vert
                    </span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
};

/* ============================================================================
 * 2. VIBE COOKER VIEW (Screen 4 + 4-Step Guided Smart Playlist Builder + Optional AI)
 * ========================================================================== */

interface VibeCookerViewProps {
  tracks: Track[];
  prompt: string;
  onChangePrompt: (p: string) => void;
  cookedMixtape: ParsedCookRules;
  seedTrackId: string | undefined;
  onSelectSeedTrack: (id: string | undefined) => void;
  energyArc: 'steady' | 'ramp' | 'peak' | 'chill';
  onChangeEnergyArc: (arc: 'steady' | 'ramp' | 'peak' | 'chill') => void;
  optionalAiKey: string;
  onChangeOptionalAiKey: (key: string) => void;
  onSpinMixtape: () => void;
  onSaveMixtapeToCrate: () => void;
  onSelectTrack: (track: Track) => void;
  onShowToast: (msg: string) => void;
}

export const VibeCookerView: React.FC<VibeCookerViewProps> = ({
  tracks,
  prompt,
  onChangePrompt,
  cookedMixtape,
  seedTrackId,
  onSelectSeedTrack,
  energyArc,
  onChangeEnergyArc,
  optionalAiKey,
  onChangeOptionalAiKey,
  onSpinMixtape,
  onSaveMixtapeToCrate,
  onSelectTrack,
  onShowToast,
}) => {
  const [cookingStatus, setCookingStatus] = useState<'idle' | 'cooking' | 'done'>('idle');
  const [savedStatus, setSavedStatus] = useState(false);
  const [micActive, setMicActive] = useState(false);
  const [showGuidedBuilder, setShowGuidedBuilder] = useState(false);
  const [showAiConfig, setShowAiConfig] = useState(false);

  const handleCook = () => {
    setCookingStatus('cooking');
    setTimeout(() => {
      setCookingStatus('done');
      onShowToast(
        optionalAiKey.trim()
          ? `AI + Camelot Engine cooked "${cookedMixtape.mixtapeTitle}"`
          : `Local Heuristic Engine cooked "${cookedMixtape.mixtapeTitle}"`
      );
      setTimeout(() => setCookingStatus('idle'), 1400);
    }, 650);
  };

  return (
    <div className="flex flex-col w-full pb-6 select-none">
      {/* Top App Header */}
      <section className="flex flex-col gap-1 mb-3.5">
        <div className="flex items-center justify-between">
          <h1 className="font-headline-md text-on-surface tracking-tight">
            Vibe Cooker 🍳
          </h1>
          <button
            type="button"
            onClick={() => setShowAiConfig((s) => !s)}
            className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-container-high shadow-sm hover:border border-primary/40"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-tertiary shadow-[0_0_6px_rgba(78,222,163,0.8)] animate-pulse"></span>
            <span className="font-telemetry-sm text-tertiary uppercase tracking-wider font-semibold">
              {optionalAiKey.trim() ? 'AI + Local Neural ⚡' : 'Local Neural ⚡'}
            </span>
          </button>
        </div>
        <div className="flex items-center justify-between">
          <p className="font-body-sm text-on-surface-variant">
            Conversational Heuristic &amp; Camelot Playlist Engine
          </p>
          <button
            type="button"
            onClick={() => setShowGuidedBuilder((g) => !g)}
            className="font-telemetry-sm text-primary hover:underline"
          >
            {showGuidedBuilder ? 'Hide 4-Step Builder' : '4-Step Harmonic Builder'}
          </button>
        </div>
      </section>

      {/* Optional AI API Key Input Panel (per user request: Local, with an API key input field for optional AI) */}
      {showAiConfig && (
        <section className="bg-surface-container p-3.5 rounded-2xl mb-3.5 border border-outline-variant/40 space-y-2">
          <div className="flex items-center justify-between">
            <span className="font-label-md text-on-surface font-semibold">
              Engine Mode: Local Heuristic + Optional AI Key
            </span>
            <span className="font-telemetry-sm text-tertiary">
              100% Offline Default
            </span>
          </div>
          <p className="font-body-sm text-on-surface-variant">
            Vibe Cooker runs locally via Essentia/Camelot heuristics. Optionally enter an API key below for cloud LLM prompt expansion:
          </p>
          <div className="flex items-center gap-2">
            <input
              type="password"
              value={optionalAiKey}
              onChange={(e) => onChangeOptionalAiKey(e.target.value)}
              placeholder="Optional AI API Key (leave blank for Local Neural)..."
              className="flex-1 h-9 px-3 rounded-xl bg-surface-container-lowest text-on-surface font-telemetry-sm focus:outline-none border border-outline-variant/40 focus:border-primary"
            />
            <button
              type="button"
              onClick={() => {
                setShowAiConfig(false);
                onShowToast(
                  optionalAiKey.trim()
                    ? 'Optional AI Key saved for Vibe Cooker'
                    : 'Using Local Heuristic Engine (Offline)'
                );
              }}
              className="h-9 px-3 rounded-xl bg-primary-container text-on-primary-container font-label-md font-bold"
            >
              Apply
            </button>
          </div>
        </section>
      )}

      {/* 4-Step Guided Smart Playlist Builder (ported from defiqantcoder/vibeplay) */}
      {showGuidedBuilder && (
        <section className="bg-surface-container p-3.5 rounded-2xl mb-3.5 border border-primary/30 space-y-3">
          <div className="flex items-center justify-between">
            <span className="font-telemetry-sm text-primary uppercase font-bold">
              4-Step Guided Harmonic Architect (vibeplay core)
            </span>
            <span className="font-telemetry-sm text-on-surface-variant">
              Camelot Wheel Sync
            </span>
          </div>

          {/* Step 1: Seed Track Picker */}
          <div>
            <label className="font-telemetry-sm text-on-surface-variant block mb-1">
              1. Seed Track (Camelot Harmonic Anchor):
            </label>
            <select
              value={seedTrackId || ''}
              onChange={(e) =>
                onSelectSeedTrack(e.target.value ? e.target.value : undefined)
              }
              className="w-full h-9 px-3 rounded-xl bg-surface-container-lowest text-on-surface font-body-sm border border-outline-variant/40"
            >
              <option value="">No Seed Track (Freeform Heuristic)</option>
              {tracks.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.title} — {t.artist} [{t.camelotKey} • {t.bpm} BPM]
                </option>
              ))}
            </select>
          </div>

          {/* Step 2: Energy Curve Trajectory */}
          <div>
            <label className="font-telemetry-sm text-on-surface-variant block mb-1">
              2. Energy Trajectory Arc:
            </label>
            <div className="grid grid-cols-2 gap-1.5">
              {(
                [
                  ['chill', '🌙 Late Night Chill'],
                  ['steady', '📏 Steady Pulse'],
                  ['ramp', '📈 Energy Ramp'],
                  ['peak', '🔥 Peak-Hour Wave'],
                ] as const
              ).map(([arcKey, arcLabel]) => (
                <button
                  key={arcKey}
                  type="button"
                  onClick={() => onChangeEnergyArc(arcKey)}
                  className={`py-1.5 px-2.5 rounded-xl font-telemetry-sm text-left transition-colors ${
                    energyArc === arcKey
                      ? 'bg-primary-container text-on-primary-container font-bold'
                      : 'bg-surface-container-lowest text-on-surface-variant'
                  }`}
                >
                  {arcLabel}
                </button>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Conversational Input Card */}
      <section className="bg-surface-container-low rounded-[24px] p-4 mb-4 shadow-[0_8px_24px_rgba(0,0,0,0.35)] relative overflow-hidden">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <span className="text-secondary text-base">🪄</span>
            <h2 className="font-headline-sm text-on-surface">
              Cook Up a Mixtape
            </h2>
          </div>
          <span className="font-telemetry-sm text-primary px-2 py-0.5 rounded-full bg-primary/10">
            v2.4 Core
          </span>
        </div>

        <div className="relative bg-surface-container-lowest rounded-2xl p-3 mb-3.5 focus-within:shadow-[0_0_12px_rgba(6,182,212,0.25)] transition-all">
          <textarea
            value={prompt}
            onChange={(e) => onChangePrompt(e.target.value)}
            rows={3}
            placeholder="Describe a scene, feeling, tempo, or acoustics..."
            className="w-full bg-transparent text-on-surface font-body-md placeholder:text-on-surface-variant/60 focus:outline-none resize-none pr-12"
          />
          <button
            aria-label="Voice input"
            type="button"
            onClick={() => {
              setMicActive((m) => !m);
              if (!micActive) {
                onShowToast('Listening for voice vibe prompt...');
                setTimeout(() => {
                  setMicActive(false);
                }, 1800);
              }
            }}
            className={`absolute right-2.5 bottom-2.5 w-10 h-10 rounded-full flex items-center justify-center active:scale-90 transition-transform shadow-md ${
              micActive
                ? 'bg-error-container text-error animate-pulse'
                : 'bg-surface-container text-primary'
            }`}
          >
            <span className="material-symbols-outlined text-[20px]">mic</span>
          </button>
        </div>

        <button
          type="button"
          onClick={handleCook}
          className="w-full h-[50px] rounded-full bg-primary-container text-on-primary-container font-headline-sm flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(6,182,212,0.4)] active:scale-[0.98] transition-transform"
        >
          <span
            className={`material-symbols-outlined text-[22px] ${
              cookingStatus === 'cooking' ? 'animate-spin' : ''
            }`}
          >
            {cookingStatus === 'cooking'
              ? 'refresh'
              : cookingStatus === 'done'
              ? 'done_all'
              : 'auto_fix_high'}
          </span>
          <span>
            {cookingStatus === 'cooking'
              ? 'Cooking Mixtape...'
              : cookingStatus === 'done'
              ? 'Freshly Brewed!'
              : 'Stir the Pot / Cook'}
          </span>
        </button>
      </section>

      {/* Quick-Vibe Preset Carousel */}
      <section className="mb-4">
        <div className="flex items-center justify-between mb-2 px-0.5">
          <span className="font-telemetry-sm text-on-surface-variant uppercase tracking-wider">
            Quick Influx Presets
          </span>
          <span className="font-telemetry-sm text-primary-fixed-dim">
            Swipe →
          </span>
        </div>
        <div className="flex gap-2 overflow-x-auto pb-1.5 no-scrollbar -mx-4 px-4">
          {[
            {
              emoji: '☕',
              label: 'Morning Coffee Lo-Fi',
              text: 'Chilled lo-fi beats, gentle Rhodes piano, vinyl crackle, around 80 BPM for sunny morning coffee...',
            },
            {
              emoji: '🔥',
              label: 'Fast Cardio >130',
              text: 'High cadence industrial techno & drum-and-bass, punching 135+ BPM, aggressive transient response!',
            },
            {
              emoji: '🌙',
              label: 'Late Night Emo',
              text: 'Late night downtempo with warm analog tape hiss, under 95 BPM for deep focus writing in the rain...',
            },
            {
              emoji: '⚡',
              label: 'Pop-Punk Blast',
              text: 'Upbeat 2000s energetic pop-punk drums, distorted power chords, high dynamic punch, 140 BPM...',
            },
            {
              emoji: '🌧️',
              label: 'Rainy Ambient DSD',
              text: 'Pure generative ambient field recordings of gentle rain, modular drones, DSD pristine fidelity...',
            },
          ].map((preset) => (
            <button
              key={preset.label}
              type="button"
              onClick={() => {
                onChangePrompt(preset.text);
                onShowToast(`Preset loaded: ${preset.label}`);
              }}
              className="flex items-center gap-1.5 px-3.5 h-9 rounded-full bg-surface-container text-on-surface font-label-md whitespace-nowrap active:bg-primary-container/20 active:text-primary transition-all shrink-0 shadow-sm"
            >
              <span>{preset.emoji}</span>
              <span>{preset.label}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Extracted Rule Badges */}
      <section className="bg-surface-container-low rounded-[20px] p-3.5 mb-4 shadow-sm">
        <div className="flex items-center justify-between mb-2.5">
          <span className="font-telemetry-sm text-on-surface-variant uppercase tracking-wider font-semibold">
            Cook Parameters Extracted (4 Rules)
          </span>
          <span className="material-symbols-outlined text-primary text-[16px]">
            tune
          </span>
        </div>
        <div className="flex flex-wrap gap-1.5">
          <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-secondary-container/20 text-secondary font-telemetry-sm font-semibold">
            <span>🐢</span>
            <span>{cookedMixtape.bpmBadge}</span>
          </div>
          <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-primary/15 text-primary font-telemetry-sm font-semibold">
            <span>🛋️</span>
            <span>{cookedMixtape.energyRule}</span>
          </div>
          <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-primary/15 text-primary font-telemetry-sm font-semibold">
            <span>☁️</span>
            <span>{cookedMixtape.moodBadge}</span>
          </div>
          <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-tertiary-container/20 text-tertiary font-telemetry-sm font-semibold">
            <span>🎧</span>
            <span>{cookedMixtape.formatBadge}</span>
          </div>
        </div>
      </section>

      {/* Matching Mixtape Queue */}
      <section className="flex flex-col gap-2.5 mb-5">
        <div className="flex items-center justify-between px-0.5">
          <div className="flex flex-col min-w-0 pr-2">
            <h3 className="font-headline-sm text-on-surface truncate">
              {cookedMixtape.mixtapeTitle}
            </h3>
            <span className="font-body-sm text-on-surface-variant">
              Simulated 14 tracks heuristically sorted ({cookedMixtape.arcName})
            </span>
          </div>
          <span className="font-telemetry-sm text-primary px-2.5 py-1 rounded-full bg-surface-container font-semibold whitespace-nowrap shrink-0">
            14 Tracks • 48m
          </span>
        </div>

        <div className="flex flex-col gap-2">
          {cookedMixtape.scoredTracks.slice(0, 6).map(({ track, matchPct, harmonicNote }) => (
            <article
              key={track.id}
              onClick={() => onSelectTrack(track)}
              className="h-[76px] px-3 rounded-[20px] bg-surface-container-low flex items-center justify-between gap-3 shadow-[0_4px_16px_rgba(0,0,0,0.25)] hover:bg-surface-container transition-colors cursor-pointer"
            >
              <div className="flex items-center gap-3 min-w-0 flex-1">
                <div className="relative w-12 h-12 rounded-2xl overflow-hidden bg-surface-container-high shrink-0 flex items-center justify-center shadow-inner">
                  <img
                    src={track.artworkUrl}
                    alt={track.title}
                    referrerPolicy="no-referrer"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-primary-container/10 flex items-center justify-center">
                    <span className="material-symbols-outlined text-primary text-[20px] drop-shadow">
                      waves
                    </span>
                  </div>
                </div>
                <div className="flex flex-col min-w-0">
                  <div className="flex items-center gap-1.5">
                    <h4 className="font-label-lg text-on-surface truncate">
                      {track.title}
                    </h4>
                    <span className="font-telemetry-sm text-secondary font-bold">
                      {matchPct}%
                    </span>
                  </div>
                  <p className="font-body-sm text-on-surface-variant truncate">
                    {track.artist}
                  </p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="font-telemetry-sm text-secondary">
                      {track.bpm} BPM
                    </span>
                    <span className="font-telemetry-sm text-tertiary">
                      {track.formatBadge}
                    </span>
                    <span className="font-telemetry-sm text-on-surface-variant truncate">
                      {track.moodLabel} {track.moodEmoji} • {harmonicNote}
                    </span>
                  </div>
                </div>
              </div>
              <button
                aria-label={`Play track ${track.title}`}
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onSelectTrack(track);
                }}
                className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-on-surface hover:text-primary active:scale-90 transition-transform shrink-0"
              >
                <span className="material-symbols-outlined text-[20px]">
                  play_arrow
                </span>
              </button>
            </article>
          ))}
        </div>
      </section>

      {/* Action Sticky Footer Bar */}
      <section className="sticky bottom-0 z-30 pt-2 pb-1 bg-surface/90 backdrop-blur-md -mx-4 px-4">
        <div className="flex items-center gap-2.5 w-full">
          <button
            type="button"
            onClick={onSpinMixtape}
            className="flex-1 h-12 rounded-full bg-primary-container text-on-primary-container font-headline-sm flex items-center justify-center gap-2 shadow-[0_0_18px_rgba(6,182,212,0.4)] active:scale-[0.98] transition-transform"
          >
            <span
              className="material-symbols-outlined text-[22px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              play_circle
            </span>
            <span>Spin Mixtape</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setSavedStatus(true);
              onSaveMixtapeToCrate();
              setTimeout(() => setSavedStatus(false), 1800);
            }}
            className="h-12 px-5 rounded-full bg-surface-container text-on-surface font-label-lg flex items-center justify-center gap-1.5 shadow-sm active:bg-surface-container-high transition-colors"
          >
            <span
              className={`material-symbols-outlined text-[18px] ${
                savedStatus ? 'text-tertiary' : ''
              }`}
            >
              {savedStatus ? 'check' : 'bookmark_add'}
            </span>
            <span className={savedStatus ? 'text-tertiary' : ''}>
              {savedStatus ? 'Saved' : 'Save'}
            </span>
          </button>
        </div>
      </section>
    </div>
  );
};

/* ============================================================================
 * 3. THE SOUNDBAR VIEW (Screen 6: 10-Band Parametric DSP Engine)
 * ========================================================================== */

interface SoundbarViewProps {
  bands: EqBand[];
  dspEnabled: boolean;
  forceDspOnDac: boolean;
  activePresetId: string;
  selectedBandIndex: number;
  activeEndpointType: 'usb-dac' | 'phone-speaker';
  activeDac: UsbDacEndpoint;
  exclusiveUsbAccess: boolean;
  dolbyAtmosEnabled: boolean;
  onToggleDsp: () => void;
  onToggleForceDsp: () => void;
  onSelectPreset: (presetId: string) => void;
  onSelectBand: (index: number) => void;
  onUpdateBandGain: (index: number, newGainDb: number) => void;
  onCycleBandFilter: (index: number) => void;
  onResetFlat: () => void;
  onApplyAiEqCurve?: (prompt: string) => Promise<void> | void;
  onShowToast: (msg: string) => void;
}

export const SoundbarView: React.FC<SoundbarViewProps> = ({
  bands,
  dspEnabled,
  forceDspOnDac,
  activePresetId,
  selectedBandIndex,
  activeEndpointType,
  activeDac,
  exclusiveUsbAccess,
  dolbyAtmosEnabled,
  onToggleDsp,
  onToggleForceDsp,
  onSelectPreset,
  onSelectBand,
  onUpdateBandGain,
  onCycleBandFilter,
  onResetFlat,
  onApplyAiEqCurve,
  onShowToast,
}) => {
  const [syncedFeedback, setSyncedFeedback] = useState(false);
  const [showAiEqModal, setShowAiEqModal] = useState(false);
  const [aiEqInput, setAiEqInput] = useState('');
  const [isGeneratingEq, setIsGeneratingEq] = useState(false);

  const selectedBand = bands[selectedBandIndex] || bands[5];
  const isBitPerfectIntercept =
    activeEndpointType === 'usb-dac' && exclusiveUsbAccess && !dolbyAtmosEnabled;

  // Convert gainDb (-12 to +12) to SVG Y (140 to 20, with 80 as 0dB)
  const gainToY = (gainDb: number) => {
    const clamped = Math.max(-12, Math.min(12, gainDb));
    return 80 - (clamped / 12) * 60;
  };

  // Build smooth SVG quadratic spline path through all 10 bands
  const points = bands.map((b) => ({ x: b.svgX, y: gainToY(b.gainDb) }));
  let strokePath = `M 0 80`;
  points.forEach((pt, idx) => {
    if (idx === 0) {
      strokePath += ` Q ${pt.x * 0.5} ${pt.y}, ${pt.x} ${pt.y}`;
    } else {
      const prev = points[idx - 1];
      const midX = (prev.x + pt.x) / 2;
      const midY = (prev.y + pt.y) / 2;
      strokePath += ` Q ${prev.x} ${prev.y}, ${midX} ${midY}`;
      if (idx === points.length - 1) {
        strokePath += ` T ${pt.x} ${pt.y} L 360 ${pt.y}`;
      }
    }
  });
  const areaPath = `${strokePath} L 360 160 L 0 160 Z`;

  // Compute Total Gain & Anti-Clipping Headroom (from vibe_dsp/peq.py)
  const avgGain =
    bands.reduce((acc, b) => acc + b.gainDb, 0) / Math.max(1, bands.length);
  const maxPeakGain = Math.max(0, ...bands.map((b) => b.gainDb));
  const headroomDbFs = -(maxPeakGain + 0.4);

  return (
    <div className="flex flex-col w-full gap-4 select-none pb-4">
      {/* Top Screen Title & DSP Toggle */}
      <div className="flex items-center justify-between pt-1">
        <div className="flex flex-col min-w-0 pr-2">
          <div className="flex items-center gap-1.5">
            <h1 className="font-headline-md text-on-surface tracking-tight">
              The Soundbar
            </h1>
            <span className="text-[19px]">🎛️</span>
          </div>
          <p className="font-telemetry-sm text-on-surface-variant flex items-center gap-1 mt-0.5">
            <span className="w-1.5 h-1.5 rounded-full bg-tertiary-container shadow-[0_0_6px_rgba(27,189,133,0.8)]"></span>
            10-Band Parametric DSP Engine • 64-bit FP
          </p>
        </div>

        <button
          aria-label="Toggle Equalizer"
          onClick={onToggleDsp}
          className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-surface-container-high transition-all active:scale-95 shadow-sm"
        >
          <span className="font-telemetry-sm text-on-surface font-semibold tracking-wider">
            DSP
          </span>
          <div
            className={`w-10 h-5 rounded-full p-0.5 flex items-center transition-colors ${
              dspEnabled
                ? 'bg-primary-container justify-end shadow-[0_0_12px_rgba(6,182,212,0.45)]'
                : 'bg-surface-container-highest justify-start'
            }`}
          >
            <div className="w-4 h-4 rounded-full bg-on-primary-container shadow-md"></div>
          </div>
        </button>
      </div>

      {/* Bit-Perfect / DAC Bypass or Dolby Atmos Advisory Banner */}
      <div className="rounded-xl p-3 bg-surface-container-high relative overflow-hidden flex items-center justify-between gap-2 shadow-sm">
        <div className="flex items-start gap-2.5 min-w-0">
          <span className="material-symbols-outlined text-secondary text-[20px] shrink-0 mt-0.5">
            {dolbyAtmosEnabled ? 'surround_sound' : 'info'}
          </span>
          <div className="flex flex-col min-w-0">
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="font-label-md text-secondary font-semibold">
                {dolbyAtmosEnabled
                  ? 'Dolby Atmos Spatial Active'
                  : 'DAC Mode Intercept'}
              </span>
              <span className="font-telemetry-sm px-1 rounded bg-secondary-container/20 text-secondary">
                {dolbyAtmosEnabled
                  ? 'BIT-PERFECT OFF'
                  : isBitPerfectIntercept && !forceDspOnDac
                  ? 'BIT-PERFECT'
                  : 'DSP FORCED'}
              </span>
            </div>
            <p className="font-body-sm text-on-surface-variant leading-snug mt-0.5 truncate">
              {dolbyAtmosEnabled
                ? 'Bit-Perfect bypassed for 3D Atmos + 10-Band PEQ.'
                : isBitPerfectIntercept && !forceDspOnDac
                ? `DSP bypassed on ${activeDac.modelName.replace(' USB DAC', '')} stream.`
                : `64-bit PEQ active on ${activeDac.modelName.replace(' USB DAC', '')}.`}
            </p>
          </div>
        </div>
        {!dolbyAtmosEnabled && (
          <button
            onClick={onToggleForceDsp}
            className={`shrink-0 px-2.5 py-1.5 rounded-lg transition-colors font-telemetry-sm font-semibold active:scale-95 ${
              forceDspOnDac
                ? 'bg-tertiary-container text-on-tertiary'
                : 'bg-surface-container-highest text-secondary hover:bg-secondary-container hover:text-on-secondary'
            }`}
          >
            {forceDspOnDac ? 'DSP Active ✓' : 'Force DSP'}
          </button>
        )}
      </div>

      {/* AI Auto-EQ Curve Generator Bar */}
      <div className="p-3 rounded-2xl bg-surface-container border border-primary/30 flex flex-col gap-2 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <span className="text-secondary text-sm">🪄</span>
            <span className="font-label-md text-on-surface font-bold">
              AI Parametric Auto-EQ
            </span>
          </div>
          <button
            onClick={() => setShowAiEqModal((s) => !s)}
            className="font-telemetry-sm text-primary hover:underline"
          >
            {showAiEqModal ? 'Close' : 'Prompt Curve'}
          </button>
        </div>

        {showAiEqModal ? (
          <div className="space-y-2 pt-1">
            <p className="font-body-sm text-on-surface-variant text-[11px]">
              Describe headphone target, acoustic fix, or genre warmth:
            </p>
            <div className="flex items-center gap-1.5">
              <input
                type="text"
                value={aiEqInput}
                onChange={(e) => setAiEqInput(e.target.value)}
                placeholder="e.g. Tame harsh 6kHz treble, warm up deep sub-bass..."
                className="flex-1 h-8 px-3 rounded-xl bg-surface-container-lowest text-xs text-on-surface border border-outline-variant/40 focus:outline-none focus:border-primary"
              />
              <button
                disabled={isGeneratingEq}
                onClick={async () => {
                  if (aiEqInput.trim() && onApplyAiEqCurve) {
                    setIsGeneratingEq(true);
                    await onApplyAiEqCurve(aiEqInput.trim());
                    setIsGeneratingEq(false);
                    setShowAiEqModal(false);
                    setAiEqInput('');
                  }
                }}
                className="h-8 px-3 rounded-xl bg-primary-container text-on-primary-container font-label-md text-xs font-bold shrink-0 active:scale-95"
              >
                {isGeneratingEq ? 'Generating...' : 'Compute'}
              </button>
            </div>
            {/* Quick AI EQ Targets */}
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pt-1">
              {[
                'Tame 6kHz IEM Sibilance',
                'Harman Deep Bass Rise',
                'Warm Analog Tube Velvet',
                'Vocal Clarity & Air',
              ].map((target) => (
                <button
                  key={target}
                  onClick={async () => {
                    if (onApplyAiEqCurve) {
                      setIsGeneratingEq(true);
                      await onApplyAiEqCurve(target);
                      setIsGeneratingEq(false);
                      setShowAiEqModal(false);
                    }
                  }}
                  className="px-2 py-0.5 rounded-full bg-surface-container-high text-[10px] font-mono text-on-surface-variant hover:text-primary whitespace-nowrap"
                >
                  {target}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-between text-[11px] font-mono text-on-surface-variant">
            <span>Type or voice-prompt custom 10-band target curves</span>
            <span className="text-tertiary">PEQ 64-bit FP</span>
          </div>
        )}
      </div>

      {/* Quick Presets Horizontal Strip */}
      <div className="flex flex-col gap-1.5">
        <div className="flex items-center justify-between px-0.5">
          <span className="font-telemetry-sm uppercase tracking-wider text-on-surface-variant font-semibold">
            Curated Curve Profiles
          </span>
          <span className="font-telemetry-sm text-primary-container">
            {EQ_PRESETS.length} Loaded
          </span>
        </div>
        <div className="flex items-center gap-2 overflow-x-auto pb-1 -mx-4 px-4 no-scrollbar">
          {EQ_PRESETS.map((preset) => {
            const active = activePresetId === preset.id;
            return (
              <button
                key={preset.id}
                onClick={() => {
                  onSelectPreset(preset.id);
                  onShowToast(`Loaded EQ Profile: ${preset.name}`);
                }}
                className={`px-3 py-1.5 rounded-full flex items-center gap-1.5 whitespace-nowrap font-label-md transition-all ${
                  active
                    ? 'bg-primary-container text-on-primary-container shadow-[0_0_14px_rgba(6,182,212,0.35)]'
                    : 'bg-surface-container-high text-on-surface hover:bg-surface-container-highest'
                }`}
              >
                <span>{preset.emoji}</span>
                <span>{preset.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Graphical Parametric Response Curve Canvas Card */}
      <div className="w-full rounded-2xl bg-surface-container-low p-3.5 flex flex-col relative overflow-hidden shadow-md">
        <div className="flex items-center justify-between mb-2 z-10">
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-primary-container animate-pulse shadow-[0_0_6px_rgba(6,182,212,0.8)]"></span>
            <span className="font-telemetry-sm font-semibold text-on-surface tracking-wider">
              LIVE FREQ RESPONSE
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-telemetry-sm px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant flex items-center gap-1">
              <span className="material-symbols-outlined text-[13px] text-primary">
                pinch
              </span>
              20Hz - 20kHz
            </span>
            <span className="font-telemetry-sm px-1.5 py-0.5 rounded bg-surface-container-highest text-tertiary font-semibold">
              0.0 ms LATENCY
            </span>
          </div>
        </div>

        {/* Active Node Tooltip Badge */}
        <div className="self-center z-20 px-2.5 py-1 rounded-full bg-surface-container-highest text-on-surface flex items-center gap-2 shadow-md">
          <span className="w-1.5 h-1.5 rounded-full bg-primary-container"></span>
          <span className="font-telemetry-sm font-semibold tracking-wider text-primary">
            {selectedBand.freqLabel}
          </span>
          <span className="text-on-surface-variant font-telemetry-sm">•</span>
          <span className="font-telemetry-sm text-on-surface font-medium">
            {selectedBand.gainDb > 0 ? '+' : ''}
            {selectedBand.gainDb.toFixed(1)} dB
          </span>
          <span className="text-on-surface-variant font-telemetry-sm">•</span>
          <span className="font-telemetry-sm text-tertiary">
            Q: {selectedBand.qFactor.toFixed(2)}
          </span>
        </div>

        {/* SVG Canvas Display Area */}
        <div className="w-full h-44 relative mt-1">
          <svg
            className="w-full h-full overflow-visible"
            preserveAspectRatio="none"
            viewBox="0 0 360 160"
          >
            <defs>
              <linearGradient
                id="eqFillGradient"
                x1="0%"
                x2="0%"
                y1="0%"
                y2="100%"
              >
                <stop offset="0%" stopColor="#06b6d4" stopOpacity="0.38" />
                <stop offset="65%" stopColor="#06b6d4" stopOpacity="0.08" />
                <stop offset="100%" stopColor="#06b6d4" stopOpacity="0" />
              </linearGradient>
            </defs>

            {/* Horizontal dB Grid Lines */}
            <line
              stroke="#353437"
              strokeDasharray="2 3"
              strokeWidth="0.8"
              x1="0"
              x2="360"
              y1="20"
              y2="20"
            />
            <line
              stroke="#353437"
              strokeDasharray="2 3"
              strokeWidth="0.8"
              x1="0"
              x2="360"
              y1="50"
              y2="50"
            />
            <line
              stroke="#3d494c"
              strokeWidth="1.2"
              x1="0"
              x2="360"
              y1="80"
              y2="80"
            />
            <line
              stroke="#353437"
              strokeDasharray="2 3"
              strokeWidth="0.8"
              x1="0"
              x2="360"
              y1="110"
              y2="110"
            />
            <line
              stroke="#353437"
              strokeDasharray="2 3"
              strokeWidth="0.8"
              x1="0"
              x2="360"
              y1="140"
              y2="140"
            />

            {/* Vertical Freq Grid Lines */}
            {[20, 72, 130, 195, 265, 335].map((x) => (
              <line
                key={x}
                stroke="#2a2a2c"
                strokeWidth="1"
                x1={x}
                x2={x}
                y1="10"
                y2="150"
              />
            ))}

            {/* Freq Text Labels */}
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="middle"
              x="20"
              y="156"
            >
              30Hz
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="middle"
              x="72"
              y="156"
            >
              100Hz
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="middle"
              x="130"
              y="156"
            >
              300Hz
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="middle"
              x="195"
              y="156"
            >
              1kHz
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="middle"
              x="265"
              y="156"
            >
              5kHz
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="middle"
              x="335"
              y="156"
            >
              16kHz
            </text>

            {/* dB Scale Readouts */}
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="end"
              x="356"
              y="24"
            >
              +12dB
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="end"
              x="356"
              y="54"
            >
              +6dB
            </text>
            <text
              fill="#4cd7f6"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="end"
              x="356"
              y="84"
            >
              0dB
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="end"
              x="356"
              y="114"
            >
              -6dB
            </text>
            <text
              fill="#869397"
              fontFamily="JetBrains Mono"
              fontSize="8"
              textAnchor="end"
              x="356"
              y="144"
            >
              -12dB
            </text>

            {/* Dynamic EQ Curve Fill & Stroke */}
            <path d={areaPath} fill="url(#eqFillGradient)" />
            <path
              d={strokePath}
              fill="none"
              stroke="#06b6d4"
              strokeLinecap="round"
              strokeWidth="2.5"
            />

            {/* Interactive DSP Frequency Nodes */}
            <g>
              {bands.map((b, idx) => {
                const cy = gainToY(b.gainDb);
                const isSelected = idx === selectedBandIndex;
                return (
                  <g
                    key={b.id}
                    onClick={() => onSelectBand(idx)}
                    className="cursor-pointer"
                  >
                    <circle
                      cx={b.svgX}
                      cy={cy}
                      fill={isSelected ? '#ffb95f' : '#4cd7f6'}
                      r={isSelected ? 7.5 : 5}
                      stroke="#0e0e10"
                      strokeWidth={isSelected ? 2.5 : 2}
                    />
                    {isSelected && (
                      <circle
                        cx={b.svgX}
                        cy={cy}
                        fill="none"
                        r="13"
                        stroke="#ffb95f"
                        strokeDasharray="2 2"
                        strokeWidth="1.5"
                      />
                    )}
                  </g>
                );
              })}
            </g>
          </svg>
        </div>
      </div>

      {/* Selected Node Fine Tuning Stepper Bar */}
      <div className="w-full rounded-xl bg-surface-container p-3 flex items-center justify-between gap-2 shadow-sm">
        <div
          onClick={() => onCycleBandFilter(selectedBandIndex)}
          className="flex items-center gap-2.5 min-w-0 cursor-pointer"
          title="Tap to cycle biquad filter topology"
        >
          <div className="w-9 h-9 rounded-xl bg-surface-container-highest flex items-center justify-center text-secondary shrink-0">
            <span className="material-symbols-outlined text-[20px]">tune</span>
          </div>
          <div className="flex flex-col min-w-0">
            <div className="flex items-center gap-1.5">
              <span className="font-label-lg text-on-surface font-semibold truncate">
                {selectedBand.fullName}
              </span>
              <span className="font-telemetry-sm px-1.5 rounded bg-surface-container-high text-primary font-bold">
                {selectedBand.freqLabel}
              </span>
            </div>
            <span className="font-body-sm text-on-surface-variant">
              Filter Type: {selectedBand.filterType} (Tap to switch)
            </span>
          </div>
        </div>

        <div className="flex items-center gap-1.5 shrink-0">
          <button
            aria-label="Decrease Gain"
            onClick={() =>
              onUpdateBandGain(
                selectedBandIndex,
                Math.max(-12, Number((selectedBand.gainDb - 0.5).toFixed(1)))
              )
            }
            className="w-10 h-10 rounded-full bg-surface-container-highest text-on-surface hover:text-primary flex items-center justify-center active:scale-90 transition-transform shadow-inner"
          >
            <span className="material-symbols-outlined text-[20px]">
              remove
            </span>
          </button>
          <div className="px-1.5 min-w-[56px] text-center font-telemetry-md text-secondary font-bold">
            {selectedBand.gainDb > 0 ? '+' : ''}
            {selectedBand.gainDb.toFixed(1)} dB
          </div>
          <button
            aria-label="Increase Gain"
            onClick={() =>
              onUpdateBandGain(
                selectedBandIndex,
                Math.min(12, Number((selectedBand.gainDb + 0.5).toFixed(1)))
              )
            }
            className="w-10 h-10 rounded-full bg-surface-container-highest text-on-surface hover:text-primary flex items-center justify-center active:scale-90 transition-transform shadow-inner"
          >
            <span className="material-symbols-outlined text-[20px]">add</span>
          </button>
        </div>
      </div>

      {/* Precision Vertical Faders Header */}
      <div className="flex items-center justify-between px-0.5 pt-1">
        <div className="flex items-center gap-2">
          <h2 className="font-headline-sm text-on-surface tracking-tight">
            Precision Faders
          </h2>
          <span className="w-2 h-2 rounded-full bg-tertiary-container shadow-[0_0_6px_rgba(27,189,133,0.8)]"></span>
        </div>
        <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-container-high text-on-surface font-telemetry-sm font-semibold">
          <span className="material-symbols-outlined text-[14px] text-primary">
            grain
          </span>
          Q: {selectedBand.qFactor.toFixed(2)} (Pro)
        </span>
      </div>

      {/* Horizontal Scrollable Strip of Tactile Faders */}
      <div className="w-full overflow-x-auto pb-2 -mx-4 px-4 no-scrollbar">
        <div className="flex items-center gap-3 w-max py-1">
          {bands.map((band, idx) => {
            const isSelected = idx === selectedBandIndex;
            const fillPct = Math.min(
              100,
              Math.max(0, ((band.gainDb + 12) / 24) * 100)
            );
            const thumbTopPct = 100 - fillPct;

            return (
              <div
                key={band.id}
                onClick={() => onSelectBand(idx)}
                className={`w-[68px] rounded-2xl p-2 flex flex-col items-center gap-2 transition-all cursor-pointer relative ${
                  isSelected
                    ? 'bg-surface-container-high shadow-md'
                    : 'bg-surface-container-low hover:bg-surface-container shadow-sm'
                }`}
              >
                {isSelected && (
                  <div className="absolute -top-1.5 px-1.5 py-0.2 rounded-full bg-secondary-container text-on-secondary font-telemetry-sm font-extrabold uppercase scale-90">
                    FOCUS
                  </div>
                )}
                <span
                  className={`font-telemetry-sm font-bold px-1.5 py-0.5 rounded bg-surface-container-lowest ${
                    isSelected ? 'text-secondary' : 'text-primary'
                  }`}
                >
                  {band.gainDb > 0 ? '+' : ''}
                  {band.gainDb.toFixed(1)}
                </span>

                {/* Vertical Track Clickable Area */}
                <div
                  onClick={(e) => {
                    e.stopPropagation();
                    onSelectBand(idx);
                    const rect = e.currentTarget.getBoundingClientRect();
                    const clickY = e.clientY - rect.top;
                    const ratio = 1 - Math.min(1, Math.max(0, clickY / rect.height));
                    const newGain = Math.round((ratio * 24 - 12) * 2) / 2;
                    onUpdateBandGain(idx, newGain);
                  }}
                  className="w-10 h-40 flex items-center justify-center relative py-1"
                >
                  <div className="w-2.5 h-full rounded-full bg-surface-container-highest relative overflow-hidden flex flex-col justify-end">
                    <div
                      style={{ height: `${fillPct}%` }}
                      className={`w-full rounded-full ${
                        isSelected ? 'bg-secondary' : 'bg-primary-container'
                      }`}
                    />
                  </div>
                  <div
                    style={{ top: `calc(${thumbTopPct}% - 14px)` }}
                    className={`absolute w-8 h-8 rounded-full flex items-center justify-center transition-transform ${
                      isSelected
                        ? 'bg-secondary text-on-secondary shadow-[0_0_14px_rgba(245,158,11,0.55)] scale-110'
                        : 'bg-surface-container-highest shadow-[0_2px_8px_rgba(0,0,0,0.6)]'
                    }`}
                  >
                    <div
                      className={`w-3.5 h-3.5 rounded-full ${
                        isSelected
                          ? 'bg-on-secondary'
                          : 'bg-primary shadow-[0_0_8px_rgba(6,182,212,0.9)]'
                      }`}
                    />
                  </div>
                </div>

                <div className="flex flex-col items-center text-center">
                  <span
                    className={`font-label-md truncate w-full ${
                      isSelected
                        ? 'text-secondary font-bold'
                        : 'text-on-surface font-semibold'
                    }`}
                  >
                    {band.label}
                  </span>
                  <span className="font-telemetry-sm text-on-surface-variant font-medium">
                    {band.freqLabel}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Audio Analysis Stat Strip */}
      <div className="grid grid-cols-3 gap-2 py-0.5">
        <div className="p-2.5 rounded-xl bg-surface-container-low flex flex-col items-center justify-center text-center shadow-sm">
          <span className="font-telemetry-sm text-on-surface-variant">
            TOTAL GAIN
          </span>
          <span className="font-telemetry-md text-on-surface font-bold mt-0.5">
            {avgGain >= 0 ? '+' : ''}
            {avgGain.toFixed(1)} dB
          </span>
        </div>
        <div className="p-2.5 rounded-xl bg-surface-container-low flex flex-col items-center justify-center text-center shadow-sm">
          <span className="font-telemetry-sm text-on-surface-variant">
            HEADROOM
          </span>
          <span className="font-telemetry-md text-tertiary font-bold mt-0.5">
            {headroomDbFs.toFixed(1)} dB FS
          </span>
        </div>
        <div className="p-2.5 rounded-xl bg-surface-container-low flex flex-col items-center justify-center text-center shadow-sm">
          <span className="font-telemetry-sm text-on-surface-variant">
            DSP LOAD
          </span>
          <span className="font-telemetry-md text-primary font-bold mt-0.5">
            {dspEnabled ? '1.2% / 64b' : '0.0% / OFF'}
          </span>
        </div>
      </div>

      {/* Thumb Zone Action Buttons */}
      <div className="flex items-center gap-2 pt-1">
        <button
          onClick={() => {
            onResetFlat();
            onShowToast('Reset all 10 parametric bands to 0.0 dB Flat');
          }}
          className="flex-1 h-12 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-container-highest transition-all flex items-center justify-center gap-2 active:scale-95 shadow-sm"
        >
          <span className="material-symbols-outlined text-[19px]">
            restart_alt
          </span>
          <span className="font-label-lg font-semibold">Reset Flat</span>
        </button>

        <button
          onClick={() => {
            setSyncedFeedback(true);
            onShowToast('Applied 64-bit Biquad Sound Curve to DSP Engine');
            setTimeout(() => setSyncedFeedback(false), 1400);
          }}
          className={`flex-[1.4] h-12 rounded-full transition-all flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(6,182,212,0.4)] active:scale-95 ${
            syncedFeedback
              ? 'bg-tertiary-container text-on-tertiary'
              : 'bg-primary-container text-on-primary-container'
          }`}
        >
          <span className="material-symbols-outlined text-[20px]">
            {syncedFeedback ? 'check_circle' : 'graphic_eq'}
          </span>
          <span className="font-label-lg font-bold">
            {syncedFeedback ? 'Curve Synced!' : 'Apply Sound Curve ✨'}
          </span>
        </button>
      </div>
    </div>
  );
};

/* ============================================================================
 * 4. ADD-ON TOYBOX VIEW (Screen 7: Downloadable Plugins & Music Deduplicator)
 * ========================================================================== */

interface AddonsViewProps {
  addons: AddonPackage[];
  unresolvedDupesCount: number;
  onToggleAddon: (id: string) => void;
  onDownloadAddon: (id: string) => void;
  onUninstallAddon: (id: string) => void;
  onOpenDeduplicator: () => void;
  onJumpToSoundbar: () => void;
  onExportM3u8: () => void;
  onShowToast: (msg: string) => void;
}

export const AddonsToyboxView: React.FC<AddonsViewProps> = ({
  addons,
  unresolvedDupesCount,
  onToggleAddon,
  onDownloadAddon,
  onUninstallAddon,
  onOpenDeduplicator,
  onJumpToSoundbar,
  onExportM3u8,
  onShowToast,
}) => {
  const [showRepoCatalog, setShowRepoCatalog] = useState(false);
  const [activeStreamerNode, setActiveStreamerNode] = useState(
    'Living Room (Kodi v21)'
  );

  const installedAddons = addons.filter((a) => a.installed);
  const activeCount = installedAddons.filter((a) => a.active).length;
  const totalRamMb = installedAddons
    .filter((a) => a.active)
    .reduce((s, a) => s + a.sizeMb, 0);

  return (
    <div className="flex flex-col w-full pb-6 select-none">
      {/* Screen Header */}
      <div className="flex flex-col gap-1 mt-1 mb-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-[26px] drop-shadow-[0_0_10px_rgba(78,222,163,0.3)]">
              🧩
            </span>
            <h1 className="font-headline-md tracking-tight text-on-surface">
              Add-on Toybox
            </h1>
          </div>
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-container-high shadow-inner">
            <span className="inline-block w-2 h-2 rounded-full bg-tertiary shadow-[0_0_8px_rgba(78,222,163,0.8)] animate-pulse"></span>
            <span className="font-telemetry-sm text-tertiary uppercase font-semibold tracking-wider">
              v2.4 Core
            </span>
          </div>
        </div>
        <p className="font-body-md text-on-surface-variant leading-relaxed">
          Modular downloadable tools that stay completely out of the core audio playback loop. Zero jitter guarantee.
        </p>
        <div className="flex items-center gap-2 mt-1 px-3 py-1.5 rounded-full bg-surface-container-low self-start shadow-sm">
          <span className="material-symbols-outlined text-[15px] text-primary">
            verified_user
          </span>
          <span className="font-telemetry-sm text-primary tracking-wider uppercase font-semibold">
            Sandboxed Runtime • 0.0ms Audio Impact
          </span>
        </div>
      </div>

      {/* Telemetry Banner */}
      <div className="w-full rounded-2xl bg-surface-container p-3.5 mb-4 shadow-md flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-primary shadow-inner">
            <span className="material-symbols-outlined text-[20px]">
              memory
            </span>
          </div>
          <div className="flex flex-col min-w-0">
            <span className="font-label-lg text-on-surface font-semibold tracking-tight">
              {activeCount} Active / {installedAddons.length - activeCount}{' '}
              Inactive ({installedAddons.length} Installed)
            </span>
            <span className="font-telemetry-sm text-on-surface-variant">
              Isolated Worker Thread
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-surface-container-lowest">
          <span className="w-1.5 h-1.5 rounded-full bg-secondary shadow-[0_0_6px_rgba(245,158,11,0.6)]"></span>
          <span className="font-telemetry-md text-on-surface font-medium">
            {totalRamMb.toFixed(1)} MB
          </span>
        </div>
      </div>

      {/* Installed Plugin Cards Stack */}
      <div className="flex flex-col gap-3">
        {installedAddons.map((addon) => (
          <div
            key={addon.id}
            className="group relative w-full rounded-2xl bg-surface-container p-4 transition-all duration-200 shadow-sm hover:shadow-md"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3 min-w-0 flex-1">
                <div className="w-12 h-12 rounded-xl bg-surface-container-high flex items-center justify-center text-2xl shrink-0 shadow-inner">
                  {addon.icon}
                </div>
                <div className="flex flex-col min-w-0">
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <span className="font-label-lg text-on-surface font-bold truncate">
                      {addon.name}
                    </span>
                    <span
                      className={`font-telemetry-sm px-2 py-0.5 rounded-full bg-surface-container-lowest font-semibold tracking-wider transition-opacity ${
                        addon.active ? 'opacity-100' : 'opacity-40'
                      } ${
                        addon.badgeColor === 'tertiary'
                          ? 'text-tertiary'
                          : addon.badgeColor === 'secondary'
                          ? 'text-secondary'
                          : 'text-primary'
                      }`}
                    >
                      {addon.badge}
                    </span>
                  </div>
                  <p className="font-body-sm text-on-surface-variant line-clamp-2">
                    {addon.description}
                  </p>
                </div>
              </div>

              <button
                aria-checked={addon.active}
                onClick={() => onToggleAddon(addon.id)}
                role="switch"
                className={`relative w-12 h-7 rounded-full p-1 transition-colors duration-200 shrink-0 ${
                  addon.active
                    ? 'bg-primary-container shadow-[0_0_12px_rgba(6,182,212,0.4)]'
                    : 'bg-surface-container-highest'
                }`}
              >
                <div
                  className={`w-5 h-5 rounded-full shadow-md transform transition-transform duration-200 ${
                    addon.active
                      ? 'translate-x-5 bg-on-primary-container'
                      : 'translate-x-0 bg-outline'
                  }`}
                />
              </button>
            </div>

            {/* Action Footer */}
            <div className="mt-3.5 pt-3 border-t border-outline-variant/20 flex items-center justify-between gap-2">
              <div className="flex items-center gap-1.5 text-on-surface-variant font-telemetry-sm truncate">
                <span className="material-symbols-outlined text-[15px] text-primary">
                  {addon.footerIcon}
                </span>
                <span className="truncate">
                  {addon.id === 'deduplicator'
                    ? unresolvedDupesCount > 0
                      ? `${unresolvedDupesCount} duplicate groups found • Tap Run`
                      : 'Last scan clean • 0 dupes'
                    : addon.id === 'kodi-streamer'
                    ? `1 Node: ${activeStreamerNode}`
                    : addon.footerMeta}
                </span>
              </div>

              <div className="flex items-center gap-1.5 shrink-0">
                {addon.id !== 'soundbar' && (
                  <button
                    onClick={() => onUninstallAddon(addon.id)}
                    title="Uninstall add-on package"
                    className="w-7 h-7 rounded-full bg-surface-container-highest text-on-surface-variant hover:text-error flex items-center justify-center"
                  >
                    <span className="material-symbols-outlined text-[15px]">
                      delete
                    </span>
                  </button>
                )}

                <button
                  onClick={() => {
                    if (addon.id === 'soundbar') {
                      onJumpToSoundbar();
                    } else if (addon.id === 'deduplicator') {
                      onOpenDeduplicator();
                    } else if (addon.id === 'kodi-streamer') {
                      const nextNode =
                        activeStreamerNode === 'Living Room (Kodi v21)'
                          ? 'Samsung QLED Atmos TV'
                          : 'Living Room (Kodi v21)';
                      setActiveStreamerNode(nextNode);
                      onShowToast(`UPnP / DLNA Routed to: ${nextNode}`);
                    } else if (addon.id === 'm3u8-exporter') {
                      onExportM3u8();
                    } else {
                      onShowToast(`Opened ${addon.name} (${addon.version})`);
                    }
                  }}
                  className="flex items-center gap-1 px-3 py-1 rounded-full bg-surface-container-high text-primary hover:text-on-surface transition-colors active:scale-95"
                >
                  <span className="font-label-md font-semibold">
                    {addon.actionLabel}
                  </span>
                  <span className="material-symbols-outlined text-[16px]">
                    chevron_right
                  </span>
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Toybox Downloadable Repository Section */}
      <div className="mt-5">
        <button
          onClick={() => setShowRepoCatalog((r) => !r)}
          className="w-full flex items-center justify-between px-5 py-3 rounded-full bg-surface-container hover:bg-surface-container-high active:scale-98 transition-all text-on-surface shadow-sm"
        >
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px] text-primary">
              cloud_download
            </span>
            <span className="font-label-lg font-semibold">
              Install from Toybox Repository (
              {addons.filter((a) => !a.installed).length} Available)
            </span>
          </div>
          <span className="material-symbols-outlined text-[18px] text-primary">
            {showRepoCatalog ? 'expand_less' : 'expand_more'}
          </span>
        </button>

        {showRepoCatalog && (
          <div className="mt-3 space-y-2.5">
            {addons
              .filter((a) => !a.installed)
              .map((pkg) => (
                <div
                  key={pkg.id}
                  className="p-3.5 rounded-2xl bg-surface-container-low border border-outline-variant/40 flex flex-col gap-2"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-2.5 min-w-0">
                      <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-xl shrink-0">
                        {pkg.icon}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5">
                          <span className="font-label-lg text-on-surface font-bold truncate">
                            {pkg.name}
                          </span>
                          <span className="font-telemetry-sm text-primary">
                            {pkg.version}
                          </span>
                        </div>
                        <p className="font-body-sm text-on-surface-variant mt-0.5">
                          {pkg.description}
                        </p>
                      </div>
                    </div>

                    <button
                      disabled={pkg.downloadingPct !== null}
                      onClick={() => onDownloadAddon(pkg.id)}
                      className="px-3 py-1.5 rounded-full bg-primary-container text-on-primary-container font-label-md font-bold flex items-center gap-1 shrink-0 active:scale-95"
                    >
                      <span className="material-symbols-outlined text-[16px]">
                        {pkg.downloadingPct !== null
                          ? 'downloading'
                          : 'download'}
                      </span>
                      <span>
                        {pkg.downloadingPct !== null
                          ? `${pkg.downloadingPct}%`
                          : `${pkg.sizeMb} MB`}
                      </span>
                    </button>
                  </div>

                  {pkg.downloadingPct !== null && (
                    <div className="w-full h-1.5 rounded-full bg-surface-container-highest overflow-hidden">
                      <div
                        style={{ width: `${pkg.downloadingPct}%` }}
                        className="h-full bg-primary transition-all duration-150"
                      />
                    </div>
                  )}
                </div>
              ))}

            {addons.filter((a) => !a.installed).length === 0 && (
              <div className="p-4 rounded-2xl bg-surface-container-low text-center font-telemetry-sm text-tertiary">
                All official Toybox repository modules are downloaded &amp; installed!
              </div>
            )}
          </div>
        )}
      </div>

      {/* Bottom Architecture Assurance Card */}
      <div className="w-full mt-5 rounded-2xl bg-surface-container-low p-4 shadow-sm">
        <div className="flex items-start gap-3">
          <div className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center text-primary shrink-0">
            <span className="material-symbols-outlined text-[18px]">bolt</span>
          </div>
          <div className="flex flex-col gap-1 min-w-0">
            <div className="font-label-lg text-on-surface font-semibold">
              Audio loop runs lean, low-latency, and sandboxed.
            </div>
            <p className="font-body-sm text-on-surface-variant leading-relaxed">
              Plugins execute exclusively in isolated background daemons. The real-time playback thread holds priority interrupts, guaranteeing 0 buffer underruns and bit-perfect DAC pass-through.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

/* ============================================================================
 * 5. SETTINGS & PREFERENCES VIEW (Screen 9 / Image 18)
 * ========================================================================== */

interface SettingsViewProps {
  activeTheme: 'teal' | 'amber' | 'lofi' | 'ink';
  ambientGlow: boolean;
  hardwareVolLock: boolean;
  dolbyAtmosEnabled: boolean;
  resamplerEngine: string;
  indexedFolderPath: string;
  onChangeTheme: (theme: 'teal' | 'amber' | 'lofi' | 'ink') => void;
  onToggleAmbientGlow: () => void;
  onToggleHardwareVolLock: () => void;
  onToggleDolbyAtmos: () => void;
  onChangeResampler: (name: string) => void;
  onChangeFolderPath: (path: string) => void;
  onUploadLocalAudio: (file: File) => void;
  onBack: () => void;
  onOpenWidgets: () => void;
  onOpenDriveMode: () => void;
  onOpenWelcome: () => void;
  onShowToast: (msg: string) => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  activeTheme,
  ambientGlow,
  hardwareVolLock,
  dolbyAtmosEnabled,
  resamplerEngine,
  indexedFolderPath,
  onChangeTheme,
  onToggleAmbientGlow,
  onToggleHardwareVolLock,
  onToggleDolbyAtmos,
  onChangeResampler,
  onChangeFolderPath,
  onUploadLocalAudio,
  onBack,
  onOpenWidgets,
  onOpenDriveMode,
  onOpenWelcome,
  onShowToast,
}) => {
  const [resamplerMenuOpen, setResamplerMenuOpen] = useState(false);
  const [rescanning, setRescanning] = useState(false);

  return (
    <div className="flex flex-col w-full gap-5 select-none pb-6">
      {/* Header Block */}
      <div className="flex flex-col gap-2 pt-1">
        <div className="flex items-center justify-between">
          <button
            aria-label="Go Back"
            onClick={onBack}
            className="w-10 h-10 -ml-2 rounded-full flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-surface-container transition-colors"
          >
            <span className="material-symbols-outlined text-[24px]">
              arrow_back
            </span>
          </button>
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-container-high shadow-sm">
            <span className="w-1.5 h-1.5 rounded-full bg-tertiary shadow-[0_0_8px_rgba(78,222,163,0.8)]"></span>
            <span className="font-telemetry-sm text-tertiary font-medium">
              v2.4.2 Core • Direct ALSA
            </span>
          </div>
        </div>
        <div className="flex flex-col">
          <h1 className="font-headline-md text-on-surface tracking-tight flex items-center gap-2">
            <span>Settings &amp; Preferences</span>
            <span className="text-[20px]">⚙️</span>
          </h1>
          <p className="font-body-sm text-on-surface-variant mt-0.5">
            Audio engine, UI styling, Dolby Atmos, and storage configuration
          </p>
        </div>
      </div>

      {/* Section 1: Appearance & Theme Palette */}
      <section className="flex flex-col gap-2">
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="text-[18px]">🎨</span>
            <h2 className="font-headline-sm text-on-surface tracking-tight">
              Appearance &amp; Theme Palette
            </h2>
          </div>
          <p className="font-body-sm text-on-surface-variant mt-0.5">
            Select your acoustic UI mood. Auto-synchronizes across playback surfaces.
          </p>
        </div>

        <div className="flex gap-2.5 overflow-x-auto pb-2 pt-1 -mx-4 px-4 no-scrollbar">
          {[
            {
              id: 'teal' as const,
              name: 'Teal Lounge',
              sub: 'Current Default',
              tag: 'OLED Cyan',
              dot: 'bg-[#4cd7f6]',
            },
            {
              id: 'amber' as const,
              name: 'Warm Amber Tape',
              sub: 'Analog Tape Warmth',
              tag: 'Vintage 432Hz',
              dot: 'bg-[#ffb95f]',
            },
            {
              id: 'lofi' as const,
              name: 'Midnight Lo-Fi',
              sub: 'Deep Violet Chill',
              tag: 'Nocturnal',
              dot: 'bg-[#c084fc]',
            },
            {
              id: 'ink' as const,
              name: 'God of Ink',
              sub: 'Linux Studio Core',
              tag: 'Mint Emerald',
              dot: 'bg-[#6ffbbe]',
            },
          ].map((t) => {
            const isSelected = activeTheme === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => {
                  onChangeTheme(t.id);
                  onShowToast(`Applied Theme: ${t.name}`);
                }}
                className={`shrink-0 w-60 p-4 rounded-2xl bg-surface-container text-left transition-all relative ${
                  isSelected
                    ? 'shadow-[0_8px_24px_rgba(0,0,0,0.45)] ring-1 ring-primary'
                    : 'opacity-80 hover:opacity-100'
                }`}
              >
                {isSelected && (
                  <div className="absolute inset-0 rounded-2xl bg-primary/10 pointer-events-none"></div>
                )}
                <div className="flex items-start justify-between relative z-10">
                  <div className="flex flex-col min-w-0 pr-2">
                    <span className="font-label-lg text-on-surface truncate">
                      {t.name}
                    </span>
                    <span className="font-telemetry-sm text-primary mt-0.5 font-medium">
                      {t.sub}
                    </span>
                  </div>
                  <div
                    className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
                      isSelected
                        ? 'bg-primary text-on-primary shadow-[0_0_12px_rgba(6,182,212,0.4)]'
                        : 'bg-surface-container-high text-transparent'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[16px] font-bold">
                      check
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2 mt-4 pt-2 relative z-10">
                  <div className="w-5 h-5 rounded-full bg-[#131315] shadow-inner"></div>
                  <div className="w-5 h-5 rounded-full bg-[#201f21] shadow-inner"></div>
                  <div className={`w-5 h-5 rounded-full ${t.dot}`}></div>
                  <span className="font-telemetry-sm text-on-surface-variant ml-auto">
                    {t.tag}
                  </span>
                </div>
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-surface-container-low text-on-surface-variant">
          <span className="text-[14px]">💾</span>
          <span className="font-telemetry-sm">
            Themes save to device storage &amp; persist on relaunch
          </span>
        </div>
      </section>

      {/* Section 2: Ambient Glow Engine */}
      <section className="flex flex-col gap-2">
        <div className="p-4 rounded-2xl bg-surface-container shadow-[0_8px_24px_rgba(0,0,0,0.45)] flex items-center justify-between gap-4">
          <div className="flex items-start gap-3 min-w-0 flex-1">
            <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-primary shrink-0 shadow-inner">
              <span className="material-symbols-outlined text-[22px]">
                blur_on
              </span>
            </div>
            <div className="flex flex-col min-w-0">
              <div className="flex items-center gap-1.5">
                <span className="font-label-lg text-on-surface">
                  Ambient Glow Engine
                </span>
                <span className="text-[12px]">✨</span>
              </div>
              <p className="font-body-sm text-on-surface-variant mt-0.5">
                Dynamic blurred backdrop reacting to album colors on full-screen player
              </p>
            </div>
          </div>

          <button
            aria-checked={ambientGlow}
            onClick={onToggleAmbientGlow}
            role="switch"
            className={`w-12 h-7 px-0.5 rounded-full flex items-center transition-colors shrink-0 ${
              ambientGlow
                ? 'bg-primary justify-end shadow-[0_0_12px_rgba(6,182,212,0.4)]'
                : 'bg-surface-container-high justify-start'
            }`}
          >
            <span className="w-6 h-6 rounded-full bg-surface flex items-center justify-center text-primary shadow-sm">
              <span className="material-symbols-outlined text-[14px] font-bold">
                {ambientGlow ? 'check' : 'close'}
              </span>
            </span>
          </button>
        </div>
      </section>

      {/* Section 3: Audio Engine, Bit-Perfect & Samsung Dolby Atmos */}
      <section className="flex flex-col gap-2">
        <div className="flex items-center gap-2">
          <span className="text-[18px]">🎛️</span>
          <h2 className="font-headline-sm text-on-surface tracking-tight">
            Audio Engine &amp; Bit-Perfect
          </h2>
        </div>

        <div className="flex flex-col gap-2.5">
          {/* Hardware Volume Lock */}
          <div className="p-4 rounded-2xl bg-surface-container shadow-md flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 min-w-0 flex-1">
              <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-secondary shrink-0 shadow-inner">
                <span className="material-symbols-outlined text-[20px]">
                  lock_open
                </span>
              </div>
              <div className="flex flex-col min-w-0">
                <div className="flex items-center gap-1.5">
                  <span className="font-label-lg text-on-surface">
                    Hardware Volume Lock
                  </span>
                  <span className="font-telemetry-sm px-1.5 py-0.5 rounded bg-surface-container-lowest text-tertiary">
                    0 dB
                  </span>
                </div>
                <p className="font-body-sm text-on-surface-variant mt-1 leading-snug">
                  Locks Android digital mixer to 100% to prevent software bit-truncation and guarantee bit-perfect DSD/PCM streams to USB DAC.
                </p>
              </div>
            </div>

            <button
              aria-checked={hardwareVolLock}
              onClick={onToggleHardwareVolLock}
              role="switch"
              className={`w-12 h-7 px-0.5 rounded-full flex items-center transition-colors shrink-0 mt-1 ${
                hardwareVolLock
                  ? 'bg-primary justify-end shadow-[0_0_12px_rgba(6,182,212,0.4)]'
                  : 'bg-surface-container-high justify-start'
              }`}
            >
              <span className="w-6 h-6 rounded-full bg-surface flex items-center justify-center text-primary shadow-sm">
                <span className="material-symbols-outlined text-[14px] font-bold">
                  {hardwareVolLock ? 'check' : 'close'}
                </span>
              </span>
            </button>
          </div>

          {/* Samsung / Android Dolby Atmos Support Toggle */}
          <div className="p-4 rounded-2xl bg-surface-container shadow-md flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 min-w-0 flex-1">
              <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-secondary shrink-0 shadow-inner">
                <span className="material-symbols-outlined text-[20px]">
                  surround_sound
                </span>
              </div>
              <div className="flex flex-col min-w-0">
                <div className="flex items-center gap-1.5 flex-wrap">
                  <span className="font-label-lg text-on-surface">
                    Dolby Atmos Spatial Audio
                  </span>
                  <span className="font-telemetry-sm px-1.5 py-0.5 rounded bg-secondary/15 text-secondary">
                    SAMSUNG / ONE UI
                  </span>
                </div>
                <p className="font-body-sm text-on-surface-variant mt-1 leading-snug">
                  Enables 3D spatial sound stage on Samsung Galaxy &amp; compatible Android devices (cancels Bit-Perfect bypass when active).
                </p>
              </div>
            </div>

            <button
              aria-checked={dolbyAtmosEnabled}
              onClick={onToggleDolbyAtmos}
              role="switch"
              className={`w-12 h-7 px-0.5 rounded-full flex items-center transition-colors shrink-0 mt-1 ${
                dolbyAtmosEnabled
                  ? 'bg-secondary justify-end shadow-[0_0_12px_rgba(245,158,11,0.4)]'
                  : 'bg-surface-container-high justify-start'
              }`}
            >
              <span className="w-6 h-6 rounded-full bg-surface flex items-center justify-center text-secondary shadow-sm">
                <span className="material-symbols-outlined text-[14px] font-bold">
                  {dolbyAtmosEnabled ? 'check' : 'close'}
                </span>
              </span>
            </button>
          </div>

          {/* Audio Resampler Engine Dropdown */}
          <div className="p-4 rounded-2xl bg-surface-container shadow-md flex flex-col gap-2">
            <div className="flex items-start gap-3 min-w-0">
              <div className="w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center text-primary shrink-0 shadow-inner">
                <span className="material-symbols-outlined text-[22px]">
                  tune
                </span>
              </div>
              <div className="flex flex-col min-w-0 flex-1">
                <span className="font-label-lg text-on-surface">
                  Audio Resampler Engine
                </span>
                <p className="font-body-sm text-on-surface-variant mt-0.5">
                  Active DSP SRC algorithm when sample rate conversion is enforced
                </p>
              </div>
            </div>

            <div className="relative w-full mt-1">
              <button
                type="button"
                onClick={() => setResamplerMenuOpen((o) => !o)}
                className="w-full py-2.5 px-4 rounded-xl bg-surface-container-high text-left flex items-center justify-between gap-2 shadow-inner"
              >
                <div className="flex flex-col min-w-0">
                  <span className="font-telemetry-md text-primary font-semibold truncate">
                    {resamplerEngine}
                  </span>
                  <span className="font-telemetry-sm text-on-surface-variant">
                    Polyphase FIR • Min. Phase
                  </span>
                </div>
                <span className="material-symbols-outlined text-primary text-[20px]">
                  {resamplerMenuOpen ? 'expand_less' : 'expand_more'}
                </span>
              </button>

              {resamplerMenuOpen && (
                <div className="mt-1.5 p-1 rounded-xl bg-surface-container-highest shadow-xl flex flex-col gap-1">
                  {[
                    'SOXR Very High Quality (96dB Stopband)',
                    'SOXR Ultra (120dB Stopband Linear)',
                    'Direct Hardware Bypass (No Resampling)',
                  ].map((opt) => (
                    <button
                      key={opt}
                      type="button"
                      onClick={() => {
                        onChangeResampler(opt);
                        setResamplerMenuOpen(false);
                        onShowToast(`Resampler set: ${opt}`);
                      }}
                      className={`w-full p-2.5 rounded-lg text-left flex items-center justify-between font-telemetry-md ${
                        resamplerEngine === opt
                          ? 'bg-primary/10 text-primary'
                          : 'text-on-surface hover:bg-surface-container-high'
                      }`}
                    >
                      <span className="truncate">{opt}</span>
                      {resamplerEngine === opt && (
                        <span className="material-symbols-outlined text-[18px]">
                          done
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Section 4: Storage & Folders */}
      <section className="flex flex-col gap-2">
        <div className="flex items-center gap-2">
          <span className="text-[18px]">📁</span>
          <h2 className="font-headline-sm text-on-surface tracking-tight">
            Storage &amp; Folders
          </h2>
        </div>

        <div className="p-4 rounded-2xl bg-surface-container shadow-md flex flex-col gap-3">
          <div className="flex flex-col">
            <span className="font-label-md text-on-surface-variant font-medium">
              Indexed Audio Source
            </span>
            <div className="flex items-center gap-2 mt-2 px-3 py-2.5 rounded-xl bg-surface-container-lowest shadow-inner">
              <span className="material-symbols-outlined text-primary text-[18px]">
                folder_open
              </span>
              <span className="font-telemetry-md text-on-surface font-semibold truncate flex-1">
                {indexedFolderPath}
              </span>
              <span className="font-telemetry-sm px-1.5 py-0.5 rounded bg-surface-container-high text-on-surface-variant">
                FLAC/DSD
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2 text-on-surface-variant">
            <span className="material-symbols-outlined text-[16px] text-tertiary">
              check_circle
            </span>
            <span className="font-telemetry-sm">
              1,428 lossless files indexed • 64.2 GB
            </span>
          </div>

          <div className="grid grid-cols-2 gap-2 pt-1">
            <label className="h-11 px-3 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright/50 active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer">
              <span className="material-symbols-outlined text-[18px]">
                create_new_folder
              </span>
              <span className="font-label-md font-medium">Import Audio</span>
              <input
                type="file"
                accept="audio/*"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    onUploadLocalAudio(file);
                    onChangeFolderPath(`/storage/emulated/0/Music/${file.name}`);
                  }
                }}
              />
            </label>

            <button
              type="button"
              onClick={() => {
                setRescanning(true);
                setTimeout(() => {
                  setRescanning(false);
                  onShowToast('Crate Synced: 1,428 lossless files verified');
                }, 1000);
              }}
              className="h-11 px-3 rounded-full bg-primary/10 text-primary hover:bg-primary/20 active:scale-95 transition-all flex items-center justify-center gap-1.5"
            >
              <span
                className={`material-symbols-outlined text-[18px] ${
                  rescanning ? 'animate-spin' : ''
                }`}
              >
                sync
              </span>
              <span className="font-label-md font-semibold">
                {rescanning ? 'Indexing...' : 'Rescan Crate'}
              </span>
            </button>
          </div>
        </div>
      </section>

      {/* Quick Android Experience Launchers */}
      <section className="flex flex-col gap-2">
        <span className="font-telemetry-sm uppercase text-on-surface-variant px-1">
          Android System Experiences
        </span>
        <div className="grid grid-cols-3 gap-2">
          <button
            onClick={onOpenWidgets}
            className="p-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high flex flex-col items-center gap-1 text-center"
          >
            <span className="material-symbols-outlined text-primary text-[20px]">
              widgets
            </span>
            <span className="font-label-md text-on-surface">Widgets</span>
          </button>
          <button
            onClick={onOpenDriveMode}
            className="p-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high flex flex-col items-center gap-1 text-center"
          >
            <span className="material-symbols-outlined text-secondary text-[20px]">
              directions_car
            </span>
            <span className="font-label-md text-on-surface">Drive Mode</span>
          </button>
          <button
            onClick={onOpenWelcome}
            className="p-2.5 rounded-xl bg-surface-container hover:bg-surface-container-high flex flex-col items-center gap-1 text-center"
          >
            <span className="material-symbols-outlined text-tertiary text-[20px]">
              verified_user
            </span>
            <span className="font-label-md text-on-surface">Permissions</span>
          </button>
        </div>
      </section>

      <footer className="flex flex-col items-center justify-center gap-1 pt-2 text-center">
        <div className="flex items-center gap-1.5 text-on-surface-variant">
          <span className="material-symbols-outlined text-[16px] text-primary">
            verified
          </span>
          <span className="font-telemetry-sm text-on-surface-variant">
            Zero-Jitter Hardware Pipeline
          </span>
        </div>
        <span className="font-telemetry-sm text-on-surface-variant/70">
          VibePlay Android • Bit-Perfect ALSA Core • OpenSL ES / AAudio
        </span>
      </footer>
    </div>
  );
};
