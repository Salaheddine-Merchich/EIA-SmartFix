import { describe, expect, it } from 'vitest';
import { formatRelativeLive } from './formatRelativeLive';

describe('formatRelativeLive', () => {
  it('formats seconds', () => {
    const now = Date.parse('2026-08-05T12:00:30Z');
    const date = '2026-08-05T12:00:15Z';
    expect(formatRelativeLive(date, now)).toBe('il y a 15 sec');
  });

  it('formats minutes', () => {
    const now = Date.parse('2026-08-05T12:02:00Z');
    const date = '2026-08-05T12:00:00Z';
    expect(formatRelativeLive(date, now)).toBe('il y a 2 min');
  });

  it('formats hours', () => {
    const now = Date.parse('2026-08-05T15:00:00Z');
    const date = '2026-08-05T12:00:00Z';
    expect(formatRelativeLive(date, now)).toBe('il y a 3 h');
  });
});
