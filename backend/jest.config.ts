import type { Config } from 'jest';

const config: Config = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testMatch: ['**/tests/**/*.test.ts'],
  moduleFileExtensions: ['ts', 'js', 'json', 'node'],
  transform: {
    '^.+\\.(ts|tsx)$': [
      'ts-jest',
      {
        tsconfig: 'tsconfig.json',
        diagnostics: {
          // ignore the hybrid module kind diagnostic or set isolatedModules in tsconfig
          ignoreCodes: [151002],
        },
        // enable isolatedModules for better compatibility with NodeNext modules
        isolatedModules: true,
      },
    ],
  },
  collectCoverage: true,
  collectCoverageFrom: ['src/**/*.{ts,js}'],
  coverageDirectory: '<rootDir>/coverage',
  testTimeout: 30000, 
};

export default config;