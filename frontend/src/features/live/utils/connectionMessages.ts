import type { LiveConnectionState } from '../types';

/** Empty-state copy for the activity feed when there are no events yet. */
export function liveFeedEmptyMessage(connectionState: LiveConnectionState): string {
  switch (connectionState) {
    case 'connected':
      return "En attente d'événements — le flux SSE est connecté.";
    case 'connecting':
      return 'Connexion au flux SSE en cours…';
    case 'error':
      return 'Flux SSE indisponible — reconnexion automatique en cours…';
    case 'disconnected':
    default:
      return 'Flux SSE déconnecté — reconnexion automatique en cours…';
  }
}

/** Short status-bar label for the live stream. */
export function liveStreamStatusLabel(
  connectionState: LiveConnectionState,
  statusLoading: boolean,
): string {
  if (statusLoading) return 'Vérification…';
  switch (connectionState) {
    case 'connected':
      return 'Temps réel actif (SSE)';
    case 'connecting':
      return 'Connexion SSE…';
    case 'error':
      return 'SSE en erreur — reconnexion…';
    case 'disconnected':
    default:
      return 'SSE déconnecté — reconnexion…';
  }
}
