import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { failuresApi } from '@/shared/api';
import type { Failure } from '@/shared/types';

export interface FailuresListParams {
  search?: string;
  page?: number;
  size?: number;
}

export function useFailuresList({ search = '', page = 0, size = 50 }: FailuresListParams = {}) {
  const queryClient = useQueryClient();

  const listQuery = useQuery({
    queryKey: ['failures', 'list', { search, page, size }],
    queryFn: () => failuresApi.list({ search, page, size }),
  });

  const createFailure = useMutation({
    mutationFn: (data: Partial<Failure>) => failuresApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['failures', 'list'] });
    },
  });

  return {
    failures: listQuery.data?.content ?? [],
    isLoading: listQuery.isLoading,
    isError: listQuery.isError,
    refetch: listQuery.refetch,
    createFailure,
  };
}
