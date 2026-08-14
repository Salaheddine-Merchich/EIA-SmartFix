import { EnterprisePanel } from '@/design-system';
import { useState } from 'react';
import { SchemaLightboxModal } from '@/features/ai-assistant/components/SchemaLightboxModal';
import type { EquipmentSchemaRef } from '@/shared/types';
import type { AiDiagnosticTrace, RetrievedSchema } from '../types';
import { ConfidenceBadge } from './ConfidenceBadge';
import { ReasoningStep } from './ReasoningStep';
import { RetrievalTimeline } from './RetrievalTimeline';
import { SchemaEvidenceCard } from './SchemaEvidenceCard';
import { SourceEvidenceCard } from './SourceEvidenceCard';

interface AiDiagnosticTracePanelProps {
  trace: AiDiagnosticTrace;
}

export function AiDiagnosticTracePanel({ trace }: AiDiagnosticTracePanelProps) {
  const [activeSchema, setActiveSchema] = useState<EquipmentSchemaRef | null>(null);

  const openSchema = (schema: RetrievedSchema) => {
    setActiveSchema({
      schemaId: schema.schemaId,
      equipmentId: schema.equipmentId,
      equipmentCode: schema.equipmentCode,
      equipmentDesignation: schema.equipmentDesignation,
      label: schema.label,
      schemaType: schema.schemaType,
      sourcePdf: schema.sourcePdf,
      sourcePage: schema.sourcePage,
      caption: schema.caption,
      totalSchemasForEquipment: schema.totalSchemasForEquipment,
      downloadUrl: schema.downloadUrl,
    });
  };

  return (
    <div className="space-y-6">
      <section>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Niveau de confiance</h3>
        <ConfidenceBadge score={trace.confidenceScore} level={trace.confidenceLevel} />
      </section>

      <EnterprisePanel title="Timeline diagnostic" subtitle="Parcours de la requête">
        <RetrievalTimeline trace={trace} />
      </EnterprisePanel>

      <EnterprisePanel title="Recherche effectuée" subtitle={`Retrieval ${trace.retrievalDurationMs} ms`}>
        <div className="space-y-2">
          {trace.retrievalSteps.map((step, index) => (
            <ReasoningStep key={`${step.step}-${index}`} step={step} index={index} />
          ))}
        </div>
      </EnterprisePanel>

      <EnterprisePanel
        title="Sources consultées"
        subtitle={`${trace.retrievedDocuments.length} intervention(s) retenue(s)`}
      >
        {trace.retrievedDocuments.length === 0 ? (
          <p className="text-sm text-slate-500">Aucune source retenue après filtrage.</p>
        ) : (
          <div className="space-y-3">
            {trace.retrievedDocuments.map((doc, index) => (
              <SourceEvidenceCard key={`${doc.interventionId}-${index}`} document={doc} index={index} />
            ))}
          </div>
        )}
      </EnterprisePanel>

      <EnterprisePanel
        title="Schémas techniques"
        subtitle={`${trace.retrievedSchemas?.length ?? 0} schéma(s) retenu(s)`}
      >
        {!trace.retrievedSchemas?.length ? (
          <p className="text-sm text-slate-500">Aucun schéma technique associé à cette requête.</p>
        ) : (
          <div className="space-y-3">
            {trace.retrievedSchemas.map((schema, index) => (
              <SchemaEvidenceCard
                key={`${schema.schemaId}-${index}`}
                schema={schema}
                index={index}
                onOpen={openSchema}
              />
            ))}
          </div>
        )}
      </EnterprisePanel>

      {activeSchema && (
        <SchemaLightboxModal schema={activeSchema} onClose={() => setActiveSchema(null)} />
      )}
    </div>
  );
}
