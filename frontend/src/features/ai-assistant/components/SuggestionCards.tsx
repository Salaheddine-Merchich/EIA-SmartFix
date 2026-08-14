import { useState, memo } from 'react';
import { useNavigate } from 'react-router-dom';
import { EnterpriseLoader } from '@/design-system';
import { interventionsApi } from '@/shared/api';
import type { SimilarInterventionItem } from '../types';

interface SuggestionCardsProps {
  items: SimilarInterventionItem[];
  loading: boolean;
}

function SuggestionCardsComponent({ items, loading }: SuggestionCardsProps) {
  const navigate = useNavigate();
  const [openingId, setOpeningId] = useState<string | null>(null);
  const [openError, setOpenError] = useState('');

  const openIntervention = async (interventionId: string) => {
    setOpenError('');
    setOpeningId(interventionId);
    try {
      const intervention = await interventionsApi.get(interventionId);
      navigate(`/failures/${intervention.failureId}`);
    } catch {
      setOpenError("Impossible d'ouvrir la fiche intervention.");
    } finally {
      setOpeningId(null);
    }
  };

  return (
    <aside
      className="flex h-full min-h-0 flex-col border-t border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950 lg:border-l lg:border-t-0"
      aria-label="Interventions similaires"
    >
      <div className="border-b border-slate-200 px-4 py-3.5 dark:border-slate-800">
        <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Interventions similaires</h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">Validées · contexte RAG</p>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-3">
        {loading && items.length === 0 && (
          <div className="flex justify-center py-8">
            <EnterpriseLoader label="Recherche en cours" />
          </div>
        )}

        {!loading && items.length === 0 && (
          <div className="rounded-lg border border-slate-200 bg-slate-50/80 px-3 py-3 dark:border-slate-800 dark:bg-slate-900/40">
            <p className="text-sm font-medium text-slate-800 dark:text-slate-200">Aucune intervention similaire</p>
            <p className="mt-1 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
              Aucune fiche validée ne correspond suffisamment à cette demande.
            </p>
          </div>
        )}

        {openError && (
          <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-300" role="alert">
            {openError}
          </p>
        )}

        <div className="space-y-2.5">
          {items.map((item) => {
            const title = item.symptomes || item.causeRacine || 'Intervention validée';
            const score = Math.round(item.similarity * 100);
            const busy = openingId === item.interventionId;

            return (
              <button
                key={item.interventionId}
                type="button"
                onClick={() => openIntervention(item.interventionId)}
                disabled={busy}
                className="w-full rounded-lg border border-slate-200 bg-white p-3.5 text-left transition-colors hover:border-slate-300 hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-slate-600 dark:hover:bg-slate-800"
              >
                <div className="mb-2 flex items-start justify-between gap-2">
                  <h3 className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">{item.equipmentCode}</h3>
                  <span className="shrink-0 rounded-md bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                    {score}%
                  </span>
                </div>
                <p className="line-clamp-3 text-xs leading-relaxed text-slate-600 dark:text-slate-400">{title}</p>
                {busy && (
                  <div className="mt-2">
                    <EnterpriseLoader label="Ouverture" />
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </aside>
  );
}

export const SuggestionCards = memo(SuggestionCardsComponent);
