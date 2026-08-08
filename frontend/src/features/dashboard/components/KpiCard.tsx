import type { ReactNode } from 'react';
import { motion } from 'framer-motion';
import { EnterpriseCard } from '@/design-system';
import type { KpiCardData } from '../types';

interface KpiCardProps {
  card: KpiCardData;
}

const iconClass = 'h-5 w-5 text-emerald-700 dark:text-emerald-400';

const iconMap: Record<KpiCardData['icon'], ReactNode> = {
  interventions: (
    <svg className={iconClass} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M11.42 15.17l-5.42 5.42a2.121 2.121 0 01-3-3l5.42-5.42m9.9-3.18l3.18-3.18a2.121 2.121 0 00-3-3l-3.18 3.18m-2.12 2.12L7.5 14.25" />
    </svg>
  ),
  equipment: (
    <svg className={iconClass} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 3v1.5M4.5 8.25H3m18 0h-1.5M4.5 12H3m18 0h-1.5m-15 3.75H3m18 0h-1.5M8.25 19.5V21M12 3v1.5m0 15V21m3.75-18v1.5m0 15V21m-9-1.5h10.5a2.25 2.25 0 002.25-2.25V6.75a2.25 2.25 0 00-2.25-2.25H6.75A2.25 2.25 0 004.5 6.75v10.5a2.25 2.25 0 002.25 2.25z" />
    </svg>
  ),
  incidents: (
    <svg className={iconClass} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
    </svg>
  ),
  knowledge: (
    <svg className={iconClass} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25" />
    </svg>
  ),
  ai: (
    <svg className={iconClass} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
    </svg>
  ),
};

/** KPI card using EnterpriseCard surface. */
export function KpiCard({ card }: KpiCardProps) {
  return (
    <motion.div whileHover={{ y: -1 }} transition={{ type: 'spring', stiffness: 400, damping: 30 }}>
      <EnterpriseCard hover>
        <div className="flex items-start justify-between">
          <span
            className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-50 ring-1 ring-emerald-100 dark:bg-emerald-950/40 dark:ring-emerald-900"
            aria-hidden="true"
          >
            {iconMap[card.icon]}
          </span>
        </div>
        <p className="mt-3 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
          {card.title}
        </p>
        <p className="mt-2 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">{card.value}</p>
        <p className="mt-1 text-sm font-medium text-slate-700 dark:text-slate-300">{card.subtitle}</p>
        {card.hint && <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">{card.hint}</p>}
      </EnterpriseCard>
    </motion.div>
  );
}
