import { memo } from 'react';

import type { AssistantStatus } from '../types';

interface ConversationStateBannerProps {
  loading: boolean;
  status: AssistantStatus;
  loadingMessage?: string;
  lastMessageCancelled?: boolean;
}

function ConversationStateBannerComponent({
  loading,
  status,
  loadingMessage,
  lastMessageCancelled,
}: ConversationStateBannerProps) {
  if (loading) {
    return (
      <div
        className="mx-auto w-full max-w-3xl rounded-lg border border-emerald-200/60 bg-emerald-50/80 px-3 py-2 text-xs font-medium text-emerald-800"
        role="status"
      >
        <div className="flex items-center gap-2">
          <div className="flex space-x-1">
            <div className="h-1.5 w-1.5 rounded-full bg-emerald-600 animate-bounce [animation-delay:-0.3s]"></div>
            <div className="h-1.5 w-1.5 rounded-full bg-emerald-600 animate-bounce [animation-delay:-0.15s]"></div>
            <div className="h-1.5 w-1.5 rounded-full bg-emerald-600 animate-bounce"></div>
          </div>
          <span>{loadingMessage || 'Génération…'}</span>
        </div>
      </div>
    );
  }

  if (lastMessageCancelled) {
    return (
      <div
        className="mx-auto w-full max-w-3xl rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-600"
        role="status"
      >
        Réponse interrompue
      </div>
    );
  }

  if (status === 'offline') {
    return (
      <div
        className="mx-auto w-full max-w-3xl rounded-lg border border-red-200/60 bg-red-50 px-3 py-2 text-xs font-medium text-red-700"
        role="status"
      >
        IA indisponible — vérifiez la connexion au serveur
      </div>
    );
  }

  if (status === 'degraded') {
    return (
      <div
        className="mx-auto w-full max-w-3xl rounded-lg border border-amber-200/60 bg-amber-50 px-3 py-2 text-xs font-medium text-amber-800"
        role="status"
      >
        Service dégradé — certaines fonctionnalités IA peuvent être limitées
      </div>
    );
  }

  return null;
}

export const ConversationStateBanner = memo(ConversationStateBannerComponent);
