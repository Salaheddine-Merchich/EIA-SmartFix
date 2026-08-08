import type { AiAssistResponse } from '@/shared/types';
import type { AssistantMessage } from '../types';
import { createId } from './createId';
import { isEmptyAssistResult } from './mapAssistError';

export function buildAssistantMessage(response: AiAssistResponse): AssistantMessage {
  const suggestions = response.suggestions ?? {
    probableCauses: [],
    correctiveActions: [],
    summary: '',
    advice: '',
  };
  const unavailable = suggestions.probableCauses?.[0]?.includes('temporairement indisponible');
  if (unavailable) {
    return {
      id: createId('assistant'),
      role: 'assistant',
      createdAt: new Date().toISOString(),
      response,
      error: {
        kind: 'ollama',
        message:
          response.suggestions.summary ||
          "L'assistance IA est temporairement indisponible. Réessayez ultérieurement.",
      },
    };
  }

  if (isEmptyAssistResult(response)) {
    return {
      id: createId('assistant'),
      role: 'assistant',
      createdAt: new Date().toISOString(),
      response,
      error: {
        kind: 'empty',
        message:
          "Aucune intervention validée suffisamment similaire n'a été trouvée. Affinez la description ou consultez la documentation constructeur.",
      },
    };
  }

  return {
    id: createId('assistant'),
    role: 'assistant',
    createdAt: new Date().toISOString(),
    response,
  };
}

export function buildCancelledMessage(): AssistantMessage {
  return {
    id: createId('assistant'),
    role: 'assistant',
    createdAt: new Date().toISOString(),
    response: null,
    error: {
      kind: 'cancelled',
      message: 'La génération a été interrompue. Vous pouvez poser une nouvelle question.',
    },
  };
}
