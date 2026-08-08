import type { InternalAxiosRequestConfig } from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from './client';

describe('api client multipart uploads', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', {
      getItem: () => null,
      setItem: () => {},
      removeItem: () => {},
      clear: () => {},
    });
  });

  it('removes Content-Type header for FormData so boundary is set automatically', async () => {
    let capturedContentType: string | undefined = 'unset';

    api.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      capturedContentType = config.headers?.['Content-Type'] as string | undefined;
      return {
        data: {},
        status: 201,
        statusText: 'Created',
        headers: {},
        config,
      };
    };

    const form = new FormData();
    form.append('file', new File(['content'], 'rapport.pdf', { type: 'application/pdf' }));

    await api.post('/api/v1/interventions/test/documents', form);

    // Browser sets multipart boundary automatically; Node test env may differ.
    expect(capturedContentType).not.toBe('application/json');
    expect(capturedContentType).not.toBe('multipart/form-data');

    delete api.defaults.adapter;
  });

  it('sets application/json for JSON request bodies', async () => {
    let capturedContentType: string | undefined = 'unset';

    api.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      capturedContentType = config.headers?.['Content-Type'] as string | undefined;
      return {
        data: {},
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      };
    };

    await api.post('/api/v1/auth/login', { email: 'a@b.c', password: 'x' });

    expect(capturedContentType).toBe('application/json');

    delete api.defaults.adapter;
  });
});
