import type { Conversation } from '../types';
import { createId } from './createId';

const STORAGE_PREFIX = 'eia-smartfix:ai-conversation:';

export function getConversationStorageKey(email: string | undefined): string | null {
  if (!email) return null;
  return `${STORAGE_PREFIX}${email}`;
}

export function createConversation(): Conversation {
  const now = new Date().toISOString();
  return {
    id: createId('conv'),
    title: 'Nouvelle conversation',
    messages: [],
    createdAt: now,
    updatedAt: now,
  };
}

export function loadConversation(key: string | null): Conversation {
  if (!key) return createConversation();
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return createConversation();
    const parsed = JSON.parse(raw) as Conversation;
    if (!parsed?.id || !Array.isArray(parsed.messages)) {
      return createConversation();
    }
    return parsed;
  } catch {
    return createConversation();
  }
}

export function saveConversation(key: string | null, conversation: Conversation): void {
  if (!key) return;
  try {
    localStorage.setItem(key, JSON.stringify(conversation));
  } catch {
    // Ignore quota / private mode errors
  }
}

export function clearConversationStorage(key: string | null): void {
  if (!key) return;
  localStorage.removeItem(key);
}
