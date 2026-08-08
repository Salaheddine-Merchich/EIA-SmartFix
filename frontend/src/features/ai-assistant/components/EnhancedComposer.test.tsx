import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { EnhancedComposer } from './EnhancedComposer';

describe('EnhancedComposer', () => {
  it('sends on button click', async () => {
    const user = userEvent.setup();
    const onSend = vi.fn();
    render(<EnhancedComposer loading={false} onSend={onSend} onStop={vi.fn()} />);

    await user.type(screen.getByLabelText('Décrivez votre panne'), 'Panne E001');
    await user.click(screen.getByRole('button', { name: 'Envoyer le message' }));

    expect(onSend).toHaveBeenCalledWith('Panne E001');
  });

  it('shows stop button while loading', () => {
    const onStop = vi.fn();
    render(<EnhancedComposer loading onSend={vi.fn()} onStop={onStop} />);

    fireEvent.click(screen.getByRole('button', { name: 'Arrêter la génération' }));
    expect(onStop).toHaveBeenCalled();
  });

  it('does not send on Enter while loading', () => {
    const onSend = vi.fn();
    render(<EnhancedComposer loading onSend={onSend} onStop={vi.fn()} />);
    const input = screen.getByLabelText('Décrivez votre panne');

    fireEvent.change(input, { target: { value: 'Test' } });
    fireEvent.keyDown(input, { key: 'Enter', shiftKey: false });

    expect(onSend).not.toHaveBeenCalled();
  });
});
