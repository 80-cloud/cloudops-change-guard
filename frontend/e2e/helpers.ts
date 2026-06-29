import { type Page, expect } from '@playwright/test';

export type Cred = { username: string; password: string };

// seed 認証（SEED_ENABLED=true・user_id 1..5 固定）
export const USERS = {
  admin: { username: 'admin', password: 'ChangeMe!2026' },
  requester: { username: 'req1', password: 'demo' },
  reviewer1: { username: 'rev1', password: 'demo' },
  reviewer2: { username: 'rev2', password: 'demo' },
  operator: { username: 'op1', password: 'demo' },
} satisfies Record<string, Cred>;

// ログインフォーム(SCR-00)経由でログインし、認証後ヘッダー（ログアウト）の出現で成功を待つ。
export async function login(page: Page, cred: Cred) {
  await page.goto('/login');
  await page.getByLabel('ユーザー名').fill(cred.username);
  await page.getByLabel('パスワード').fill(cred.password);
  await page.getByRole('button', { name: 'ログイン' }).click();
  await expect(page.getByRole('button', { name: 'ログアウト' })).toBeVisible();
}
