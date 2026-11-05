/** The only module that reads Vite env variables (Jest swaps it for src/__mocks__/appConfig.ts). */
export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
};
