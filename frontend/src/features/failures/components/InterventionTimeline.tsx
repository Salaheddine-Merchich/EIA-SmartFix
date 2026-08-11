import { EnterpriseEmptyState, EnterpriseButton, EnterprisePanel } from '@/design-system';
import type { Intervention } from '@/shared/types';
import { InterventionCard } from './InterventionCard';

export interface InterventionTimelineProps {
  interventions: Intervention[];
  loading: boolean;
  canValidate: boolean;
  onSubmit: (id: string) => void;
  onValidate: (id: string, approved: boolean) => void;
  onExportPdf: (id: string) => void;
  onCreateIntervention: () => void;
  onEdit?: (item: Intervention) => void;
  onUploadDocument?: (interventionId: string, file: File) => void;
  onDownloadDocument?: (interventionId: string, documentId: string, filename: string) => void;
  onDeleteDocument?: (interventionId: string, documentId: string) => void;
}

export function InterventionTimeline({
  interventions,
  loading,
  canValidate,
  onSubmit,
  onValidate,
  onExportPdf,
  onCreateIntervention,
  onEdit,
  onUploadDocument,
  onDownloadDocument,
  onDeleteDocument,
}: InterventionTimelineProps) {
  return (
    <EnterprisePanel
      title="Interventions"
      subtitle={`${interventions.length} intervention${interventions.length > 1 ? 's' : ''} enregistrée${interventions.length > 1 ? 's' : ''}`}
      action={
        <EnterpriseButton size="sm" onClick={onCreateIntervention}>
          Nouvelle intervention
        </EnterpriseButton>
      }
    >
      {interventions.length === 0 ? (
        <EnterpriseEmptyState
          title="Aucune intervention"
          description="Commencez par enregistrer une intervention pour documenter la résolution de cette panne."
          action={
            <EnterpriseButton onClick={onCreateIntervention}>Nouvelle intervention</EnterpriseButton>
          }
        />
      ) : (
        <div className="space-y-2">
          {interventions.map((item, index) => (
            <InterventionCard
              key={item.id}
              item={item}
              loading={loading}
              canValidate={canValidate}
              onSubmit={onSubmit}
              onValidate={onValidate}
              onExportPdf={onExportPdf}
              onEdit={onEdit}
              onUploadDocument={onUploadDocument}
              onDownloadDocument={onDownloadDocument}
              onDeleteDocument={onDeleteDocument}
              isLast={index === interventions.length - 1}
            />
          ))}
        </div>
      )}
    </EnterprisePanel>
  );
}
