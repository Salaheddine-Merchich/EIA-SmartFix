import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { DashboardHeader } from './DashboardHeader';

vi.mock('@/features/auth/context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      nomPrenom: 'Salaheddine El Amrani',
      role: 'ADMIN',
      email: 'admin@ocp.ma',
    },
  }),
}));

describe('DashboardHeader', () => {
  it('renders greeting, breadcrumb and search action', () => {
    render(
      <MemoryRouter>
        <DashboardHeader />
      </MemoryRouter>,
    );

    expect(screen.getByText('Bonjour Salaheddine')).toBeInTheDocument();
    expect(screen.getByText('Tableau de bord')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Recherche' })).toBeInTheDocument();
  });
});
