import { motion } from 'framer-motion';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { EnterpriseButton } from '@/design-system';
import { useAuth } from '@/features/auth/context/AuthContext';
import { fadeInUp } from '@/features/ai-assistant/animations';
import { formatDateTime, formatTime, greetingName } from '../utils/formatters';

export function DashboardHeader() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [currentTime, setCurrentTime] = useState(formatTime());

  useEffect(() => {
    const interval = setInterval(() => setCurrentTime(formatTime()), 1000);
    return () => clearInterval(interval);
  }, []);

  const initials = user?.nomPrenom
    ?.split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'U';

  return (
    <motion.header
      className="border-b border-slate-200/70 bg-gradient-to-r from-white to-slate-50/40 dark:border-slate-800 dark:from-slate-900 dark:to-slate-950"
      variants={fadeInUp}
      initial="initial"
      animate="animate"
    >
      <div className="flex flex-col gap-4 px-4 py-5 sm:px-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="min-w-0">
          <nav aria-label="Fil d'Ariane" className="mb-2 text-xs text-slate-500 dark:text-slate-400">
            <ol className="flex items-center gap-2">
              <li>EIA SmartFix</li>
              <li aria-hidden="true">/</li>
              <li className="font-medium text-slate-700 dark:text-slate-300">Tableau de bord</li>
            </ol>
          </nav>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
            {greetingName(user?.nomPrenom)}
          </h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            {formatDateTime(new Date())} · {currentTime}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <EnterpriseButton variant="secondary" onClick={() => navigate('/search')}>
            <svg className="h-4 w-4 text-slate-500" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
            </svg>
            Recherche
          </EnterpriseButton>

          <EnterpriseButton onClick={() => navigate('/ai-assistant')}>
            Assistant IA
          </EnterpriseButton>

          <div
            className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-900 text-xs font-semibold text-white ring-2 ring-white"
            aria-label={`Profil ${user?.nomPrenom ?? 'utilisateur'}`}
            title={user?.nomPrenom}
          >
            {initials}
          </div>
        </div>
      </div>
    </motion.header>
  );
}
