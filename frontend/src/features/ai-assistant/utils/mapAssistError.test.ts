import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import { isEmptyAssistResult, mapAssistError } from './mapAssistError';

function axiosError(partial: {
  status?: number;
  code?: string;
  message?: string;
  data?: unknown;
  noResponse?: boolean;
}): AxiosError {
  const error = new AxiosError(partial.message ?? 'error');
  error.code = partial.code;
  if (!partial.noResponse && partial.status != null) {
    error.response = {
      status: partial.status,
      data: partial.data ?? {},
      statusText: '',
      headers: {},
      config: {} as never,
    };
  }
  return error;
}

describe('mapAssistError', () => {
  it('maps network failure to connection', () => {
    const result = mapAssistError(axiosError({ noResponse: true, message: 'Network Error' }));
    expect(result.kind).toBe('connection');
  });

  it('maps cancelled requests', () => {
    const error = new AxiosError('canceled');
    error.code = 'ERR_CANCELED';
    const result = mapAssistError(error);
    expect(result.kind).toBe('cancelled');
  });

  it('maps timeout', () => {
    const result = mapAssistError(axiosError({ code: 'ECONNABORTED', message: 'timeout' }));
    expect(result.kind).toBe('timeout');
  });

  it('maps 401 to auth', () => {
    const result = mapAssistError(axiosError({ status: 401 }));
    expect(result.kind).toBe('auth');
  });

  it('maps 504 gateway timeout to timeout', () => {
    const result = mapAssistError(axiosError({ status: 504 }));
    expect(result.kind).toBe('timeout');
    expect(result.message).toContain('trop de temps');
  });

  it('maps 502 bad gateway to timeout', () => {
    const result = mapAssistError(axiosError({ status: 502 }));
    expect(result.kind).toBe('timeout');
    expect(result.message).toContain('proxy');
  });

  it('maps ollama unavailable message', () => {
    const result = mapAssistError(
      axiosError({ status: 500, data: { message: 'Ollama indisponible' } }),
    );
    expect(result.kind).toBe('ollama');
  });

  it('maps truncated SSE stream to backend error', () => {
    const result = mapAssistError(new Error('Stream ended before complete response'));
    expect(result.kind).toBe('backend');
    expect(result.message).toContain('interrompu');
  });
});

describe('isEmptyAssistResult', () => {
  it('detects empty similar interventions with fallback copy', () => {
    expect(
      isEmptyAssistResult({
        similarInterventions: [],
        suggestions: {
          probableCauses: ['Aucune intervention similaire validée trouvée'],
          summary: 'Pas assez de données',
        },
      }),
    ).toBe(true);
  });

  it('returns false when similar interventions exist', () => {
    expect(
      isEmptyAssistResult({
        similarInterventions: [{}],
        suggestions: { probableCauses: ['Cause'], summary: 'OK' },
      }),
    ).toBe(false);
  });
});
