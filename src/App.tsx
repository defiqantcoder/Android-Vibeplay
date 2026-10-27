import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  AddonPackage,
  CrateFolder,
  DuplicateGroup,
  EQ_PRESETS,
  EqBand,
  INITIAL_ADDONS,
  INITIAL_CRATES,
  INITIAL_DUPLICATE_GROUPS,
  INITIAL_EQ_BANDS,
  INITIAL_TRACKS,
  INITIAL_USB_DAC_MODELS,
  Track,
  UsbDacEndpoint,
  cookHeuristicMixtape,
  parseAnyDacHardwareProfile,
} from './data/vibeplayData';
import {
  probeConnectedAudioHardware,
  requestWebUsbDacHardware,
  subscribeToHardwareHotplug,
  vibeAudioEngine,
} from './utils/audioEngine';
import {
  generateAiEqCurve,
  sequenceHarmonicDjQueue,
} from './utils/aiEngine';
import {
  VoiceIntent,
  pixelVoiceAssistant,
} from './utils/voiceAssistant';
import {
  DacDisconnectedModal,
  DacOutputModal,
  DriveModeModal,
  MusicDeduplicatorModal,
  NowPlayingModal,
  PixelAssistantVoiceModal,
  SynchronizedLyricsModal,
  TrackActionsSheet,
  WelcomeOnboardingModal,
  WidgetsShowcaseModal,
} from './components/OverlaysAndModals';
import {
  AddonsToyboxView,
  CrateView,
  SettingsView,
  SoundbarView,
  VibeCookerView,
} from './components/TabViews';
import { PixelWatchCompanion } from './components/PixelWatchCompanion';

type MainTab = 'crate' | 'cooker' | 'soundbar' | 'addons' | 'settings';
type ModalOverlay =
  | null
  | 'now-playing'
  | 'lyrics'
  | 'dac-output'
  | 'dac-disconnected'
  | 'track-actions'
  | 'drive-mode'
  | 'widgets'
  | 'welcome'
  | 'deduplicator'
  | 'voice-assistant';

type ViewportMode = 'side-by-side' | 'phone-only' | 'watch-only';

export default function App() {
  // Core Library & Playback State
  const [tracks, setTracks] = useState<Track[]>(INITIAL_TRACKS);
  const [crates, setCrates] = useState<CrateFolder[]>(INITIAL_CRATES);
  const [activeTrackId, setActiveTrackId] = useState<string>('midnight-resonance');
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTimeSec, setCurrentTimeSec] = useState<number>(134);
  const [shuffle, setShuffle] = useState<boolean>(true);
  const [repeatMode, setRepeatMode] = useState<'off' | 'all' | 'one'>('all');

  // Navigation & Viewport State
  const [viewportMode, setViewportMode] = useState<ViewportMode>('side-by-side');
  const [activeTab, setActiveTab] = useState<MainTab>('crate');
  const [crateSubMode, setCrateSubMode] = useState<'overview' | 'indexed'>('overview');
  const [activeOverlay, setActiveOverlay] = useState<ModalOverlay>(null);
  const [actionSheetTrackId, setActionSheetTrackId] = useState<string>('dead-on-arrival');
  const [searchOpen, setSearchOpen] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Voice Assistant State
  const [isVoiceListening, setIsVoiceListening] = useState<boolean>(false);
  const [voiceTranscript, setVoiceTranscript] = useState<string>('');

  // Dynamic USB DAC & Dolby Atmos State
  const [availableDacs, setAvailableDacs] = useState<UsbDacEndpoint[]>(
    INITIAL_USB_DAC_MODELS
  );
  const [activeDacId, setActiveDacId] = useState<string>('fiio-ka15');
  const [activeEndpointType, setActiveEndpointType] = useState<
    'usb-dac' | 'phone-speaker'
  >('usb-dac');
  const [exclusiveUsbAccess, setExclusiveUsbAccess] = useState<boolean>(true);
  const [autoSwapOnPlug, setAutoSwapOnPlug] = useState<boolean>(true);
  const [dolbyAtmosEnabled, setDolbyAtmosEnabled] = useState<boolean>(false);
  const [dolbyAtmosProfile, setDolbyAtmosProfile] = useState<
    'auto' | 'music' | 'movie' | 'voice'
  >('music');

  // 10-Band Parametric Equalizer (The Soundbar) State
  const [eqBands, setEqBands] = useState<EqBand[]>(INITIAL_EQ_BANDS);
  const [activePresetId, setActivePresetId] = useState<string>('warm-tape');
  const [selectedBandIndex, setSelectedBandIndex] = useState<number>(5);
  const [dspEnabled, setDspEnabled] = useState<boolean>(true);
  const [forceDspOnDac, setForceDspOnDac] = useState<boolean>(false);

  // Vibe Cooker State
  const [cookPrompt, setCookPrompt] = useState<string>(
    'Late night downtempo with warm analog tape hiss, under 95 BPM for deep focus writing in the rain...'
  );
  const [seedTrackId, setSeedTrackId] = useState<string | undefined>(undefined);
  const [energyArc, setEnergyArc] = useState<'steady' | 'ramp' | 'peak' | 'chill'>(
    'chill'
  );
  const [optionalAiKey, setOptionalAiKey] = useState<string>(() => {
    try {
      return localStorage.getItem('vibeplay_ai_key') || '';
    } catch {
      return '';
    }
  });

  // Add-ons Toybox & Music Deduplicator State
  const [addons, setAddons] = useState<AddonPackage[]>(INITIAL_ADDONS);
  const [duplicateGroups, setDuplicateGroups] = useState<DuplicateGroup[]>(
    INITIAL_DUPLICATE_GROUPS
  );

  // Settings & Persistent Theme State
  const [activeTheme, setActiveTheme] = useState<
    'teal' | 'amber' | 'lofi' | 'ink'
  >(() => {
    try {
      const saved = localStorage.getItem('vibeplay_theme');
      if (
        saved === 'teal' ||
        saved === 'amber' ||
        saved === 'lofi' ||
        saved === 'ink'
      ) {
        return saved;
      }
    } catch {
      // ignore
    }
    return 'teal';
  });
  const [ambientGlow, setAmbientGlow] = useState<boolean>(true);
  const [hardwareVolLock, setHardwareVolLock] = useState<boolean>(true);
  const [resamplerEngine, setResamplerEngine] = useState<string>(
    'SOXR Very High Quality (96dB Stopband)'
  );
  const [indexedFolderPath, setIndexedFolderPath] = useState<string>(
    '/storage/emulated/0/Music'
  );
  const [storageGranted, setStorageGranted] = useState<boolean>(true);
  const [usbGranted, setUsbGranted] = useState<boolean>(true);

  // Toast Feedback
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const toastTimerRef = useRef<number | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }
    toastTimerRef.current = window.setTimeout(() => {
      setToastMessage(null);
    }, 2500);
  };

  // Persist theme & optional AI key
  useEffect(() => {
    try {
      localStorage.setItem('vibeplay_theme', activeTheme);
    } catch {
      // ignore
    }
  }, [activeTheme]);

  useEffect(() => {
    try {
      localStorage.setItem('vibeplay_ai_key', optionalAiKey);
    } catch {
      // ignore
    }
  }, [optionalAiKey]);

  // Active Track & Active DAC Computed Objects
  const activeTrack = useMemo(
    () => tracks.find((t) => t.id === activeTrackId) || tracks[0],
    [tracks, activeTrackId]
  );

  const actionSheetTrack = useMemo(
    () => tracks.find((t) => t.id === actionSheetTrackId) || activeTrack,
    [tracks, actionSheetTrackId, activeTrack]
  );

  const activeDac = useMemo(
    () =>
      availableDacs.find((d) => d.id === activeDacId) || availableDacs[0],
    [availableDacs, activeDacId]
  );

  // Route Web Audio output sink to the active hardware DAC sinkId when available
  useEffect(() => {
    const targetSink =
      activeEndpointType === 'usb-dac' ? activeDac?.sinkId : '';
    vibeAudioEngine.setHardwareSink(targetSink);
  }, [activeDac, activeEndpointType]);

  // Subscribe to real-time OS MediaDevices & WebUSB hot-plug / unplug events for ANY DAC
  useEffect(() => {
    let mounted = true;
    const runInitialAndHotplugScan = async () => {
      const updated = await probeConnectedAudioHardware(availableDacs);
      if (!mounted) return;
      if (updated.length > availableDacs.length) {
        setAvailableDacs(updated);
        if (autoSwapOnPlug && updated[0]) {
          setActiveDacId(updated[0].id);
          setActiveEndpointType('usb-dac');
        }
      }
    };
    runInitialAndHotplugScan();
    const unsubscribe = subscribeToHardwareHotplug(runInitialAndHotplugScan);
    return () => {
      mounted = false;
      unsubscribe();
    };
  }, [autoSwapOnPlug, availableDacs]);

  // Sync Web Audio DSP Engine whenever EQ, Bit-Perfect, or Dolby Atmos changes
  useEffect(() => {
    const bitPerfectActive =
      activeEndpointType === 'usb-dac' &&
      exclusiveUsbAccess &&
      !dolbyAtmosEnabled;

    vibeAudioEngine.updateDspChain(
      eqBands,
      dspEnabled,
      bitPerfectActive,
      forceDspOnDac,
      dolbyAtmosEnabled,
      dolbyAtmosProfile
    );
  }, [
    eqBands,
    dspEnabled,
    activeEndpointType,
    exclusiveUsbAccess,
    dolbyAtmosEnabled,
    dolbyAtmosProfile,
    forceDspOnDac,
  ]);

  // Playback Progress Ticker
  useEffect(() => {
    if (!isPlaying) return;
    const interval = window.setInterval(() => {
      setCurrentTimeSec((prev) => {
        if (prev + 1 >= activeTrack.durationSec) {
          if (repeatMode === 'one') {
            return 0;
          }
          const idx = tracks.findIndex((t) => t.id === activeTrack.id);
          const nextTrack = tracks[(idx + 1) % tracks.length];
          if (nextTrack) {
            setActiveTrackId(nextTrack.id);
            vibeAudioEngine.startPlayback(nextTrack);
          }
          return 0;
        }
        return prev + 1;
      });
    }, 1000);
    return () => window.clearInterval(interval);
  }, [isPlaying, activeTrack, repeatMode, tracks]);

  // Cooked Mixtape Computed Result
  const cookedMixtape = useMemo(
    () => cookHeuristicMixtape(cookPrompt, tracks, seedTrackId, energyArc),
    [cookPrompt, tracks, seedTrackId, energyArc]
  );

  // Handlers
  const handleTogglePlay = () => {
    const nextPlaying = !isPlaying;
    setIsPlaying(nextPlaying);
    if (nextPlaying) {
      vibeAudioEngine.startPlayback(activeTrack);
    } else {
      vibeAudioEngine.stopPlayback();
    }
  };

  const handleSelectTrack = (selected: Track) => {
    setActiveTrackId(selected.id);
    setCurrentTimeSec(0);
    setIsPlaying(true);
    vibeAudioEngine.startPlayback(selected);
    showToast(`Now Playing: ${selected.title} (${selected.shortFormat})`);
  };

  const handlePrevTrack = () => {
    if (currentTimeSec > 10) {
      setCurrentTimeSec(0);
      return;
    }
    const idx = tracks.findIndex((t) => t.id === activeTrack.id);
    const prevIdx = (idx - 1 + tracks.length) % tracks.length;
    handleSelectTrack(tracks[prevIdx]);
  };

  const handleNextTrack = () => {
    if (shuffle && tracks.length > 1) {
      const others = tracks.filter((t) => t.id !== activeTrack.id);
      const randomNext = others[Math.floor(Math.random() * others.length)];
      handleSelectTrack(randomNext);
      return;
    }
    const idx = tracks.findIndex((t) => t.id === activeTrack.id);
    const nextIdx = (idx + 1) % tracks.length;
    handleSelectTrack(tracks[nextIdx]);
  };

  const handleToggleLike = (id: string) => {
    setTracks((prev) =>
      prev.map((t) => {
        if (t.id === id) {
          const nextLiked = !t.liked;
          showToast(
            nextLiked ? 'Added to Crate Favorites' : 'Removed from favorites'
          );
          return { ...t, liked: nextLiked };
        }
        return t;
      })
    );
  };

  // Mutual Exclusivity: Dolby Atmos vs. Bit-Perfect Exclusive USB Access
  const handleToggleDolbyAtmos = () => {
    setDolbyAtmosEnabled((prev) => {
      const next = !prev;
      if (next) {
        setExclusiveUsbAccess(false);
        showToast(
          `Dolby Atmos (${dolbyAtmosProfile.toUpperCase()}) Enabled • Bit-Perfect Bypassed`
        );
      } else {
        setExclusiveUsbAccess(true);
        showToast('Dolby Atmos Off • Bit-Perfect USB Mode Restored');
      }
      return next;
    });
  };

  const handleToggleExclusiveUsb = () => {
    setExclusiveUsbAccess((prev) => {
      const next = !prev;
      if (next && dolbyAtmosEnabled) {
        setDolbyAtmosEnabled(false);
        showToast(
          'Exclusive Bit-Perfect USB Active • Dolby Atmos Disabled'
        );
      } else {
        showToast(
          next
            ? `Exclusive Bit-Perfect Active on ${activeDac.modelName}`
            : 'Switched to Android System Audio Mixer (48kHz)'
        );
      }
      return next;
    });
  };

  // Voice Assistant Intent Dispatcher
  const handleVoiceIntent = (intent: VoiceIntent) => {
    setActiveOverlay(null);
    if (intent.type === 'play') {
      setIsPlaying(true);
      vibeAudioEngine.startPlayback(activeTrack);
      showToast('Assistant: Resumed playback');
    } else if (intent.type === 'pause') {
      setIsPlaying(false);
      vibeAudioEngine.stopPlayback();
      showToast('Assistant: Paused playback');
    } else if (intent.type === 'next') {
      handleNextTrack();
      showToast('Assistant: Skipped to next track');
    } else if (intent.type === 'previous') {
      handlePrevTrack();
      showToast('Assistant: Previous track');
    } else if (intent.type === 'bit-perfect') {
      handleToggleExclusiveUsb();
    } else if (intent.type === 'dolby-atmos') {
      handleToggleDolbyAtmos();
    } else if (intent.type === 'auto-eq') {
      handleApplyAiEq(intent.payload || 'Harman Bass Rise');
    } else if (intent.type === 'cook-vibe') {
      const p = intent.payload || 'Late night downtempo under 95 BPM';
      setCookPrompt(p);
      setActiveTab('cooker');
      showToast(`Assistant: Cooking "${p}"`);
    }
  };

  // Trigger Pixel Assistant Voice Capture
  const handleStartVoiceListening = () => {
    setIsVoiceListening(true);
    setVoiceTranscript('');
    setActiveOverlay('voice-assistant');

    const started = pixelVoiceAssistant.startListening(
      (intent) => {
        setIsVoiceListening(false);
        handleVoiceIntent(intent);
      },
      (interim) => setVoiceTranscript(interim)
    );

    if (!started) {
      setVoiceTranscript('Listening simulated (click any command chip below)');
    }
  };

  // AI Auto-EQ Callback
  const handleApplyAiEq = async (promptText: string) => {
    showToast(`AI DSP: Computing parametric curve for "${promptText}"...`);
    const result = await generateAiEqCurve(promptText, eqBands, optionalAiKey);
    setEqBands(result.bands);
    setDspEnabled(true);
    showToast(`AI Auto-EQ Synced: ${result.profileName}`);
  };

  // AI Harmonic DJ Sequencer Callback
  const handleAiHarmonicDjSort = () => {
    const { orderedTracks, transitions } = sequenceHarmonicDjQueue(tracks);
    setTracks(orderedTracks);
    showToast(
      `AI DJ: Re-sequenced ${orderedTracks.length} tracks by Camelot Key adjacency (${transitions[0]?.rule || 'Harmonic Flow'})`
    );
  };

  // Biometric BPM Sync from Pixel Watch 3
  const handleSyncBiometricBpm = (targetBpm: number) => {
    const promptText = `Biometric pulse tempo matching ${targetBpm} BPM workout rhythm, high kinetic drive, lossless`;
    setCookPrompt(promptText);
    setActiveTab('cooker');
    const closest = [...tracks].sort(
      (a, b) => Math.abs(a.bpm - targetBpm) - Math.abs(b.bpm - targetBpm)
    )[0];
    if (closest) {
      handleSelectTrack(closest);
    }
    showToast(`Wrist Heart Rate Synced: Steered crate to ${targetBpm} BPM!`);
  };

  // Dynamic USB DAC Hardware Probe
  const handleProbeHardware = async () => {
    const updatedDacs = await probeConnectedAudioHardware(availableDacs);
    setAvailableDacs(updatedDacs);
    if (updatedDacs.length > availableDacs.length) {
      const newest = updatedDacs[0];
      setActiveDacId(newest.id);
      setActiveEndpointType('usb-dac');
      showToast(
        `Detected Hardware DAC: ${newest.modelName} [${newest.usbVidPid || 'UAC 2.0'}]`
      );
    } else {
      showToast(
        `USB-C OTG Bus Scanned • ${availableDacs.length} Universal DAC Endpoints Ready`
      );
    }
  };

  // WebUSB Direct Hardware Descriptor Pairing for ANY physical USB DAC
  const handlePairWebUsbDac = async () => {
    const pairedDac = await requestWebUsbDacHardware();
    if (pairedDac) {
      setAvailableDacs((prev) => [
        pairedDac,
        ...prev.filter((d) => d.usbVidPid !== pairedDac.usbVidPid),
      ]);
      setActiveDacId(pairedDac.id);
      setActiveEndpointType('usb-dac');
      showToast(
        `Paired Physical USB DAC: ${pairedDac.modelName} (${pairedDac.usbVidPid})`
      );
    } else {
      showToast(
        'WebUSB chooser closed or restricted in preview iframe — use "Recognize & Mount" below to test any DAC model'
      );
    }
  };

  const handleAddCustomDac = (modelName: string) => {
    const customDac = parseAnyDacHardwareProfile(
      modelName,
      undefined,
      undefined,
      undefined,
      'Android UsbManager + Dynamic UAC 2.0'
    );
    setAvailableDacs((prev) => [customDac, ...prev]);
    setActiveDacId(customDac.id);
    setActiveEndpointType('usb-dac');
    showToast(
      `Recognized & Mounted: ${customDac.modelName} (${customDac.chipset} · ${customDac.maxBitDepth}/${customDac.maxSampleRate})`
    );
  };

  // Simulate USB DAC Abrupt Unplug
  const handleSimulateUnplug = () => {
    setIsPlaying(false);
    vibeAudioEngine.stopPlayback();
    setActiveOverlay('dac-disconnected');
  };

  // Downloadable Add-ons Handlers
  const handleDownloadAddon = (addonId: string) => {
    setAddons((prev) =>
      prev.map((a) => (a.id === addonId ? { ...a, downloadingPct: 15 } : a))
    );
    let pct = 15;
    const timer = window.setInterval(() => {
      pct += 30;
      if (pct >= 100) {
        window.clearInterval(timer);
        setAddons((prev) =>
          prev.map((a) => {
            if (a.id === addonId) {
              showToast(`Installed "${a.name}" into sandboxed runtime`);
              return {
                ...a,
                installed: true,
                active: true,
                downloadingPct: null,
              };
            }
            return a;
          })
        );
      } else {
        setAddons((prev) =>
          prev.map((a) =>
            a.id === addonId ? { ...a, downloadingPct: Math.min(99, pct) } : a
          )
        );
      }
    }, 180);
  };

  const handleUninstallAddon = (addonId: string) => {
    setAddons((prev) =>
      prev.map((a) => {
        if (a.id === addonId) {
          showToast(`Uninstalled ${a.name}`);
          return { ...a, installed: false, active: false, downloadingPct: null };
        }
        return a;
      })
    );
  };

  // Export M3U8 Handler
  const handleExportM3u8 = () => {
    const lines = [
      '#EXTM3U',
      `#PLAYLIST:${cookedMixtape.mixtapeTitle}`,
      ...cookedMixtape.scoredTracks.map(
        ({ track }) =>
          `#EXTINF:${track.durationSec},${track.artist} - ${track.title}\n/storage/emulated/0/Music/${track.title.replace(
            /\s+/g,
            '_'
          )}.flac`
      ),
    ].join('\n');
    const blob = new Blob([lines], { type: 'audio/x-mpegurl' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${cookedMixtape.mixtapeTitle
      .toLowerCase()
      .replace(/\s+/g, '_')}.m3u8`;
    a.click();
    URL.revokeObjectURL(url);
    showToast(`Exported ${cookedMixtape.mixtapeTitle}.m3u8`);
  };

  // Upload Local Audio File
  const handleUploadLocalAudio = (file: File) => {
    const newId = `local-${Date.now()}`;
    const cleanTitle = file.name.replace(/\.[^/.]+$/, '');
    const objectUrl = URL.createObjectURL(file);
    vibeAudioEngine.registerLocalFile(newId, objectUrl);

    const newTrack: Track = {
      id: newId,
      title: cleanTitle,
      artist: 'Local Device Storage',
      album: 'Imported Lossless Stem',
      bpm: 122,
      camelotKey: '8A',
      musicalKey: 'A Minor',
      energy: 0.78,
      energyBars: 4,
      danceability: 0.74,
      formatBadge: 'FLAC 24/96',
      shortFormat: '24/96',
      sampleRate: '96kHz',
      bitDepth: '24-bit',
      bitrateKbps: 3028,
      fileSizeMb: Math.max(12, Math.round(file.size / (1024 * 1024))),
      acoustId: `local-${Date.now()}`,
      lufs: -9.4,
      durationSec: 215,
      durationLabel: '03:35',
      liked: true,
      isHiRes: true,
      artworkUrl: '/assets/aistudio/Image_5_ac32a477.jpeg',
      vibeCategory: ['all', 'hires', 'recent'],
      crateId: 'crate-stash',
      moodLabel: 'Custom',
      moodEmoji: '🎧',
      tempoLabel: 'Allegro',
      energyLabel: 'High',
      danceLabel: 'Groove',
      waveform: INITIAL_TRACKS[0].waveform,
      synthNotes: INITIAL_TRACKS[0].synthNotes,
      lyrics: INITIAL_TRACKS[0].lyrics,
    };
    setTracks((prev) => [newTrack, ...prev]);
    setActiveTrackId(newId);
    setCurrentTimeSec(0);
    setIsPlaying(true);
    vibeAudioEngine.startPlayback(newTrack);
    showToast(`Playing imported audio: ${cleanTitle}`);
  };

  const unresolvedDupesCount = duplicateGroups.filter((g) => !g.resolved).length;

  const themeClass =
    activeTheme === 'amber'
      ? 'theme-amber'
      : activeTheme === 'lofi'
      ? 'theme-lofi'
      : activeTheme === 'ink'
      ? 'theme-ink'
      : '';

  return (
    <div
      className={`${themeClass} min-h-screen w-full bg-[#050508] text-on-surface flex flex-col items-center justify-start sm:py-3 select-none`}
    >
      {/* Top Bar: Viewport Mode Switcher & Quick Screen Jumper */}
      <div className="w-full max-w-[1280px] px-3 py-1.5 flex flex-wrap items-center justify-between gap-2 z-40">
        {/* Viewport Layout Mode Selector */}
        <div className="flex items-center gap-1.5 p-1 rounded-full bg-[#141419] border border-[#23232c] text-xs font-mono">
          <span className="text-primary font-bold px-2">VIEWPORT:</span>
          {(
            [
              ['side-by-side', '📱 + ⌚ Side-by-Side (Pixel + Watch 3 44mm)'],
              ['phone-only', '📱 Phone (412×915)'],
              ['watch-only', '⌚ Pixel Watch 3 (456×456)'],
            ] as const
          ).map(([mode, label]) => (
            <button
              key={mode}
              onClick={() => setViewportMode(mode)}
              className={`px-3 py-1 rounded-full transition-all ${
                viewportMode === mode
                  ? 'bg-primary text-[#001f26] font-bold shadow-[0_0_12px_rgba(6,182,212,0.4)]'
                  : 'text-[#9ca3af] hover:text-white'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Quick Screen Jumper Pills */}
        <div className="hidden lg:flex items-center gap-1.5 overflow-x-auto no-scrollbar text-[11px] font-mono">
          {[
            {
              label: '1. Crate Home',
              run: () => {
                setActiveOverlay(null);
                setActiveTab('crate');
                setCrateSubMode('overview');
              },
            },
            {
              label: '2. Indexed Stream',
              run: () => {
                setActiveOverlay(null);
                setActiveTab('crate');
                setCrateSubMode('indexed');
              },
            },
            {
              label: '3. Now Playing',
              run: () => {
                setActiveTrackId('dead-on-arrival');
                setActiveOverlay('now-playing');
              },
            },
            {
              label: '4. Vibe Cooker AI',
              run: () => {
                setActiveOverlay(null);
                setActiveTab('cooker');
              },
            },
            {
              label: '5. DAC & Atmos',
              run: () => setActiveOverlay('dac-output'),
            },
            {
              label: '6. The Soundbar (AI EQ)',
              run: () => {
                setActiveOverlay(null);
                setActiveTab('soundbar');
              },
            },
            {
              label: '7. Add-on Toybox',
              run: () => {
                setActiveOverlay(null);
                setActiveTab('addons');
              },
            },
            {
              label: '8. Track Actions',
              run: () => {
                setActionSheetTrackId('dead-on-arrival');
                setActiveOverlay('track-actions');
              },
            },
            {
              label: '9. Settings',
              run: () => {
                setActiveOverlay(null);
                setActiveTab('settings');
              },
            },
            {
              label: '10. Synced Lyrics',
              run: () => {
                setActiveTrackId('dead-on-arrival');
                setCurrentTimeSec(28);
                setActiveOverlay('lyrics');
              },
            },
            {
              label: '11. Drive Mode',
              run: () => {
                setActiveTrackId('dead-on-arrival');
                setActiveOverlay('drive-mode');
              },
            },
            {
              label: '12. Widgets',
              run: () => {
                setActiveTrackId('dead-on-arrival');
                setActiveOverlay('widgets');
              },
            },
            {
              label: '13. Onboarding',
              run: () => setActiveOverlay('welcome'),
            },
            {
              label: '14. DAC Guard',
              run: () => handleSimulateUnplug(),
            },
            {
              label: '🧹 Deduplicator',
              run: () => setActiveOverlay('deduplicator'),
            },
            {
              label: '🎙️ Pixel Voice',
              run: () => handleStartVoiceListening(),
            },
          ].map((item) => (
            <button
              key={item.label}
              onClick={item.run}
              className="px-2.5 py-1 rounded-full bg-[#181820] hover:bg-primary hover:text-[#001f26] text-[#e5e1e4] transition-colors whitespace-nowrap shrink-0"
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {/* Main Responsive Viewport Area (Side-by-side or Standalone) */}
      <div className="flex-1 w-full flex items-center justify-center gap-8 py-2 overflow-x-auto no-scrollbar">
        {/* ===================================================================
         * 1. ANDROID PHONE VIEWPORT (412px × 915px)
         * ================================================================= */}
        {viewportMode !== 'watch-only' && (
          <div className="relative w-full max-w-[412px] h-[915px] max-h-[915px] bg-surface rounded-[38px] border border-[#2a2a35] shadow-[0_24px_70px_rgba(0,0,0,0.85)] overflow-hidden flex flex-col shrink-0">
            {/* Persistent Top App Bar */}
            <header className="w-full z-30 bg-surface/85 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.25)] shrink-0">
              <div className="h-14 px-4 flex items-center justify-between">
                <button
                  onClick={() => {
                    setActiveTab('crate');
                    setCrateSubMode('overview');
                  }}
                  className="flex items-center gap-1.5 text-left"
                >
                  <span className="font-headline-md tracking-tight text-on-surface flex items-center gap-1.5">
                    <span>✨</span>
                    <span>vibeplay</span>
                  </span>
                  <div className="w-1.5 h-1.5 rounded-full bg-primary-container shadow-[0_0_8px_rgba(6,182,212,0.8)]"></div>
                </button>

                <div className="flex items-center gap-1">
                  {/* Dynamic DAC / Dolby Atmos Status Pill */}
                  <button
                    onClick={() => setActiveOverlay('dac-output')}
                    className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-container-high hover:bg-surface-container-highest transition-colors"
                    title="Audio Output & USB DAC Routing"
                  >
                    <div
                      className={`w-2 h-2 rounded-full ${
                        dolbyAtmosEnabled
                          ? 'bg-secondary shadow-[0_0_6px_rgba(245,158,11,0.8)]'
                          : activeEndpointType === 'usb-dac'
                          ? 'bg-tertiary-container shadow-[0_0_6px_rgba(27,189,133,0.7)]'
                          : 'bg-primary'
                      }`}
                    ></div>
                    <span className="font-telemetry-sm text-on-surface font-medium max-w-[108px] truncate">
                      {dolbyAtmosEnabled
                        ? '🌐 Atmos 3D'
                        : activeEndpointType === 'usb-dac'
                        ? `🎧 ${activeDac.modelName.replace(' USB DAC', '')}`
                        : '📱 Speaker'}
                    </span>
                  </button>

                  {/* Pixel Voice Assistant Mic Trigger */}
                  <button
                    aria-label="Pixel Voice Assistant"
                    onClick={handleStartVoiceListening}
                    className="w-9 h-9 flex items-center justify-center rounded-full bg-primary/15 text-primary hover:bg-primary/25 transition-all active:scale-90"
                    title="Ask Pixel Assistant (Voice Control)"
                  >
                    <span
                      className="material-symbols-outlined text-[20px]"
                      style={{ fontVariationSettings: "'FILL' 1" }}
                    >
                      mic
                    </span>
                  </button>

                  {/* Search Toggle */}
                  <button
                    aria-label="Search Crate"
                    onClick={() => setSearchOpen((s) => !s)}
                    className={`w-9 h-9 flex items-center justify-center rounded-full transition-colors ${
                      searchOpen
                        ? 'bg-primary/20 text-primary'
                        : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[20px]">
                      search
                    </span>
                  </button>

                  {/* Settings Gear */}
                  <button
                    aria-label="Settings"
                    onClick={() =>
                      setActiveTab((t) => (t === 'settings' ? 'crate' : 'settings'))
                    }
                    className={`w-9 h-9 flex items-center justify-center rounded-full transition-colors ${
                      activeTab === 'settings'
                        ? 'bg-primary/20 text-primary'
                        : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[20px]">
                      settings
                    </span>
                  </button>

                  {/* Widgets / Profile Avatar */}
                  <button
                    aria-label="Android Widgets & Onboarding"
                    onClick={() => setActiveOverlay('widgets')}
                    className="w-8 h-8 rounded-full bg-primary flex items-center justify-center ml-0.5 shadow-sm active:scale-95"
                    title="Open Android Widgets & Complications"
                  >
                    <span className="material-symbols-outlined text-on-primary text-[17px]">
                      person
                    </span>
                  </button>
                </div>
              </div>

              {/* Expandable Instant Search Bar */}
              {searchOpen && (
                <div className="px-4 pb-2.5 flex items-center gap-2">
                  <div className="flex-1 h-10 px-3.5 rounded-full bg-surface-container-lowest border border-outline-variant/50 flex items-center gap-2">
                    <span className="material-symbols-outlined text-[18px] text-primary">
                      search
                    </span>
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Search tracks, artists, Camelot keys (e.g. 8A), DSD..."
                      className="flex-1 bg-transparent text-xs text-on-surface focus:outline-none"
                      autoFocus
                    />
                    {searchQuery && (
                      <button
                        onClick={() => setSearchQuery('')}
                        className="text-on-surface-variant hover:text-on-surface"
                      >
                        <span className="material-symbols-outlined text-[16px]">
                          close
                        </span>
                      </button>
                    )}
                  </div>
                </div>
              )}
            </header>

            {/* Main Scrollable Screen Body */}
            <main className="flex-1 overflow-y-auto no-scrollbar px-4 pt-1 pb-40 bg-surface">
              <div className="w-full flex items-center justify-center pt-1 pb-2">
                <div className="w-6 h-1 rounded-full bg-surface-container-highest/60"></div>
              </div>

              {/* Search Filter Results Banner */}
              {searchOpen && searchQuery.trim() !== '' && (
                <div className="mb-3 p-3 rounded-2xl bg-surface-container border border-primary/30 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-telemetry-sm text-primary uppercase">
                      Search Matches ({
                        tracks.filter((t) =>
                          `${t.title} ${t.artist} ${t.camelotKey} ${t.formatBadge}`
                            .toLowerCase()
                            .includes(searchQuery.toLowerCase())
                        ).length
                      })
                    </span>
                    <button
                      onClick={() => {
                        setSearchQuery('');
                        setSearchOpen(false);
                      }}
                      className="font-telemetry-sm text-on-surface-variant"
                    >
                      Close
                    </button>
                  </div>
                  {tracks
                    .filter((t) =>
                      `${t.title} ${t.artist} ${t.camelotKey} ${t.formatBadge}`
                        .toLowerCase()
                        .includes(searchQuery.toLowerCase())
                    )
                    .map((t) => (
                      <div
                        key={t.id}
                        onClick={() => {
                          handleSelectTrack(t);
                          setSearchOpen(false);
                        }}
                        className="p-2 rounded-xl bg-surface-container-low hover:bg-surface-container-high flex items-center justify-between cursor-pointer"
                      >
                        <div className="truncate pr-2">
                          <div className="font-label-md text-on-surface font-semibold truncate">
                            {t.title}
                          </div>
                          <div className="font-telemetry-sm text-on-surface-variant truncate">
                            {t.artist} • {t.bpm} BPM • {t.camelotKey}
                          </div>
                        </div>
                        <span className="font-telemetry-sm text-tertiary shrink-0">
                          {t.shortFormat}
                        </span>
                      </div>
                    ))}
                </div>
              )}

              {activeTab === 'crate' && (
                <CrateView
                  tracks={tracks}
                  crates={crates}
                  activeTrackId={activeTrackId}
                  isPlaying={isPlaying}
                  crateSubMode={crateSubMode}
                  onChangeSubMode={setCrateSubMode}
                  onSelectTrack={handleSelectTrack}
                  onToggleLike={handleToggleLike}
                  onOpenTrackActions={(selected) => {
                    setActionSheetTrackId(selected.id);
                    setActiveOverlay('track-actions');
                  }}
                  onCreateCrate={(name, badge) => {
                    setCrates((prev) => [
                      ...prev,
                      {
                        id: `crate-${Date.now()}`,
                        name,
                        subtitle: `${tracks.length} lossless cuts`,
                        badge,
                        badgeColor: 'primary',
                        icon: 'folder_special',
                        iconBg: 'bg-primary-container/15 text-primary',
                        trackIds: tracks.map((t) => t.id),
                      },
                    ]);
                  }}
                  onShufflePlay={() => {
                    setShuffle(true);
                    handleNextTrack();
                  }}
                  onAiHarmonicSort={handleAiHarmonicDjSort}
                  onShowToast={showToast}
                />
              )}

              {activeTab === 'cooker' && (
                <VibeCookerView
                  tracks={tracks}
                  prompt={cookPrompt}
                  onChangePrompt={setCookPrompt}
                  cookedMixtape={cookedMixtape}
                  seedTrackId={seedTrackId}
                  onSelectSeedTrack={setSeedTrackId}
                  energyArc={energyArc}
                  onChangeEnergyArc={setEnergyArc}
                  optionalAiKey={optionalAiKey}
                  onChangeOptionalAiKey={setOptionalAiKey}
                  onSpinMixtape={() => {
                    const firstMatch = cookedMixtape.scoredTracks[0]?.track;
                    if (firstMatch) {
                      handleSelectTrack(firstMatch);
                      showToast(
                        `Spinning "${cookedMixtape.mixtapeTitle}" (${cookedMixtape.scoredTracks.length} tracks)`
                      );
                    }
                  }}
                  onSaveMixtapeToCrate={() => {
                    setCrates((prev) => [
                      {
                        id: `crate-cooked-${Date.now()}`,
                        name: cookedMixtape.mixtapeTitle,
                        subtitle: `${cookedMixtape.scoredTracks.length} cooked cuts`,
                        badge: cookedMixtape.bpmBadge.replace('BPM: ', ''),
                        badgeColor: 'secondary',
                        icon: 'soup_kitchen',
                        iconBg: 'bg-secondary-container/20 text-secondary',
                        trackIds: cookedMixtape.scoredTracks.map((s) => s.track.id),
                      },
                      ...prev,
                    ]);
                    showToast(
                      `Saved "${cookedMixtape.mixtapeTitle}" to Active Crates`
                    );
                  }}
                  onSelectTrack={handleSelectTrack}
                  onShowToast={showToast}
                />
              )}

              {activeTab === 'soundbar' && (
                <SoundbarView
                  bands={eqBands}
                  dspEnabled={dspEnabled}
                  forceDspOnDac={forceDspOnDac}
                  activePresetId={activePresetId}
                  selectedBandIndex={selectedBandIndex}
                  activeEndpointType={activeEndpointType}
                  activeDac={activeDac}
                  exclusiveUsbAccess={exclusiveUsbAccess}
                  dolbyAtmosEnabled={dolbyAtmosEnabled}
                  onToggleDsp={() => {
                    setDspEnabled((d) => {
                      const next = !d;
                      showToast(
                        next
                          ? '10-Band Parametric DSP Enabled'
                          : 'DSP Bypassed (Flat Pass-Through)'
                      );
                      return next;
                    });
                  }}
                  onToggleForceDsp={() => {
                    setForceDspOnDac((f) => {
                      const next = !f;
                      showToast(
                        next
                          ? `64-bit PEQ Forced onto ${activeDac.modelName}`
                          : 'Restored Bit-Perfect DAC Bypass'
                      );
                      return next;
                    });
                  }}
                  onSelectPreset={(presetId) => {
                    const found = EQ_PRESETS.find((p) => p.id === presetId);
                    if (found) {
                      setActivePresetId(found.id);
                      setEqBands((prev) =>
                        prev.map((b, i) => ({
                          ...b,
                          gainDb: found.gains[i] ?? 0,
                        }))
                      );
                    }
                  }}
                  onSelectBand={setSelectedBandIndex}
                  onUpdateBandGain={(idx, gainDb) => {
                    setEqBands((prev) =>
                      prev.map((b, i) => (i === idx ? { ...b, gainDb } : b))
                    );
                  }}
                  onCycleBandFilter={(idx) => {
                    const order: EqBand['filterType'][] = [
                      'Bell Peak',
                      'Low-Shelf',
                      'High-Shelf',
                      'Notch',
                    ];
                    setEqBands((prev) =>
                      prev.map((b, i) => {
                        if (i === idx) {
                          const nextFilter =
                            order[(order.indexOf(b.filterType) + 1) % order.length];
                          showToast(`${b.freqLabel} Filter: ${nextFilter}`);
                          return { ...b, filterType: nextFilter };
                        }
                        return b;
                      })
                    );
                  }}
                  onResetFlat={() => {
                    setActivePresetId('flat-reference');
                    setEqBands((prev) =>
                      prev.map((b) => ({ ...b, gainDb: 0 }))
                    );
                    showToast('Reset all 10 bands to 0.0 dB Flat Reference');
                  }}
                  onApplyAiEqCurve={handleApplyAiEq}
                  onShowToast={showToast}
                />
              )}

              {activeTab === 'addons' && (
                <AddonsToyboxView
                  addons={addons}
                  unresolvedDupesCount={unresolvedDupesCount}
                  onToggleAddon={(id) => {
                    setAddons((prev) =>
                      prev.map((a) => {
                        if (a.id === id) {
                          const next = !a.active;
                          showToast(
                            `${a.name}: ${next ? 'Activated' : 'Suspended'}`
                          );
                          return { ...a, active: next };
                        }
                        return a;
                      })
                    );
                  }}
                  onDownloadAddon={handleDownloadAddon}
                  onUninstallAddon={handleUninstallAddon}
                  onOpenDeduplicator={() => setActiveOverlay('deduplicator')}
                  onJumpToSoundbar={() => setActiveTab('soundbar')}
                  onExportM3u8={handleExportM3u8}
                  onShowToast={showToast}
                />
              )}

              {activeTab === 'settings' && (
                <SettingsView
                  activeTheme={activeTheme}
                  ambientGlow={ambientGlow}
                  hardwareVolLock={hardwareVolLock}
                  dolbyAtmosEnabled={dolbyAtmosEnabled}
                  resamplerEngine={resamplerEngine}
                  indexedFolderPath={indexedFolderPath}
                  onChangeTheme={setActiveTheme}
                  onToggleAmbientGlow={() => setAmbientGlow((g) => !g)}
                  onToggleHardwareVolLock={() => {
                    setHardwareVolLock((v) => {
                      const next = !v;
                      showToast(
                        next
                          ? 'Hardware Volume Locked at 0 dB (100%)'
                          : 'Hardware Volume Lock Released'
                      );
                      return next;
                    });
                  }}
                  onToggleDolbyAtmos={handleToggleDolbyAtmos}
                  onChangeResampler={setResamplerEngine}
                  onChangeFolderPath={setIndexedFolderPath}
                  onUploadLocalAudio={handleUploadLocalAudio}
                  onBack={() => setActiveTab('crate')}
                  onOpenWidgets={() => setActiveOverlay('widgets')}
                  onOpenDriveMode={() => setActiveOverlay('drive-mode')}
                  onOpenWelcome={() => setActiveOverlay('welcome')}
                  onShowToast={showToast}
                />
              )}
            </main>

            {/* Delight Micro-Interaction Toast Pill */}
            {toastMessage && (
              <div className="absolute bottom-36 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full bg-surface-container-highest/95 backdrop-blur-md text-on-surface shadow-[0_8px_24px_rgba(0,0,0,0.6)] flex items-center gap-2 pointer-events-none z-40 whitespace-nowrap max-w-[90%]">
                <span className="material-symbols-outlined text-primary text-[18px]">
                  check_circle
                </span>
                <span className="font-label-md truncate">{toastMessage}</span>
              </div>
            )}

            {/* Persistent Floating Mini-Player Pill */}
            <div className="absolute bottom-20 inset-x-0 z-30 px-3 pb-1">
              <div
                onClick={() => setActiveOverlay('now-playing')}
                className="h-16 w-full rounded-full bg-surface-container/95 backdrop-blur-xl shadow-[0_8px_24px_rgba(0,0,0,0.55)] px-2.5 flex items-center justify-between gap-2 cursor-pointer border border-outline-variant/25 hover:border-primary/40 transition-colors"
              >
                <div className="flex items-center gap-2.5 min-w-0 flex-1">
                  <div className="w-11 h-11 rounded-xl bg-surface-container-high shrink-0 flex items-center justify-center overflow-hidden relative shadow-inner">
                    <img
                      src={activeTrack.artworkUrl}
                      alt={activeTrack.title}
                      referrerPolicy="no-referrer"
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-primary-container/15 flex items-center justify-center">
                      <span className="material-symbols-outlined text-primary text-[20px] drop-shadow">
                        album
                      </span>
                    </div>
                  </div>
                  <div className="flex flex-col min-w-0 pr-2">
                    <div className="flex items-center gap-1.5">
                      <span className="font-label-lg text-on-surface truncate">
                        {activeTrack.title}
                      </span>
                      <span className="font-telemetry-sm px-1.5 py-0.2 rounded bg-surface-container-lowest text-tertiary-container tracking-wider shrink-0">
                        {activeTrack.shortFormat}
                      </span>
                    </div>
                    <span className="font-body-sm text-on-surface-variant truncate">
                      {activeTrack.artist}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1 shrink-0">
                  <button
                    aria-label="Skip Next"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleNextTrack();
                    }}
                    className="w-11 h-11 rounded-full flex items-center justify-center text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high transition-colors"
                  >
                    <span className="material-symbols-outlined text-[22px]">
                      skip_next
                    </span>
                  </button>
                  <button
                    aria-label="Play or Pause"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleTogglePlay();
                    }}
                    className="w-11 h-11 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center shadow-[0_0_16px_rgba(6,182,212,0.45)] hover:scale-105 active:scale-95 transition-transform"
                  >
                    <span className="material-symbols-outlined text-[24px]">
                      {isPlaying ? 'pause' : 'play_arrow'}
                    </span>
                  </button>
                </div>
              </div>
            </div>

            {/* Persistent Bottom Navigation Bar */}
            <nav className="absolute bottom-0 w-full z-30 bg-surface/90 backdrop-blur-xl shadow-[0_-4px_16px_rgba(0,0,0,0.35)]">
              <div className="h-[68px] px-2 flex items-center justify-around">
                {(
                  [
                    ['crate', 'library_music', 'Crate'],
                    ['cooker', 'soup_kitchen', 'Cooker'],
                    ['soundbar', 'equalizer', 'Soundbar'],
                    ['addons', 'extension', 'Add-ons'],
                  ] as const
                ).map(([tabKey, iconName, label]) => {
                  const isActive = activeTab === tabKey;
                  return (
                    <button
                      key={tabKey}
                      onClick={() => setActiveTab(tabKey)}
                      className={`flex flex-col items-center justify-center gap-0.5 min-w-[68px] h-12 px-2.5 rounded-full transition-all ${
                        isActive
                          ? 'bg-primary-container/15 text-primary font-bold shadow-[0_0_12px_rgba(6,182,212,0.2)]'
                          : 'text-on-surface-variant hover:text-on-surface'
                      }`}
                    >
                      <span className="material-symbols-outlined text-[22px]">
                        {iconName}
                      </span>
                      <span className="font-label-md">{label}</span>
                    </button>
                  );
                })}
              </div>
              <div className="w-32 h-1 bg-surface-bright/70 rounded-full mx-auto mb-1.5"></div>
            </nav>

            {/* Full-Screen & Bottom-Sheet Overlays */}
            {activeOverlay === 'now-playing' && (
              <NowPlayingModal
                track={activeTrack}
                isPlaying={isPlaying}
                currentTimeSec={currentTimeSec}
                shuffle={shuffle}
                repeatMode={repeatMode}
                ambientGlow={ambientGlow}
                activeEndpointType={activeEndpointType}
                activeDac={activeDac}
                exclusiveUsbAccess={exclusiveUsbAccess}
                dolbyAtmosEnabled={dolbyAtmosEnabled}
                dolbyAtmosProfile={dolbyAtmosProfile}
                onClose={() => setActiveOverlay(null)}
                onTogglePlay={handleTogglePlay}
                onPrev={handlePrevTrack}
                onNext={handleNextTrack}
                onSeek={setCurrentTimeSec}
                onToggleLike={handleToggleLike}
                onToggleShuffle={() => setShuffle((s) => !s)}
                onCycleRepeat={() =>
                  setRepeatMode((r) =>
                    r === 'all' ? 'one' : r === 'one' ? 'off' : 'all'
                  )
                }
                onOpenLyrics={() => setActiveOverlay('lyrics')}
                onOpenDacSheet={() => setActiveOverlay('dac-output')}
                onOpenDriveMode={() => setActiveOverlay('drive-mode')}
                onOpenTrackActions={(selected) => {
                  setActionSheetTrackId(selected.id);
                  setActiveOverlay('track-actions');
                }}
              />
            )}

            {activeOverlay === 'lyrics' && (
              <SynchronizedLyricsModal
                track={activeTrack}
                isPlaying={isPlaying}
                currentTimeSec={currentTimeSec}
                onClose={() => setActiveOverlay('now-playing')}
                onTogglePlay={handleTogglePlay}
                onSeek={setCurrentTimeSec}
                onPrev={handlePrevTrack}
                onNext={handleNextTrack}
                onToggleLike={handleToggleLike}
                onShowToast={showToast}
              />
            )}

            {activeOverlay === 'dac-output' && (
              <DacOutputModal
                availableDacs={availableDacs}
                activeDac={activeDac}
                activeEndpointType={activeEndpointType}
                exclusiveUsbAccess={exclusiveUsbAccess}
                autoSwapOnPlug={autoSwapOnPlug}
                dolbyAtmosEnabled={dolbyAtmosEnabled}
                dolbyAtmosProfile={dolbyAtmosProfile}
                onSelectDacModel={(dacId) => {
                  setActiveDacId(dacId);
                  const found = availableDacs.find((d) => d.id === dacId);
                  if (found) {
                    showToast(`Switched USB DAC to ${found.modelName}`);
                  }
                }}
                onAddCustomDac={handleAddCustomDac}
                onProbeHardware={handleProbeHardware}
                onPairWebUsbDac={handlePairWebUsbDac}
                onSelectEndpointType={(type) => {
                  setActiveEndpointType(type);
                  showToast(
                    type === 'usb-dac'
                      ? `Routed to ${activeDac.modelName}`
                      : 'Routed to Phone Speaker / Bluetooth'
                  );
                }}
                onToggleExclusiveUsb={handleToggleExclusiveUsb}
                onToggleAutoSwap={() => setAutoSwapOnPlug((a) => !a)}
                onToggleDolbyAtmos={handleToggleDolbyAtmos}
                onSelectAtmosProfile={(profile) => {
                  setDolbyAtmosProfile(profile);
                  showToast(`Dolby Atmos Profile: ${profile.toUpperCase()}`);
                }}
                onSimulateUnplug={handleSimulateUnplug}
                onClose={() => setActiveOverlay(null)}
              />
            )}

            {activeOverlay === 'dac-disconnected' && (
              <DacDisconnectedModal
                track={activeTrack}
                unpluggedDac={activeDac}
                currentTimeSec={currentTimeSec}
                onSwitchToSpeakers={() => {
                  setActiveEndpointType('phone-speaker');
                  setActiveOverlay(null);
                  showToast('Audio safely routed to Phone Speakers');
                }}
                onReconnectDac={() => {
                  setActiveEndpointType('usb-dac');
                  setActiveOverlay(null);
                  showToast(`${activeDac.modelName} Reconnected • Bit-Perfect Ready`);
                }}
              />
            )}

            {activeOverlay === 'track-actions' && (
              <TrackActionsSheet
                track={actionSheetTrack}
                onClose={() => setActiveOverlay(null)}
                onPlayNext={(selected) => {
                  setActiveTrackId(selected.id);
                  showToast(`Queued "${selected.title}" to Play Next`);
                  setActiveOverlay(null);
                }}
                onStageToCooker={(selected) => {
                  setSeedTrackId(selected.id);
                  setCookPrompt(
                    `Harmonic mixtape seeded from "${selected.title}" (${selected.camelotKey}, ${selected.bpm} BPM) with matching ${selected.moodLabel.toLowerCase()} acoustics...`
                  );
                  setActiveOverlay(null);
                  setActiveTab('cooker');
                  showToast(`Staged "${selected.title}" into Vibe Cooker`);
                }}
                onToggleLike={handleToggleLike}
                onFindSimilarTempo={(selected) => {
                  setSeedTrackId(selected.id);
                  setCookPrompt(
                    `Tracks with tempo around ${selected.bpm} BPM (±5 BPM) and compatible ${selected.camelotKey} harmonic key`
                  );
                  setActiveOverlay(null);
                  setActiveTab('cooker');
                  showToast(`Filtered ±5 BPM (${selected.bpm - 5}–${selected.bpm + 5} BPM)`);
                }}
                onRemoveTrack={(id) => {
                  if (tracks.length <= 2) {
                    showToast('Protected core crate tracks');
                    return;
                  }
                  setTracks((prev) => prev.filter((t) => t.id !== id));
                  setActiveOverlay(null);
                  showToast('Removed track from local crate');
                }}
              />
            )}

            {activeOverlay === 'drive-mode' && (
              <DriveModeModal
                track={activeTrack}
                isPlaying={isPlaying}
                currentTimeSec={currentTimeSec}
                activeDac={activeDac}
                onClose={() => setActiveOverlay(null)}
                onTogglePlay={handleTogglePlay}
                onPrev={handlePrevTrack}
                onNext={handleNextTrack}
                onSeek={setCurrentTimeSec}
                onToggleLike={handleToggleLike}
                onApplyPresetPrompt={(promptText) => {
                  setCookPrompt(promptText);
                  handleNextTrack();
                }}
                onShowToast={showToast}
              />
            )}

            {activeOverlay === 'widgets' && (
              <WidgetsShowcaseModal
                track={activeTrack}
                isPlaying={isPlaying}
                currentTimeSec={currentTimeSec}
                activeDac={activeDac}
                activeTheme={activeTheme}
                onClose={() => setActiveOverlay(null)}
                onTogglePlay={handleTogglePlay}
                onPrev={handlePrevTrack}
                onNext={handleNextTrack}
                onToggleLike={handleToggleLike}
                onTriggerVibePrompt={(promptText) => {
                  setCookPrompt(promptText);
                  setActiveTab('cooker');
                  showToast(`Widget triggered Vibe Cooker: "${promptText}"`);
                }}
                onChangeTheme={setActiveTheme}
                onShowToast={showToast}
              />
            )}

            {activeOverlay === 'welcome' && (
              <WelcomeOnboardingModal
                storageGranted={storageGranted}
                usbGranted={usbGranted}
                onToggleStorage={() => setStorageGranted((s) => !s)}
                onToggleUsb={() => setUsbGranted((u) => !u)}
                onCompleteOnboarding={() => {
                  setActiveOverlay(null);
                  showToast('1,248 Lossless Tracks Indexed & USB DAC Ready');
                }}
              />
            )}

            {activeOverlay === 'deduplicator' && (
              <MusicDeduplicatorModal
                groups={duplicateGroups}
                onResolveGroup={(groupId) => {
                  setDuplicateGroups((prev) =>
                    prev.map((g) =>
                      g.id === groupId ? { ...g, resolved: true } : g
                    )
                  );
                  showToast('Purged redundant duplicate audio files');
                }}
                onPurgeAll={() => {
                  setDuplicateGroups((prev) =>
                    prev.map((g) => ({ ...g, resolved: true }))
                  );
                  showToast('All duplicate groups purged • 81.2 MB reclaimed!');
                }}
                onClose={() => setActiveOverlay(null)}
              />
            )}

            {activeOverlay === 'voice-assistant' && (
              <PixelAssistantVoiceModal
                isListening={isVoiceListening}
                transcript={voiceTranscript}
                onClose={() => {
                  pixelVoiceAssistant.stopListening();
                  setIsVoiceListening(false);
                  setActiveOverlay(null);
                }}
                onExecuteCommand={(cmd) => {
                  const intent = pixelVoiceAssistant.parseTranscript(cmd);
                  handleVoiceIntent(intent);
                }}
              />
            )}
          </div>
        )}

        {/* ===================================================================
         * 2. PIXEL WATCH 3 (44mm EDITION) VIEWPORT (456px × 456px AMOLED)
         * ================================================================= */}
        {viewportMode !== 'phone-only' && (
          <div className="flex flex-col items-center justify-center shrink-0">
            <PixelWatchCompanion
              track={activeTrack}
              isPlaying={isPlaying}
              currentTimeSec={currentTimeSec}
              activeDac={activeDac}
              activeEndpointType={activeEndpointType}
              exclusiveUsbAccess={exclusiveUsbAccess}
              dolbyAtmosEnabled={dolbyAtmosEnabled}
              dolbyAtmosProfile={dolbyAtmosProfile}
              crates={crates}
              tracks={tracks}
              onTogglePlay={handleTogglePlay}
              onPrev={handlePrevTrack}
              onNext={handleNextTrack}
              onSeek={setCurrentTimeSec}
              onSelectTrack={handleSelectTrack}
              onToggleDolbyAtmos={handleToggleDolbyAtmos}
              onToggleBitPerfect={handleToggleExclusiveUsb}
              onTriggerVoicePrompt={(promptText) => {
                setCookPrompt(promptText);
                setActiveTab('cooker');
                showToast(`Watch triggered Vibe Cooker: "${promptText}"`);
              }}
              onSyncBiometricBpm={handleSyncBiometricBpm}
              onShowToast={showToast}
            />
          </div>
        )}
      </div>
    </div>
  );
}
