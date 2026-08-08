import { describe, expect, it } from 'vitest';
import { formatInterventionLinkLabel } from './formatInterventionLinkLabel';

describe('formatInterventionLinkLabel', () => {
  it('returns Ajouter intervention when count is zero', () => {
    expect(formatInterventionLinkLabel({ interventionCount: 0 })).toBe('Ajouter intervention');
  });

  it('formats single brouillon intervention', () => {
    expect(
      formatInterventionLinkLabel({ interventionCount: 1, latestInterventionStatut: 'BROUILLON' }),
    ).toBe('1 intervention · brouillon');
  });

  it('formats single validated intervention', () => {
    expect(
      formatInterventionLinkLabel({ interventionCount: 1, latestInterventionStatut: 'VALIDEE' }),
    ).toBe('1 intervention · validée');
  });

  it('formats multiple interventions with latest statut', () => {
    expect(
      formatInterventionLinkLabel({ interventionCount: 3, latestInterventionStatut: 'SOUMISE' }),
    ).toBe('3 interventions · en attente');
  });
});
