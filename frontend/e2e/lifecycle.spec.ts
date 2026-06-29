import { test, expect, type Page } from '@playwright/test';
import { login, USERS } from './helpers';

// 操作ボタン（操作セクション）→ ConfirmDialog 確認 の2段。同名ボタンは dialog scope で分離。
async function confirmAction(page: Page, name: string) {
  await page.getByRole('button', { name, exact: true }).first().click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name, exact: true }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
}

// 各実行で一意タイトルのCRを新規作成するため再実行可能（完了CRが残るだけ）。
test('lifecycle: 申請→承認→実施→完了→監査', async ({ page }) => {
  test.setTimeout(60_000);
  const title = `E2E通し 開発タグ追加 ${Date.now()}`;

  // 1) REQUESTER が作成して提出（dev・無害＝LOW・非BLOCK）
  await login(page, USERS.requester);
  await page.goto('/change-requests/new');
  await page.getByLabel('タイトル').fill(title);
  await page.getByLabel('対象環境').selectOption('development');
  await page.getByLabel('IaC 種別').selectOption('TERRAFORM');
  await page.getByLabel('対象サービス').fill('EC2');
  await page.getByLabel('対象リソース名').fill('web-server-01');
  await page.getByLabel('変更理由').fill('命名規約に合わせて Name タグを付与する');
  await page.getByLabel('変更概要').fill('Name タグを追加する');
  await page.getByLabel('差分').fill('  ~ tags = {\n      + Name = "web-server-01"\n    }');
  await page.getByRole('button', { name: '保存して提出' }).click();
  await expect(page).toHaveURL(/\/change-requests$/);

  // 作成CRの詳細を開き id を確保
  await page.getByRole('link', { name: title }).click();
  await expect(page.getByRole('heading', { name: title })).toBeVisible();
  const id = page.url().split('/').pop()!;

  // 2) REVIEWER が査閲開始→承認（dev LOW=1名でAPPROVED）
  await login(page, USERS.reviewer1);
  await page.goto('/change-requests/' + id);
  await confirmAction(page, '査閲開始');
  await confirmAction(page, '承認');

  // 3) OPERATOR が実施予定→開始→適用成功+必須health正常→完了
  await login(page, USERS.operator);
  await page.goto('/change-requests/' + id);

  await page.getByRole('button', { name: '実施予定登録', exact: true }).first().click();
  const sched = page.getByRole('dialog');
  await sched.getByLabel('実施予定日時').fill('2026-07-10T10:00');
  await sched.getByRole('button', { name: '実施予定登録', exact: true }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);

  await confirmAction(page, '実施開始');

  await page.getByRole('button', { name: '適用成功を記録' }).click();
  for (const item of ['IAC_APPLY', 'HTTP_HEALTH', 'DB_CONNECTION']) {
    await page.getByLabel('項目').selectOption(item);
    await page.getByLabel('結果').selectOption('HEALTHY');
    await page.getByRole('button', { name: '記録', exact: true }).click();
  }

  await confirmAction(page, '完了');

  // 4) 監査に完了が記録され、状態は終端（操作ボタン消滅）
  await expect(page.getByText('EXECUTION_COMPLETE')).toBeVisible();
  await expect(page.getByRole('button', { name: '完了', exact: true })).toHaveCount(0);
});
