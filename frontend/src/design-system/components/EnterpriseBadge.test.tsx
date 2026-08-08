import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { EnterpriseBadge, criticiteVariant } from './EnterpriseBadge';

describe('EnterpriseBadge', () => {
  it('renders label', () => {
    render(<EnterpriseBadge label="CRITIQUE" variant={criticiteVariant('CRITIQUE')} />);
    expect(screen.getByText('CRITIQUE')).toBeInTheDocument();
  });
});
