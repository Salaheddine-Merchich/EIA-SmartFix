import { memo } from 'react';
import type { UserMessage, AssistantMessage } from '../types';
import { getAssistErrorTitle } from '../utils/assistErrorLabels';
import { SchemaIconButton } from './SchemaIconButton';

interface UserMessageBubbleProps {
  message: UserMessage;
}

function UserMessageBubbleComponent({ message }: UserMessageBubbleProps) {
  return (
    <div className="flex justify-end">
      <div className="max-w-[85%] rounded-2xl rounded-br-md bg-slate-800 px-4 py-3 text-sm leading-relaxed text-white sm:max-w-[75%]">
        <p className="whitespace-pre-wrap">{message.content}</p>
        <p className="mt-2 text-xs text-slate-300">
          {new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(
            new Date(message.createdAt),
          )}
        </p>
      </div>
    </div>
  );
}

interface AssistantMessageBubbleProps {
  message: AssistantMessage;
  onViewAnalysis?: () => void;
}

function AssistantMessageBubbleComponent({
  message,
  onViewAnalysis,
}: AssistantMessageBubbleProps) {
  if (message.error && !message.response) {
    const isCancelled = message.error.kind === 'cancelled';
    const isConnection =
      message.error.kind === 'backend' ||
      message.error.kind === 'connection' ||
      message.error.kind === 'auth';

    return (
      <div className="flex justify-start">
        <div
          className={`max-w-[90%] rounded-2xl rounded-bl-md border px-4 py-3.5 text-sm sm:max-w-[80%] ${
            isCancelled
              ? 'border-slate-200 bg-slate-50 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300'
              : isConnection
                ? 'border-red-200 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-300'
                : 'border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-200'
          }`}
          role="status"
        >
          <p className="font-medium">{getAssistErrorTitle(message.error.kind)}</p>
          <p className="mt-1 leading-relaxed">{message.error.message}</p>
        </div>
      </div>
    );
  }

  const suggestions = message.response?.suggestions;
  const disclaimer = message.response?.disclaimer;
  const relevantSchemas = message.response?.relevantSchemas ?? [];
  const isEmpty = message.error?.kind === 'empty';
  const isUnavailable = message.error?.kind === 'ollama';

  return (
    <div className="flex justify-start">
      <div className="max-w-[92%] rounded-2xl rounded-bl-md border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900 sm:max-w-[82%]">
        <div className="space-y-4 px-4 py-4 text-sm text-slate-700 dark:text-slate-300 sm:px-5">
          {(isEmpty || isUnavailable) && message.error && (
            <div
              className={`rounded-lg px-3 py-2.5 text-sm ${
                isUnavailable
                  ? 'border border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-200'
                  : 'border border-slate-200 bg-slate-50 text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300'
              }`}
              role="status"
            >
              <p className="font-medium">{getAssistErrorTitle(message.error.kind)}</p>
              <p className="mt-1">{message.error.message}</p>
            </div>
          )}

          {suggestions && !isUnavailable && (
            <div className="space-y-4">
              <section>
                <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Résumé</h3>
                <p className="mt-1.5 leading-relaxed text-slate-800 dark:text-slate-200">{suggestions.summary || '—'}</p>
              </section>

              {suggestions.probableCauses.length > 0 && !isEmpty && (
                <section>
                  <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Causes probables
                  </h3>
                  <ul className="mt-2 space-y-1.5">
                    {suggestions.probableCauses.map((cause, index) => (
                      <li key={`${cause}-${index}`} className="flex gap-2 leading-relaxed">
                        <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-slate-400" />
                        <span>{cause}</span>
                      </li>
                    ))}
                  </ul>
                </section>
              )}

              {suggestions.correctiveActions.length > 0 && !isEmpty && (
                <section>
                  <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Actions recommandées
                  </h3>
                  <ul className="mt-2 space-y-1.5">
                    {suggestions.correctiveActions.map((action, index) => (
                      <li key={`${action}-${index}`} className="flex gap-2 leading-relaxed">
                        <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
                        <span>{action}</span>
                      </li>
                    ))}
                  </ul>
                </section>
              )}

              {suggestions.advice && !isEmpty && (
                <section>
                  <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500">Conseil</h3>
                  <p className="mt-1.5 rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 leading-relaxed dark:border-slate-700 dark:bg-slate-800">
                    {suggestions.advice}
                  </p>
                </section>
              )}
            </div>
          )}

          {disclaimer && (
            <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 pt-3 dark:border-slate-800">
              <p className="text-xs leading-relaxed text-slate-500 dark:text-slate-400">{disclaimer}</p>
              <div className="flex flex-wrap items-center gap-2">
                {relevantSchemas.length > 0 && !isEmpty && !isUnavailable && (
                  <SchemaIconButton schemas={relevantSchemas} />
                )}
                {message.response?.diagnosticTrace && onViewAnalysis && (
                  <button
                    type="button"
                    onClick={onViewAnalysis}
                    className="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-700 hover:border-emerald-300 hover:text-emerald-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:border-emerald-700 dark:hover:text-emerald-400"
                  >
                    Voir l&apos;analyse
                  </button>
                )}
              </div>
            </div>
          )}
        </div>

        <div className="border-t border-slate-100 px-4 py-2 dark:border-slate-800 sm:px-5">
          <p className="text-xs text-slate-400 dark:text-slate-500">
            {new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(
              new Date(message.createdAt),
            )}
          </p>
        </div>
      </div>
    </div>
  );
}

export const UserMessageBubble = memo(UserMessageBubbleComponent);
export const AssistantMessageBubble = memo(AssistantMessageBubbleComponent);
