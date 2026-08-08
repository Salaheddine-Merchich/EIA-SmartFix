import { Link } from 'react-router-dom';
import type { ActivityItem } from '../types';
import { DashboardPanel } from '../components/DashboardPanel';
import { EmptyState } from '../components/EmptyState';
import { formatRelativeTime } from '../utils/formatters';

interface RecentActivityTimelineProps {
  items: ActivityItem[];
}

export function RecentActivityTimeline({ items }: RecentActivityTimelineProps) {
  return (
    <DashboardPanel title="Activité récente" subtitle="Dernières pannes déclarées">
      {items.length === 0 ? (
        <EmptyState
          title="Aucune activité récente"
          description="Les événements récents s'afficheront dès qu'une panne sera enregistrée."
        />
      ) : (
        <ol className="space-y-4">
          {items.map((item) => (
            <li key={item.id} className="relative pl-8">
              <span className="absolute left-0 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-emerald-50 text-emerald-700 ring-1 ring-emerald-100">
                <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126z" />
                </svg>
              </span>
              <div className="rounded-xl border border-slate-200 bg-slate-50/40 px-4 py-3 transition-colors hover:bg-white">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                    <p className="mt-1 text-xs leading-relaxed text-slate-600">{item.description}</p>
                  </div>
                  <time className="text-[11px] text-slate-500">{formatRelativeTime(item.timestamp)}</time>
                </div>
                <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-slate-500">
                  <span>Panne déclarée</span>
                  {item.actor && <span>· {item.actor}</span>}
                  <Link to={`/failures/${item.id}`} className="font-medium text-emerald-700 hover:underline">
                    Voir la fiche
                  </Link>
                </div>
              </div>
            </li>
          ))}
        </ol>
      )}
    </DashboardPanel>
  );
}
