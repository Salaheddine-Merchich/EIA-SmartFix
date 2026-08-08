import { useQuery } from '@tanstack/react-query';
import { usersApi } from '@/shared/api';

export function useAssignableUsers() {
  return useQuery({
    queryKey: ['users', 'assignable'],
    queryFn: () => usersApi.assignable(),
  });
}
