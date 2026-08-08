import { EnterpriseBadge } from '@/design-system';
import type { AiDiagnosticTrace } from '../types';

const LEVEL_CONFIG = {
  VERY_HIGH: { label: 'Très fiable', variant: 'success' as const },
  HIGH: { label: 'Fiable', variant: 'warning' as const },
  LOW: { label: 'Faible', variant: 'danger' as const },
};

interface ConfidenceBadgeProps {
  score: number;
  level: AiDiagnosticTrace['confidenceLevel'];
}

export function ConfidenceBadge({ score, level }: ConfidenceBadgeProps) {
  const config = LEVEL_CONFIG[level] ?? LEVEL_CONFIG.LOW;
  return (
    <div className="flex items-center gap-2" role="status" aria-label={`Confiance ${config.label}`}>
      <EnterpriseBadge label={config.label} variant={config.variant} />
      <span className="text-sm font-semibold tabular-nums text-slate-900 dark:text-slate-100">
        {score.toFixed(1)}%
      </span>
    </div>
  );
}
