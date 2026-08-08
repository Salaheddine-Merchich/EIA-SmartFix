import { memo } from 'react';
import { LiveAiStatusBadges } from '@/features/live';
import type { AssistantStatus } from '../types';
import { ASSISTANT_LAYOUT } from '../constants/layout';

interface PremiumHeaderProps {
  status: AssistantStatus;
  onNewConversation: () => void;
  hasMessages: boolean;
}

function PremiumHeaderComponent({ status, onNewConversation, hasMessages }: PremiumHeaderProps) {
  return (
    <header className="border-b border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
      <div
        className={`flex flex-col gap-3 ${ASSISTANT_LAYOUT.pagePaddingX} ${ASSISTANT_LAYOUT.pagePaddingY} sm:flex-row sm:items-center sm:justify-between`}
      >
        <div className="flex min-w-0 items-center gap-3">
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
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <LiveAiStatusBadges assistantStatus={status} />
          {hasMessages && (
            <button
              type="button"
              onClick={onNewConversation}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
            >
              Nouvelle conversation
            </button>
          )}
        </div>
      </div>
    </header>
  );
}

export const PremiumHeader = memo(PremiumHeaderComponent);
