import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SourceEvidenceCard } from './SourceEvidenceCard';

describe('SourceEvidenceCard', () => {
  it('renders intervention source with similarity', () => {
    render(
      <SourceEvidenceCard
        index={0}
        document={{
          interventionId: '4582abcd-1234-5678-90ab-cdef12345678',
          equipmentCode: 'CV-101',
          title: 'Convoyeur Siemens',
          detail: 'Code défaut E001',
          similarityPercent: 91,
        }}
      />,
    );

    expect(screen.getByText('CV-101')).toBeInTheDocument();
    expect(screen.getByText('Convoyeur Siemens')).toBeInTheDocument();
    expect(screen.getByText(/Similarité 91%/)).toBeInTheDocument();
  });
});
