import { useQuery } from '@tanstack/react-query';
import { equipmentApi } from '@/shared/api';

export function useEquipmentHistory(equipmentId: string | null, enabled = true) {
  return useQuery({
    queryKey: ['equipment', equipmentId, 'history'],
    queryFn: () => equipmentApi.history(equipmentId!),
    enabled: Boolean(equipmentId) && enabled,
  });
}
