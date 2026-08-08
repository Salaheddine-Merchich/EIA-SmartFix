import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { equipmentApi } from '@/shared/api';
import type { Equipment } from '@/shared/types';

export interface EquipmentListParams {
  search?: string;
  page?: number;
  size?: number;
}

export function useEquipmentList({ search = '', page = 0, size = 50 }: EquipmentListParams = {}) {
  const queryClient = useQueryClient();

  const listQuery = useQuery({
    queryKey: ['equipment', 'list', { search, page, size }],
    queryFn: () => equipmentApi.list({ search, page, size }),
  });

  const createEquipment = useMutation({
    mutationFn: (data: Partial<Equipment>) => equipmentApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['equipment', 'list'] });
    },
  });

  const updateEquipment = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Equipment> }) =>
      equipmentApi.update(id, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['equipment', 'list'] });
    },
  });

  const deleteEquipment = useMutation({
    mutationFn: (id: string) => equipmentApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['equipment', 'list'] });
    },
  });

  return {
    equipment: listQuery.data?.content ?? [],
    isLoading: listQuery.isLoading,
    isError: listQuery.isError,
    refetch: listQuery.refetch,
    createEquipment,
    updateEquipment,
    deleteEquipment,
  };
}
