import { motion } from 'framer-motion';
import type { AiDiagnosticTrace } from '../types';

const TIMELINE = [
  { key: 'query', label: 'Question utilisateur' },
  { key: 'search', label: 'Recherche connaissances' },
  { key: 'similar', label: 'Interventions similaires trouvées' },
  { key: 'analysis', label: 'Analyse IA' },
  { key: 'recommendation', label: 'Recommandation' },
] as const;

interface RetrievalTimelineProps {
  trace: AiDiagnosticTrace;
}

export function RetrievalTimeline({ trace }: RetrievalTimelineProps) {
  const details: Record<string, string> = {
    query: trace.query,
    search: `${trace.vectorResultCount} résultat(s) vectoriels`,
    similar: `${trace.filteredCount} intervention(s) retenue(s)`,
    analysis: trace.llmDurationMs > 0 ? `${trace.llmDurationMs} ms` : 'Fallback historique',
    recommendation: `Confiance ${trace.confidenceScore.toFixed(1)}%`,
  };

  return (
    <ol className="relative space-y-0 border-l border-slate-200 pl-4 dark:border-slate-700" aria-label="Timeline diagnostic">
      {TIMELINE.map((item, index) => (
        <motion.li
          key={item.key}
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: index * 0.08 }}
          className="relative pb-5 last:pb-0"
        >
          <span className="absolute -left-[1.125rem] top-1 h-2 w-2 rounded-full bg-emerald-500 ring-4 ring-white dark:ring-slate-900" />
          <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{item.label}</p>
          <p className="mt-0.5 text-xs text-slate-500">{details[item.key]}</p>
        </motion.li>
      ))}
    </ol>
  );
}
