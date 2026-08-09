import { clearAuthSession, refreshAccessToken } from './client';

export type SseHandlers = {
  onEvent: (event: string, data: string) => void;
  onError?: (error: Error) => void;
};

/**
 * SSE client with Authorization Bearer (EventSource cannot set headers).
 * Retries once after refresh on 401/403.
 */
export async function connectSse(
  url: string,
  handlers: SseHandlers,
  options: { signal?: AbortSignal } = {},
): Promise<void> {
  const run = async (accessToken: string | null, allowRefresh: boolean): Promise<void> => {
    const headers: Record<string, string> = {
      Accept: 'text/event-stream',
    };
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }

    const response = await fetch(url, {
      method: 'GET',
      headers,
      signal: options.signal,
    });

    if ((response.status === 401 || response.status === 403) && allowRefresh) {
      const newToken = await refreshAccessToken();
      if (!newToken) {
        clearAuthSession();
        throw new Error('Session expirée.');
      }
      return run(newToken, false);
    }

    if (!response.ok) {
      throw new Error(`SSE connection failed (${response.status})`);
    }
    if (!response.body) {
      throw new Error('SSE response body is empty');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let eventName = 'message';
    let dataLines: string[] = [];

    const flush = () => {
      if (dataLines.length === 0) {
        eventName = 'message';
        return;
      }
      handlers.onEvent(eventName, dataLines.join('\n'));
      eventName = 'message';
      dataLines = [];
    };

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        flush();
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const parts = buffer.split(/\r?\n/);
      buffer = parts.pop() ?? '';

      for (const line of parts) {
        if (line === '') {
          flush();
          continue;
        }
        if (line.startsWith(':')) {
          continue;
        }
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim();
          continue;
        }
        if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trimStart());
        }
      }
    }
  };

  try {
    await run(localStorage.getItem('accessToken'), true);
  } catch (error) {
    if (options.signal?.aborted) {
      return;
    }
    const err = error instanceof Error ? error : new Error(String(error));
    handlers.onError?.(err);
    throw err;
  }
}
