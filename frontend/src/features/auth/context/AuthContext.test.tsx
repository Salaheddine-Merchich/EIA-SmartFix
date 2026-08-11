import { act, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';
import { authApi } from '@/shared/api';
import { clearAuthSession, refreshAccessToken, storeUserProfile, readUserProfile } from '@/shared/api/client';

vi.mock('@/shared/api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
  },
}));

vi.mock('@/shared/api/client', async () => {
  const actual = await vi.importActual<typeof import('@/shared/api/client')>('@/shared/api/client');
  return {
    ...actual,
    clearAuthSession: vi.fn(actual.clearAuthSession),
    refreshAccessToken: vi.fn(),
  };
});

vi.mock('@/features/ai-assistant/utils/conversationStorage', () => ({
  clearConversationStorageForUser: vi.fn(),
}));

function stubSessionStorage(initial: Record<string, string> = {}) {
  const store = new Map<string, string>(Object.entries(initial));
  vi.stubGlobal('sessionStorage', {
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
  vi.stubGlobal('localStorage', {
    getItem: () => null,
    setItem: () => {},
    removeItem: () => {},
    clear: () => {},
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

const storedUser = {
  accessToken: '',
  refreshToken: '',
  tokenType: 'Bearer',
  role: 'ADMIN' as const,
  nomPrenom: 'Admin',
  email: 'admin@ocp.ma',
};

describe('AuthContext', () => {
  beforeEach(() => {
    stubSessionStorage();
    vi.clearAllMocks();
    vi.mocked(refreshAccessToken).mockResolvedValue(null);
  });

  it('logs in, stores profile without JWT, and exposes role helpers', async () => {
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
    const profile = readUserProfile();
    expect(profile?.email).toBe('tech@ocp.ma');
    expect(profile?.accessToken).toBe('');
    expect(profile?.refreshToken).toBe('');
  });

  it('refreshes on bootstrap when profile is present', async () => {
    stubSessionStorage({
      eia_user_profile: JSON.stringify(storedUser),
    });
    vi.mocked(refreshAccessToken).mockImplementation(async () => {
      storeUserProfile({ ...storedUser, accessToken: 'fresh-access' });
      return 'cookie';
    });

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });
    expect(refreshAccessToken).toHaveBeenCalled();
    expect(screen.getByTestId('email')).toHaveTextContent('admin@ocp.ma');
  });

  it('clears session when bootstrap refresh fails', async () => {
    stubSessionStorage({
      eia_user_profile: JSON.stringify(storedUser),
    });
    vi.mocked(refreshAccessToken).mockResolvedValue(null);

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('bootstrapping')).toHaveTextContent('false');
    });
    expect(clearAuthSession).toHaveBeenCalled();
    expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
  });
});
