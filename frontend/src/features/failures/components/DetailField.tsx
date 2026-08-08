import type { ReactNode } from 'react';

export interface DetailFieldProps {
  label: string;
  value?: string | number | null;
  icon?: ReactNode;
  className?: string;
}

export function DetailField({ label, value, icon, className }: DetailFieldProps) {
  if (value === undefined || value === null || value === '') {
    return null;
  }

  return (
    <div className={className}>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
        {icon && <span className="mr-1.5 inline-flex align-middle">{icon}</span>}
        {label}
      </p>
      <p className="mt-1 text-sm leading-relaxed text-slate-800 dark:text-slate-200">{value}</p>
    </div>
  );
}
