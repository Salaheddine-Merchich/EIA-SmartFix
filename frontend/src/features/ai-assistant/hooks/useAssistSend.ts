import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type Dispatch,
  type SetStateAction,
} from 'react';
import axios from 'axios';

import { aiApi } from '@/shared/api';

import type {
  AssistantMessage,
  AssistantStatus,
  AssistContext,
  Conversation,
  UserMessage,
} from '../types';
import { buildAssistantMessage, buildCancelledMessage } from '../utils/buildAssistantMessage';
import { createId } from '../utils/createId';
import {
  appendMessages,
  removeStreamingPlaceholder,
} from '../utils/conversationMessageHelpers';
import { isAssistCancelled, mapAssistError } from '../utils/mapAssistError';
import { isValidAssistQuery } from '../utils/isValidAssistQuery';

interface UseAssistSendOptions {
  setConversation: Dispatch<SetStateAction<Conversation>>;
  setStatus: Dispatch<SetStateAction<AssistantStatus>>;
  assistContext: AssistContext;
  onTurnComplete?: (userContent: string, assistant: AssistantMessage) => Promise<void>;
}

export function useAssistSend({
  setConversation,
  setStatus,
  assistContext,
  onTurnComplete,
}: UseAssistSendOptions) {
  const [loading, setLoading] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);
  const requestIdRef = useRef(0);

  const abortActive = useCallback((bumpRequest = true) => {
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    if (bumpRequest) {
      requestIdRef.current += 1;
    }
  }, []);

  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort();
    };
  }, []);

  const cancelGeneration = useCallback(() => {
    if (!loading) return;
    abortActive(true);
    setLoading(false);
    setConversation((prev) =>
      appendMessages(removeStreamingPlaceholder(prev), [buildCancelledMessage()]),
    );
  }, [abortActive, loading, setConversation]);

  const sendMessage = useCallback(
    async (rawContent: string) => {
      const content = rawContent.trim();
      if (!content || loading || !isValidAssistQuery(content)) return;

      abortActive(false);
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
        const response = await aiApi.assist(content, {
          signal: controller.signal,
          topK: 3,
          failureId: assistContext.failureId,
          equipmentId: assistContext.equipmentId,
        });

        if (requestId !== requestIdRef.current) return;

        const assistantMessage = buildAssistantMessage(response);
        setConversation((prev) => appendMessages(prev, [assistantMessage]));
        setStatus(assistantMessage.error?.kind === 'ollama' ? 'degraded' : 'online');
        if (assistantMessage.response) {
          await onTurnComplete?.(content, assistantMessage);
        }
      } catch (error) {
        if (requestId !== requestIdRef.current) return;
        if (isAssistCancelled(error) || axios.isCancel(error)) {
          setConversation((prev) => appendMessages(prev, [buildCancelledMessage()]));
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
        setConversation((prev) => appendMessages(prev, [assistantMessage]));
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
    [abortActive, assistContext.equipmentId, assistContext.failureId, loading, onTurnComplete, setConversation, setStatus],
  );

  return {
    loading,
    setLoading,
    abortActive,
    sendMessage,
    cancelGeneration,
  };
}
