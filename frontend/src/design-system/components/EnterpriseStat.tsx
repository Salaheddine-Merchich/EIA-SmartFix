import { EnterpriseCard } from './EnterpriseCard';

export interface EnterpriseStatProps {
  label: string;
  value: string | number;
  hint?: string;
  className?: string;
}

/** KPI stat block for dashboards and summaries. */
export function EnterpriseStat({ label, value, hint, className }: EnterpriseStatProps) {
  return (
    <EnterpriseCard className={className} hover>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
        {label}
      </p>
      <p className="mt-2 text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
        {value}
      </p>
      {hint && (
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{hint}</p>
      )}
    </EnterpriseCard>
  );
}
