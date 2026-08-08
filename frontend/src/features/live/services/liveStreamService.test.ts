import { describe, expect, it, vi, beforeEach } from 'vitest';
import { buildLiveStreamUrl, connectLiveStream } from '../services/liveStreamService';

describe('liveStreamService', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('buildLiveStreamUrl includes access token', () => {
    const url = buildLiveStreamUrl('abc123');
    expect(url).toContain('/api/v1/live/events');
    expect(url).toContain('access_token=abc123');
  });

  it('connectLiveStream forwards live-event payload', () => {
    const listeners: Record<string, (event: MessageEvent) => void> = {};
    const close = vi.fn();
    vi.stubGlobal(
      'EventSource',
      vi.fn(() => ({
        addEventListener: (name: string, cb: (event: MessageEvent) => void) => {
          listeners[name] = cb;
        },
        close,
        onerror: null,
      })),
    );

    const onEvent = vi.fn();
    const disconnect = connectLiveStream('token', { onEvent });

    listeners['live-event']({
      data: JSON.stringify({
        id: '1',
        type: 'FAILURE_CREATED',
        category: 'maintenance',
        title: 'Nouvelle panne',
        message: 'EQ-1',
        occurredAt: new Date().toISOString(),
        metadata: {},
      }),
    } as MessageEvent);

    expect(onEvent).toHaveBeenCalledOnce();
    disconnect();
    expect(close).toHaveBeenCalled();
  });
});
