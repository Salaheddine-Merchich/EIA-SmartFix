import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { KpiCard } from './KpiCard';

describe('KpiCard', () => {
  it('renders title, value and hint', () => {
    render(
      <KpiCard
        card={{
          id: 'interventions',
          title: 'Interventions',
          value: '12',
          subtitle: 'Validées',
          hint: '2 en attente de validation',
          icon: 'interventions',
        }}
      />,
    );

    expect(screen.getByText('Interventions')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('2 en attente de validation')).toBeInTheDocument();
  });
});
