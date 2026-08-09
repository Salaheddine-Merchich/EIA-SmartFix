import { describe, expect, it } from 'vitest';
import { liveFeedEmptyMessage, liveStreamStatusLabel } from './connectionMessages';

describe('connectionMessages', () => {
  it('liveFeedEmptyMessage reflects connection state', () => {
    expect(liveFeedEmptyMessage('connected')).toContain('connecté');
    expect(liveFeedEmptyMessage('connecting')).toContain('en cours');
    expect(liveFeedEmptyMessage('error')).toContain('indisponible');
    expect(liveFeedEmptyMessage('error')).toContain('reconnexion');
    expect(liveFeedEmptyMessage('disconnected')).toContain('déconnecté');
    expect(liveFeedEmptyMessage('disconnected')).toContain('reconnexion');
  });

  it('liveStreamStatusLabel uses SSE wording and reconnect hints', () => {
    expect(liveStreamStatusLabel('connected', false)).toContain('SSE');
    expect(liveStreamStatusLabel('connecting', false)).toBe('Connexion SSE…');
    expect(liveStreamStatusLabel('error', false)).toContain('erreur');
    expect(liveStreamStatusLabel('error', false)).toContain('reconnexion');
    expect(liveStreamStatusLabel('disconnected', false)).toContain('déconnecté');
    expect(liveStreamStatusLabel('connected', true)).toBe('Vérification…');
  });
});
