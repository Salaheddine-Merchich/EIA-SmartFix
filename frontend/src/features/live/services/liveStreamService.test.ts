import { describe, expect, it, vi, beforeEach } from 'vitest';
import { buildLiveStreamUrl, connectLiveStream } from '../services/liveStreamService';

function stubLocalStorage(initial: Record<string, string> = {}) {
  const store = new Map<string, string>(Object.entries(initial));
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, value);
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
  });
}

describe('liveStreamService', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    stubLocalStorage({ accessToken: 'token' });
  });

  it('buildLiveStreamUrl has no access_token query param', () => {
    const url = buildLiveStreamUrl();
    expect(url).toContain('/api/v1/live/events');
    expect(url).not.toContain('access_token');
  });

  it('connectLiveStream forwards live-event payload via fetch SSE', async () => {
    const encoder = new TextEncoder();
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(
          encoder.encode(
            'event: live-event\ndata: {"id":"1","type":"FAILURE_CREATED","category":"maintenance","title":"Nouvelle panne","message":"EQ-1","occurredAt":"2026-01-01T00:00:00Z","metadata":{}}\n\n',
          ),
        );
        controller.close();
      },
    });
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        body,
      }),
    );

    const onEvent = vi.fn();
    const disconnect = connectLiveStream('token', { onEvent });

    await vi.waitFor(() => expect(onEvent).toHaveBeenCalledOnce());
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/live/events'),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer token',
        }),
      }),
    );
    disconnect();
  });
});
