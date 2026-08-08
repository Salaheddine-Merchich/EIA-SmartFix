import { describe, expect, it } from 'vitest';
import { formatDurationHours, formatDurationMinutes, greetingName } from './formatters';

describe('dashboard formatters', () => {
  it('formats greeting with first name', () => {
    expect(greetingName('Salaheddine El Amrani')).toBe('Bonjour Salaheddine');
  });

  it('returns generic greeting when name is missing', () => {
    expect(greetingName()).toBe('Bonjour');
  });

  it('formats MTTR and MTBF safely', () => {
    expect(formatDurationMinutes(45.6)).toBe('46 min');
    expect(formatDurationHours(120.4)).toBe('120.4 h');
    expect(formatDurationMinutes(null)).toBe('N/A');
  });
});
