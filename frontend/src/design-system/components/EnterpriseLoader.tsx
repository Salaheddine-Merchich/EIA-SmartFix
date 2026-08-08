/** Copilot-style three-dot loader. */
export function EnterpriseLoader({ label = 'Chargement' }: { label?: string }) {
  return (
    <div className="flex items-center gap-2" role="status" aria-live="polite" aria-label={label}>
      {[0, 1, 2].map((i) => (
        <span
          key={i}
          className="h-1.5 w-1.5 animate-pulse rounded-full bg-slate-400 dark:bg-slate-500"
          style={{ animationDelay: `${i * 150}ms` }}
        />
      ))}
      <span className="sr-only">{label}</span>
    </div>
  );
}

export function EnterprisePageLoader({ message = 'Chargement…' }: { message?: string }) {
  return (
    <div className="flex min-h-[200px] flex-col items-center justify-center gap-3">
      <EnterpriseLoader label={message} />
      <p className="text-sm text-slate-500 dark:text-slate-400">{message}</p>
    </div>
  );
}
