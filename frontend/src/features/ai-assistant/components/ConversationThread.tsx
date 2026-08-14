import { memo } from 'react';
import type { ConversationMessage } from '../types';
import { UserMessageBubble, AssistantMessageBubble } from './MessageBubble';
import { CopilotDotsOnly } from './CopilotLoader';
import { useConversationAutoScroll } from '../hooks/useConversationAutoScroll';
import { ASSISTANT_LAYOUT, EXAMPLE_QUERIES } from '../constants/layout';

import type { AiDiagnosticTrace } from '@/shared/types';

interface ConversationThreadProps {
  messages: ConversationMessage[];
  loading: boolean;
  loadingMessage?: string;
  onViewAnalysis?: (trace: AiDiagnosticTrace) => void;
  onExampleSelect?: (text: string) => void;
}

function ConversationThreadComponent({
  messages,
  loading,
  loadingMessage = 'Génération…',
  onViewAnalysis,
  onExampleSelect,
}: ConversationThreadProps) {
  const { containerRef, sentinelRef } = useConversationAutoScroll({
    messageCount: messages.length,
    loading,
  });

  const isEmpty = messages.length === 0 && !loading;

  if (isEmpty) {
    return (
      <div
        className={`flex h-full flex-col items-stretch justify-start overflow-y-auto ${ASSISTANT_LAYOUT.pagePaddingX} py-8 sm:py-10`}
      >
        <div className={`mx-auto w-full ${ASSISTANT_LAYOUT.threadMaxWidth}`}>
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Par où commencer ?</h2>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-slate-600 dark:text-slate-400">
            Décrivez une panne, un code défaut ou un équipement du parc. L&apos;assistant s&apos;appuie sur les
            interventions validées — jamais un diagnostic définitif.
          </p>

          <p className="mt-8 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Exemples du parc
          </p>
          <div className="mt-3 grid auto-rows-fr grid-cols-1 items-stretch gap-3 sm:grid-cols-2">
            {EXAMPLE_QUERIES.map((example) => (
              <button
                key={example.text}
                type="button"
                onClick={() => onExampleSelect?.(example.text)}
                className="group flex h-full min-h-[6.5rem] w-full items-start gap-3 rounded-lg border border-slate-200 bg-white p-3.5 text-left transition-colors hover:border-emerald-500/40 hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-emerald-500/40 dark:hover:bg-slate-800"
              >
                <div className="min-w-0 flex-1">
                  <span className="inline-flex rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300">
                    {example.category}
                  </span>
                  <p className="mt-1.5 line-clamp-2 text-sm font-medium text-slate-800 dark:text-slate-200">
                    {example.text}
                  </p>
                </div>
                <svg
                  className="mt-0.5 h-4 w-4 shrink-0 text-slate-400 transition-colors group-hover:text-emerald-600 dark:group-hover:text-emerald-400"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.8}
                  stroke="currentColor"
                  aria-hidden="true"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                </svg>
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className={`relative flex h-full flex-col overflow-y-auto bg-slate-50/50 dark:bg-slate-950/50 ${ASSISTANT_LAYOUT.pagePaddingX} py-4`}
      role="log"
      aria-live="polite"
      aria-relevant="additions"
    >
      <div className={`mx-auto w-full ${ASSISTANT_LAYOUT.threadMaxWidth} ${ASSISTANT_LAYOUT.threadGap} flex flex-col`}>
        {messages.map((message) => (
          <div key={message.id}>
            {message.role === 'user' ? (
              <UserMessageBubble message={message} />
            ) : (
              <AssistantMessageBubble
                message={message}
                onViewAnalysis={
                  message.response?.diagnosticTrace && onViewAnalysis
                    ? () => onViewAnalysis(message.response!.diagnosticTrace!)
                    : undefined
                }
              />
            )}
          </div>
        ))}

        {loading && (
          <div className="flex justify-start">
            <div className="rounded-xl rounded-bl-md border border-slate-200 bg-white px-4 py-3 shadow-sm dark:border-slate-700 dark:bg-slate-900">
              <div className="flex items-center gap-2.5">
                <CopilotDotsOnly />
                <span className="text-sm text-slate-600 dark:text-slate-400">{loadingMessage}</span>
              </div>
            </div>
          </div>
        )}
      </div>

      <div ref={sentinelRef} className="h-px shrink-0" aria-hidden="true" />
    </div>
  );
}

export const ConversationThread = memo(ConversationThreadComponent);
