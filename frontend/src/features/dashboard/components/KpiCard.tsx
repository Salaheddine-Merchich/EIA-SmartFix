import { motion } from 'framer-motion';
import { EnterpriseCard } from '@/design-system';
import type { KpiCardData } from '../types';

interface KpiCardProps {
  card: KpiCardData;
}

const iconMap = {
  interventions: '🔧',
  equipment: '⚙️',
  incidents: '⚠️',
  knowledge: '📚',
  ai: '🤖',
};

/** KPI card using EnterpriseCard surface. */
export function KpiCard({ card }: KpiCardProps) {
  return (
    <motion.div whileHover={{ y: -1 }} transition={{ type: 'spring', stiffness: 400, damping: 30 }}>
      <EnterpriseCard hover>
        <div className="flex items-start justify-between">
          <span className="text-lg" aria-hidden="true">{iconMap[card.icon]}</span>
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
