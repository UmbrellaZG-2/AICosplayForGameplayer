module.exports = {
  testEnvironment: 'jsdom',
  transform: {
    '^.+\.vue$': '@vue/vue3-jest',
    '^.+\.js$': 'babel-jest'
  },
  moduleFileExtensions: ['vue', 'js', 'json', 'jsx', 'ts', 'tsx', 'node'],
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/src/$1'
  },
  testMatch: ['**/tests/unit/**/*.(test|spec).(js|ts)'],
  collectCoverageFrom: [
    'src/**/*.{vue,js}',
    '!src/main.js',
    '!src/router/index.js',
    '!src/store/index.js'
  ],
  coverageDirectory: '<rootDir>/tests/unit/coverage',
  setupFilesAfterEnv: ['<rootDir>/tests/unit/setup.js']
};