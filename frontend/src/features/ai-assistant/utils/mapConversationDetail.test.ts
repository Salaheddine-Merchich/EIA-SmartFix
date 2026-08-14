import { describe, expect, it } from 'vitest';
import { mapConversationDetail } from './mapConversationDetail';

describe('mapConversationDetail', () => {
  it('maps user and assistant messages from the API payload', () => {
    const conversation = mapConversationDetail({
      id: 'c1',
      title: 'Pompe PV',
      createdAt: '2026-08-14T10:00:00Z',
      updatedAt: '2026-08-14T10:01:00Z',
      messages: [
        {
          id: 'm1',
          role: 'user',
          content: 'Pompe PV ne démarre plus',
          createdAt: '2026-08-14T10:00:00Z',
        },
        {
          id: 'm2',
          role: 'assistant',
          content: 'Résumé',
          payload: {
            disclaimer: 'x',
            similarInterventions: [],
            suggestions: {
              summary: 'Résumé',
              probableCauses: ['Cause'],
              correctiveActions: ['Action'],
              advice: 'Conseil',
            },
          },
          createdAt: '2026-08-14T10:01:00Z',
        },
      ],
    });

    expect(conversation.messages).toHaveLength(2);
    expect(conversation.messages[0].role).toBe('user');
    expect(conversation.messages[1].role).toBe('assistant');
    if (conversation.messages[1].role === 'assistant') {
      expect(conversation.messages[1].response?.suggestions.summary).toBe('Résumé');
    }
  });
});
