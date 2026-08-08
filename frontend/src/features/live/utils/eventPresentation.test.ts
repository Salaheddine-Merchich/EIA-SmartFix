import { describe, expect, it } from 'vitest';
import { isCriticalLiveEvent, liveEventIcon } from './eventPresentation';

describe('eventPresentation', () => {
  it('maps intervention validated icon', () => {
    expect(liveEventIcon('INTERVENTION_VALIDATED')).toBe('OK');
  });

  it('detects critical events', () => {
    expect(isCriticalLiveEvent('CRITICAL_ALERT')).toBe(true);
    expect(isCriticalLiveEvent('INTERVENTION_CREATED')).toBe(false);
  });
});
