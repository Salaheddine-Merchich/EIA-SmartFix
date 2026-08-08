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
import axios from 'axios';

import { aiApi } from '@/shared/api';
import { useAuth } from '@/features/auth/context/AuthContext';

import type {
  AssistantMessage,
  AssistantStatus,
  Conversation,
  ConversationMessage,
  SimilarInterventionItem,
  UserMessage,
} from '../types';
import { buildAssistantMessage, buildCancelledMessage } from '../utils/buildAssistantMessage';
import { createId } from '../utils/createId';
import {
  clearConversationStorage,
  createConversation,
  getConversationStorageKey,
  loadConversation,
  saveConversation,
} from '../utils/conversationStorage';
import { isAssistCancelled, mapAssistError } from '../utils/mapAssistError';

const LOADING_MESSAGES = [
  'Analyse de votre demande…',
  "Recherche d'interventions et documents similaires…", 
  'Génération de la réponse… (peut prendre 1-2 minutes)',
  'Finalisation de la réponse…',
] as const;

const PERSIST_DEBOUNCE_MS = 300;

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

  const [conversation, setConversation] = useState<Conversation>(() => loadConversation(storageKey));
  const [loading, setLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState<string>(LOADING_MESSAGES[0]);
  const [status, setStatus] = useState<AssistantStatus>('online');

  const abortControllerRef = useRef<AbortController | null>(null);
  const requestIdRef = useRef(0);
  const persistTimerRef = useRef<number | null>(null);

  useEffect(() => {
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    requestIdRef.current += 1;
    setConversation(loadConversation(storageKey));
    setLoading(false);
    setStatus('online');
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

  useEffect(() => {
    if (!user) {
      abortControllerRef.current?.abort();
      abortControllerRef.current = null;
    }
  }, [user]);

  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort();
    };
  }, []);

  useEffect(() => {
    if (!loading) {
      setLoadingMessage(LOADING_MESSAGES[0]);
      return;
    }

    setLoadingMessage(LOADING_MESSAGES[0]);
    const retrievalTimer = window.setTimeout(() => {
      if (loading) {
        setLoadingMessage(LOADING_MESSAGES[1]);
      }
    }, 3_000);
    const llmTimer = window.setTimeout(() => {
      if (loading) {
        setLoadingMessage(LOADING_MESSAGES[2]);
      }
    }, 10_000);
    const finalTimer = window.setTimeout(() => {
      if (loading) {
        setLoadingMessage(LOADING_MESSAGES[3]);
      }
    }, 120_000);

    return () => {
      window.clearTimeout(retrievalTimer);
      window.clearTimeout(llmTimer);
      window.clearTimeout(finalTimer);
    };
  }, [loading]);

  const lastAssistant = useMemo(() => {
    for (let i = conversation.messages.length - 1; i >= 0; i -= 1) {
      const message = conversation.messages[i];
      if (message.role === 'assistant') return message;
    }
    return null;
  }, [conversation.messages]);

  const similarInterventions: SimilarInterventionItem[] = useMemo(() => {
    return lastAssistant?.response?.similarInterventions ?? [];
  }, [lastAssistant]);

  const cancelGeneration = useCallback(() => {
    if (!loading) return;
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    requestIdRef.current += 1;
    setLoading(false);
    setConversation((prev) => {
      const withoutPlaceholder = prev.messages.filter(
        (msg) => !(msg.role === 'assistant' && msg.response === null && !msg.error),
      );
      return {
        ...prev,
        messages: [...withoutPlaceholder, buildCancelledMessage()],
        updatedAt: new Date().toISOString(),
      };
    });
  }, [loading]);

  const sendMessage = useCallback(
    async (rawContent: string) => {
      const content = rawContent.trim();
      if (!content || loading) return;

      abortControllerRef.current?.abort();
      const controller = new AbortController();
      abortControllerRef.current = controller;
      const requestId = ++requestIdRef.current;

      const now = new Date().toISOString();
      const userMessage: UserMessage = {
        id: createId('user'),
        role: 'user',
        content,
        createdAt: now,
      };

      setConversation((prev) => ({
        ...prev,
        title: prev.messages.length === 0 ? content.slice(0, 60) : prev.title,
        messages: [...prev.messages, userMessage],
        updatedAt: now,
      }));
      setLoading(true);

      try {
        // Use non-streaming API for more reliable responses with Ollama
        const response = await aiApi.assist(content, { 
          signal: controller.signal,
          topK: 3,
        });
        
        if (requestId !== requestIdRef.current) return;

        // Build the assistant message from the complete response
        const assistantMessage = buildAssistantMessage(response);
        setConversation((prev) => ({
          ...prev,
          messages: [...prev.messages, assistantMessage],
          updatedAt: new Date().toISOString(),
        }));
        
        setStatus(assistantMessage.error?.kind === 'ollama' ? 'degraded' : 'online');
      } catch (error) {
        if (requestId !== requestIdRef.current) return;
        if (isAssistCancelled(error) || axios.isCancel(error)) {
          setConversation((prev) => ({
            ...prev,
            messages: [...prev.messages, buildCancelledMessage()],
            updatedAt: new Date().toISOString(),
          }));
          return;
        }

        const mapped = mapAssistError(error);
        const assistantMessage: AssistantMessage = {
          id: createId('assistant'),
          role: 'assistant',
          createdAt: new Date().toISOString(),
          response: null,
          error: mapped,
        };
        setConversation((prev) => ({
          ...prev,
          messages: [...prev.messages, assistantMessage],
          updatedAt: new Date().toISOString(),
        }));
        setStatus(
          mapped.kind === 'backend' || mapped.kind === 'connection' || mapped.kind === 'auth'
            ? 'offline'
            : 'degraded',
        );
      } finally {
        if (requestId === requestIdRef.current) {
          setLoading(false);
          abortControllerRef.current = null;
        }
      }
    },
    [loading],
  );

  const clearConversation = useCallback(() => {
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    requestIdRef.current += 1;
    setLoading(false);
    const next = createConversation();
    setConversation(next);
    setStatus('online');
    if (storageKey) {
      clearConversationStorage(storageKey);
      saveConversation(storageKey, next);
    }
  }, [storageKey]);

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
