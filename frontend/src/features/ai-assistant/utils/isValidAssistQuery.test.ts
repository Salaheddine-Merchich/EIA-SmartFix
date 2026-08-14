import { describe, expect, it } from 'vitest';
import { isValidAssistQuery } from './isValidAssistQuery';

describe('isValidAssistQuery', () => {
  it('rejects single letters and very short text without fault code', () => {
    expect(isValidAssistQuery('I')).toBe(false);
    expect(isValidAssistQuery('P')).toBe(false);
    expect(isValidAssistQuery('PV')).toBe(false);
    expect(isValidAssistQuery('   ')).toBe(false);
  });

  it('rejects repeated or meaningless text even when long enough', () => {
    expect(isValidAssistQuery('iiiiiiiiii')).toBe(false);
    expect(isValidAssistQuery('aaaaaaaaaa')).toBe(false);
    expect(isValidAssistQuery('abcdefghij')).toBe(false);
  });

  it('accepts recognized fault codes', () => {
    expect(isValidAssistQuery('E21')).toBe(true);
    expect(isValidAssistQuery('F001')).toBe(true);
  });

  it('accepts descriptions with equipment or symptoms', () => {
    expect(isValidAssistQuery('Pompe PV ne démarre plus')).toBe(true);
    expect(isValidAssistQuery('panne variateur')).toBe(true);
  });
});
