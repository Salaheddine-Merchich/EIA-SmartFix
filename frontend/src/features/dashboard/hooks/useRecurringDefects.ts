import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { analyticsApi } from '@/shared/api';
import type { RecurringDefectsAnalysis } from '@/shared/types';

const RECURRING_DEFECTS_KEY = ['analytics', 'recurring-defects'] as const;

export function useRecurringDefects(limit = 8) {
  const queryClient = useQueryClient();

  const defectsQuery = useQuery({
    queryKey: [...RECURRING_DEFECTS_KEY, limit],
    queryFn: () => analyticsApi.recurringDefects(limit),
  });

  const analyzeMutation = useMutation({
    mutationFn: () => analyticsApi.analyzeRecurringDefects(limit),
    onSuccess: (result) => {
      queryClient.setQueryData([...RECURRING_DEFECTS_KEY, limit], {
        defects: result.defects,
        totalRecurringCodes: result.defects.length,
      });
    },
  });

  const baseAnalysis: RecurringDefectsAnalysis | null =
    defectsQuery.data && defectsQuery.data.defects.length > 0
      ? {
          defects: defectsQuery.data.defects,
          analysis: '',
          recommendations: '',
          disclaimer: '',
        }
      : null;

  const analysis: RecurringDefectsAnalysis | null = analyzeMutation.data ?? baseAnalysis;

  return {
    analysis,
    isLoading: defectsQuery.isLoading,
    isError: defectsQuery.isError && !analysis,
    refetch: defectsQuery.refetch,
    runAnalysis: analyzeMutation.mutateAsync,
    isAnalyzing: analyzeMutation.isPending,
  };
}
