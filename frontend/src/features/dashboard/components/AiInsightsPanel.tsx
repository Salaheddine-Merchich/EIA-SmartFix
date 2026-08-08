import { Link } from 'react-router-dom';
import { DashboardPanel } from './DashboardPanel';
import { EmptyState } from './EmptyState';

export function AiInsightsPanel() {
  return (
    <DashboardPanel
      title="IA Insights"
      subtitle="Suggestions de l'assistant IA"
      action={
        <Link to="/ai-assistant" className="text-xs font-medium text-emerald-700 hover:underline">
          Ouvrir l'assistant
        </Link>
      }
    >
      <EmptyState
        title="Disponible prochainement"
        description="Les suggestions contextuelles nécessitent une requête explicite via l'assistant IA. Aucune donnée n'est simulée sur le tableau de bord."
        icon={
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
          </svg>
        }
      />
    </DashboardPanel>
  );
}
