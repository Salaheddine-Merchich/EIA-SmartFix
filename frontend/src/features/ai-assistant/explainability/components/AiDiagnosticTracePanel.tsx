import { EnterprisePanel } from '@/design-system';
import type { AiDiagnosticTrace } from '../types';
import { ConfidenceBadge } from './ConfidenceBadge';
import { ReasoningStep } from './ReasoningStep';
import { RetrievalTimeline } from './RetrievalTimeline';
import { SourceEvidenceCard } from './SourceEvidenceCard';

interface AiDiagnosticTracePanelProps {
  trace: AiDiagnosticTrace;
}

export function AiDiagnosticTracePanel({ trace }: AiDiagnosticTracePanelProps) {
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
    </div>
  );
}
