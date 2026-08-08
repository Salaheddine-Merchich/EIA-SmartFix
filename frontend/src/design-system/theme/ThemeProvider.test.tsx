import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeProvider, useTheme } from '../theme/ThemeProvider';

function ThemeReader() {
  const { resolvedTheme } = useTheme();
  return <span>{resolvedTheme}</span>;
}

describe('ThemeProvider', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => null),
      setItem: vi.fn(),
    });
  });

  it('provides resolved theme', () => {
    render(
      <ThemeProvider>
        <ThemeReader />
      </ThemeProvider>,
    );
    expect(screen.getByText(/light|dark/)).toBeInTheDocument();
  });
});
