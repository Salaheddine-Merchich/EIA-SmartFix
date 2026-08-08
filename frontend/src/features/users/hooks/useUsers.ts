import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '@/shared/api';
import type { User } from '@/shared/types';

export function useUsers() {
  const queryClient = useQueryClient();

  const listQuery = useQuery({
    queryKey: ['users', 'list'],
    queryFn: () => usersApi.list(),
  });

  const createUser = useMutation({
    mutationFn: (data: Partial<User> & { password: string }) => usersApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users', 'list'] });
      void queryClient.invalidateQueries({ queryKey: ['users', 'assignable'] });
    },
  });

  const updateUser = useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Partial<User> & { password?: string };
    }) => usersApi.update(id, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users', 'list'] });
      void queryClient.invalidateQueries({ queryKey: ['users', 'assignable'] });
    },
  });

  const deleteUser = useMutation({
    mutationFn: (id: string) => usersApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users', 'list'] });
      void queryClient.invalidateQueries({ queryKey: ['users', 'assignable'] });
    },
  });

  return {
    users: listQuery.data ?? [],
    isLoading: listQuery.isLoading,
    isError: listQuery.isError,
    refetch: listQuery.refetch,
    createUser,
    updateUser,
    deleteUser,
  };
}
