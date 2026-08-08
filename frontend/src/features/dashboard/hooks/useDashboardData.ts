import { useQuery } from '@tanstack/react-query';
import { dashboardApi, equipmentApi, failuresApi } from '@/shared/api';
import type { DashboardViewModel } from '../types';

async function fetchCriticalFailures() {
  const [critique, haute] = await Promise.all([
    failuresApi.list({ criticite: 'CRITIQUE', page: 0, size: 5 }),
    failuresApi.list({ criticite: 'HAUTE', page: 0, size: 5 }),
  ]);

  const open = [...critique.content, ...haute.content].filter(
    (failure) => failure.statut === 'OUVERTE' || failure.statut === 'EN_COURS',
  );

  const unique = new Map(open.map((failure) => [failure.id, failure]));
  return [...unique.values()].slice(0, 6);
}

export function useDashboardData() {
  const statsQuery = useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: dashboardApi.stats,
  });

  const equipmentQuery = useQuery({
    queryKey: ['dashboard', 'equipment-count'],
    queryFn: () => equipmentApi.list({ page: 0, size: 1 }),
    select: (response) => response.totalElements,
  });

  const recentFailuresQuery = useQuery({
    queryKey: ['dashboard', 'recent-failures'],
    queryFn: () => failuresApi.list({ page: 0, size: 8 }),
    select: (response) => response.content,
  });

  const criticalFailuresQuery = useQuery({
    queryKey: ['dashboard', 'critical-failures'],
    queryFn: fetchCriticalFailures,
  });

  const isLoading =
    statsQuery.isLoading ||
    equipmentQuery.isLoading ||
    recentFailuresQuery.isLoading ||
    criticalFailuresQuery.isLoading;

  const isError =
    statsQuery.isError ||
    equipmentQuery.isError ||
    recentFailuresQuery.isError ||
    criticalFailuresQuery.isError;

  const viewModel: DashboardViewModel | null =
    statsQuery.data &&
    equipmentQuery.data != null &&
    recentFailuresQuery.data &&
    criticalFailuresQuery.data
      ? {
          stats: statsQuery.data,
          equipmentCount: equipmentQuery.data,
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
      equipmentQuery.refetch();
      recentFailuresQuery.refetch();
      criticalFailuresQuery.refetch();
    },
  };
}
