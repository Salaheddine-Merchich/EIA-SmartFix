import type { Failure } from '@/shared/types';
import type { ActivityItem, AlertItem } from '../types';

export function mapFailuresToActivity(failures: Failure[]): ActivityItem[] {
  return failures.map((failure) => ({
    id: failure.id,
    title: failure.equipmentCode,
    description: failure.descriptionInitiale || failure.codeDefaut || 'Panne déclarée',
    timestamp: failure.dateHeure,
    actor: failure.responsableNom,
    kind: 'failure',
  }));
}

export function mapFailuresToAlerts(failures: Failure[]): AlertItem[] {
  return failures.map((failure) => ({
    id: failure.id,
    title: failure.descriptionInitiale || failure.codeDefaut || 'Incident actif',
    equipmentCode: failure.equipmentCode,
    criticite: failure.criticite,
    statut: failure.statut,
    timestamp: failure.dateHeure,
  }));
}
