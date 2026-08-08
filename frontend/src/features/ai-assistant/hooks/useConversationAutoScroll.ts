import { useEffect, useRef } from 'react';

const BOTTOM_THRESHOLD_PX = 80;

interface UseConversationAutoScrollOptions {
  messageCount: number;
  loading: boolean;
}

export function useConversationAutoScroll({
  messageCount,
  loading,
}: UseConversationAutoScrollOptions) {
  const containerRef = useRef<HTMLDivElement>(null);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = containerRef.current;
    const sentinel = sentinelRef.current;
    if (!container || !sentinel) return;

    const distanceFromBottom =
      container.scrollHeight - container.scrollTop - container.clientHeight;
    const isNearBottom = distanceFromBottom <= BOTTOM_THRESHOLD_PX;

    if (!isNearBottom && !loading) return;

    sentinel.scrollIntoView({
      block: 'end',
      behavior: loading ? 'smooth' : 'auto',
    });
  }, [messageCount, loading]);

  return { containerRef, sentinelRef };
}
