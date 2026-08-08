import { cn } from '@/design-system';
import type { ServiceState } from '../types';

const STATE_STYLES: Record<ServiceState, string> = {
  ONLINE: 'bg-emerald-500',
  DEGRADED: 'bg-amber-500',
  OFFLINE: 'bg-red-500',
};

interface LiveServiceBadgeProps {
  label: string;
  state: ServiceState;
  pulse?: boolean;
  className?: string;
}

export function LiveServiceBadge({ label, state, pulse = false, className }: LiveServiceBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md px-2 py-0.5 text-[11px] font-medium text-slate-600 dark:text-slate-300',
        className,
      )}
      title={`${label}: ${state}`}
    >
      <span className="relative flex h-2 w-2">
        {pulse && state === 'ONLINE' && (
          <span className={cn('absolute inline-flex h-full w-full animate-ping rounded-full opacity-40', STATE_STYLES[state])} />
        )}
        <span className={cn('relative inline-flex h-2 w-2 rounded-full', STATE_STYLES[state])} />
      </span>
      {label}
    </span>
  );
}
