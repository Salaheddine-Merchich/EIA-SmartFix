import type { DashboardStats } from '@/shared/types';
import { DashboardPanel } from '../components/DashboardPanel';
import { formatDurationHours, formatDurationMinutes, formatNumber } from '../utils/formatters';

interface ReliabilityMetricsProps {
  stats: DashboardStats;
}

export function ReliabilityMetrics({ stats }: ReliabilityMetricsProps) {
  return (
    <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
      <DashboardPanel title="Pannes totales" subtitle="Volume global">
        <p className="text-3xl font-bold text-slate-900 dark:text-slate-100">{formatNumber(stats.totalFailures)}</p>
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
          {formatNumber(stats.openFailures)} ouvertes ou en cours
        </p>
      </DashboardPanel>

      <DashboardPanel title="MTTR" subtitle="Temps moyen de réparation">
        <p className="text-3xl font-bold text-slate-900 dark:text-slate-100">{formatDurationMinutes(stats.mttrMinutes)}</p>
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Calculé sur les interventions validées</p>
      </DashboardPanel>

      <DashboardPanel title="MTBF" subtitle="Temps moyen entre pannes">
        <p className="text-3xl font-bold text-slate-900 dark:text-slate-100">{formatDurationHours(stats.mtbfHours)}</p>
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Indicateur de fiabilité du parc</p>
      </DashboardPanel>
    </section>
  );
}
