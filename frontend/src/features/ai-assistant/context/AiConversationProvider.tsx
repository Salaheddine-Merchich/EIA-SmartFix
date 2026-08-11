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
  AssistContext,
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
  assistContext: AssistContext;
  composerPrefill: string;
  sendMessage: (rawContent: string) => Promise<void>;
  cancelGeneration: () => void;
  clearConversation: () => void;
  setAssistContext: (context: AssistContext) => void;
  setComposerPrefill: (value: string) => void;
  clearComposerPrefill: () => void;
}

const AiConversationContext = createContext<AiConversationContextValue | null>(null);

export function AiConversationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const storageKey = getConversationStorageKey(user?.email);
  const [status, setStatus] = useState<AssistantStatus>('online');
  const [assistContext, setAssistContext] = useState<AssistContext>({});
  const [composerPrefill, setComposerPrefill] = useState('');

  const { conversation, setConversation, resetConversation } =
    useConversationPersistence(storageKey);

  const { loading, setLoading, abortActive, sendMessage, cancelGeneration } = useAssistSend({
    setConversation,
    setStatus,
    assistContext,
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
    setAssistContext({});
    setComposerPrefill('');
  }, [abortActive, resetConversation, setLoading]);

  const clearComposerPrefill = useCallback(() => {
    setComposerPrefill('');
  }, []);

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
      assistContext,
      composerPrefill,
      sendMessage,
      cancelGeneration,
      clearConversation,
      setAssistContext,
      setComposerPrefill,
      clearComposerPrefill,
    }),
    [
      conversation,
      loading,
      loadingMessage,
      status,
      similarInterventions,
      assistContext,
      composerPrefill,
      sendMessage,
      cancelGeneration,
      clearConversation,
      clearComposerPrefill,
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
