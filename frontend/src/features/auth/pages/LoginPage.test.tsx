import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from '@/features/auth/context/AuthContext';
import LoginPage from './LoginPage';

vi.mock('@/features/auth/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

function stubLocalStorage() {
  const store = new Map<string, string>();
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
}

describe('LoginPage', () => {
  const login = vi.fn();

  beforeEach(() => {
    stubLocalStorage();
    login.mockReset();
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      login,
      logout: vi.fn(),
      hasRole: vi.fn(() => false),
      isAuthenticated: false,
      isBootstrapping: false,
    });
  });

  it('renders empty form without prefilled demo credentials', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    expect(screen.getByLabelText('Email')).toHaveValue('');
    expect(screen.getByLabelText('Mot de passe')).toHaveValue('');
  });

  it('calls login with entered credentials on submit', async () => {
    const user = userEvent.setup();
    login.mockResolvedValue(undefined);

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Email'), 'tech@ocp.ma');
    await user.type(screen.getByLabelText('Mot de passe'), 'Password123!');
    await user.click(screen.getByRole('button', { name: 'Se connecter' }));

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith('tech@ocp.ma', 'Password123!');
    });
  });

  it('shows error message when login fails', async () => {
    const user = userEvent.setup();
    login.mockRejectedValue(new Error('auth failed'));

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Email'), 'tech@ocp.ma');
    await user.type(screen.getByLabelText('Mot de passe'), 'Password123!');
    await user.click(screen.getByRole('button', { name: 'Se connecter' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Email ou mot de passe incorrect',
    );
  });
});
