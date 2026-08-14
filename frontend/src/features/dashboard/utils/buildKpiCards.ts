import type { DashboardStats } from '@/shared/types';
import type { KpiCardData } from '../types';
import { formatNumber } from './formatters';

export function buildKpiCards(
  stats: DashboardStats,
  equipmentCount: number,
  criticalCount: number,
): KpiCardData[] {
  const cards: KpiCardData[] = [
    {
      id: 'interventions',
      title: 'Interventions',
      value: formatNumber(stats.validatedInterventions),
      subtitle: 'Validées',
      hint: `${formatNumber(stats.pendingValidations)} en attente de validation`,
      icon: 'interventions',
    },
    {
      id: 'equipment',
      title: 'Équipements',
      value: formatNumber(equipmentCount),
      subtitle: 'Référencés',
      hint: 'Top 5 équipements les plus impactés',
      icon: 'equipment',
    },
    {
      id: 'incidents',
      title: 'Incidents critiques',
      value: formatNumber(criticalCount),
      subtitle: 'Criticité haute ou critique',
      hint: `${formatNumber(stats.openFailures)} pannes ouvertes au total`,
      icon: 'incidents',
    },
    {
      id: 'knowledge',
      title: 'Documents techniques',
      value: formatNumber(stats.activeKnowledgeDocuments),
      subtitle: 'Base de connaissances',
      hint: `${formatNumber(stats.activeEquipmentSchemas)} schémas · ${formatNumber(stats.indexedInterventions)} fiches RAG`,
      icon: 'knowledge',
    },
  ];

  if (stats.aiReliability) {
    cards.push({
      id: 'ai-reliability',
      title: 'AI Reliability',
      value: `${stats.aiReliability.averageConfidence.toFixed(1)}%`,
      subtitle: `${formatNumber(stats.aiReliability.diagnosticsCount)} diagnostics`,
      hint: `${formatNumber(stats.aiReliability.totalRetrievals)} recherches effectuées`,
      icon: 'ai',
    });
  } else {
    cards.push({
      id: 'ai-reliability',
      title: 'AI Reliability',
      value: '—',
      subtitle: 'Collecte en cours',
      hint: 'Les métriques apparaîtront après les premiers diagnostics IA',
      icon: 'ai',
    });
  }

  return cards;
}
