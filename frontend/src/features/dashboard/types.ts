import type { DashboardStats, Failure } from '@/shared/types';

export type DashboardQueryState = 'loading' | 'error' | 'success' | 'empty';

export interface KpiCardData {
  id: string;
  title: string;
  value: string | number;
  subtitle: string;
  hint?: string;
  icon: 'interventions' | 'equipment' | 'incidents' | 'knowledge' | 'ai';
}

export interface DashboardViewModel {
  stats: DashboardStats;
  recentFailures: Failure[];
  criticalFailures: Failure[];
}

export interface ActivityItem {
  id: string;
  title: string;
  description: string;
  timestamp: string;
  actor?: string;
  kind: 'failure' | 'intervention';
}

export interface AlertItem {
  id: string;
  title: string;
  equipmentCode: string;
  criticite: Failure['criticite'];
  statut: Failure['statut'];
  timestamp: string;
}
