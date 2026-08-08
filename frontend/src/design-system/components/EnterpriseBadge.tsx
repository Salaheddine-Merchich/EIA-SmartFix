import { cn } from '../utils/cn';

export type BadgeVariant = 'default' | 'success' | 'warning' | 'danger' | 'info';

export interface EnterpriseBadgeProps {
  label: string;
  variant?: BadgeVariant;
  className?: string;
}

const variants: Record<BadgeVariant, string> = {
  default: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
  success: 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300',
  warning: 'bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-300',
  danger: 'bg-red-50 text-red-800 dark:bg-red-950 dark:text-red-300',
  info: 'bg-sky-50 text-sky-800 dark:bg-sky-950 dark:text-sky-300',
};

/** Status badge for criticity, validation, etc. */
export function EnterpriseBadge({ label, variant = 'default', className }: EnterpriseBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset ring-black/5 dark:ring-white/10',
        variants[variant],
        className,
      )}
    >
      {label}
    </span>
  );
}

export function statutPanneVariant(statut: string): BadgeVariant {
  switch (statut) {
    case 'OUVERTE': return 'danger';
    case 'EN_COURS': return 'warning';
    case 'RESOLUE': return 'info';
    case 'CLOTUREE': return 'success';
    default: return 'default';
  }
}

export function criticiteVariant(c: string): BadgeVariant {
  switch (c) {
    case 'CRITIQUE': return 'danger';
    case 'HAUTE': return 'warning';
    case 'MOYENNE': return 'info';
    default: return 'default';
  }
}

export function validationVariant(s: string): BadgeVariant {
  switch (s) {
    case 'VALIDEE': return 'success';
    case 'SOUMISE': return 'warning';
    case 'REJETEE': return 'danger';
    default: return 'default';
  }
}
