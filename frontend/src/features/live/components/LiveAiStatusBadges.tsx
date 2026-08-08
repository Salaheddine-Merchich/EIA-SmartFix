import { useMemo } from 'react';
import { useLive } from '../providers/LiveProvider';
import { LiveServiceBadge } from './LiveServiceBadge';
import type { ServiceState } from '../types';

interface LiveAiStatusBadgesProps {
  assistantStatus: 'online' | 'degraded' | 'offline';
}

function mapAssistant(state: string): ServiceState {
  if (state === 'online') return 'ONLINE';
  if (state === 'degraded') return 'DEGRADED';
  return 'OFFLINE';
}

export function LiveAiStatusBadges({ assistantStatus }: LiveAiStatusBadgesProps) {
  const { status, recentEvents } = useLive();

  const ragIndexing = useMemo(
    () => recentEvents.some((e) => e.type === 'INTERVENTION_VALIDATED'),
    [recentEvents],
  );
  const embeddingDown = useMemo(
    () => recentEvents.some((e) => e.type === 'AI_UNAVAILABLE' && e.message.toLowerCase().includes('embedding')),
    [recentEvents],
  );

  const aiState = status?.ai.state ?? mapAssistant(assistantStatus);
  const ragState = status?.rag.state ?? 'DEGRADED';

  return (
    <div className="flex flex-wrap items-center gap-2" role="group" aria-label="État IA temps réel">
      <LiveServiceBadge label={aiState === 'ONLINE' ? 'IA Online' : 'IA Offline'} state={aiState} pulse />
      {embeddingDown && <LiveServiceBadge label="Embedding indisponible" state="OFFLINE" />}
      <LiveServiceBadge label="Recherche hybride" state={ragState === 'ONLINE' ? 'ONLINE' : 'DEGRADED'} />
      {ragIndexing && <LiveServiceBadge label="RAG Indexing" state="DEGRADED" pulse />}
    </div>
  );
}
