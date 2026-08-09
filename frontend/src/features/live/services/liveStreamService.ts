import type { LiveEvent } from '../types';
import { getApiBaseUrl } from '@/shared/api/baseUrl';
import { connectSse } from '@/shared/api/sseFetch';

const API_URL = getApiBaseUrl();

export type LiveStreamHandlers = {
  onEvent: (event: LiveEvent) => void;
  onConnected?: () => void;
  onDisconnected?: () => void;
  onError?: () => void;
};

export function buildLiveStreamUrl(): string {
  return `${API_URL}/api/v1/live/events`;
}

/**
 * Opens a live SSE stream. Returns an abort function (does not emit disconnect handlers).
 * Stream end or transport error notify via onDisconnected / onError so the provider can reconnect.
 */
export function connectLiveStream(
  _accessToken: string,
  handlers: LiveStreamHandlers,
): () => void {
  const controller = new AbortController();
  let settled = false;

  const abort = () => {
    if (settled) return;
    settled = true;
    controller.abort();
  };

  void connectSse(
    buildLiveStreamUrl(),
    {
      onEvent: (event, data) => {
        if (event === 'connected' || event === 'heartbeat') {
          handlers.onConnected?.();
          return;
        }
        if (event === 'live-event') {
          try {
            handlers.onEvent(JSON.parse(data) as LiveEvent);
          } catch {
            if (!settled) {
              settled = true;
              handlers.onError?.();
            }
          }
        }
      },
      onError: () => {
        if (!settled) {
          settled = true;
          handlers.onError?.();
        }
      },
    },
    { signal: controller.signal },
  )
    .then(() => {
      // Clean stream end (proxy idle, backend restart) — not an intentional abort.
      if (!settled) {
        settled = true;
        handlers.onDisconnected?.();
      }
    })
    .catch(() => {
      // onError already notified (unless aborted)
    });

  return abort;
}
