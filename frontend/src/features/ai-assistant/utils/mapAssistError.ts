import axios from 'axios';
import type { AssistError } from '../types';

export function isAssistCancelled(error: unknown): boolean {
  if (axios.isCancel(error)) return true;
  if (axios.isAxiosError(error)) {
    return error.code === 'ERR_CANCELED';
  }
  if (error instanceof Error && error.message === 'Request was aborted') {
    return true;
  }
  return false;
}

export function mapAssistError(error: unknown): AssistError {
  if (isAssistCancelled(error)) {
    return {
      kind: 'cancelled',
      message: 'La génération a été interrompue. Vous pouvez poser une nouvelle question.',
    };
  }

  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED' || error.message?.toLowerCase().includes('timeout')) {
      return {
        kind: 'timeout',
        message: 'La requête a expiré. Réessayez avec une description plus courte, ou vérifiez la disponibilité du service.',
      };
    }

    if (!error.response) {
      return {
        kind: 'connection',
        message: 'Erreur de connexion. Vérifiez que le backend est démarré, puis réessayez.',
      };
    }
    const status = error.response.status;
    const bodyMessage = extractBodyMessage(error.response.data);

    if (status === 401 || status === 403) {
      return {
        kind: 'auth',
        message: 'Votre session a expiré. Reconnectez-vous pour continuer.',
      };
    }

    if (status === 504 || status === 408) {
      return {
        kind: 'timeout',
        message:
          'Le modèle IA met trop de temps à répondre. Attendez la fin de la génération ou réessayez dans quelques instants.',
      };
    }

    if (status === 503 || looksLikeOllamaUnavailable(bodyMessage)) {
      return {
        kind: 'ollama',
        message: 'L\'assistance IA est temporairement indisponible (service Ollama). Réessayez ultérieurement ou poursuivez le diagnostic manuellement.',
      };
    }

    if (status === 502) {
      return {
        kind: 'timeout',
        message:
          'Le proxy a interrompu la requête avant la réponse IA. Réessayez — la première génération peut prendre jusqu\'à 90 secondes.',
      };
    }

    if (status >= 500) {
      return {
        kind: 'backend',
        message: 'Une erreur serveur est survenue. Réessayez dans quelques instants.',
      };
    }

    return {
      kind: 'unknown',
      message: bodyMessage || 'Impossible d\'obtenir une assistance pour le moment.',
    };
  }

  if (error instanceof Error) {
    if (error.message === 'Stream ended before complete response') {
      return {
        kind: 'backend',
        message: 'Le flux IA s\'est interrompu avant la fin de la réponse. Réessayez.',
      };
    }
    if (error.message === 'EventSource connection failed' || error.message === 'SSE connection failed') {
      return {
        kind: 'connection',
        message: 'Erreur de connexion au service IA. Vérifiez que le backend et Ollama sont démarrés.',
      };
    }
    if (error.message === 'Failed to parse complete response') {
      return {
        kind: 'backend',
        message: 'Réponse IA invalide reçue du serveur. Réessayez.',
      };
    }
  }

  return {
    kind: 'unknown',
    message: 'Une erreur inattendue est survenue. Réessayez.',
  };
}

function extractBodyMessage(data: unknown): string {
  if (!data || typeof data !== 'object') return '';
  const record = data as Record<string, unknown>;
  if (typeof record.message === 'string') return record.message;
  if (typeof record.error === 'string') return record.error;
  return '';
}

function looksLikeOllamaUnavailable(message: string): boolean {
  const lower = message.toLowerCase();
  return (
    lower.includes('ollama') ||
    lower.includes('temporairement indisponible') ||
    lower.includes('embedding') ||
    lower.includes('llm')
  );
}

export function isEmptyAssistResult(response: {
  similarInterventions: unknown[];
  suggestions: { probableCauses: string[]; summary: string };
}): boolean {
  const noSimilar = response.similarInterventions.length === 0;
  const fallbackCause = response.suggestions.probableCauses[0] ?? '';
  return (
    noSimilar &&
    (fallbackCause.includes('Aucune intervention similaire') ||
      fallbackCause.includes('temporairement indisponible'))
  );
}
