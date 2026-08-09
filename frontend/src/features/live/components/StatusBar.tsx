import { motion } from 'framer-motion';
import { useLive } from '../providers/LiveProvider';
import { LiveServiceBadge } from './LiveServiceBadge';
import type { ServiceState } from '../types';
import { liveStreamStatusLabel } from '../utils/connectionMessages';

const LABELS: Record<string, string> = {
  backend: 'Backend',
  database: 'Database',
  ai: 'IA',
  rag: 'RAG',
  liveStream: 'SSE',
};

function mapConnection(state: string): ServiceState {
  if (state === 'connected') return 'ONLINE';
  if (state === 'connecting') return 'DEGRADED';
  return 'OFFLINE';
}

export function StatusBar() {
  const { status, connectionState, statusLoading } = useLive();

  const streamState = mapConnection(connectionState);
  const services = status
    ? [
        { key: 'backend', state: status.backend.state },
        { key: 'database', state: status.database.state },
        { key: 'ai', state: status.ai.state },
        { key: 'rag', state: status.rag.state },
        { key: 'liveStream', state: streamState },
      ]
    : [
        { key: 'backend', state: 'DEGRADED' as ServiceState },
        { key: 'database', state: 'DEGRADED' as ServiceState },
        { key: 'ai', state: 'DEGRADED' as ServiceState },
        { key: 'rag', state: 'DEGRADED' as ServiceState },
        { key: 'liveStream', state: streamState },
      ];

  return (
    <motion.div
      className="border-b border-slate-200/80 bg-slate-900/95 px-3 py-1.5 text-white backdrop-blur dark:border-slate-800"
      initial={{ opacity: 0, y: -4 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      role="status"
      aria-label="Statut des services"
    >
      <div className="mx-auto flex max-w-[1600px] flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-3">
          {services.map(({ key, state }) => (
            <LiveServiceBadge
              key={key}
              label={LABELS[key] ?? key}
              state={state}
              pulse={key === 'liveStream'}
              className="text-slate-300"
            />
          ))}
        </div>
        <span className="text-[10px] uppercase tracking-wider text-slate-500">
          {liveStreamStatusLabel(connectionState, statusLoading)}
        </span>
      </div>
    </motion.div>
  );
}
