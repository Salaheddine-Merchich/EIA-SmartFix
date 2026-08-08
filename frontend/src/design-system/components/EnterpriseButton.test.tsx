import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { EnterpriseButton } from './EnterpriseButton';

describe('EnterpriseButton', () => {
  it('renders and handles click', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<EnterpriseButton onClick={onClick}>Action</EnterpriseButton>);
    await user.click(screen.getByRole('button', { name: 'Action' }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('shows loading state', () => {
    render(<EnterpriseButton loading>Envoi</EnterpriseButton>);
    expect(screen.getByRole('button')).toBeDisabled();
  });
});
