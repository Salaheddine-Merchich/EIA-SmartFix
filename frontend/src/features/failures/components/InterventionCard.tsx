import {
  EnterpriseAvatar,
  EnterpriseBadge,
  EnterpriseButton,
  EnterpriseCard,
  validationVariant,
} from '@/design-system';
import type { Intervention } from '@/shared/types';
import { DetailField } from './DetailField';

const INTERVENTION_FIELD_LABELS: Record<string, string> = {
  description: 'Description détaillée',
  symptomes: 'Symptômes',
  causeRacine: 'Cause racine',
  analyseTechnique: 'Analyse technique',
  actionsCorrectives: 'Actions correctives',
  piecesRemplacees: 'Pièces remplacées',
};

export interface InterventionCardProps {
  item: Intervention;
  loading: boolean;
  canValidate: boolean;
  onSubmit: (id: string) => void;
  onValidate: (id: string, approved: boolean) => void;
  onExportPdf: (id: string) => void;
  isLast?: boolean;
}

export function InterventionCard({
  item,
  loading,
  canValidate,
  onSubmit,
  onValidate,
  onExportPdf,
  isLast = false,
}: InterventionCardProps) {
  return (
    <div className="relative pl-8">
      <span
        className={`absolute left-[11px] top-0 w-px bg-slate-200 dark:bg-slate-700 ${isLast ? 'h-8' : 'h-full'}`}
        aria-hidden
      />
      <span
        className="absolute left-0 top-2 flex h-6 w-6 items-center justify-center rounded-full border-2 border-emerald-500 bg-white text-[10px] font-bold text-emerald-700 dark:bg-slate-900 dark:text-emerald-300"
        aria-hidden
      />

      <EnterpriseCard className="mb-4">
        <div className="mb-4 flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <EnterpriseAvatar name={item.technicienNom} size="sm" />
            <div>
              <p className="font-semibold text-slate-900 dark:text-slate-100">{item.technicienNom}</p>
              <p className="text-xs text-slate-500">
                {new Date(item.createdAt).toLocaleString('fr-FR')}
              </p>
            </div>
          </div>
          <EnterpriseBadge label={item.statutValidation} variant={validationVariant(item.statutValidation)} />
        </div>

        <div className="grid gap-4 lg:grid-cols-2">
          <section className="rounded-lg border border-slate-200 bg-slate-50/60 p-4 dark:border-slate-700 dark:bg-slate-800/30">
            <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-emerald-700 dark:text-emerald-300">
              Diagnostic
            </p>
            <div className="space-y-3">
              <DetailField label={INTERVENTION_FIELD_LABELS.symptomes} value={item.symptomes} />
              <DetailField label={INTERVENTION_FIELD_LABELS.causeRacine} value={item.causeRacine} />
              <DetailField label={INTERVENTION_FIELD_LABELS.analyseTechnique} value={item.analyseTechnique} />
              <DetailField label={INTERVENTION_FIELD_LABELS.description} value={item.description} />
            </div>
          </section>

          <section className="rounded-lg border border-slate-200 bg-slate-50/60 p-4 dark:border-slate-700 dark:bg-slate-800/30">
            <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-sky-700 dark:text-sky-300">
              Actions
            </p>
            <div className="space-y-3">
              <DetailField label={INTERVENTION_FIELD_LABELS.actionsCorrectives} value={item.actionsCorrectives} />
              <DetailField label={INTERVENTION_FIELD_LABELS.piecesRemplacees} value={item.piecesRemplacees} />
              <div className="grid grid-cols-2 gap-3">
                <DetailField label="Durée d'arrêt (min)" value={item.dureeArretMinutes} />
                <DetailField label="Temps intervention (min)" value={item.tempsInterventionMinutes} />
              </div>
            </div>
          </section>
        </div>

        {(item.validateurNom || item.dateValidation || item.commentaireValidation) && (
          <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50/60 p-3 text-sm dark:border-emerald-900/50 dark:bg-emerald-950/20">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-emerald-800 dark:text-emerald-300">
              Validation
            </p>
            {item.validateurNom && (
              <p className="text-slate-700 dark:text-slate-300">
                <strong>Validateur :</strong> {item.validateurNom}
              </p>
            )}
            {item.dateValidation && (
              <p className="text-slate-700 dark:text-slate-300">
                <strong>Date :</strong> {new Date(item.dateValidation).toLocaleString('fr-FR')}
              </p>
            )}
            {item.commentaireValidation && (
              <p className="text-slate-700 dark:text-slate-300">
                <strong>Commentaire :</strong> {item.commentaireValidation}
              </p>
            )}
          </div>
        )}

        <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-4 dark:border-slate-800">
          {(item.statutValidation === 'BROUILLON' || item.statutValidation === 'REJETEE') && (
            <EnterpriseButton variant="secondary" size="sm" onClick={() => onSubmit(item.id)} disabled={loading}>
              Soumettre
            </EnterpriseButton>
          )}

          {item.statutValidation === 'SOUMISE' && canValidate && (
            <>
              <EnterpriseButton size="sm" onClick={() => onValidate(item.id, true)} disabled={loading}>
                Valider
              </EnterpriseButton>
              <EnterpriseButton variant="danger" size="sm" onClick={() => onValidate(item.id, false)} disabled={loading}>
                Rejeter
              </EnterpriseButton>
            </>
          )}

          <EnterpriseButton
            variant="secondary"
            size="sm"
            onClick={() => onExportPdf(item.id)}
            disabled={loading}
          >
            Télécharger PDF
          </EnterpriseButton>
        </div>
      </EnterpriseCard>
    </div>
  );
}
