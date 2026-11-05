import { http } from './client';
import type { User } from './types';

export const usersApi = {
  list: () => http.get<User[]>('/users').then((r) => r.data),
};
