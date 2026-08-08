import { describe, expect, it } from 'vitest';
import { buildKpiCards } from './buildKpiCards';

const stats = {
  totalFailures: 12,
  openFailures: 4,
  validatedInterventions: 8,
  pendingValidations: 2,
  mttrMinutes: 45,
  mtbfHours: 120,
  topFailingEquipment: [{ equipmentId: '1', code: 'EQ-1', designation: 'Pompe', failureCount: 3 }],
  topCauses: [{ cause: 'Usure', count: 2 }],
  failuresByFamille: [{ famille: 'Hydraulique', count: 5 }],
  failuresByMonth: [{ month: '2025-07', count: 3 }],
};

describe('buildKpiCards', () => {
  it('builds five KPI cards including AI reliability placeholder', () => {
    const cards = buildKpiCards(stats, 25, 2);

    expect(cards).toHaveLength(5);
    expect(cards[0].value).toBe('8');
    expect(cards[4].title).toBe('AI Reliability');
    expect(cards[4].subtitle).toBe('Collecte en cours');
  });

  it('shows AI reliability metrics when available', () => {
    const cards = buildKpiCards(
      { ...stats, aiReliability: { diagnosticsCount: 12, averageConfidence: 86.4, totalRetrievals: 34 } },
      25,
      2,
    );
    expect(cards[4].value).toBe('86.4%');
    expect(cards[4].subtitle).toContain('12');
  });

  it('includes pending validation hint without fake trends', () => {
    const cards = buildKpiCards(stats, 25, 2);
    expect(cards[0].hint).toContain('2');
    expect(cards[0].hint).toContain('validation');
    expect(cards[3].hint).toContain('indexables RAG');
  });
});
