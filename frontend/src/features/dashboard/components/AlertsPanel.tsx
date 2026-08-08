import { Link } from 'react-router-dom';
import { EnterpriseBadge, criticiteVariant } from '@/design-system';
import type { AlertItem } from '../types';
import { DashboardPanel } from './DashboardPanel';
import { EmptyState } from './EmptyState';
import { formatRelativeTime } from '../utils/formatters';

interface AlertsPanelProps {
  alerts: AlertItem[];
}

export function AlertsPanel({ alerts }: AlertsPanelProps) {
  return (
    <DashboardPanel title="Alertes" subtitle="Incidents prioritaires actifs">
      {alerts.length === 0 ? (
        <EmptyState
          title="Aucune alerte active"
          description="Aucun incident critique ou haute criticité n'est actuellement ouvert."
          icon={
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
            </svg>
          }
        />
      ) : (
        <ul className="space-y-3">
          {alerts.map((alert) => (
            <li
              key={alert.id}
              className="rounded-xl border border-slate-200 bg-white px-4 py-3 transition-shadow hover:shadow-sm dark:border-slate-700 dark:bg-slate-900"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100">{alert.equipmentCode}</p>
                  <p className="mt-1 line-clamp-2 text-xs leading-relaxed text-slate-600 dark:text-slate-400">{alert.title}</p>
                </div>
                <EnterpriseBadge label={alert.criticite} variant={criticiteVariant(alert.criticite)} />
              </div>
              <div className="mt-3 flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400">
                <span>{alert.statut.replace('_', ' ')} · {formatRelativeTime(alert.timestamp)}</span>
                <Link to={`/failures/${alert.id}`} className="font-medium text-emerald-700 hover:underline dark:text-emerald-400">
                  Traiter
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </DashboardPanel>
  );
}
