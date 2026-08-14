import type { AiConversationDetail } from '@/shared/types';
import type { Conversation, ConversationMessage } from '../types';

export function mapConversationDetail(detail: AiConversationDetail): Conversation {
  const messages: ConversationMessage[] = detail.messages.map((message) => {
    if (message.role === 'user') {
      return {
        id: message.id,
        role: 'user',
        content: message.content,
        createdAt: message.createdAt,
      };
    }
    return {
      id: message.id,
      role: 'assistant',
      createdAt: message.createdAt,
      response: message.payload ?? null,
    };
  });

  return {
    id: detail.id,
    title: detail.title,
    messages,
    createdAt: detail.createdAt,
    updatedAt: detail.updatedAt,
  };
}
