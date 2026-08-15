import { useQuery } from '@tanstack/react-query';
import { dashboardApi, failuresApi } from '@/shared/api';
import type { Failure } from '@/shared/types';
import type { DashboardViewModel } from '../types';

async function fetchCriticalFailurePreview(): Promise<Failure[]> {
  const [critiqueOuverte, critiqueEnCours, hauteOuverte, hauteEnCours] = await Promise.all([
    failuresApi.list({ criticite: 'CRITIQUE', statut: 'OUVERTE', page: 0, size: 6 }),
    failuresApi.list({ criticite: 'CRITIQUE', statut: 'EN_COURS', page: 0, size: 6 }),
    failuresApi.list({ criticite: 'HAUTE', statut: 'OUVERTE', page: 0, size: 6 }),
    failuresApi.list({ criticite: 'HAUTE', statut: 'EN_COURS', page: 0, size: 6 }),
  ]);

  const unique = new Map(
    [...critiqueOuverte.content, ...critiqueEnCours.content, ...hauteOuverte.content, ...hauteEnCours.content].map(
      (failure) => [failure.id, failure],
    ),
  );

  return [...unique.values()]
    .sort((a, b) => new Date(b.dateHeure).getTime() - new Date(a.dateHeure).getTime())
    .slice(0, 6);
}

export function useDashboardData() {
  const statsQuery = useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: dashboardApi.stats,
  });

  const recentFailuresQuery = useQuery({
    queryKey: ['dashboard', 'recent-failures'],
    queryFn: () => failuresApi.list({ page: 0, size: 8 }),
    select: (response) => response.content,
  });

  const criticalFailuresQuery = useQuery({
    queryKey: ['dashboard', 'critical-failures'],
    queryFn: fetchCriticalFailurePreview,
  });

  const isLoading =
    statsQuery.isLoading || recentFailuresQuery.isLoading || criticalFailuresQuery.isLoading;

  const isError = statsQuery.isError || recentFailuresQuery.isError || criticalFailuresQuery.isError;

  const viewModel: DashboardViewModel | null =
    statsQuery.data && recentFailuresQuery.data && criticalFailuresQuery.data
      ? {
          stats: statsQuery.data,
          recentFailures: recentFailuresQuery.data,
          criticalFailures: criticalFailuresQuery.data,
        }
      : null;

  return {
    viewModel,
    isLoading,
    isError,
    refetch: () => {
      statsQuery.refetch();
      recentFailuresQuery.refetch();
      criticalFailuresQuery.refetch();
    },
  };
}
