import type { Failure, StatutValidation } from '@/shared/types';

const STATUT_LABELS: Record<StatutValidation, string> = {
  BROUILLON: 'brouillon',
  SOUMISE: 'en attente',
  VALIDEE: 'validée',
  REJETEE: 'rejetée',
};

function formatStatutLabel(statut?: StatutValidation): string {
  if (!statut) return '';
  return STATUT_LABELS[statut];
}

export function formatInterventionLinkLabel(failure: Pick<Failure, 'interventionCount' | 'latestInterventionStatut'>): string {
  if (failure.interventionCount === 0) {
    return 'Ajouter intervention';
  }

  const statutLabel = formatStatutLabel(failure.latestInterventionStatut);
  const countLabel = failure.interventionCount === 1 ? '1 intervention' : `${failure.interventionCount} interventions`;

  if (!statutLabel) {
    return countLabel;
  }

  return `${countLabel} · ${statutLabel}`;
}
