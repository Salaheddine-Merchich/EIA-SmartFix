import { EnterpriseInput, EnterpriseTextarea } from '@/design-system';

export const INTERVENTION_FIELD_LABELS: Record<string, string> = {
  description: 'Description détaillée',
  symptomes: 'Symptômes',
  causeRacine: 'Cause racine',
  analyseTechnique: 'Analyse technique',
  actionsCorrectives: 'Actions correctives',
  piecesRemplacees: 'Pièces remplacées',
};

export type InterventionFormState = {
  symptomes: string;
  causeRacine: string;
  analyseTechnique: string;
  actionsCorrectives: string;
  piecesRemplacees: string;
  dureeArretMinutes: string;
  tempsInterventionMinutes: string;
  description: string;
};

export const EMPTY_INTERVENTION_FORM: InterventionFormState = {
  symptomes: '',
  causeRacine: '',
  analyseTechnique: '',
  actionsCorrectives: '',
  piecesRemplacees: '',
  dureeArretMinutes: '',
  tempsInterventionMinutes: '',
  description: '',
};

export function interventionToForm(intervention: {
  description?: string;
  symptomes?: string;
  causeRacine?: string;
  analyseTechnique?: string;
  actionsCorrectives?: string;
  piecesRemplacees?: string;
  dureeArretMinutes?: number;
  tempsInterventionMinutes?: number;
}): InterventionFormState {
  return {
    description: intervention.description ?? '',
    symptomes: intervention.symptomes ?? '',
    causeRacine: intervention.causeRacine ?? '',
    analyseTechnique: intervention.analyseTechnique ?? '',
    actionsCorrectives: intervention.actionsCorrectives ?? '',
    piecesRemplacees: intervention.piecesRemplacees ?? '',
    dureeArretMinutes: intervention.dureeArretMinutes != null ? String(intervention.dureeArretMinutes) : '',
    tempsInterventionMinutes:
      intervention.tempsInterventionMinutes != null ? String(intervention.tempsInterventionMinutes) : '',
  };
}

interface InterventionFormFieldsProps {
  form: InterventionFormState;
  onChange: (form: InterventionFormState) => void;
}

export function InterventionFormFields({ form, onChange }: InterventionFormFieldsProps) {
  const textFields = [
    'description',
    'symptomes',
    'causeRacine',
    'analyseTechnique',
    'actionsCorrectives',
    'piecesRemplacees',
  ] as const;

  return (
    <div className="space-y-3">
      {textFields.map((field) => (
        <EnterpriseTextarea
          key={field}
          label={INTERVENTION_FIELD_LABELS[field]}
          value={form[field]}
          onChange={(event) => onChange({ ...form, [field]: event.target.value })}
          rows={2}
        />
      ))}
      <div className="grid grid-cols-2 gap-3">
        <EnterpriseInput
          label="Durée arrêt (min)"
          type="number"
          value={form.dureeArretMinutes}
          onChange={(event) => onChange({ ...form, dureeArretMinutes: event.target.value })}
        />
        <EnterpriseInput
          label="Temps intervention (min)"
          type="number"
          value={form.tempsInterventionMinutes}
          onChange={(event) => onChange({ ...form, tempsInterventionMinutes: event.target.value })}
        />
      </div>
    </div>
  );
}
