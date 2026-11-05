import type { AuthResponse } from '../api/types';

const KEY = 'taskflow.auth';

export const tokenStorage = {
  load(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(KEY);
      return raw ? (JSON.parse(raw) as AuthResponse) : null;
    } catch {
      return null;
    }
  },
  save(auth: AuthResponse) {
    try {
      localStorage.setItem(KEY, JSON.stringify(auth));
    } catch {
      /* storage unavailable: session stays in memory only */
    }
  },
  clear() {
    try {
      localStorage.removeItem(KEY);
    } catch {
      /* ignore */
    }
  },
};

export function isExpired(auth: AuthResponse, now: number = Date.now()): boolean {
  return new Date(auth.expiresAt).getTime() <= now;
}
