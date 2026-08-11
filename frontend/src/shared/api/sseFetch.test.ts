import { describe, expect, it, vi, beforeEach } from 'vitest';
import { connectSse } from './sseFetch';

describe('connectSse', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('parses named SSE events with credentials include', async () => {
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
        credentials: 'include',
        headers: expect.objectContaining({ Accept: 'text/event-stream' }),
      }),
    );
    expect(onEvent).toHaveBeenCalledWith('status', 'hello');
  });
});
