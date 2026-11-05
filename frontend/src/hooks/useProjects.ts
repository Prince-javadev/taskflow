import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { projectsApi } from '../api/projects';

export const projectKeys = {
  all: ['projects'] as const,
  detail: (id: number) => ['projects', id] as const,
  summary: (id: number) => ['projects', id, 'summary'] as const,
};

export const useProjects = () => useQuery({ queryKey: projectKeys.all, queryFn: projectsApi.list });

export const useProject = (id: number) =>
  useQuery({ queryKey: projectKeys.detail(id), queryFn: () => projectsApi.get(id) });

export const useProjectSummary = (id: number) =>
  useQuery({ queryKey: projectKeys.summary(id), queryFn: () => projectsApi.summary(id) });

export function useCreateProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: projectsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectKeys.all }),
  });
}
