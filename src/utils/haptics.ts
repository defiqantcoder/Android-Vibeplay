/**
 * Haptic & Tactile Feedback Service for Pixel Watch 3 & Mobile
 * Combines:
 * 1. Hardware Web Vibration API (navigator.vibrate) for physical mobile/watch devices
 * 2. Ultra-low latency synthesized mechanical watch escapement clicks (Web Audio API) for auditory/tactile feedback on desktop & web
 */

export type HapticType = 'crown_tick' | 'tap' | 'cue' | 'swipe' | 'error';

class WatchHapticsService {
  private audioCtx: AudioContext | null = null;
  private soundEnabled = true;

  constructor() {
    // Lazy initialize AudioContext on user interaction
  }

  private getAudioContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    if (!this.audioCtx) {
      const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioCtxClass) {
        try {
          this.audioCtx = new AudioCtxClass();
        } catch {
          // ignore
        }
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume().catch(() => {});
    }
    return this.audioCtx;
  }

  public setSoundEnabled(enabled: boolean) {
    this.soundEnabled = enabled;
  }

  public isSoundEnabled(): boolean {
    return this.soundEnabled;
  }

  /**
   * Triggers multi-sensory haptic feedback
   */
  public trigger(type: HapticType = 'tap') {
    // 1. Device Vibration (Wear OS / Android device)
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      try {
        if (type === 'crown_tick') {
          navigator.vibrate(8);
        } else if (type === 'tap') {
          navigator.vibrate(15);
        } else if (type === 'cue') {
          navigator.vibrate([15, 30, 20]);
        } else if (type === 'swipe') {
          navigator.vibrate(12);
        } else if (type === 'error') {
          navigator.vibrate([40, 50, 40]);
        }
      } catch {
        // ignore vibration restrictions
      }
    }

    // 2. Synthesized Mechanical Watch Detent Click (Web Audio API)
    if (!this.soundEnabled) return;

    try {
      const ctx = this.getAudioContext();
      if (!ctx) return;

      const now = ctx.currentTime;

      if (type === 'crown_tick') {
        // High-precision mechanical ratchet detent tick (~1.8kHz damped burst, 6ms duration)
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        const filter = ctx.createBiquadFilter();

        osc.type = 'triangle';
        osc.frequency.setValueAtTime(1850, now);
        osc.frequency.exponentialRampToValueAtTime(320, now + 0.007);

        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(1600, now);
        filter.Q.setValueAtTime(2.5, now);

        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.007);

        osc.connect(filter);
        filter.connect(gain);
        gain.connect(ctx.destination);

        osc.start(now);
        osc.stop(now + 0.008);
      } else if (type === 'tap') {
        // Solid tactile tap (lower damped transient, ~800Hz to 180Hz)
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(820, now);
        osc.frequency.exponentialRampToValueAtTime(140, now + 0.015);

        gain.gain.setValueAtTime(0.12, now);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.015);

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start(now);
        osc.stop(now + 0.016);
      } else if (type === 'cue') {
        // Double resonant confirmation chime
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(587.33, now); // D5
        osc.frequency.setValueAtTime(880, now + 0.04); // A5

        gain.gain.setValueAtTime(0.14, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start(now);
        osc.stop(now + 0.13);
      }
    } catch {
      // ignore web audio restrictions
    }
  }
}

export const watchHaptics = new WatchHapticsService();
