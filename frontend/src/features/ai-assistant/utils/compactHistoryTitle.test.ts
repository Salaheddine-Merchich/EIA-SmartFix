import { describe, expect, it } from 'vitest';
import { compactHistoryTitle } from './compactHistoryTitle';

describe('compactHistoryTitle', () => {
  it('keeps short titles unchanged', () => {
    expect(compactHistoryTitle('Pompe PV')).toBe('Pompe PV');
  });

  it('cuts at a word boundary without ellipsis', () => {
    expect(compactHistoryTitle('Code OUt1 affiché sur variateur Goodrive 100-PV')).toBe(
      'Code OUt1 affiché sur variateur',
    );
  });

  it('normalizes whitespace', () => {
    expect(compactHistoryTitle('  Defaut   E21  Hitachi  ')).toBe('Defaut E21 Hitachi');
  });
});
