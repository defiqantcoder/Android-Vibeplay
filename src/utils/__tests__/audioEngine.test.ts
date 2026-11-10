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

  it('should keep MediaDevices results when WebUSB enumeration fails', async () => {
    const mockGetDevices = vi.fn().mockRejectedValue(new Error('Permission denied'));
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
        usb: { getDevices: mockGetDevices },
        mediaDevices: { enumerateDevices: mockEnumerateDevices }
      },
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware([]);

    expect(result.some((model) => model.modelName === 'Speakers (Realtek Audio)')).toBe(true);
  });

  it('should keep WebUSB results when MediaDevices enumeration fails', async () => {
    const mockGetDevices = vi.fn().mockResolvedValue([
      {
        vendorId: 0x2fc6,
        productId: 0xf702,
        productName: 'Hugo 2',
        manufacturerName: 'Chord'
      }
    ]);
    const mockEnumerateDevices = vi.fn().mockRejectedValue(new Error('Permission denied'));

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: { getDevices: mockGetDevices },
        mediaDevices: { enumerateDevices: mockEnumerateDevices }
      },
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware([]);

    expect(result.some((model) => model.modelName === 'Hugo 2')).toBe(true);
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

  it('should ignore MediaDevices that are not audiooutput or have invalid deviceIds or short labels', async () => {
    const mockEnumerateDevices = vi.fn().mockResolvedValue([
      { kind: 'videoinput', label: 'Camera', deviceId: 'cam-1' },
      { kind: 'audiooutput', label: 'Default', deviceId: 'default' },
      { kind: 'audiooutput', label: 'Communications', deviceId: 'communications' },
      { kind: 'audiooutput', label: 'a ', deviceId: 'short-1' },
      { kind: 'audiooutput', label: 'Good Speaker', deviceId: 'speaker-1' },
    ]);

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        mediaDevices: { enumerateDevices: mockEnumerateDevices }
      },
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware([]);
    expect(result.length).toBe(1);
    expect(result[0].modelName).toBe('Good Speaker');
    expect(result[0].sinkId).toBe('speaker-1');
  });

  it('should provide fallback productName for WebUSB devices if productName is empty', async () => {
    const mockGetDevices = vi.fn().mockResolvedValue([
      { vendorId: 0x1234, productId: 0x5678, productName: '   ', manufacturerName: 'TestCo' },
      { vendorId: 0xabcd, productId: 0xef01 }
    ]);

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: { getDevices: mockGetDevices }
      },
      writable: true,
      configurable: true
    });

    const result = await probeConnectedAudioHardware([]);
    expect(result.length).toBe(2);
    // Unshifted, so index 0 is the second device (abcd:ef01)
    expect(result[0].modelName).toBe('USB Audio DAC (ABCD:EF01)');
    expect(result[1].modelName).toBe('USB Audio DAC (1234:5678)');
  });
});

describe('requestWebUsbDacHardware', () => {
  let originalNavigator: any;

  beforeEach(() => {
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

  it('should return null if navigator is undefined', async () => {
    Object.defineProperty(global, 'navigator', {
      value: undefined,
      writable: true,
      configurable: true
    });

    const { requestWebUsbDacHardware } = await import('../audioEngine');
    const result = await requestWebUsbDacHardware();

    expect(result).toBeNull();
  });

  it('should return null if navigator.usb is undefined', async () => {
    Object.defineProperty(global, 'navigator', {
      value: { ...originalNavigator, usb: undefined },
      writable: true,
      configurable: true
    });

    const { requestWebUsbDacHardware } = await import('../audioEngine');
    const result = await requestWebUsbDacHardware();

    expect(result).toBeNull();
  });

  it('should request a device with classCode 1 first, and return a parsed profile', async () => {
    const mockRequestDevice = vi.fn().mockResolvedValue({
      vendorId: 0x2fc6,
      productId: 0xf702,
      productName: 'Hugo 2',
      manufacturerName: 'Chord'
    });

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: { requestDevice: mockRequestDevice }
      },
      writable: true,
      configurable: true
    });

    const { requestWebUsbDacHardware } = await import('../audioEngine');
    const result = await requestWebUsbDacHardware();

    expect(mockRequestDevice).toHaveBeenCalledWith({ filters: [{ classCode: 1 }] });
    expect(result).toBeDefined();
    expect(result!.modelName).toBe('Hugo 2');
    expect(result!.vendor).toBe('Chord Electronics');
    expect(result!.detectionSource).toBe('WebUSB Direct Pairing');
    expect(result!.usbVidPid).toBe('2FC6:F702');
  });

  it('should fallback to empty filters if classCode 1 request fails', async () => {
    const mockRequestDevice = vi.fn()
      .mockRejectedValueOnce(new Error('No device found'))
      .mockResolvedValueOnce({
        vendorId: 0x2fc6,
        productId: 0xf702,
        productName: 'Hugo 2',
        manufacturerName: 'Chord'
      });

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: { requestDevice: mockRequestDevice }
      },
      writable: true,
      configurable: true
    });

    const { requestWebUsbDacHardware } = await import('../audioEngine');
    const result = await requestWebUsbDacHardware();

    expect(mockRequestDevice).toHaveBeenCalledTimes(2);
    expect(mockRequestDevice).toHaveBeenNthCalledWith(1, { filters: [{ classCode: 1 }] });
    expect(mockRequestDevice).toHaveBeenNthCalledWith(2, { filters: [] });

    expect(result).toBeDefined();
    expect(result!.modelName).toBe('Hugo 2');
  });

  it('should return null if both requests fail', async () => {
    const mockRequestDevice = vi.fn().mockRejectedValue(new Error('Cancelled'));

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: { requestDevice: mockRequestDevice }
      },
      writable: true,
      configurable: true
    });

    const { requestWebUsbDacHardware } = await import('../audioEngine');
    const result = await requestWebUsbDacHardware();

    expect(mockRequestDevice).toHaveBeenCalledTimes(2);
    expect(result).toBeNull();
  });
});

describe('subscribeToHardwareHotplug', () => {
  let originalNavigator: any;

  beforeEach(() => {
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

  it('should return a no-op function if navigator is undefined', async () => {
    Object.defineProperty(global, 'navigator', {
      value: undefined,
      writable: true,
      configurable: true
    });

    const { subscribeToHardwareHotplug } = await import('../audioEngine');
    const unsubscribe = subscribeToHardwareHotplug(() => {});

    expect(typeof unsubscribe).toBe('function');
    // Calling it should not throw
    unsubscribe();
  });

  it('should add event listeners and return a function to remove them', async () => {
    const mockMediaDevicesAdd = vi.fn();
    const mockMediaDevicesRemove = vi.fn();
    const mockUsbAdd = vi.fn();
    const mockUsbRemove = vi.fn();

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        mediaDevices: {
          addEventListener: mockMediaDevicesAdd,
          removeEventListener: mockMediaDevicesRemove
        },
        usb: {
          addEventListener: mockUsbAdd,
          removeEventListener: mockUsbRemove
        }
      },
      writable: true,
      configurable: true
    });

    const mockCallback = vi.fn();
    const { subscribeToHardwareHotplug } = await import('../audioEngine');
    const unsubscribe = subscribeToHardwareHotplug(mockCallback);

    expect(mockMediaDevicesAdd).toHaveBeenCalledWith('devicechange', expect.any(Function));
    expect(mockUsbAdd).toHaveBeenCalledWith('connect', expect.any(Function));
    expect(mockUsbAdd).toHaveBeenCalledWith('disconnect', expect.any(Function));

    // Simulate devicechange event
    const handleDeviceChange = mockMediaDevicesAdd.mock.calls[0][1];
    handleDeviceChange();
    expect(mockCallback).toHaveBeenCalledTimes(1);

    unsubscribe();

    expect(mockMediaDevicesRemove).toHaveBeenCalledWith('devicechange', expect.any(Function));
    expect(mockUsbRemove).toHaveBeenCalledWith('connect', expect.any(Function));
    expect(mockUsbRemove).toHaveBeenCalledWith('disconnect', expect.any(Function));
  });

  it('should handle undefined event listeners safely', async () => {
    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        mediaDevices: {},
        usb: {}
      },
      writable: true,
      configurable: true
    });

    const mockCallback = vi.fn();
    const { subscribeToHardwareHotplug } = await import('../audioEngine');
    const unsubscribe = subscribeToHardwareHotplug(mockCallback);

    expect(typeof unsubscribe).toBe('function');
    unsubscribe(); // Should not throw
  });
});

describe('probeConnectedAudioHardware (Extended Coverage)', () => {
  let originalNavigator: any;

  beforeEach(() => {
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

  it('should ignore default and communications devices in MediaDevices', async () => {
    const mockGetDevices = vi.fn().mockResolvedValue([]);
    const mockEnumerateDevices = vi.fn().mockResolvedValue([
      { kind: 'audiooutput', label: 'Default', deviceId: 'default' },
      { kind: 'audiooutput', label: 'Communications', deviceId: 'communications' },
      { kind: 'audiooutput', label: 'Real Speakers', deviceId: 'real-123' },
      { kind: 'audioinput', label: 'Mic', deviceId: 'mic-123' }, // Should ignore audioinput
      { kind: 'audiooutput', label: '', deviceId: 'empty-label' } // Should ignore empty label
    ]);

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: { getDevices: mockGetDevices },
        mediaDevices: { enumerateDevices: mockEnumerateDevices }
      },
      writable: true,
      configurable: true
    });

    const { probeConnectedAudioHardware } = await import('../audioEngine');
    const result = await probeConnectedAudioHardware([]);

    expect(result.length).toBe(1);
    expect(result[0].modelName).toBe('Real Speakers');
    expect(result[0].sinkId).toBe('real-123');
  });

  it('should correctly format default product names for WebUSB devices without product name', async () => {
    const mockGetDevices = vi.fn().mockResolvedValue([
      {
        vendorId: 0x1234,
        productId: 0x5678,
        manufacturerName: 'Generic Audio'
        // productName is missing
      }
    ]);
    const mockEnumerateDevices = vi.fn().mockResolvedValue([]);

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: { getDevices: mockGetDevices },
        mediaDevices: { enumerateDevices: mockEnumerateDevices }
      },
      writable: true,
      configurable: true
    });

    const { probeConnectedAudioHardware } = await import('../audioEngine');
    const result = await probeConnectedAudioHardware([]);

    expect(result.length).toBe(1);
    expect(result[0].modelName).toBe('USB Audio DAC (1234:5678)');
    expect(result[0].usbVidPid).toBe('1234:5678');
  });

  it('should handle undefined usb in navigator gracefully', async () => {
    const mockEnumerateDevices = vi.fn().mockResolvedValue([
      { kind: 'audiooutput', label: 'Speakers', deviceId: 'real-123' }
    ]);

    Object.defineProperty(global, 'navigator', {
      value: {
        ...originalNavigator,
        usb: undefined, // USB API not supported
        mediaDevices: { enumerateDevices: mockEnumerateDevices }
      },
      writable: true,
      configurable: true
    });

    const { probeConnectedAudioHardware } = await import('../audioEngine');
    const result = await probeConnectedAudioHardware([]);

    expect(result.length).toBe(1);
    expect(result[0].modelName).toBe('Speakers');
  });
});
