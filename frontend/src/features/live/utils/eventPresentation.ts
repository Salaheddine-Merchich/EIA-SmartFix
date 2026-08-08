import type { LiveEventCategory, LiveEventType } from '../types';

const TYPE_ICONS: Partial<Record<LiveEventType, string>> = {
  INTERVENTION_VALIDATED: '✓',
  CRITICAL_ALERT: '⚠',
  FAILURE_CREATED: '⚠',
  RAG_REINDEXED: '🤖',
  INTERVENTION_CREATED: '👷',
  INTERVENTION_SUBMITTED: '👷',
  AI_UNAVAILABLE: '⚡',
};

export function liveEventIcon(type: LiveEventType): string {
  return TYPE_ICONS[type] ?? '•';
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
