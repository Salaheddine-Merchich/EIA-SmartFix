import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { EnterpriseDrawer } from '@/design-system';

describe('EnterpriseDrawer explainability', () => {
  it('opens and closes diagnostic drawer', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(
      <EnterpriseDrawer open title="Analyse du diagnostic IA" onClose={onClose}>
        <p>Trace content</p>
      </EnterpriseDrawer>,
    );

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Trace content')).toBeInTheDocument();

    await user.click(screen.getByLabelText('Fermer'));
    expect(onClose).toHaveBeenCalled();
  });
});
