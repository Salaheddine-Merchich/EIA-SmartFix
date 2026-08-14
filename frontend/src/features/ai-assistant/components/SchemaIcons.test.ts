import { describe, expect, it } from 'vitest';
import { schemaIcon, schemaTypeTone } from './SchemaIcons';

describe('schemaIcon', () => {
  it('returns a distinct icon component per schema type', () => {
    const wiring = schemaIcon('wiring');
    const terminal = schemaIcon('terminal');
    const dimension = schemaIcon('dimension');
    const install = schemaIcon('install');
    const block = schemaIcon('block');

    expect(wiring).not.toBe(dimension);
    expect(wiring).not.toBe(terminal);
    expect(terminal).not.toBe(dimension);
    expect(install).not.toBe(wiring);
    expect(block).not.toBe(terminal);
    expect(schemaIcon('unknown')).toBe(wiring);
  });
});

describe('schemaTypeTone', () => {
  it('assigns a distinct tone class per known type', () => {
    expect(schemaTypeTone('wiring')).toContain('sky');
    expect(schemaTypeTone('terminal')).toContain('amber');
    expect(schemaTypeTone('dimension')).toContain('emerald');
    expect(schemaTypeTone('install')).toContain('violet');
    expect(schemaTypeTone('block')).toContain('slate');
  });
});
