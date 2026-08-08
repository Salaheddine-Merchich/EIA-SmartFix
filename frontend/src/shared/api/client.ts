import axios, { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from '@/shared/types';
import { getApiBaseUrl } from './baseUrl';

const API_URL = getApiBaseUrl();

export const AUTH_TOKEN_REFRESHED_EVENT = 'auth:token-refreshed';

export const api = axios.create({
  baseURL: API_URL,
});

let refreshPromise: Promise<string | null> | null = null;

function notifyTokenRefreshed(accessToken: string, authResponse: AuthResponse) {
  localStorage.setItem('accessToken', authResponse.accessToken);
  localStorage.setItem('refreshToken', authResponse.refreshToken);
  localStorage.setItem('user', JSON.stringify(authResponse));
  window.dispatchEvent(
    new CustomEvent(AUTH_TOKEN_REFRESHED_EVENT, { detail: { accessToken } }),
  );
}

export function clearAuthSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

export async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = (async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      return null;
    }

    try {
      const { data } = await axios.post<AuthResponse>(`${API_URL}/api/v1/auth/refresh`, {
        refreshToken,
      });
      notifyTokenRefreshed(data.accessToken, data);
      return data.accessToken;
    } catch {
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

function hadAuthorizationHeader(config: InternalAxiosRequestConfig): boolean {
  const header = config.headers?.Authorization;
  return typeof header === 'string' && header.length > 0;
}

type RetryableRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean };

function shouldAttemptTokenRefresh(
  status: number | undefined,
  config: RetryableRequestConfig,
): boolean {
  if (config._retry || isPublicAuthRequest(config.url)) {
    return false;
  }
  if (status !== 401 && status !== 403) {
    return false;
  }
  return hadAuthorizationHeader(config) || !!localStorage.getItem('accessToken');
}

function isPublicAuthRequest(url: string | undefined): boolean {
  if (!url) return false;
  return url.includes('/api/v1/auth/login') || url.includes('/api/v1/auth/refresh');
}

function shouldUseJsonContentType(data: unknown): boolean {
  if (data == null) return false;
  if (data instanceof FormData) return false;
  if (typeof data === 'string') return false;
  if (data instanceof Blob) return false;
  if (data instanceof ArrayBuffer) return false;
  return typeof data === 'object';
}

function clearContentTypeHeader(config: InternalAxiosRequestConfig): void {
  if (!config.headers) return;
  if (config.headers instanceof AxiosHeaders) {
    config.headers.setContentType(false);
    return;
  }
  const headers = config.headers as Record<string, unknown> & { delete?: (name: string) => void };
  headers.delete?.('Content-Type');
  delete headers['Content-Type'];
}

function setJsonContentType(config: InternalAxiosRequestConfig): void {
  if (!config.headers) {
    config.headers = new AxiosHeaders();
  }
  if (config.headers instanceof AxiosHeaders) {
    config.headers.setContentType('application/json');
    return;
  }
  (config.headers as Record<string, string>)['Content-Type'] = 'application/json';
}

export function createApiError(status: number, data: unknown, statusText = 'Error'): AxiosError {
  return new AxiosError(
    'Request failed',
    String(status),
    undefined,
    undefined,
    {
      status,
      data,
      statusText,
      headers: {},
      config: { headers: new AxiosHeaders() } as InternalAxiosRequestConfig,
    },
  );
}

api.interceptors.request.use((config) => {
  if (config.data instanceof FormData) {
    clearContentTypeHeader(config);
  } else if (shouldUseJsonContentType(config.data)) {
    setJsonContentType(config);
  }
  if (isPublicAuthRequest(config.url)) {
    delete config.headers.Authorization;
    return config;
  }
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as RetryableRequestConfig | undefined;
    if (!original || !shouldAttemptTokenRefresh(error.response?.status, original)) {
      return Promise.reject(error);
    }

    original._retry = true;
    const newToken = await refreshAccessToken();
    if (!newToken) {
      clearAuthSession();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    original.headers.Authorization = `Bearer ${newToken}`;
    return api(original);
  },
);

export default api;
