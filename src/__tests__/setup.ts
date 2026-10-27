import '@testing-library/jest-dom';
import { vi } from 'vitest';

class MockAudioContext {
  currentTime = 0;
  createGain() {
    return {
      gain: { value: 1, setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn(), linearRampToValueAtTime: vi.fn() },
      connect: vi.fn(),
    };
  }
  createBiquadFilter() {
    return {
      type: 'peaking',
      frequency: { value: 1000, setValueAtTime: vi.fn() },
      Q: { value: 1, setValueAtTime: vi.fn() },
      gain: { value: 0, setValueAtTime: vi.fn() },
      connect: vi.fn(),
    };
  }
  createDelay() {
    return {
      delayTime: { value: 0 },
      connect: vi.fn(),
    }
  }
  createOscillator() {
      return {
          type: 'sine',
          frequency: { value: 440, setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
          connect: vi.fn(),
          start: vi.fn(),
          stop: vi.fn()
      }
  }
  createBufferSource() {
    return {
      buffer: null,
      connect: vi.fn(),
      start: vi.fn(),
      stop: vi.fn(),
      onended: null,
    };
  }
  decodeAudioData() {
    return Promise.resolve({ duration: 100 });
  }
  resume() {
    return Promise.resolve();
  }
  state = 'running';
  destination = {};
}

global.window.AudioContext = MockAudioContext as any;
global.window.webkitAudioContext = MockAudioContext as any;
