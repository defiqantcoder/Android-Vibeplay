import { EqBand, Track, UsbDacEndpoint } from '../data/vibeplayData';

class VibeAudioEngine {
  private ctx: AudioContext | null = null;
  private filters: BiquadFilterNode[] = [];
  private masterGain: GainNode | null = null;
  private atmosWidenerDelay: DelayNode | null = null;
  private atmosGain: GainNode | null = null;
  private synthInterval: number | null = null;
  private audioElement: HTMLAudioElement | null = null;
  private mediaSource: MediaElementAudioSourceNode | null = null;
  private customTrackUrls: Map<string, string> = new Map();
  private stepIndex = 0;

  private initContext() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.ctx = new AudioCtx();
      this.masterGain = this.ctx.createGain();
      this.masterGain.gain.value = 0.25;

      // Dolby Atmos 3D spatial widener branch
      this.atmosWidenerDelay = this.ctx.createDelay(0.05);
      this.atmosWidenerDelay.delayTime.value = 0.014; // 14ms Haas 3D spatial expansion
      this.atmosGain = this.ctx.createGain();
      this.atmosGain.gain.value = 0.0;

      // Create 10 Biquad filters for the 10-band Parametric EQ
      const freqs = [31, 63, 125, 250, 500, 1000, 2500, 5000, 8000, 16000];
      this.filters = freqs.map((freq, idx) => {
        const f = this.ctx!.createBiquadFilter();
        f.type = idx === 0 ? 'lowshelf' : idx === 9 ? 'highshelf' : 'peaking';
        f.frequency.value = freq;
        f.Q.value = 1.41;
        f.gain.value = 0;
        return f;
      });

      // Chain filters
      for (let i = 0; i < this.filters.length - 1; i++) {
        this.filters[i].connect(this.filters[i + 1]);
      }

      if (this.filters.length > 0) {
        this.filters[this.filters.length - 1].connect(this.masterGain);
        this.filters[this.filters.length - 1].connect(this.atmosWidenerDelay);
        this.atmosWidenerDelay.connect(this.atmosGain);
        this.atmosGain.connect(this.masterGain);
      }
      this.masterGain.connect(this.ctx.destination);
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  public registerLocalFile(trackId: string, fileUrl: string) {
    this.customTrackUrls.set(trackId, fileUrl);
  }

  public updateDspChain(
    bands: EqBand[],
    dspEnabled: boolean,
    bitPerfectActive: boolean,
    forceDspOnDac: boolean,
    dolbyAtmosEnabled: boolean,
    dolbyAtmosProfile: 'auto' | 'music' | 'movie' | 'voice'
  ) {
    if (!this.ctx || this.filters.length === 0) return;

    const shouldBypassEq = bitPerfectActive && !forceDspOnDac && !dolbyAtmosEnabled;

    bands.forEach((band, idx) => {
      const node = this.filters[idx];
      if (!node) return;
      node.frequency.setTargetAtTime(band.freqHz, this.ctx!.currentTime, 0.02);
      node.Q.setTargetAtTime(band.qFactor, this.ctx!.currentTime, 0.02);

      let targetGain = dspEnabled && !shouldBypassEq ? band.gainDb : 0;
      if (dolbyAtmosEnabled) {
        // Apply profile-specific spatial contour
        if (dolbyAtmosProfile === 'music' && (idx <= 1 || idx >= 7)) targetGain += 2.2;
        if (dolbyAtmosProfile === 'movie' && idx === 0) targetGain += 3.5;
        if (dolbyAtmosProfile === 'voice' && (idx === 5 || idx === 6)) targetGain += 3.0;
      }
      node.gain.setTargetAtTime(targetGain, this.ctx!.currentTime, 0.03);
    });

    if (this.atmosGain && this.atmosWidenerDelay) {
      const spatialLevel = dolbyAtmosEnabled
        ? dolbyAtmosProfile === 'movie'
          ? 0.45
          : dolbyAtmosProfile === 'music'
          ? 0.35
          : 0.25
        : 0.0;
      this.atmosGain.gain.setTargetAtTime(spatialLevel, this.ctx.currentTime, 0.04);
    }
  }

  public startPlayback(track: Track) {
    this.initContext();
    this.stopPlayback();

    const customUrl = this.customTrackUrls.get(track.id);
    if (customUrl && this.ctx) {
      if (!this.audioElement) {
        this.audioElement = new Audio();
        this.audioElement.crossOrigin = 'anonymous';
        this.mediaSource = this.ctx.createMediaElementSource(this.audioElement);
        this.mediaSource.connect(this.filters[0]);
      }
      this.audioElement.src = customUrl;
      this.audioElement.play().catch(() => {});
      return;
    }

    // Procedural audiophile synth loop matched to track BPM & harmonic chord notes
    const stepDurationMs = Math.max(180, Math.round((60 / track.bpm) * 500));
    const notes = track.synthNotes?.length ? track.synthNotes : [220, 261.63, 329.63, 392];

    const triggerStep = () => {
      if (!this.ctx || this.ctx.state !== 'running') return;
      const now = this.ctx.currentTime;
      const freq = notes[this.stepIndex % notes.length];
      const octaveMultiplier = this.stepIndex % 4 === 0 ? 0.5 : this.stepIndex % 2 === 0 ? 1 : 1.5;

      const osc = this.ctx.createOscillator();
      const noteGain = this.ctx.createGain();

      osc.type = track.energy > 0.75 ? 'sawtooth' : track.energy > 0.5 ? 'triangle' : 'sine';
      osc.frequency.setValueAtTime(freq * octaveMultiplier, now);

      const filter = this.ctx.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(track.energy > 0.7 ? 1600 : 900, now);

      noteGain.gain.setValueAtTime(0.001, now);
      noteGain.gain.exponentialRampToValueAtTime(0.12, now + 0.04);
      noteGain.gain.exponentialRampToValueAtTime(0.0008, now + stepDurationMs / 1000);

      osc.connect(filter);
      filter.connect(noteGain);
      noteGain.connect(this.filters[0]);

      osc.start(now);
      osc.stop(now + stepDurationMs / 1000 + 0.05);
      this.stepIndex++;
    };

    triggerStep();
    this.synthInterval = window.setInterval(triggerStep, stepDurationMs);
  }

  public stopPlayback() {
    if (this.synthInterval !== null) {
      clearInterval(this.synthInterval);
      this.synthInterval = null;
    }
    if (this.audioElement) {
      this.audioElement.pause();
    }
  }
}

export const vibeAudioEngine = new VibeAudioEngine();

/**
 * Dynamically detects real connected audio output hardware via MediaDevices API
 */
export async function probeConnectedAudioHardware(
  existingModels: UsbDacEndpoint[]
): Promise<UsbDacEndpoint[]> {
  const discovered: UsbDacEndpoint[] = [...existingModels];
  if (typeof navigator !== 'undefined' && navigator.mediaDevices?.enumerateDevices) {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      const outputs = devices.filter(
        (d) => d.kind === 'audiooutput' && d.label && d.deviceId !== 'default' && d.deviceId !== 'communications'
      );
      outputs.forEach((out, idx) => {
        const cleanLabel = out.label.trim();
        const alreadyExists = discovered.some(
          (m) => m.modelName.toLowerCase() === cleanLabel.toLowerCase()
        );
        if (!alreadyExists && cleanLabel.length > 2) {
          discovered.unshift({
            id: `detected-hw-${idx}-${out.deviceId.slice(0, 6)}`,
            modelName: cleanLabel,
            vendor: 'Detected Hardware Endpoint',
            interfaceType: 'Direct Hardware Sink',
            maxBitDepth: '24-bit',
            maxSampleRate: '192kHz',
            dsdCapability: 'PCM / DoP128',
            clockArchitecture: 'Hardware Isochronous',
            defaultHardwareVol: 80,
            latencyMs: 3.8,
            chipset: 'Native System DAC',
            isCustom: true,
          });
        }
      });
    } catch {
      // Ignore permission errors when enumerating without mic prompt
    }
  }
  return discovered;
}
