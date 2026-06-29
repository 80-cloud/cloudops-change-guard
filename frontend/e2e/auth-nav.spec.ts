import { test, expect } from '@playwright/test';
import { login, USERS } from './helpers';

test.describe('認証とロール別ナビゲーション（読み取り専用）', () => {
  test('admin はログインでき ADMIN 専用ナビ（ポリシー）が見える', async ({ page }) => {
    await login(page, USERS.admin);
    await expect(page.getByRole('link', { name: 'ダッシュボード' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'ポリシー' })).toBeVisible();
    await expect(page.getByRole('link', { name: '承認待ち' })).toHaveCount(0);
  });

  test('rev1(REVIEWER) は承認待ちナビが見え ポリシーは無い', async ({ page }) => {
    await login(page, USERS.reviewer1);
    await expect(page.getByRole('link', { name: '承認待ち' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'ポリシー' })).toHaveCount(0);
  });

  test('req1(REQUESTER) は承認待ち/ポリシーナビが無く 一覧に新規作成導線がある', async ({ page }) => {
    await login(page, USERS.requester);
    await expect(page.getByRole('link', { name: '承認待ち' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'ポリシー' })).toHaveCount(0);
    await page.getByRole('link', { name: '変更申請' }).click();
    await expect(page.getByRole('link', { name: '新規作成' })).toBeVisible();
  });

  test('REQUESTER が ADMIN 専用 /policies へ直アクセスすると / へ弾かれる', async ({ page }) => {
    await login(page, USERS.requester);
    await page.goto('/policies');
    await expect(page).toHaveURL('http://localhost:5177/');
  });

  test('ログアウトでログイン画面へ戻る', async ({ page }) => {
    await login(page, USERS.admin);
    await page.getByRole('button', { name: 'ログアウト' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible();
  });
});
