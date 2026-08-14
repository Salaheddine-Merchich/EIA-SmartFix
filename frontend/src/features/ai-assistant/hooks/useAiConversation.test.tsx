import { act, renderHook, waitFor } from '@testing-library/react';

import { AxiosError } from 'axios';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAiConversation } from './useAiConversation';

import { createAiConversationWrapper } from '../test/aiConversationTestUtils';



vi.mock('@/shared/api', () => ({
  aiApi: {
    assist: vi.fn(),
    listConversations: vi.fn(async () => []),
    createConversation: vi.fn(async (title?: string) => ({
      id: 'server-conv-1',
      title: title ?? 'Nouvelle conversation',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      messages: [],
    })),
    appendConversationMessages: vi.fn(async (id: string) => ({
      id,
      title: 'saved',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      messages: [],
    })),
    getConversation: vi.fn(),
    deleteConversation: vi.fn(),
    deleteAllConversations: vi.fn(),
  },
}));



vi.mock('@/features/auth/context/AuthContext', () => ({

  useAuth: () => ({

    user: { email: 'test@ocp.ma', role: 'TECHNICIEN', nomPrenom: 'Test' },

    isAuthenticated: true,

  }),

}));



import { aiApi } from '@/shared/api';



const assistMock = vi.mocked(aiApi.assist);

const wrapper = createAiConversationWrapper();



const mockResponse = {

  disclaimer: 'Assistance uniquement',

  similarInterventions: [

    {

      interventionId: 'i1',

      equipmentCode: 'EQ-1',

      symptomes: 'Surchauffe',

      similarity: 0.91,

    },

  ],

  suggestions: {

    summary: 'Résumé',

    probableCauses: ['Cause'],

    correctiveActions: ['Action'],

    advice: 'Conseil',

  },

};



function mockLocalStorage() {

  const store: Record<string, string> = {};

  vi.stubGlobal('localStorage', {

    getItem: (key: string) => store[key] ?? null,

    setItem: (key: string, value: string) => {

      store[key] = value;

    },

    removeItem: (key: string) => {

      delete store[key];

    },

    clear: () => {

      Object.keys(store).forEach((key) => delete store[key]);

    },

  });

  return store;

}



describe('useAiConversation', () => {

  beforeEach(() => {

    assistMock.mockReset();

    mockLocalStorage();

  });



  it('appends user and assistant messages on success', async () => {

    assistMock.mockResolvedValue(mockResponse);



    const { result } = renderHook(() => useAiConversation(), { wrapper });



    await act(async () => {

      await result.current.sendMessage('Panne variateur');

    });



    await waitFor(() => {

      expect(result.current.messages).toHaveLength(2);

    });



    expect(result.current.messages[0].role).toBe('user');

    expect(result.current.messages[1].role).toBe('assistant');

    expect(result.current.similarInterventions).toHaveLength(1);

    expect(result.current.status).toBe('online');

    expect(assistMock).toHaveBeenCalledWith(

      'Panne variateur',

      expect.objectContaining({ 
        signal: expect.any(AbortSignal),
        topK: 3
      }),

    );

  });



  it('stores mapped error when request fails', async () => {

    assistMock.mockRejectedValue(new AxiosError('Network Error'));



    const { result } = renderHook(() => useAiConversation(), { wrapper });



    await act(async () => {

      await result.current.sendMessage('Panne');

    });



    await waitFor(() => {

      expect(result.current.messages).toHaveLength(2);

    });



    const assistant = result.current.messages[1];

    expect(assistant.role).toBe('assistant');

    if (assistant.role === 'assistant') {

      expect(assistant.error?.kind).toBe('connection');

    }

    expect(result.current.status).toBe('offline');

  });



  it('cancels in-flight generation and appends interrupted message', async () => {

    assistMock.mockImplementation((_content, options) => {

      return new Promise((_resolve, reject) => {

        options?.signal?.addEventListener('abort', () => {

          reject(new Error('Request was aborted'));

        });

      });

    });



    const { result } = renderHook(() => useAiConversation(), { wrapper });



    act(() => {

      void result.current.sendMessage('Panne longue');

    });



    await waitFor(() => {

      expect(result.current.loading).toBe(true);

    });



    act(() => {

      result.current.cancelGeneration();

    });



    await waitFor(() => {

      expect(result.current.loading).toBe(false);

    });



    expect(result.current.messages).toHaveLength(2);

    const assistant = result.current.messages[1];

    if (assistant.role === 'assistant') {

      expect(assistant.error?.kind).toBe('cancelled');

    }

  });



  it('persists conversation via the API', async () => {
    assistMock.mockResolvedValue({
      ...mockResponse,
      similarInterventions: [],
      suggestions: {
        summary: 'OK',
        probableCauses: ['Cause'],
        correctiveActions: ['Action'],
        advice: 'Conseil',
      },
    });

    const { result } = renderHook(() => useAiConversation(), { wrapper });

    await act(async () => {
      await result.current.sendMessage('Panne persistée');
    });

    await waitFor(() => {
      expect(aiApi.createConversation).toHaveBeenCalled();
      expect(aiApi.appendConversationMessages).toHaveBeenCalled();
    });
  });

});


