import type { LiveEvent } from '../types';
import { getApiBaseUrl } from '@/shared/api/baseUrl';

const API_URL = getApiBaseUrl();

export type LiveStreamHandlers = {
  onEvent: (event: LiveEvent) => void;
  onConnected?: () => void;
  onDisconnected?: () => void;
  onError?: () => void;
};

export function buildLiveStreamUrl(accessToken: string): string {
  const base = `${API_URL}/api/v1/live/events`;
  const params = new URLSearchParams({ access_token: accessToken });
  return `${base}?${params.toString()}`;
}

export function connectLiveStream(
  accessToken: string,
  handlers: LiveStreamHandlers,
): () => void {
  const source = new EventSource(buildLiveStreamUrl(accessToken));

  source.addEventListener('connected', () => {
    handlers.onConnected?.();
  });

  source.addEventListener('live-event', (message) => {
    try {
      const payload = JSON.parse((message as MessageEvent<string>).data) as LiveEvent;
      handlers.onEvent(payload);
    } catch {
      handlers.onError?.();
    }
  });

  source.addEventListener('heartbeat', () => {
    handlers.onConnected?.();
  });

  source.onerror = () => {
    handlers.onError?.();
    handlers.onDisconnected?.();
  };

  return () => {
    source.close();
    handlers.onDisconnected?.();
  };
}
