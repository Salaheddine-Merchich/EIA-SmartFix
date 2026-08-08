import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { failuresApi, interventionsApi } from '@/shared/api';
import type { Failure, Intervention } from '@/shared/types';

function invalidateFailureDetail(queryClient: ReturnType<typeof useQueryClient>, id: string) {
  void queryClient.invalidateQueries({ queryKey: ['failures', id] });
  void queryClient.invalidateQueries({ queryKey: ['interventions', { failureId: id }] });
  void queryClient.invalidateQueries({ queryKey: ['failures', 'list'] });
}

export function useFailureDetail(id: string | undefined) {
  const queryClient = useQueryClient();

  const failureQuery = useQuery({
    queryKey: ['failures', id],
    queryFn: () => failuresApi.get(id!),
    enabled: Boolean(id),
    retry: (failureCount, error) => {
      if (axios.isAxiosError(error) && error.response?.status === 404) return false;
      return failureCount < 2;
    },
  });

  const interventionsQuery = useQuery({
    queryKey: ['interventions', { failureId: id }],
    queryFn: () => interventionsApi.list(id!).then((response) => response.content),
    enabled: Boolean(id),
  });

  const updateFailure = useMutation({
    mutationFn: (data: Partial<Failure>) => failuresApi.update(id!, data),
    onSuccess: () => {
      if (id) invalidateFailureDetail(queryClient, id);
    },
  });

  const deleteFailure = useMutation({
    mutationFn: () => failuresApi.delete(id!),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['failures', 'list'] });
      if (id) {
        queryClient.removeQueries({ queryKey: ['failures', id] });
        queryClient.removeQueries({ queryKey: ['interventions', { failureId: id }] });
      }
    },
  });

  const createIntervention = useMutation({
    mutationFn: (data: Partial<Intervention>) => interventionsApi.create(data),
    onSuccess: () => {
      if (id) invalidateFailureDetail(queryClient, id);
    },
  });

  const submitIntervention = useMutation({
    mutationFn: (interventionId: string) => interventionsApi.submit(interventionId),
    onSuccess: () => {
      if (id) invalidateFailureDetail(queryClient, id);
    },
  });

  const validateIntervention = useMutation({
    mutationFn: ({
      interventionId,
      approved,
      commentaire,
    }: {
      interventionId: string;
      approved: boolean;
      commentaire?: string;
    }) => interventionsApi.validate(interventionId, approved, commentaire),
    onSuccess: () => {
      if (id) invalidateFailureDetail(queryClient, id);
    },
  });

  const isNotFound =
    axios.isAxiosError(failureQuery.error) && failureQuery.error.response?.status === 404;

  const isLoading =
    Boolean(id) &&
    (failureQuery.isLoading || interventionsQuery.isLoading) &&
    !failureQuery.isError;

  const isError =
    (failureQuery.isError && !isNotFound) ||
    (interventionsQuery.isError && !failureQuery.isError);

  const refetch = () => {
    void failureQuery.refetch();
    void interventionsQuery.refetch();
  };

  return {
    failure: failureQuery.data ?? null,
    interventions: interventionsQuery.data ?? [],
    isLoading,
    isError,
    isNotFound,
    loadError: isNotFound
      ? 'Cette panne est introuvable ou a été supprimée.'
      : isError
        ? 'Impossible de charger les détails de la panne.'
        : null,
    refetch,
    updateFailure,
    deleteFailure,
    createIntervention,
    submitIntervention,
    validateIntervention,
  };
}
