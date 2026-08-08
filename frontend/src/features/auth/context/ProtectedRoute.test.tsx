import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from './AuthContext';
import { ProtectedRoute } from './ProtectedRoute';

vi.mock('./AuthContext', () => ({
  useAuth: vi.fn(),
}));

function renderWithRoutes(roles?: Array<'ADMIN' | 'RESPONSABLE_EIA' | 'TECHNICIEN'>) {
  return render(
    <MemoryRouter initialEntries={['/secure']}>
      <Routes>
        <Route path="/login" element={<div>login-page</div>} />
        <Route path="/" element={<div>home-page</div>} />
        <Route element={<ProtectedRoute roles={roles} />}>
          <Route path="/secure" element={<div>secure-page</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset();
  });

  it('shows bootstrap state while restoring session', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      login: vi.fn(),
      logout: vi.fn(),
      hasRole: vi.fn(() => false),
      isAuthenticated: false,
      isBootstrapping: true,
    });

    renderWithRoutes();
    expect(screen.getByText('Restauration de la session…')).toBeInTheDocument();
  });

  it('redirects unauthenticated users to login', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      login: vi.fn(),
      logout: vi.fn(),
      hasRole: vi.fn(() => false),
      isAuthenticated: false,
      isBootstrapping: false,
    });

    renderWithRoutes();
    expect(screen.getByText('login-page')).toBeInTheDocument();
  });

  it('redirects authenticated users without required role to home', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      login: vi.fn(),
      logout: vi.fn(),
      hasRole: (role) => role === 'TECHNICIEN',
      isAuthenticated: true,
      isBootstrapping: false,
    });

    renderWithRoutes(['ADMIN']);
    expect(screen.getByText('home-page')).toBeInTheDocument();
  });

  it('renders outlet when authenticated with allowed role', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      login: vi.fn(),
      logout: vi.fn(),
      hasRole: (role) => role === 'ADMIN',
      isAuthenticated: true,
      isBootstrapping: false,
    });

    renderWithRoutes(['ADMIN']);
    expect(screen.getByText('secure-page')).toBeInTheDocument();
  });
});
