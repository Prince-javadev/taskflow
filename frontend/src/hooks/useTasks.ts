import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tasksApi } from '../api/tasks';
import { usersApi } from '../api/users';
import type { TaskInput, TaskQuery, TaskStatus } from '../api/types';
import { projectKeys } from './useProjects';

export const taskKeys = {
  list: (projectId: number, query: TaskQuery) => ['projects', projectId, 'tasks', query] as const,
  all: (projectId: number) => ['projects', projectId, 'tasks'] as const,
};

export const useTasks = (projectId: number, query: TaskQuery) =>
  useQuery({
    queryKey: taskKeys.list(projectId, query),
    queryFn: () => tasksApi.list(projectId, query),
    placeholderData: (previous) => previous,
  });

export const useUsers = () => useQuery({ queryKey: ['users'], queryFn: usersApi.list, staleTime: 60_000 });

/** After any task change, refresh this project's task lists and summary bar. */
function useInvalidateProject(projectId: number) {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: taskKeys.all(projectId) }),
      queryClient.invalidateQueries({ queryKey: projectKeys.summary(projectId) }),
    ]);
}

export function useSaveTask(projectId: number) {
  const invalidate = useInvalidateProject(projectId);
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: TaskInput }) =>
      id ? tasksApi.update(id, input) : tasksApi.create(projectId, input),
    onSuccess: invalidate,
  });
}

export function useUpdateTaskStatus(projectId: number) {
  const invalidate = useInvalidateProject(projectId);
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: TaskStatus }) => tasksApi.updateStatus(id, status),
    onSuccess: invalidate,
  });
}

export function useDeleteTask(projectId: number) {
  const invalidate = useInvalidateProject(projectId);
  return useMutation({ mutationFn: (id: number) => tasksApi.remove(id), onSuccess: invalidate });
}
