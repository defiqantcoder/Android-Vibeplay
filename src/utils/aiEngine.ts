import { GoogleGenAI, Type } from '@google/genai';
import { EqBand, Track } from '../data/vibeplayData';

/**
 * AI Engine for VibePlay Mobile & Pixel Watch
 * Provides both Cloud Gemini API integration and instant offline local heuristic fallbacks
 * for:
 * 1. Plain-text prompt playlist generation ("Vibe Cooker AI")
 * 2. Soundbar 10-band Parametric Auto-EQ curve generation
 * 3. AI Harmonic DJ queue sequencer (Camelot Wheel 1A–12B auto-ordering)
 * 4. AI Crate Historian & Audiophile Liner Notes
 */

export interface AiCookResult {
  mixtapeTitle: string;
  arcName: string;
  bpmBadge: string;
  energyRule: string;
  moodBadge: string;
  formatBadge: string;
  explanation: string;
  orderedTrackIds: string[];
}

export interface AiAutoEqResult {
  profileName: string;
  rationale: string;
  recommendedHeadroomDb: number;
  bands: EqBand[];
}

export interface AiLinerNotesResult {
  tagline: string;
  masteringTrivia: string;
  instrumentationBreakdown: string[];
  recommendedDacTuning: string;
  acousticVectorSummary: string;
}

// Helper to get Gemini client
function getGenAiClient(apiKey?: string): GoogleGenAI | null {
  const key = apiKey?.trim() || (typeof process !== 'undefined' ? process.env.GEMINI_API_KEY : '');
  if (!key) return null;
  return new GoogleGenAI({ apiKey: key });
}

/**
 * 1. Plain-Text AI Playlist Generation (Vibe Cooker AI)
 */
export async function generateAiMixtape(
  prompt: string,
  availableTracks: Track[],
  apiKey?: string
): Promise<AiCookResult> {
  const ai = getGenAiClient(apiKey);

  if (ai) {
    try {
      const trackCatalog = availableTracks.map((t) => ({
        id: t.id,
        title: t.title,
        artist: t.artist,
        bpm: t.bpm,
        camelotKey: t.camelotKey,
        energy: t.energy,
        format: t.shortFormat,
        mood: t.moodLabel,
      }));

      const systemPrompt = `You are VibePlay AI, an audiophile DJ & Camelot harmonic mixing curator.
Given the user's natural language music brief and the catalog of available lossless tracks, generate an optimal playlist.
Return a structured JSON object matching the requested schema.`;

      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: [
          { role: 'user', parts: [{ text: `User Vibe Brief: "${prompt}"\n\nCatalog:\n${JSON.stringify(trackCatalog)}` }] }
        ],
        config: {
          systemInstruction: systemPrompt,
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              mixtapeTitle: { type: Type.STRING },
              arcName: { type: Type.STRING },
              bpmBadge: { type: Type.STRING },
              energyRule: { type: Type.STRING },
              moodBadge: { type: Type.STRING },
              formatBadge: { type: Type.STRING },
              explanation: { type: Type.STRING },
              orderedTrackIds: {
                type: Type.ARRAY,
                items: { type: Type.STRING },
              },
            },
            required: [
              'mixtapeTitle',
              'arcName',
              'bpmBadge',
              'energyRule',
              'moodBadge',
              'formatBadge',
              'explanation',
              'orderedTrackIds',
            ],
          },
        },
      });

      if (response.text) {
        const parsed = JSON.parse(response.text) as AiCookResult;
        // Verify track IDs exist in catalog
        const validIds = parsed.orderedTrackIds.filter((id) =>
          availableTracks.some((t) => t.id === id)
        );
        if (validIds.length > 0) {
          return {
            ...parsed,
            orderedTrackIds: validIds,
          };
        }
      }
    } catch (err) {
      console.warn('Gemini API playlist generation fallback to local heuristic:', err);
    }
  }

  // Local Offline Heuristic Fallback
  return fallbackLocalHeuristicCook(prompt, availableTracks);
}

function fallbackLocalHeuristicCook(prompt: string, tracks: Track[]): AiCookResult {
  const lower = prompt.toLowerCase();
  let targetBpmMin = 70;
  let targetBpmMax = 145;
  let maxEnergy = 1.0;
  let arcName = 'Heuristic Harmonic Flow';
  let title = 'Cooked Session';
  let mood = 'Balanced 🎧';

  if (lower.includes('rain') || lower.includes('chill') || lower.includes('focus') || lower.includes('downtempo')) {
    targetBpmMin = 65;
    targetBpmMax = 98;
    maxEnergy = 0.55;
    arcName = 'Late Night Chill & Rain';
    title = 'Nocturnal Rain Flow';
    mood = 'Chill ☁️';
  } else if (lower.includes('cardio') || lower.includes('run') || lower.includes('fast') || lower.includes('techno') || lower.includes('workout')) {
    targetBpmMin = 125;
    targetBpmMax = 160;
    maxEnergy = 1.0;
    arcName = 'High Cadence Peak';
    title = 'Peak Energy Surge';
    mood = 'Energetic 🔥';
  } else if (lower.includes('coffee') || lower.includes('morning') || lower.includes('lofi') || lower.includes('lo-fi')) {
    targetBpmMin = 74;
    targetBpmMax = 95;
    maxEnergy = 0.65;
    arcName = 'Rhodes Warmth Arc';
    title = 'Morning Brew Vinyl';
    mood = 'Lo-Fi ☕';
  }

  const scored = tracks
    .map((track) => {
      let score = 50;
      if (track.bpm >= targetBpmMin && track.bpm <= targetBpmMax) score += 30;
      if (track.energy <= maxEnergy) score += 20;
      return { track, score };
    })
    .sort((a, b) => b.score - a.score);

  return {
    mixtapeTitle: title,
    arcName,
    bpmBadge: `BPM: ${targetBpmMin}–${targetBpmMax}`,
    energyRule: `Energy ≤ ${maxEnergy.toFixed(2)}`,
    moodBadge: mood,
    formatBadge: 'Lossless 24-bit',
    explanation: `Synthesized from natural language keywords and local Camelot wheel matching.`,
    orderedTrackIds: scored.slice(0, 8).map((s) => s.track.id),
  };
}

/**
 * 2. Soundbar AI Parametric Auto-EQ Curve Generator
 */
export async function generateAiEqCurve(
  prompt: string,
  currentBands: EqBand[],
  apiKey?: string
): Promise<AiAutoEqResult> {
  const ai = getGenAiClient(apiKey);

  if (ai) {
    try {
      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: [
          {
            role: 'user',
            parts: [
              {
                text: `You are an expert audio DSP engineer for a 10-band parametric equalizer (Bands: 31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz).
The user wants to adjust their sound curve with this prompt: "${prompt}".
Available gain range is -12.0dB to +12.0dB for each band.
Return JSON with the profileName, rationale, recommendedHeadroomDb (e.g. -2.5dB), and the 10 bands with target gainDb values.`,
              },
            ],
          },
        ],
        config: {
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              profileName: { type: Type.STRING },
              rationale: { type: Type.STRING },
              recommendedHeadroomDb: { type: Type.NUMBER },
              bandGains: {
                type: Type.ARRAY,
                items: { type: Type.NUMBER },
                description: 'Array of exactly 10 gain values in dB for [31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz]',
              },
            },
            required: ['profileName', 'rationale', 'recommendedHeadroomDb', 'bandGains'],
          },
        },
      });

      if (response.text) {
        const parsed = JSON.parse(response.text) as {
          profileName: string;
          rationale: string;
          recommendedHeadroomDb: number;
          bandGains: number[];
        };

        if (Array.isArray(parsed.bandGains) && parsed.bandGains.length === 10) {
          const updatedBands = currentBands.map((b, i) => ({
            ...b,
            gainDb: Math.max(-12, Math.min(12, Number(parsed.bandGains[i]) || 0)),
          }));

          return {
            profileName: parsed.profileName,
            rationale: parsed.rationale,
            recommendedHeadroomDb: parsed.recommendedHeadroomDb || -2.0,
            bands: updatedBands,
          };
        }
      }
    } catch (err) {
      console.warn('AI EQ generation fallback to local rules:', err);
    }
  }

  // Local fallback curve generator
  return fallbackLocalEqCurve(prompt, currentBands);
}

function fallbackLocalEqCurve(prompt: string, currentBands: EqBand[]): AiAutoEqResult {
  const lower = prompt.toLowerCase();
  let gains = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
  let name = 'AI Custom Sound Curve';
  let rationale = 'Heuristic curve calculated based on acoustic keywords.';

  if (lower.includes('treble') || lower.includes('sibilan') || lower.includes('harsh') || lower.includes('tame')) {
    name = 'Treble Tamer (Anti-Sibilance)';
    rationale = 'Dampened 4kHz–8kHz presence region while lifting sub-bass warmth to smooth sharp transients.';
    gains = [3.5, 3.0, 1.5, 0.5, 0, -1.0, -2.5, -4.5, -3.0, 0.5];
  } else if (lower.includes('bass') || lower.includes('sub') || lower.includes('punch') || lower.includes('kick')) {
    name = 'Harman Deep Bass Punch';
    rationale = 'Targeted sub-bass acoustic rise (31Hz–63Hz) with clean mid-bass contour to prevent boxiness.';
    gains = [6.0, 4.5, 2.5, 0.5, -0.5, 0, 0.5, 1.5, 2.0, 1.0];
  } else if (lower.includes('vocal') || lower.includes('mid') || lower.includes('voice') || lower.includes('podcast')) {
    name = 'Vocal Forward Sparkle';
    rationale = 'Boosted 1kHz–4kHz vocal fundamental frequencies with gentle low-cut to clear mud.';
    gains = [-1.5, -1.0, 0, 1.0, 2.0, 3.5, 2.5, 1.0, 0.5, 0];
  } else if (lower.includes('warm') || lower.includes('vinyl') || lower.includes('tube') || lower.includes('analog')) {
    name = 'Vintage Tube Amp Velvet';
    rationale = 'Analog warm tilt with 2nd order harmonic presence and rolled-off harsh extreme air.';
    gains = [4.5, 4.0, 3.0, 1.5, 0.5, -0.5, 0, -1.5, -2.0, -1.0];
  }

  const updatedBands = currentBands.map((b, i) => ({
    ...b,
    gainDb: gains[i] ?? 0,
  }));

  return {
    profileName: name,
    rationale,
    recommendedHeadroomDb: -2.8,
    bands: updatedBands,
  };
}

/**
 * 3. AI Harmonic DJ Queue Sequencer (Camelot Wheel 1A–12B auto-ordering)
 */
export function sequenceHarmonicDjQueue(tracks: Track[]): {
  orderedTracks: Track[];
  transitions: Array<{ from: string; to: string; rule: string; score: number }>;
} {
  if (tracks.length <= 1) {
    return { orderedTracks: tracks, transitions: [] };
  }

  const remaining = [...tracks];
  const ordered: Track[] = [remaining.shift()!];
  const transitions: Array<{ from: string; to: string; rule: string; score: number }> = [];

  while (remaining.length > 0) {
    const current = ordered[ordered.length - 1];
    let bestIdx = 0;
    let bestScore = -1;
    let bestRule = 'Energy Match';

    for (let i = 0; i < remaining.length; i++) {
      const candidate = remaining[i];
      let score = 0;
      let rule = 'Key Shift';

      // Parse Camelot key: e.g. "8A" -> num=8, letter="A"
      const curMatch = current.camelotKey.match(/(\d+)([AB])/);
      const candMatch = candidate.camelotKey.match(/(\d+)([AB])/);

      if (curMatch && candMatch) {
        const curNum = parseInt(curMatch[1], 10);
        const curLet = curMatch[2];
        const candNum = parseInt(candMatch[1], 10);
        const candLet = candMatch[2];

        if (curNum === candNum && curLet === candLet) {
          score += 50;
          rule = 'Exact Harmonic Key (Same Key)';
        } else if (curNum === candNum && curLet !== candLet) {
          score += 45;
          rule = 'Relative Major/Minor (Emotional Shift)';
        } else if (Math.abs(curNum - candNum) === 1 || (curNum === 1 && candNum === 12) || (curNum === 12 && candNum === 1)) {
          score += 40;
          rule = 'Camelot Adjacent (Energy Transition)';
        }
      }

      // Tempo compatibility (±8 BPM)
      const bpmDiff = Math.abs(current.bpm - candidate.bpm);
      if (bpmDiff <= 4) {
        score += 30;
      } else if (bpmDiff <= 10) {
        score += 20;
      } else if (bpmDiff <= 18) {
        score += 10;
      }

      // Energy continuity
      const energyDiff = Math.abs(current.energy - candidate.energy);
      if (energyDiff < 0.2) score += 20;

      if (score > bestScore) {
        bestScore = score;
        bestIdx = i;
        bestRule = rule;
      }
    }

    const nextTrack = remaining.splice(bestIdx, 1)[0];
    transitions.push({
      from: current.title,
      to: nextTrack.title,
      rule: bestRule,
      score: bestScore,
    });
    ordered.push(nextTrack);
  }

  return { orderedTracks: ordered, transitions };
}

/**
 * 4. AI Crate Historian & Audiophile Liner Notes
 */
export async function generateCrateLinerNotes(
  track: Track,
  apiKey?: string
): Promise<AiLinerNotesResult> {
  const ai = getGenAiClient(apiKey);

  if (ai) {
    try {
      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: [
          {
            role: 'user',
            parts: [
              {
                text: `You are an audiophile music historian and mastering engineer. Provide deep sonic liner notes for:
Title: "${track.title}"
Artist: "${track.artist}"
Album: "${track.album}"
Format: ${track.formatBadge} (${track.sampleRate}, ${track.bitDepth})
BPM: ${track.bpm}, Camelot Key: ${track.camelotKey}, Energy: ${track.energy}

Return JSON with tagline, masteringTrivia, instrumentationBreakdown (3-4 items), recommendedDacTuning, and acousticVectorSummary.`,
              },
            ],
          },
        ],
        config: {
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              tagline: { type: Type.STRING },
              masteringTrivia: { type: Type.STRING },
              instrumentationBreakdown: {
                type: Type.ARRAY,
                items: { type: Type.STRING },
              },
              recommendedDacTuning: { type: Type.STRING },
              acousticVectorSummary: { type: Type.STRING },
            },
            required: [
              'tagline',
              'masteringTrivia',
              'instrumentationBreakdown',
              'recommendedDacTuning',
              'acousticVectorSummary',
            ],
          },
        },
      });

      if (response.text) {
        return JSON.parse(response.text) as AiLinerNotesResult;
      }
    } catch (err) {
      console.warn('AI liner notes generation fallback:', err);
    }
  }

  // Local fallback
  return {
    tagline: `Pristine ${track.sampleRate} master cut with high transient dynamic range.`,
    masteringTrivia: `Rendered in uncompressed ${track.bitDepth} stereo from reference ALSA studio stems, preserving zero-clipping headroom.`,
    instrumentationBreakdown: [
      `Analog synthesizers & tube-compressed percussion (${track.bpm} BPM)`,
      `Harmonic center anchored in ${track.camelotKey} camelot wheel space`,
      `Wide stereo imaging optimized for bit-perfect USB-C DAC host output`,
    ],
    recommendedDacTuning: `Best paired with Dual ES9069Q or CS43198 DACs in Bit-Perfect Exclusive mode with Fast Linear phase filter.`,
    acousticVectorSummary: `Energy: ${Math.round(track.energy * 100)}% • Danceability: ${Math.round(track.danceability * 100)}% • Tempo: ${track.tempoLabel}`,
  };
}
