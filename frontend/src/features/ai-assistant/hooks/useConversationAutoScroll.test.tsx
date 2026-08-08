import { render } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { useConversationAutoScroll } from './useConversationAutoScroll';

function ScrollHarness({ messageCount, loading }: { messageCount: number; loading: boolean }) {
  const { containerRef, sentinelRef } = useConversationAutoScroll({ messageCount, loading });
  return (
    <div ref={containerRef} data-testid="container" style={{ height: 200, overflow: 'auto' }}>
      <div style={{ height: 400 }} />
      <div ref={sentinelRef} data-testid="sentinel" />
    </div>
  );
}

describe('useConversationAutoScroll', () => {
  beforeEach(() => {
    Element.prototype.scrollIntoView = vi.fn();
  });

  it('scrolls when loading starts', () => {
    const scrollIntoView = vi.mocked(Element.prototype.scrollIntoView);
    const { rerender } = render(<ScrollHarness messageCount={1} loading={false} />);

    rerender(<ScrollHarness messageCount={2} loading />);

    expect(scrollIntoView).toHaveBeenCalled();
  });
});
