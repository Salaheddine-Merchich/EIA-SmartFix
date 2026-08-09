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

export function connectLiveStream(
  _accessToken: string,
  handlers: LiveStreamHandlers,
): () => void {
  const controller = new AbortController();
  let closed = false;

  const disconnect = () => {
    if (closed) return;
    closed = true;
    controller.abort();
    handlers.onDisconnected?.();
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
            handlers.onError?.();
          }
        }
      },
      onError: () => {
        if (!closed) {
          handlers.onError?.();
          handlers.onDisconnected?.();
        }
      },
    },
    { signal: controller.signal },
  ).catch(() => {
    // onError already notified
  });

  return disconnect;
}
