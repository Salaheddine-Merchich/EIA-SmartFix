import type { AssistErrorKind } from '../types';

export function getAssistErrorTitle(kind: AssistErrorKind): string {
  switch (kind) {
    case 'cancelled':
      return 'Réponse interrompue';
    case 'timeout':
      return 'Délai dépassé';
    case 'auth':
      return 'Session expirée';
    case 'ollama':
      return 'IA indisponible';
    case 'backend':
    case 'connection':
      return 'Erreur de connexion';
    case 'empty':
      return 'Peu de correspondances';
    default:
      return 'Impossible de traiter la demande';
  }
}
