import { memo } from 'react';
import { cn } from '@/design-system';
import { LiveAiStatusBadges } from '@/features/live';
import type { AssistantStatus } from '../types';
import { ASSISTANT_LAYOUT } from '../constants/layout';

interface AiAssistantHeaderProps {
  status: AssistantStatus;
  contextHint?: string;
  historyOpen: boolean;
  onToggleHistory: () => void;
}

function HistoryIcon() {
  return (
    <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
  );
}

function CollapsePanelIcon() {
  return (
    <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l-7.5 7.5 7.5 7.5" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 12h16.5" />
    </svg>
  );
}

function AiAssistantHeaderComponent({ status, contextHint, historyOpen, onToggleHistory }: AiAssistantHeaderProps) {
  const historyLabel = historyOpen ? 'Fermer l\'historique' : 'Ouvrir l\'historique';

  return (
    <header className="border-b border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
      <div
        className={`flex flex-col gap-3 ${ASSISTANT_LAYOUT.pagePaddingX} py-3 sm:flex-row sm:items-center sm:justify-between`}
      >
        <div className="flex min-w-0 items-center gap-2 sm:gap-3">
          <button
            type="button"
            onClick={onToggleHistory}
            title={historyLabel}
            aria-label={historyLabel}
            aria-expanded={historyOpen}
            className={cn(
              'inline-flex h-9 shrink-0 items-center gap-2 rounded-lg border px-2.5 text-sm font-medium transition-colors',
              'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600',
              historyOpen
                ? 'border-emerald-500/40 bg-emerald-50 text-emerald-800 ring-1 ring-emerald-500/30 dark:bg-emerald-950/40 dark:text-emerald-200'
                : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
            )}
          >
            {historyOpen ? <CollapsePanelIcon /> : <HistoryIcon />}
            <span className="hidden sm:inline">Historique</span>
          </button>
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emerald-600 text-white">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"
              />
            </svg>
          </div>
          <div className="min-w-0">
            <h1 className="text-lg font-semibold tracking-tight text-slate-900 dark:text-slate-100 sm:text-xl">
              Assistant IA Maintenance
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">Diagnostic assisté basé sur les interventions validées</p>
            {contextHint && (
              <p className="mt-1 text-xs font-medium text-emerald-700 dark:text-emerald-300">{contextHint}</p>
            )}
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <LiveAiStatusBadges assistantStatus={status} />
        </div>
      </div>
    </header>
  );
}

export const AiAssistantHeader = memo(AiAssistantHeaderComponent);
