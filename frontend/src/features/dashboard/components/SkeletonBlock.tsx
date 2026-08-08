import { EnterpriseSkeleton, EnterpriseSkeletonCard, EnterpriseSkeletonTable } from '@/design-system';

export { EnterpriseSkeleton as SkeletonBlock };

export function KpiSkeletonGrid() {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: 4 }).map((_, i) => (
        <EnterpriseSkeletonCard key={i} />
      ))}
    </div>
  );
}

export function PanelSkeleton({ height = 'h-72' }: { height?: string }) {
  return (
    <div className={`rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900 ${height}`}>
      <EnterpriseSkeleton className="h-4 w-40" />
      <EnterpriseSkeletonTable rows={4} />
    </div>
  );
}
