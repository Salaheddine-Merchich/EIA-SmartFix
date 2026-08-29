import { memo } from 'react';
import type { AiConversationSummary } from '@/shared/types';
import { ASSISTANT_LAYOUT } from '../constants/layout';
import { compactHistoryTitle } from '../utils/compactHistoryTitle';
import { cn } from '@/design-system';

interface ConversationHistorySidebarProps {
  items: AiConversationSummary[];
  activeId: string | null;
  loading?: boolean;
  onSelect: (id: string) => void;
  onNew: () => void;
  onDelete: (id: string) => void;
  onDeleteAll: () => void;
  onClose?: () => void;
  showHeader?: boolean;
  className?: string;
}

function formatUpdatedAt(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function ConversationHistorySidebarComponent({
  items,
  activeId,
  loading = false,
  onSelect,
  onNew,
  onDelete,
  onDeleteAll,
  onClose,
  showHeader = true,
  className,
}: ConversationHistorySidebarProps) {
  return (
    <aside
      className={cn(
        'flex h-full min-h-0 flex-col bg-white dark:bg-slate-900',
        ASSISTANT_LAYOUT.historyPanelWidth,
        showHeader && 'border-r border-slate-200 dark:border-slate-800',
        className,
      )}
      aria-label="Historique des conversations"
    >
      {showHeader ? (
        <div className="flex items-center justify-between gap-2 border-b border-slate-200 px-3 py-3 dark:border-slate-800">
          <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Historique</h2>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={onNew}
              className="rounded-lg bg-emerald-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-emerald-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600"
            >
              Nouveau
            </button>
            {onClose && (
              <button
                type="button"
                onClick={onClose}
                title="Fermer l'historique"
                aria-label="Fermer l'historique"
                className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200"
              >
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.8} stroke="currentColor" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className="border-b border-slate-200 px-3 py-2 dark:border-slate-800">
          <button
            type="button"
            onClick={onNew}
            className="w-full rounded-lg bg-emerald-600 px-2.5 py-1.5 text-xs font-medium text-white hover:bg-emerald-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600"
          >
            Nouvelle conversation
          </button>
        </div>
      )}

      <div className="min-h-0 flex-1 overflow-y-auto px-2 py-2">
        {loading && items.length === 0 && (
          <p className="px-2 py-6 text-center text-xs text-slate-500">Chargement…</p>
        )}
        {!loading && items.length === 0 && (
          <p className="px-2 py-6 text-center text-xs text-slate-500 dark:text-slate-400">
            Aucune conversation
          </p>
        )}
        <ul className="space-y-1">
          {items.map((item) => {
            const active = item.id === activeId;
            return (
              <li key={item.id}>
                <div
                  className={`flex items-start gap-1 rounded-lg px-2 py-2 ${
                    active
                      ? 'bg-slate-50 ring-1 ring-emerald-500/40 dark:bg-slate-800'
                      : 'hover:bg-slate-50 dark:hover:bg-slate-800/80'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => onSelect(item.id)}
                    className="min-w-0 flex-1 text-left"
                  >
                    <p className="break-words text-sm font-medium leading-snug text-slate-800 dark:text-slate-200">
                      {compactHistoryTitle(item.title)}
                    </p>
                    <p className="mt-0.5 text-[11px] text-slate-500">{formatUpdatedAt(item.updatedAt)}</p>
                  </button>
                  <button
                    type="button"
                    aria-label={`Supprimer ${item.title}`}
                    onClick={() => onDelete(item.id)}
                    className="mt-0.5 shrink-0 rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-red-600 dark:hover:bg-slate-700"
                  >
                    <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" strokeWidth={1.8} stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6 7h12M9 7V5h6v2m-7 3v8m4-8v8m4-8v8M5 7l1 12a2 2 0 002 2h8a2 2 0 002-2l1-12" />
                    </svg>
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      </div>

      {items.length > 0 && (
        <div className="border-t border-slate-200 px-3 py-3 dark:border-slate-800">
          <button
            type="button"
            onClick={onDeleteAll}
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600 hover:border-red-300 hover:bg-red-50 hover:text-red-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:border-red-800 dark:hover:bg-red-950/40 dark:hover:text-red-300"
          >
            Tout supprimer
          </button>
        </div>
      )}
    </aside>
  );
}

export const ConversationHistorySidebar = memo(ConversationHistorySidebarComponent);
