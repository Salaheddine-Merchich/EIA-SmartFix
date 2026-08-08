import { motion } from 'framer-motion';
import type { RetrievalStep } from '../types';

const STEP_LABELS: Record<string, string> = {
  embedding: 'Embedding',
  vector_search: 'Vector Search',
  hybrid_search: 'Hybrid Search',
  filtering: 'Filtrage',
};

interface ReasoningStepProps {
  step: RetrievalStep;
  index: number;
}

export function ReasoningStep({ step, index }: ReasoningStepProps) {
  const label = STEP_LABELS[step.step] ?? step.step;
  const ok = step.status === 'OK';

  return (
    <motion.div
      initial={{ opacity: 0, x: -6 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.06, duration: 0.2 }}
      className="flex items-start justify-between gap-3 rounded-lg border border-slate-200/80 px-3 py-2.5 dark:border-slate-700"
    >
      <div>
        <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{label}</p>
        <p className="text-xs text-slate-500">{step.detail}</p>
      </div>
      <span
        className={`shrink-0 text-xs font-semibold ${ok ? 'text-emerald-600' : 'text-red-600'}`}
        aria-label={ok ? 'OK' : 'Échec'}
      >
        {step.status}
      </span>
    </motion.div>
  );
}
