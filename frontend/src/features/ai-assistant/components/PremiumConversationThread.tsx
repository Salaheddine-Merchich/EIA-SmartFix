import { memo } from 'react';
import type { ConversationMessage } from '../types';
import { PremiumUserMessageBubble, PremiumAssistantMessageBubble } from './PremiumMessageBubble';
import { CopilotDotsOnly } from './CopilotLoader';
import { useConversationAutoScroll } from '../hooks/useConversationAutoScroll';
import { ASSISTANT_LAYOUT, EXAMPLE_QUERIES } from '../constants/layout';

import type { AiDiagnosticTrace } from '@/shared/types';

interface PremiumConversationThreadProps {
  messages: ConversationMessage[];
  loading: boolean;
  loadingMessage?: string;
  onViewAnalysis?: (trace: AiDiagnosticTrace) => void;
  onExampleSelect?: (text: string) => void;
}

function PremiumConversationThreadComponent({
  messages,
  loading,
  loadingMessage = 'Génération…',
  onViewAnalysis,
  onExampleSelect,
}: PremiumConversationThreadProps) {
  const { containerRef, sentinelRef } = useConversationAutoScroll({
    messageCount: messages.length,
    loading,
  });

  const isEmpty = messages.length === 0 && !loading;

  if (isEmpty) {
    return (
      <div className="flex h-full flex-col items-center justify-center px-4 py-10 text-center sm:px-6">
        <div className="mx-auto w-full max-w-lg">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Conversation vide</h2>
          <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-400">
            Décrivez une panne, un symptôme ou un code défaut. L&apos;assistant s&apos;appuie sur les
            interventions validées pour proposer des pistes — jamais un diagnostic définitif.
          </p>

          <div className="mt-8 space-y-2 text-left">
            {EXAMPLE_QUERIES.map((example) => (
              <button
                key={example.text}
                type="button"
                onClick={() => onExampleSelect?.(example.text)}
                className="w-full rounded-lg border border-slate-200 bg-white p-3.5 text-left transition-colors hover:border-slate-300 hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-slate-600 dark:hover:bg-slate-800"
              >
                <div className="flex items-start gap-3">
                  <span aria-hidden="true">{example.icon}</span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-slate-800 dark:text-slate-200">{example.text}</p>
                    <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{example.category}</p>
                  </div>
                </div>
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
              <PremiumUserMessageBubble message={message} />
            ) : (
              <PremiumAssistantMessageBubble
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

export const PremiumConversationThread = memo(PremiumConversationThreadComponent);
