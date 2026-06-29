import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Pager from './Pager';
import type { PageMeta } from '../types/api';

const meta = (over: Partial<PageMeta> = {}): PageMeta => ({
  page: 0, size: 20, totalElements: 100, totalPages: 5, ...over,
});

describe('Pager', () => {
  it('総ページが1以下なら何も描画しない', () => {
    const { container } = render(<Pager meta={meta({ totalPages: 1 })} page={0} onChange={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('先頭ページでは「前へ」が無効・「次へ」が有効', () => {
    render(<Pager meta={meta()} page={0} onChange={() => {}} />);
    expect(screen.getByRole('button', { name: '前へ' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '次へ' })).toBeEnabled();
  });

  it('「次へ」で次ページ番号を通知する', () => {
    const onChange = vi.fn();
    render(<Pager meta={meta()} page={0} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: '次へ' }));
    expect(onChange).toHaveBeenCalledWith(1);
  });
});
