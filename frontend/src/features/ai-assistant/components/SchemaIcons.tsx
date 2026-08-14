export function SchemaSheetIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z" strokeLinejoin="round" />
      <path d="M14 2v6h6M8 13h8M8 17h5" strokeLinecap="round" />
      <rect x="8" y="9" width="5" height="4" rx="0.5" />
    </svg>
  );
}

export function ChevronRightIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M9 6l6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function WiringIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="2" y="8" width="5" height="8" rx="1" />
      <rect x="17" y="8" width="5" height="8" rx="1" />
      <circle cx="4.5" cy="12" r="0.8" fill="currentColor" stroke="none" />
      <circle cx="19.5" cy="12" r="0.8" fill="currentColor" stroke="none" />
      <path d="M7 12h3l2-3 2 6 1-3h2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function TerminalIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <circle cx="7.5" cy="12" r="1.4" />
      <circle cx="12" cy="12" r="1.4" />
      <circle cx="16.5" cy="12" r="1.4" />
      <path d="M7.5 8v1.2M12 8v1.2M16.5 8v1.2M7.5 14.8V16M12 14.8V16M16.5 14.8V16" strokeLinecap="round" />
    </svg>
  );
}

function DimensionIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="6" y="5" width="12" height="10" rx="1" />
      <path d="M5 18h14M7 16v4M17 16v4" strokeLinecap="round" />
      <path d="M8 18h8" strokeLinecap="round" />
    </svg>
  );
}

function InstallIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="5" y="9" width="14" height="11" rx="1.5" />
      <path d="M9 9V7a3 3 0 0 1 6 0v2" strokeLinecap="round" />
      <path d="M12 13v4M10 15l2 2 2-2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function BlockIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <rect x="3" y="8" width="6" height="8" rx="1" />
      <rect x="15" y="4" width="6" height="6" rx="1" />
      <rect x="15" y="14" width="6" height="6" rx="1" />
      <path d="M9 12h6M18 10v4" strokeLinecap="round" />
    </svg>
  );
}

export function schemaIcon(type: string) {
  switch (type) {
    case 'dimension':
      return DimensionIcon;
    case 'terminal':
      return TerminalIcon;
    case 'install':
      return InstallIcon;
    case 'block':
      return BlockIcon;
    case 'wiring':
    default:
      return WiringIcon;
  }
}

export function schemaTypeTone(type: string): string {
  switch (type) {
    case 'terminal':
      return 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300';
    case 'dimension':
      return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300';
    case 'install':
      return 'bg-violet-100 text-violet-800 dark:bg-violet-950 dark:text-violet-300';
    case 'block':
      return 'bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300';
    default:
      return 'bg-sky-100 text-sky-700 dark:bg-sky-950 dark:text-sky-300';
  }
}
