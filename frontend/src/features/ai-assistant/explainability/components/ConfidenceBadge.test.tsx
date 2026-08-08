import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ConfidenceBadge } from './ConfidenceBadge';

describe('ConfidenceBadge', () => {
  it('shows very high confidence label', () => {
    render(<ConfidenceBadge score={91.2} level="VERY_HIGH" />);
    expect(screen.getByText('Très fiable')).toBeInTheDocument();
    expect(screen.getByText('91.2%')).toBeInTheDocument();
  });

  it('shows low confidence label', () => {
    render(<ConfidenceBadge score={62.0} level="LOW" />);
    expect(screen.getByText('Faible')).toBeInTheDocument();
  });
});
