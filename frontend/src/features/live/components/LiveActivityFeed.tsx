import { motion } from 'framer-motion';
import { EnterprisePanel } from '@/design-system';
import { useLive } from '../providers/LiveProvider';
import { formatRelativeLive } from '../utils/formatRelativeLive';
import { liveEventIcon } from '../utils/eventPresentation';
import { liveFeedEmptyMessage } from '../utils/connectionMessages';

export function LiveActivityFeed() {
  const { recentEvents, connectionState } = useLive();

  return (
    <EnterprisePanel title="Live Activity Feed" subtitle="Événements en temps réel">
      {recentEvents.length === 0 ? (
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {liveFeedEmptyMessage(connectionState)}
        </p>
      ) : (
        <ul className="space-y-2" aria-live="polite">
          {recentEvents.slice(0, 12).map((event) => (
            <motion.li
              key={event.id}
              layout
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.2 }}
              className="flex items-start gap-3 rounded-lg border border-slate-200/80 bg-white/60 px-3 py-2.5 dark:border-slate-700 dark:bg-slate-900/40"
            >
              <span
                className="mt-0.5 shrink-0 rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-600 dark:bg-slate-800 dark:text-slate-300"
                aria-hidden
              >
                {liveEventIcon(event.type)}
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{event.title}</p>
                <p className="text-xs text-slate-600 dark:text-slate-400">{event.message}</p>
              </div>
              <time className="shrink-0 text-[10px] text-slate-500">{formatRelativeLive(event.occurredAt)}</time>
            </motion.li>
          ))}
        </ul>
      )}
    </EnterprisePanel>
  );
}
