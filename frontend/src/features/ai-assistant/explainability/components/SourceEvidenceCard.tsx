import { EnterpriseCard } from '@/design-system';
import type { RetrievedDocument } from '../types';
import { SimilarityScore } from './SimilarityScore';

interface SourceEvidenceCardProps {
  document: RetrievedDocument;
  index: number;
}

export function SourceEvidenceCard({ document, index }: SourceEvidenceCardProps) {
  const shortId = document.interventionId.slice(0, 8).toUpperCase();

  return (
    <EnterpriseCard className="border-slate-200/80 p-4 dark:border-slate-700">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-medium uppercase tracking-wide text-emerald-700 dark:text-emerald-400">
            Intervention #{shortId}
          </p>
          <p className="mt-1 text-sm font-semibold text-slate-900 dark:text-slate-100">
            {document.equipmentCode}
          </p>
          <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-400">{document.title}</p>
          {document.detail && (
            <p className="mt-1 text-xs text-slate-500">{document.detail}</p>
          )}
        </div>
        <SimilarityScore percent={document.similarityPercent} />
      </div>
      <p className="sr-only">Source {index + 1}</p>
    </EnterpriseCard>
  );
}
