import { describe, expect, it, vi, beforeEach } from 'vitest';
import { connectSse } from './sseFetch';

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

describe('connectSse', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    stubLocalStorage({ accessToken: 'tok' });
  });

  it('parses named SSE events with Bearer header', async () => {
    const encoder = new TextEncoder();
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode('event: status\ndata: hello\n\n'));
        controller.close();
      },
    });
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, body });
    vi.stubGlobal('fetch', fetchMock);

    const onEvent = vi.fn();
    await connectSse('/api/v1/live/events', { onEvent });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/live/events',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer tok' }),
      }),
    );
    expect(onEvent).toHaveBeenCalledWith('status', 'hello');
  });
});
