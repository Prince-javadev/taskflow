import axios, { AxiosError } from 'axios';
import { config } from '../appConfig';
import { tokenStorage } from '../auth/tokenStorage';
import type { ApiError } from './types';

/** Single axios instance: every API module goes through here. */
export const http = axios.create({ baseURL: config.apiBaseUrl });

http.interceptors.request.use((request) => {
  const auth = tokenStorage.load();
  if (auth) request.headers.Authorization = `Bearer ${auth.token}`;
  return request;
});

let onUnauthorized: () => void = () => {};

/** AuthContext registers a callback so an expired/invalid token logs the user out everywhere. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isAuthCall = error.config?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && !isAuthCall) onUnauthorized();
    return Promise.reject(error);
  },
);

/** Human-readable message for any error thrown by the API layer. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (!error.response) return 'Cannot reach the server. Is the backend running?';
    return error.response.data?.message ?? `Request failed (${error.response.status})`;
  }
  return error instanceof Error ? error.message : 'Something went wrong';
}

/** Field-level validation errors from a 400 response, keyed by field name. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.fieldErrors) {
    return Object.fromEntries(error.response.data.fieldErrors.map((f) => [f.field, f.message]));
  }
  return {};
}
