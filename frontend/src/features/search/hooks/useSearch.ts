import { useMutation } from '@tanstack/react-query';
import { searchApi, type SearchResponse } from '@/shared/api';

export interface SearchParams {
  q?: string;
  symptom?: string;
  faultCode?: string;
  equipmentId?: string;
  page?: number;
  size?: number;
}

export function useSearch() {
  return useMutation<SearchResponse, Error, SearchParams>({
    mutationKey: ['search'],
    mutationFn: (params) => {
      const query: Record<string, string | number> = {
        q: params.q ?? '',
        symptom: params.symptom ?? '',
        page: params.page ?? 0,
        size: params.size ?? 50,
      };
      if (params.faultCode) query.faultCode = params.faultCode;
      if (params.equipmentId) query.equipmentId = params.equipmentId;
      return searchApi.search(query);
    },
  });
}
