import {
  EnterpriseAvatar,
  EnterpriseBadge,
  EnterprisePanel,
  criticiteVariant,
  statutPanneVariant,
} from '@/design-system';
import type { Failure } from '@/shared/types';
import { DetailField } from './DetailField';

interface FailureSummaryPanelProps {
  failure: Failure;
}

export function FailureSummaryPanel({ failure }: FailureSummaryPanelProps) {
  return (
    <EnterprisePanel title="Informations panne" subtitle="Contexte, équipement et acteurs">
      <div className="space-y-6">
        <section className="rounded-lg border border-slate-200 bg-slate-50/70 p-4 dark:border-slate-700 dark:bg-slate-800/40">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Identification équipement
          </p>
          <p className="mt-2 text-lg font-semibold text-slate-900 dark:text-slate-100">
            {failure.equipmentCode}
          </p>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
            {failure.equipmentDesignation}
          </p>
        </section>

        <section>
          <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Contexte
          </p>
          <div className="grid gap-4 sm:grid-cols-2">
            <DetailField
              label="Date et heure"
              value={new Date(failure.dateHeure).toLocaleString('fr-FR')}
            />
            <DetailField label="Zone / service" value={failure.zoneService || '—'} />
            <DetailField label="Code défaut" value={failure.codeDefaut || '—'} />
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Criticité
              </p>
              <div className="mt-2">
                <EnterpriseBadge label={failure.criticite} variant={criticiteVariant(failure.criticite)} />
              </div>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Statut
              </p>
              <div className="mt-2">
                <EnterpriseBadge label={failure.statut} variant={statutPanneVariant(failure.statut)} />
              </div>
            </div>
          </div>
        </section>

        <section>
          <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Acteurs
          </p>
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <EnterpriseAvatar name={failure.declarantNom || 'Non renseigné'} size="sm" />
              <div>
                <p className="text-xs text-slate-500 dark:text-slate-400">Déclaré par</p>
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {failure.declarantNom || '—'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <EnterpriseAvatar name={failure.responsableNom || 'Non renseigné'} size="sm" />
              <div>
                <p className="text-xs text-slate-500 dark:text-slate-400">Responsable EIA</p>
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {failure.responsableNom || '—'}
                </p>
              </div>
            </div>
          </div>
        </section>

        {failure.descriptionInitiale && (
          <section className="rounded-lg border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900/40">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Description initiale
            </p>
            <p className="mt-2 text-sm leading-relaxed text-slate-700 dark:text-slate-300">
              {failure.descriptionInitiale}
            </p>
          </section>
        )}
      </div>
    </EnterprisePanel>
  );
}
