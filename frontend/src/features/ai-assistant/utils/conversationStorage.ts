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
    const raw = sessionStorage.getItem(key) ?? localStorage.getItem(key);
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
    sessionStorage.setItem(key, JSON.stringify(conversation));
    localStorage.removeItem(key);
  } catch {
    // Ignore quota / private mode errors
  }
}

export function clearConversationStorage(key: string | null): void {
  if (!key) return;
  sessionStorage.removeItem(key);
  localStorage.removeItem(key);
}

export function clearConversationStorageForUser(email: string | undefined): void {
  clearConversationStorage(getConversationStorageKey(email));
}
