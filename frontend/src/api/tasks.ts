import { http } from './client';
import type { Page, Task, TaskInput, TaskQuery, TaskStatus } from './types';

export const tasksApi = {
  list: (projectId: number, query: TaskQuery) =>
    http
      .get<Page<Task>>(`/projects/${projectId}/tasks`, { params: { ...query, size: 100 } })
      .then((r) => r.data),
  create: (projectId: number, input: TaskInput) =>
    http.post<Task>(`/projects/${projectId}/tasks`, input).then((r) => r.data),
  update: (id: number, input: TaskInput) => http.put<Task>(`/tasks/${id}`, input).then((r) => r.data),
  updateStatus: (id: number, status: TaskStatus) =>
    http.patch<Task>(`/tasks/${id}/status`, { status }).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/tasks/${id}`).then(() => undefined),
};
