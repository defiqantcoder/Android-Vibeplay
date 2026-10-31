import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { probeConnectedAudioHardware } from '../audioEngine';
import { UsbDacEndpoint } from '../../data/vibeplayData';

describe('probeConnectedAudioHardware', () => {
  let originalNavigator: any;

  beforeEach(() => {
    // navigator is a read-only property in jsdom, so we have to use Object.defineProperty
    originalNavigator = global.navigator;
  });

  afterEach(() => {
    Object.defineProperty(global, 'navigator', {
      value: originalNavigator,
      writable: true,
      configurable: true
    });
    vi.restoreAllMocks();
  });

  it('should return existing models if navigator is undefined', async () => {
    const existingModels: UsbDacEndpoint[] = [{ id: '1', modelName: 'Test DAC' } as UsbDacEndpoint];

    Object.defineProperty(global, 'navigator', {
      value: undefined,
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware(existingModels);

    expect(result).toEqual(existingModels);
    expect(result).not.toBe(existingModels); // Should return a copy
  });

  it('should discover WebUSB devices and MediaDevices', async () => {
    const existingModels: UsbDacEndpoint[] = [];

    const mockGetDevices = vi.fn().mockResolvedValue([
      {
        vendorId: 0x2fc6,
        productId: 0xf702,
        productName: 'Hugo 2',
        manufacturerName: 'Chord'
      }
    ]);

    const mockEnumerateDevices = vi.fn().mockResolvedValue([
      {
        kind: 'audiooutput',
        label: 'Speakers (Realtek Audio)',
        deviceId: 'realtek-123'
      },
      {
        kind: 'audiooutput',
        label: 'Default',
        deviceId: 'default' // should be ignored
      }
    ]);

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: {
          getDevices: mockGetDevices
        },
        mediaDevices: {
          enumerateDevices: mockEnumerateDevices
        }
      },
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware(existingModels);

    expect(result.length).toBe(2);

    // Check MediaDevice (unshifted last, so it's at index 0)
    expect(result[0].modelName).toBe('Speakers (Realtek Audio)');
    expect(result[0].detectionSource).toBe('OS Audio Output Sink');
    expect(result[0].sinkId).toBe('realtek-123');

    // Check WebUSB device
    expect(result[1].modelName).toBe('Hugo 2');
    expect(result[1].vendor).toBe('Chord Electronics');
    expect(result[1].detectionSource).toBe('WebUSB Hardware Descriptor');
    expect(result[1].usbVidPid).toBe('2FC6:F702');
  });

  it('should not add duplicate devices based on VID/PID or name', async () => {
    const existingModels: UsbDacEndpoint[] = [
      {
        id: 'ex-1',
        modelName: 'Existing Hugo 2',
        usbVidPid: '2FC6:F702', // Matches mock WebUSB device
      } as UsbDacEndpoint,
      {
        id: 'ex-2',
        modelName: 'Speakers (Realtek Audio)', // Matches mock MediaDevice
      } as UsbDacEndpoint
    ];

    const mockGetDevices = vi.fn().mockResolvedValue([
      {
        vendorId: 0x2fc6,
        productId: 0xf702,
        productName: 'Hugo 2',
        manufacturerName: 'Chord'
      }
    ]);

    const mockEnumerateDevices = vi.fn().mockResolvedValue([
      {
        kind: 'audiooutput',
        label: 'Speakers (Realtek Audio)',
        deviceId: 'realtek-123'
      }
    ]);

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: {
          getDevices: mockGetDevices
        },
        mediaDevices: {
          enumerateDevices: mockEnumerateDevices
        }
      },
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware(existingModels);

    // No new devices should be added because they already exist
    expect(result.length).toBe(2);
    expect(result[0].id).toBe('ex-1');
    expect(result[1].id).toBe('ex-2');
  });

  it('should gracefully handle errors when enumerating devices', async () => {
    const existingModels: UsbDacEndpoint[] = [{ id: '1', modelName: 'Test DAC' } as UsbDacEndpoint];

    const mockGetDevices = vi.fn().mockRejectedValue(new Error('Permission denied'));
    const mockEnumerateDevices = vi.fn().mockRejectedValue(new Error('Permission denied'));

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: {
          getDevices: mockGetDevices
        },
        mediaDevices: {
          enumerateDevices: mockEnumerateDevices
        }
      },
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware(existingModels);

    // The errors should be caught and ignored, returning the original models
    expect(result).toEqual(existingModels);
  });
});
