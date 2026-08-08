import { useQuery } from '@tanstack/react-query';
import { liveApi } from '@/shared/api';

export function useLiveStatus(enabled = true) {
  return useQuery({
    queryKey: ['live', 'status'],
    queryFn: () => liveApi.status(),
    enabled,
    staleTime: 15_000,
    refetchInterval: 30_000,
  });
}
