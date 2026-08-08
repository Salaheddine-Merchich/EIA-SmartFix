import { EnterpriseButton } from './EnterpriseButton';

export interface EnterpriseErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

/** Error state with optional retry action. */
export function EnterpriseErrorState({
  title = 'Erreur de chargement',
  message = 'Impossible de charger les données. Réessayez.',
  onRetry,
}: EnterpriseErrorStateProps) {
  return (
    <div
      className="rounded-xl border border-red-200 bg-red-50/70 px-5 py-6 text-center dark:border-red-900 dark:bg-red-950/30"
      role="alert"
    >
      <p className="text-sm font-semibold text-red-800 dark:text-red-300">{title}</p>
      <p className="mt-1 text-sm text-red-700 dark:text-red-400">{message}</p>
      {onRetry && (
        <EnterpriseButton variant="secondary" size="sm" className="mt-4" onClick={onRetry}>
          Réessayer
        </EnterpriseButton>
      )}
    </div>
  );
}
