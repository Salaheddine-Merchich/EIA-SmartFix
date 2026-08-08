import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ReasoningStep } from './ReasoningStep';

describe('ReasoningStep', () => {
  it('renders retrieval step with status', () => {
    render(
      <ReasoningStep
        index={0}
        step={{ step: 'vector_search', status: 'OK', detail: '15 résultats' }}
      />,
    );
    expect(screen.getByText('Vector Search')).toBeInTheDocument();
    expect(screen.getByText('15 résultats')).toBeInTheDocument();
    expect(screen.getByText('OK')).toBeInTheDocument();
  });
});
