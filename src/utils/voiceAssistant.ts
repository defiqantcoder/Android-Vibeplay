/**
 * Pixel Assistant & Voice AI Service for VibePlay
 * Handles Web Speech Recognition ("Hey Google" / In-app mic trigger)
 * and dispatches structured playback, routing, and Vibe Cooker intents.
 */

export interface VoiceIntent {
  type:
    | 'play'
    | 'pause'
    | 'next'
    | 'previous'
    | 'volume'
    | 'bit-perfect'
    | 'dolby-atmos'
    | 'cook-vibe'
    | 'auto-eq'
    | 'unknown';
  payload?: any;
  rawTranscript: string;
}

export type VoiceStatusListener = (status: 'listening' | 'processing' | 'idle' | 'error', text?: string) => void;

class VoiceAssistantService {
  private recognition: any = null;
  private isListening = false;
  private statusListeners: VoiceStatusListener[] = [];

  constructor() {
    if (typeof window !== 'undefined') {
      const SpeechRecognition =
        (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      if (SpeechRecognition) {
        try {
          this.recognition = new SpeechRecognition();
          this.recognition.continuous = false;
          this.recognition.interimResults = true;
          this.recognition.lang = 'en-US';
        } catch (e) {
          console.warn('SpeechRecognition initialization error:', e);
        }
      }
    }
  }

  public subscribeStatus(listener: VoiceStatusListener) {
    this.statusListeners.push(listener);
    return () => {
      this.statusListeners = this.statusListeners.filter((l) => l !== listener);
    };
  }

  private notifyStatus(status: 'listening' | 'processing' | 'idle' | 'error', text?: string) {
    this.statusListeners.forEach((l) => l(status, text));
  }

  public startListening(
    onIntent: (intent: VoiceIntent) => void,
    onPartialTranscript?: (transcript: string) => void
  ): boolean {
    if (!this.recognition) {
      this.notifyStatus('error', 'Microphone voice recognition not supported in this browser.');
      return false;
    }

    if (this.isListening) {
      this.stopListening();
      return false;
    }

    try {
      this.isListening = true;
      this.notifyStatus('listening', 'Listening for Pixel voice command...');

      this.recognition.onresult = (event: any) => {
        let interim = '';
        let final = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            final += event.results[i][0].transcript;
          } else {
            interim += event.results[i][0].transcript;
          }
        }

        const currentText = final || interim;
        if (onPartialTranscript && currentText) {
          onPartialTranscript(currentText);
        }
        this.notifyStatus('listening', currentText);

        if (final) {
          this.isListening = false;
          this.notifyStatus('processing', final);
          const parsed = this.parseTranscript(final);
          onIntent(parsed);
          setTimeout(() => this.notifyStatus('idle'), 1600);
        }
      };

      this.recognition.onerror = (err: any) => {
        console.warn('SpeechRecognition error:', err);
        this.isListening = false;
        this.notifyStatus('error', 'Voice command timed out or cancelled.');
        setTimeout(() => this.notifyStatus('idle'), 2000);
      };

      this.recognition.onend = () => {
        this.isListening = false;
        this.notifyStatus('idle');
      };

      this.recognition.start();
      return true;
    } catch (e) {
      console.warn('Speech recognition start failed:', e);
      this.isListening = false;
      this.notifyStatus('error', 'Could not access microphone.');
      return false;
    }
  }

  public stopListening() {
    if (this.recognition && this.isListening) {
      try {
        this.recognition.stop();
      } catch {
        // ignore
      }
      this.isListening = false;
      this.notifyStatus('idle');
    }
  }

  /**
   * Parse transcript into structured voice intent
   */
  public parseTranscript(raw: string): VoiceIntent {
    const text = raw.toLowerCase().trim();

    // 1. Play / Resume
    if (text === 'play' || text === 'resume' || text.startsWith('play music') || text === 'start music') {
      return { type: 'play', rawTranscript: raw };
    }

    // 2. Pause / Stop
    if (text === 'pause' || text === 'stop' || text === 'halt' || text.includes('pause music')) {
      return { type: 'pause', rawTranscript: raw };
    }

    // 3. Skip / Next Track
    if (text.includes('next') || text.includes('skip') || text.includes('forward')) {
      return { type: 'next', rawTranscript: raw };
    }

    // 4. Previous / Back
    if (text.includes('previous') || text.includes('go back') || text.includes('replay') || text.includes('last song')) {
      return { type: 'previous', rawTranscript: raw };
    }

    // 5. Dolby Atmos toggle
    if (text.includes('atmos') || text.includes('dolby') || text.includes('spatial')) {
      return { type: 'dolby-atmos', rawTranscript: raw };
    }

    // 6. Bit-Perfect / USB DAC toggle
    if (text.includes('bit perfect') || text.includes('dac') || text.includes('lossless direct')) {
      return { type: 'bit-perfect', rawTranscript: raw };
    }

    // 7. Auto-EQ Commands
    if (text.includes('bass') || text.includes('treble') || text.includes('equalizer') || text.includes('auto eq') || text.includes('eq')) {
      return { type: 'auto-eq', payload: raw, rawTranscript: raw };
    }

    // 8. Vibe Cooker Playlist Prompt
    if (
      text.startsWith('cook') ||
      text.startsWith('make a playlist') ||
      text.startsWith('vibe') ||
      text.includes('playlist') ||
      text.startsWith('play some') ||
      text.startsWith('i want')
    ) {
      let promptPayload = raw
        .replace(/^hey google/i, '')
        .replace(/^assistant/i, '')
        .replace(/^cook( up)?( a)?/i, '')
        .replace(/^make a playlist of/i, '')
        .replace(/^play some/i, '')
        .trim();

      if (!promptPayload) promptPayload = raw;
      return { type: 'cook-vibe', payload: promptPayload, rawTranscript: raw };
    }

    // Default: treat as play or vibe search
    return { type: 'cook-vibe', payload: raw, rawTranscript: raw };
  }
}

export const pixelVoiceAssistant = new VoiceAssistantService();
