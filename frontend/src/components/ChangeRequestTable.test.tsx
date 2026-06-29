import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ChangeRequestTable from './ChangeRequestTable';
import type { ChangeRequestSummary } from '../types/api';

const row = (over: Partial<ChangeRequestSummary> = {}): ChangeRequestSummary => ({
  id: 1, title: 'テスト変更', targetEnvironment: 'development', iacType: 'TERRAFORM',
  status: 'DRAFT', riskLevel: 'LOW', requesterId: 1,
  scheduledAt: null, createdAt: '2026-06-29T00:00:00Z', updatedAt: '2026-06-29T00:00:00Z',
  ...over,
});

const renderTable = (rows: ChangeRequestSummary[], emptyMessage?: string) =>
  render(
    <MemoryRouter>
      <ChangeRequestTable rows={rows} emptyMessage={emptyMessage} />
    </MemoryRouter>,
  );

describe('ChangeRequestTable', () => {
  it('行が無いとき空メッセージを表示する', () => {
    renderTable([], '該当なし');
    expect(screen.getByText('該当なし')).toBeInTheDocument();
  });

  it('タイトルが詳細へのリンクになる', () => {
    renderTable([row({ id: 7, title: '配線確認' })]);
    expect(screen.getByRole('link', { name: '配線確認' })).toHaveAttribute('href', '/change-requests/7');
  });

  it('production 行は本番バッジを表示する', () => {
    renderTable([row({ targetEnvironment: 'production' })]);
    expect(screen.getAllByText('本番').length).toBeGreaterThan(0);
  });
});
