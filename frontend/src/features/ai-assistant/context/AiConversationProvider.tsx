import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';

import { useAuth } from '@/features/auth/context/AuthContext';
import { aiApi } from '@/shared/api';
import { queryClient } from '@/shared/queryClient';
import type { AiConversationSummary } from '@/shared/types';

import { useAssistSend } from '../hooks/useAssistSend';
import { useLoadingStatusMessage } from '../hooks/useLoadingStatusMessage';
import type {
  AssistantMessage,
  AssistantStatus,
  AssistContext,
  Conversation,
  ConversationMessage,
  SimilarInterventionItem,
} from '../types';
import { createConversation } from '../utils/conversationStorage';
import { getSimilarInterventions } from '../utils/conversationMessageHelpers';
import { mapConversationDetail } from '../utils/mapConversationDetail';
import { compactHistoryTitle } from '../utils/compactHistoryTitle';

interface AiConversationContextValue {
  conversation: Conversation;
  conversationId: string | null;
  history: AiConversationSummary[];
  historyLoading: boolean;
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
  openConversation: (id: string) => Promise<void>;
  deleteConversation: (id: string) => Promise<void>;
  clearAllHistory: () => Promise<void>;
  setAssistContext: (context: AssistContext) => void;
  setComposerPrefill: (value: string) => void;
  clearComposerPrefill: () => void;
}

const AiConversationContext = createContext<AiConversationContextValue | null>(null);

export function AiConversationProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [status, setStatus] = useState<AssistantStatus>('online');
  const [assistContext, setAssistContext] = useState<AssistContext>({});
  const [composerPrefill, setComposerPrefill] = useState('');
  const [conversation, setConversation] = useState<Conversation>(createConversation);
  const [history, setHistory] = useState<AiConversationSummary[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const conversationIdRef = useRef<string | null>(null);

  const invalidateDashboard = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  }, []);

  const refreshHistory = useCallback(async () => {
    const items = await aiApi.listConversations();
    setHistory(items);
  }, []);

  useEffect(() => {
    if (!user) {
      setHistory([]);
      return;
    }
    setHistoryLoading(true);
    refreshHistory()
      .catch(() => setHistory([]))
      .finally(() => setHistoryLoading(false));
  }, [user, refreshHistory]);

  const persistTurn = useCallback(
    async (userContent: string, assistant: AssistantMessage) => {
      if (!assistant.response) return;
      try {
        let id = conversationIdRef.current;
        if (!id) {
          const created = await aiApi.createConversation(compactHistoryTitle(userContent));
          id = created.id;
          conversationIdRef.current = id;
          setConversation((prev) => ({
            ...prev,
            id: created.id,
            title: created.title || prev.title,
          }));
        }
        await aiApi.appendConversationMessages(id, userContent, assistant.response);
        await refreshHistory();
        invalidateDashboard();
      } catch {
        // Keep the local thread if persistence fails.
      }
    },
    [refreshHistory, invalidateDashboard],
  );

  const { loading, setLoading, abortActive, sendMessage, cancelGeneration } = useAssistSend({
    setConversation,
    setStatus,
    assistContext,
    onTurnComplete: persistTurn,
  });

  const loadingMessage = useLoadingStatusMessage(loading);

  useEffect(() => {
    abortActive(true);
    setLoading(false);
    setStatus('online');
    conversationIdRef.current = null;
    setConversation(createConversation());
    setAssistContext({});
    setComposerPrefill('');
  }, [user?.email, abortActive, setLoading]);

  useEffect(() => {
    if (!user) {
      abortActive(false);
    }
  }, [user, abortActive]);

  const clearConversation = useCallback(() => {
    abortActive(true);
    setLoading(false);
    conversationIdRef.current = null;
    setConversation(createConversation());
    setStatus('online');
    setAssistContext({});
    setComposerPrefill('');
  }, [abortActive, setLoading]);

  const openConversation = useCallback(
    async (id: string) => {
      abortActive(true);
      setLoading(false);
      const detail = await aiApi.getConversation(id);
      conversationIdRef.current = detail.id;
      setConversation(mapConversationDetail(detail));
      setStatus('online');
      setAssistContext({});
      setComposerPrefill('');
    },
    [abortActive, setLoading],
  );

  const deleteConversation = useCallback(
    async (id: string) => {
      await aiApi.deleteConversation(id);
      if (conversationIdRef.current === id) {
        clearConversation();
      }
      await refreshHistory();
      invalidateDashboard();
    },
    [clearConversation, refreshHistory, invalidateDashboard],
  );

  const clearAllHistory = useCallback(async () => {
    await aiApi.deleteAllConversations();
    clearConversation();
    setHistory([]);
    invalidateDashboard();
  }, [clearConversation, invalidateDashboard]);

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
      conversationId: conversationIdRef.current,
      history,
      historyLoading,
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
      openConversation,
      deleteConversation,
      clearAllHistory,
      setAssistContext,
      setComposerPrefill,
      clearComposerPrefill,
    }),
    [
      conversation,
      history,
      historyLoading,
      loading,
      loadingMessage,
      status,
      similarInterventions,
      assistContext,
      composerPrefill,
      sendMessage,
      cancelGeneration,
      clearConversation,
      openConversation,
      deleteConversation,
      clearAllHistory,
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
