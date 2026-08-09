import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '@/features/auth/context/AuthContext';
import { cardVariants } from '@/design-system/animations';
import { DashboardPanel } from './DashboardPanel';

const actions = [
  {
    to: '/failures',
    label: 'Nouvelle intervention',
    description: 'Déclarer ou suivre une panne',
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
      </svg>
    ),
  },
  {
    to: '/ai-assistant',
    label: 'Assistant IA',
    description: 'Diagnostic assisté RAG',
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
      </svg>
    ),
  },
  {
    to: '/equipment',
    label: 'Équipements',
    description: 'Parc et historique',
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 3v1.5M4.5 8.25H3m18 0h-1.5M4.5 12H3m18 0h-1.5m-15 3.75H3m18 0h-1.5M8.25 19.5V21M12 3v1.5m0 15V21m3.75-18v1.5m0 15V21m-9-1.5h10.5a2.25 2.25 0 002.25-2.25V6.75a2.25 2.25 0 00-2.25-2.25H6.75A2.25 2.25 0 004.5 6.75v10.5a2.25 2.25 0 002.25 2.25z" />
      </svg>
    ),
  },
  {
    to: '/search',
    label: 'Knowledge',
    description: 'Recherche interventions',
    icon: (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
      </svg>
    ),
  },
];

export function QuickActionsGrid() {
  const { hasRole } = useAuth();
  const filteredActions = hasRole('ADMIN')
    ? [
        ...actions,
        {
          to: '/users',
          label: 'Utilisateurs',
          description: 'Administration IAM',
          icon: (
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
            </svg>
          ),
        },
      ]
    : actions;

  return (
    <DashboardPanel title="Actions rapides" subtitle="Navigation opérationnelle">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {filteredActions.map((action) => (
          <motion.div key={action.to + action.label} variants={cardVariants} whileHover="hover" whileTap="tap">
            <Link
              to={action.to}
              className="flex items-start gap-3 rounded-xl border border-slate-200 bg-slate-50/40 px-4 py-3 transition-colors hover:border-slate-300 hover:bg-white dark:border-slate-700 dark:bg-slate-900/40 dark:hover:border-slate-600 dark:hover:bg-slate-800"
            >
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-emerald-700 ring-1 ring-slate-200 dark:bg-slate-800 dark:text-emerald-400 dark:ring-slate-700">
                {action.icon}
              </span>
              <span>
                <span className="block text-sm font-semibold text-slate-900 dark:text-slate-100">{action.label}</span>
                <span className="mt-0.5 block text-xs text-slate-500 dark:text-slate-400">{action.description}</span>
              </span>
            </Link>
          </motion.div>
        ))}
      </div>
    </DashboardPanel>
  );
}
