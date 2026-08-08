import type { LiveEventCategory, LiveEventType } from '../types';

const TYPE_ICONS: Partial<Record<LiveEventType, string>> = {
  INTERVENTION_VALIDATED: 'OK',
  CRITICAL_ALERT: 'ALRT',
  FAILURE_CREATED: 'PANNE',
  RAG_REINDEXED: 'RAG',
  INTERVENTION_CREATED: 'INT',
  INTERVENTION_SUBMITTED: 'INT',
  AI_UNAVAILABLE: 'IA',
};

export function liveEventIcon(type: LiveEventType): string {
  return TYPE_ICONS[type] ?? 'EVT';
}

export const CATEGORY_LABELS: Record<LiveEventCategory, string> = {
  maintenance: 'Maintenance',
  alert: 'Alertes',
  knowledge: 'Knowledge',
  ai: 'Intelligence artificielle',
  system: 'Système',
};

export function isCriticalLiveEvent(type: LiveEventType): boolean {
  return type === 'CRITICAL_ALERT' || type === 'AI_UNAVAILABLE';
}
