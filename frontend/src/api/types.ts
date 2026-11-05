export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
export const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  DONE: 'Done',
};

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface Project {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
}

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  overdue: boolean;
  assignee: User | null;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TaskSummary {
  projectId: number;
  counts: Record<TaskStatus, number>;
  total: number;
  overdue: number;
}

export interface TaskInput {
  title: string;
  description: string | null;
  priority: TaskPriority;
  status: TaskStatus;
  dueDate: string | null;
  assigneeId: number | null;
}

export interface TaskQuery {
  priority?: TaskPriority;
  assigneeId?: number;
  sort?: 'dueDate' | 'priority' | 'createdAt';
  direction?: 'asc' | 'desc';
}

/** Error body returned by the backend's GlobalExceptionHandler. */
export interface ApiError {
  status: number;
  message: string;
  fieldErrors: { field: string; message: string }[];
}
