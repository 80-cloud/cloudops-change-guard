import { defineConfig, devices } from '@playwright/test';

// 前提：3サービス起動済み（DB:5436 / backend:8084 / frontend:5177）。
// dev DB は空起動時にデモ用データ(申請4件)が入る決定的状態を使う（e2e/README.md）。
// CI 常設は backend+DB が要るため当面ローカル限定。
export default defineConfig({
  testDir: './e2e',
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5177',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
