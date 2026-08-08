import { cn } from '../utils/cn';

/** Skeleton placeholder block. */
export function EnterpriseSkeleton({ className }: { className?: string }) {
  return (
    <div
      className={cn('animate-pulse rounded-md bg-slate-200/80 dark:bg-slate-700/60', className)}
      aria-hidden="true"
    />
  );
}

export function EnterpriseSkeletonCard() {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
      <EnterpriseSkeleton className="h-3 w-24" />
      <EnterpriseSkeleton className="mt-4 h-8 w-20" />
      <EnterpriseSkeleton className="mt-3 h-3 w-32" />
    </div>
  );
}

export function EnterpriseSkeletonTable({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-2">
      <EnterpriseSkeleton className="h-10 w-full" />
      {Array.from({ length: rows }).map((_, i) => (
        <EnterpriseSkeleton key={i} className="h-12 w-full" />
      ))}
    </div>
  );
}
