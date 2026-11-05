import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { authApi } from '../api/auth';
import { setUnauthorizedHandler } from '../api/client';
import type { AuthResponse, User } from '../api/types';
import { isExpired, tokenStorage } from './tokenStorage';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  /** True when the last session ended because the token expired or was rejected. */
  sessionExpired: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

function initialAuth(): AuthResponse | null {
  const stored = tokenStorage.load();
  if (stored && isExpired(stored)) {
    tokenStorage.clear();
    return null;
  }
  return stored;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [auth, setAuth] = useState<AuthResponse | null>(initialAuth);
  const [sessionExpired, setSessionExpired] = useState(false);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setAuth(null);
    queryClient.clear();
  }, [queryClient]);

  const expire = useCallback(() => {
    setSessionExpired(true);
    logout();
  }, [logout]);

  const login = useCallback(async (email: string, password: string) => {
    const result = await authApi.login(email, password);
    tokenStorage.save(result);
    setSessionExpired(false);
    setAuth(result);
  }, []);

  // Any 401 from the API (expired or invalid token) ends the session.
  useEffect(() => setUnauthorizedHandler(expire), [expire]);

  // Proactively log out the moment the token expires, even if the user is idle.
  useEffect(() => {
    if (!auth) return;
    const msLeft = new Date(auth.expiresAt).getTime() - Date.now();
    const timer = window.setTimeout(expire, Math.max(msLeft, 0));
    return () => window.clearTimeout(timer);
  }, [auth, expire]);

  const value = useMemo<AuthState>(
    () => ({ user: auth?.user ?? null, isAuthenticated: !!auth, sessionExpired, login, logout }),
    [auth, sessionExpired, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
