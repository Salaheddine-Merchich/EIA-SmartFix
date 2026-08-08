import { act, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';
import { authApi } from '@/shared/api';
import { clearAuthSession, refreshAccessToken } from '@/shared/api/client';

vi.mock('@/shared/api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
  },
}));

vi.mock('@/shared/api/client', () => ({
  AUTH_TOKEN_REFRESHED_EVENT: 'auth-token-refreshed',
  clearAuthSession: vi.fn(),
  refreshAccessToken: vi.fn(),
}));

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
    clear: () => {
      store.clear();
    },
  });
  return store;
}

function Probe() {
  const { isAuthenticated, isBootstrapping, user, login, logout, hasRole } = useAuth();
  return (
    <div>
      <span data-testid="bootstrapping">{String(isBootstrapping)}</span>
      <span data-testid="authenticated">{String(isAuthenticated)}</span>
      <span data-testid="email">{user?.email ?? ''}</span>
      <span data-testid="has-admin">{String(hasRole('ADMIN'))}</span>
      <button type="button" onClick={() => login('tech@ocp.ma', 'Password123!')}>
        login
      </button>
      <button type="button" onClick={() => logout()}>
        logout
      </button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    stubLocalStorage();
    vi.clearAllMocks();
    vi.mocked(refreshAccessToken).mockResolvedValue(null);
  });

  it('logs in, stores session, and exposes role helpers', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      role: 'TECHNICIEN',
      nomPrenom: 'Tech OCP',
      email: 'tech@ocp.ma',
    });

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('bootstrapping')).toHaveTextContent('false');
    });

    await act(async () => {
      screen.getByRole('button', { name: 'login' }).click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });
    expect(screen.getByTestId('email')).toHaveTextContent('tech@ocp.ma');
    expect(screen.getByTestId('has-admin')).toHaveTextContent('false');
    expect(localStorage.getItem('accessToken')).toBe('access');
    expect(localStorage.getItem('refreshToken')).toBe('refresh');
  });

  it('clears session on logout', async () => {
    stubLocalStorage({
      accessToken: 'access',
      refreshToken: 'refresh',
      user: JSON.stringify({
        accessToken: 'access',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        role: 'ADMIN',
        nomPrenom: 'Admin',
        email: 'admin@ocp.ma',
      }),
    });

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    await act(async () => {
      screen.getByRole('button', { name: 'logout' }).click();
    });

    expect(clearAuthSession).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });
  });
});
