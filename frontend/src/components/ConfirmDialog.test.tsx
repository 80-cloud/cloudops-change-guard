import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ConfirmDialog from './ConfirmDialog';

describe('ConfirmDialog', () => {
  it('確認で onConfirm、キャンセルで onCancel を発火する', () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(<ConfirmDialog title="本当に実行しますか" confirmLabel="実行" onConfirm={onConfirm} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: '実行' }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('button', { name: 'キャンセル' }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('error を渡すと alert として表示する', () => {
    render(<ConfirmDialog title="t" error="権限がありません" onConfirm={() => {}} onCancel={() => {}} />);
    expect(screen.getByRole('alert')).toHaveTextContent('権限がありません');
  });

  it('busy のときボタンを無効化する', () => {
    render(<ConfirmDialog title="t" confirmLabel="実行" busy onConfirm={() => {}} onCancel={() => {}} />);
    expect(screen.getByRole('button', { name: '実行' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'キャンセル' })).toBeDisabled();
  });
});
