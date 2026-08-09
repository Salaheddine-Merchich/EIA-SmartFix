import { DashboardPanel } from './DashboardPanel';
import { EmptyState } from './EmptyState';
import { EnterpriseButton, EnterpriseErrorState, EnterpriseSkeleton } from '@/design-system';
import { useRecurringDefects } from '../hooks/useRecurringDefects';

export function RecurringDefectsPanel() {
  const { analysis, isLoading, isError, refetch, runAnalysis, isAnalyzing } = useRecurringDefects(8);

  if (isLoading) {
    return (
      <DashboardPanel title="Défauts récurrents" subtitle="Analyse agrégée + IA">
        <EnterpriseSkeleton className="h-24 w-full" />
      </DashboardPanel>
    );
  }

  if (isError) {
    return (
      <DashboardPanel title="Défauts récurrents" subtitle="Analyse agrégée + IA">
        <EnterpriseErrorState
          title="Données indisponibles"
          message="Impossible de charger les défauts récurrents. Vérifiez vos droits ou réessayez."
          onRetry={() => {
            void refetch();
          }}
        />
      </DashboardPanel>
    );
  }

  if (!analysis || analysis.defects.length === 0) {
    return (
      <DashboardPanel title="Défauts récurrents" subtitle="Analyse agrégée + IA">
        <EmptyState
          title="Aucun défaut récurrent"
          description="Les codes défaut apparaissant plus d'une fois seront listés ici."
        />
      </DashboardPanel>
    );
  }

  return (
    <DashboardPanel
      title="Défauts récurrents"
      subtitle="Analyse agrégée + IA"
      action={
        <EnterpriseButton
          variant="ghost"
          size="sm"
          loading={isAnalyzing}
          onClick={() => {
            void runAnalysis();
          }}
        >
          Analyser avec l'IA
        </EnterpriseButton>
      }
    >
      <ul className="mb-4 space-y-2">
        {analysis.defects.map((d) => (
          <li
            key={d.codeDefaut}
            className="flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700"
          >
            <span className="font-medium text-slate-900 dark:text-slate-100">{d.codeDefaut}</span>
            <span className="text-xs text-slate-500">
              {d.occurrenceCount}× · {d.affectedEquipmentCount} équip.
            </span>
          </li>
        ))}
      </ul>

      {analysis.analysis && (
        <div className="space-y-3 text-sm text-slate-700 dark:text-slate-300">
          <div>
            <p className="mb-1 text-xs font-medium uppercase text-slate-500">Analyse</p>
            <p>{analysis.analysis}</p>
          </div>
          {analysis.recommendations && (
            <div>
              <p className="mb-1 text-xs font-medium uppercase text-slate-500">Recommandations</p>
              <p className="whitespace-pre-line">{analysis.recommendations}</p>
            </div>
          )}
          {analysis.disclaimer && (
            <p className="text-xs italic text-slate-500">{analysis.disclaimer}</p>
          )}
        </div>
      )}
    </DashboardPanel>
  );
}
