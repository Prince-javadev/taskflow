import { http } from './client';
import type { AuthResponse, User } from './types';

export const authApi = {
  login: (email: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),
  register: (name: string, email: string, password: string) =>
    http.post<User>('/auth/register', { name, email, password }).then((r) => r.data),
};
