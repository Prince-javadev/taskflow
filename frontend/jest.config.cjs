/** Jest runs CommonJS; Vite-only code (import.meta.env) is isolated in src/appConfig.ts and mocked here. */
module.exports = {
  testEnvironment: 'jsdom',
  transform: { '^.+\\.tsx?$': ['ts-jest', { tsconfig: 'tsconfig.jest.json' }] },
  moduleNameMapper: {
    '\\.(css|less|svg|png)$': 'identity-obj-proxy',
    '^(\\.{1,2}/)+appConfig$': '<rootDir>/src/__mocks__/appConfig.ts',
  },
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
};
