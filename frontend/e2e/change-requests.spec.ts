import { test, expect } from '@playwright/test';
import { login, USERS } from './helpers';

test.describe('一覧と承認待ち（読み取り専用・デモseed前提）', () => {
  test('変更申請一覧が表示され、状態フィルタが API クエリに反映される', async ({ page }) => {
    await login(page, USERS.requester);
    await page.getByRole('link', { name: '変更申請' }).click();
    await expect(page.getByRole('heading', { name: '変更申請一覧' })).toBeVisible();
    // seed の id1-4 はすべて req1 所有 → 最低1データ行が出る
    await expect(page.getByRole('row').nth(1)).toBeVisible();

    // フィルタ→API クエリの配線（ブラウザURLには出ないため waitForRequest で確認）
    const req = page.waitForRequest(
      (r) => r.url().includes('/change-requests') && r.url().includes('status=UNDER_REVIEW'),
    );
    await page.getByLabel('状態').selectOption('UNDER_REVIEW');
    await req;
  });

  test('rev1 の承認待ちに査閲中の申請が表示される', async ({ page }) => {
    await login(page, USERS.reviewer1);
    await page.getByRole('link', { name: '承認待ち' }).click();
    await expect(page.getByRole('heading', { name: '承認待ち' })).toBeVisible();
    // seed の id2(staging/MEDIUM)・id4(dev/CRITICAL) が UNDER_REVIEW・未投票 → 表示
    await expect(page.getByText('承認待ちはありません。')).toHaveCount(0);
    await expect(page.getByRole('row').nth(1)).toBeVisible();
  });
});
