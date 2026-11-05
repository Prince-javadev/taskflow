import { http } from './client';
import type { Project, TaskSummary } from './types';

export const projectsApi = {
  list: () => http.get<Project[]>('/projects').then((r) => r.data),
  get: (id: number) => http.get<Project>(`/projects/${id}`).then((r) => r.data),
  create: (input: { name: string; description: string | null }) =>
    http.post<Project>('/projects', input).then((r) => r.data),
  summary: (id: number) => http.get<TaskSummary>(`/projects/${id}/summary`).then((r) => r.data),
};
