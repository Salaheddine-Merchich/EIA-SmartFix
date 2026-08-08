import { useCallback, useEffect, useRef, useState } from 'react';

import type { Conversation } from '../types';
import {
  clearConversationStorage,
  createConversation,
  loadConversation,
  saveConversation,
} from '../utils/conversationStorage';

const PERSIST_DEBOUNCE_MS = 300;

export function useConversationPersistence(storageKey: string | null) {
  const [conversation, setConversation] = useState<Conversation>(() => loadConversation(storageKey));
  const persistTimerRef = useRef<number | null>(null);

  useEffect(() => {
    setConversation(loadConversation(storageKey));
  }, [storageKey]);

  useEffect(() => {
    if (!storageKey) return;
    if (persistTimerRef.current) {
      window.clearTimeout(persistTimerRef.current);
    }
    persistTimerRef.current = window.setTimeout(() => {
      saveConversation(storageKey, conversation);
    }, PERSIST_DEBOUNCE_MS);
    return () => {
      if (persistTimerRef.current) {
        window.clearTimeout(persistTimerRef.current);
      }
    };
  }, [conversation, storageKey]);

  const resetConversation = useCallback(() => {
    const next = createConversation();
    setConversation(next);
    if (storageKey) {
      clearConversationStorage(storageKey);
      saveConversation(storageKey, next);
    }
    return next;
  }, [storageKey]);

  return {
    conversation,
    setConversation,
    resetConversation,
  };
}
