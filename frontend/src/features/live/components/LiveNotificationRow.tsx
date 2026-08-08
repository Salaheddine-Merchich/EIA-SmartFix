import { useEffect, useState } from 'react';
import { formatRelativeLive } from '../utils/formatRelativeLive';
import { CATEGORY_LABELS } from '../utils/eventPresentation';
import type { LiveNotification } from '../types';

interface LiveNotificationRowProps {
  item: LiveNotification;
  onMarkRead: (id: string) => void;
  onDismiss: (id: string) => void;
}

export function LiveNotificationRow({ item, onMarkRead, onDismiss }: LiveNotificationRowProps) {
  const [, tick] = useState(0);

  useEffect(() => {
    const id = window.setInterval(() => tick((v) => v + 1), 15_000);
    return () => window.clearInterval(id);
  }, []);

  return (
    <li
      className={`rounded-lg border px-3 py-2.5 transition-colors ${
        item.read
          ? 'border-slate-200/60 bg-transparent dark:border-slate-800'
          : 'border-emerald-200/60 bg-emerald-50/40 dark:border-emerald-900/40 dark:bg-emerald-950/20'
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{item.title}</p>
          <p className="mt-0.5 text-xs text-slate-600 dark:text-slate-400">{item.message}</p>
          <p className="mt-1 text-[10px] uppercase tracking-wide text-slate-500">
            {CATEGORY_LABELS[item.category]}
          </p>
        </div>
        <time className="shrink-0 text-[10px] text-slate-500">{formatRelativeLive(item.occurredAt)}</time>
      </div>
      <div className="mt-2 flex gap-2">
        {!item.read && (
          <button
            type="button"
            onClick={() => onMarkRead(item.id)}
            className="text-[11px] font-medium text-emerald-700 hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 dark:text-emerald-400"
          >
            Marquer lu
          </button>
        )}
        <button
          type="button"
          onClick={() => onDismiss(item.id)}
          className="text-[11px] text-slate-500 hover:text-slate-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 dark:hover:text-slate-300"
        >
          Supprimer
        </button>
      </div>
    </li>
  );
}
