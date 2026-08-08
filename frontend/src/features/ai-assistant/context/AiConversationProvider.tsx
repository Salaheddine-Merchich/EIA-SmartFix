import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

import { useAuth } from '@/features/auth/context/AuthContext';

import { useAssistSend } from '../hooks/useAssistSend';
import { useConversationPersistence } from '../hooks/useConversationPersistence';
import { useLoadingStatusMessage } from '../hooks/useLoadingStatusMessage';
import type {
  AssistantStatus,
  Conversation,
  ConversationMessage,
  SimilarInterventionItem,
} from '../types';
import { getConversationStorageKey } from '../utils/conversationStorage';
import { getSimilarInterventions } from '../utils/conversationMessageHelpers';

interface AiConversationContextValue {
  conversation: Conversation;
  messages: ConversationMessage[];
  loading: boolean;
  loadingMessage: string;
  status: AssistantStatus;
  similarInterventions: SimilarInterventionItem[];
  sendMessage: (rawContent: string) => Promise<void>;
  cancelGeneration: () => void;
  clearConversation: () => void;
}

const AiConversationContext = createContext<AiConversationContextValue | null>(null);

export function AiConversationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const storageKey = getConversationStorageKey(user?.email);
  const [status, setStatus] = useState<AssistantStatus>('online');

  const { conversation, setConversation, resetConversation } =
    useConversationPersistence(storageKey);

  const { loading, setLoading, abortActive, sendMessage, cancelGeneration } = useAssistSend({
    setConversation,
    setStatus,
  });

  const loadingMessage = useLoadingStatusMessage(loading);

  useEffect(() => {
    abortActive(true);
    setLoading(false);
    setStatus('online');
  }, [storageKey, abortActive, setLoading]);

  useEffect(() => {
    if (!user) {
      abortActive(false);
    }
  }, [user, abortActive]);

  const clearConversation = useCallback(() => {
    abortActive(true);
    setLoading(false);
    resetConversation();
    setStatus('online');
  }, [abortActive, resetConversation, setLoading]);

  const similarInterventions = useMemo(
    () => getSimilarInterventions(conversation.messages),
    [conversation.messages],
  );

  const value = useMemo(
    () => ({
      conversation,
      messages: conversation.messages as ConversationMessage[],
      loading,
      loadingMessage,
      status,
      similarInterventions,
      sendMessage,
      cancelGeneration,
      clearConversation,
    }),
    [
      conversation,
      loading,
      loadingMessage,
      status,
      similarInterventions,
      sendMessage,
      cancelGeneration,
      clearConversation,
    ],
  );

  return <AiConversationContext.Provider value={value}>{children}</AiConversationContext.Provider>;
}

export function useAiConversationContext(): AiConversationContextValue {
  const ctx = useContext(AiConversationContext);
  if (!ctx) {
    throw new Error('useAiConversation must be used within AiConversationProvider');
  }
  return ctx;
}
