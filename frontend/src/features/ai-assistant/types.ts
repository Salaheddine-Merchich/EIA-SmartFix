import type { AiAssistResponse } from '@/shared/types';

export type AssistantStatus = 'online' | 'degraded' | 'offline';

export type AssistErrorKind =
  | 'backend'
  | 'connection'
  | 'ollama'
  | 'auth'
  | 'timeout'
  | 'empty'
  | 'cancelled'
  | 'unknown';

export interface AssistError {
  kind: AssistErrorKind;
  message: string;
}

export interface UserMessage {
  id: string;
  role: 'user';
  content: string;
  createdAt: string;
}

export interface AssistantMessage {
  id: string;
  role: 'assistant';
  createdAt: string;
  response: AiAssistResponse | null;
  error?: AssistError;
}

export type ConversationMessage = UserMessage | AssistantMessage;

export interface Conversation {
  id: string;
  title: string;
  messages: ConversationMessage[];
  createdAt: string;
  updatedAt: string;
}

export type SimilarInterventionItem = AiAssistResponse['similarInterventions'][number];
