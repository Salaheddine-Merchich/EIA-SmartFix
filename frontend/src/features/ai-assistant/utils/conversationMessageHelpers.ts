import type {
  AssistantMessage,
  Conversation,
  ConversationMessage,
  SimilarInterventionItem,
} from '../types';

export function getLastAssistantMessage(
  messages: ConversationMessage[],
): AssistantMessage | null {
  for (let i = messages.length - 1; i >= 0; i -= 1) {
    const message = messages[i];
    if (message.role === 'assistant') return message;
  }
  return null;
}

export function getSimilarInterventions(
  messages: ConversationMessage[],
): SimilarInterventionItem[] {
  return getLastAssistantMessage(messages)?.response?.similarInterventions ?? [];
}

export function appendMessages(
  conversation: Conversation,
  messages: ConversationMessage[],
  extras?: Partial<Pick<Conversation, 'title'>>,
): Conversation {
  return {
    ...conversation,
    ...extras,
    messages: [...conversation.messages, ...messages],
    updatedAt: new Date().toISOString(),
  };
}

export function removeStreamingPlaceholder(conversation: Conversation): Conversation {
  return {
    ...conversation,
    messages: conversation.messages.filter(
      (msg) => !(msg.role === 'assistant' && msg.response === null && !msg.error),
    ),
  };
}
