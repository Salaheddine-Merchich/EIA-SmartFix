import { describe, expect, it } from 'vitest';
import { schemaTypeLabel } from './schemaTypeLabel';

describe('schemaTypeLabel', () => {
  it('maps known schema types to French labels', () => {
    expect(schemaTypeLabel('wiring')).toBe('Câblage');
    expect(schemaTypeLabel('terminal')).toBe('Bornes');
    expect(schemaTypeLabel('dimension')).toBe('Dimensions');
    expect(schemaTypeLabel('install')).toBe('Installation');
    expect(schemaTypeLabel('block')).toBe('Bloc fonctionnel');
  });

  it('returns the original value for unknown types', () => {
    expect(schemaTypeLabel('custom')).toBe('custom');
  });
});
