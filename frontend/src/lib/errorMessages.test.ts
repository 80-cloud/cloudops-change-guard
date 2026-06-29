import { describe, it, expect } from 'vitest';
import { AxiosError } from 'axios';
import { getErrorMessage, NETWORK_MESSAGE } from './errorMessages';

describe('getErrorMessage', () => {
  it('ネットワークエラーは接続案内を返す', () => {
    expect(getErrorMessage(new AxiosError('Network Error', 'ERR_NETWORK'))).toBe(NETWORK_MESSAGE);
  });

  it('backend の error.message を優先する', () => {
    const e = new AxiosError('x');
    e.response = { data: { error: { message: '権限がありません' } }, status: 403 } as never;
    expect(getErrorMessage(e)).toBe('権限がありません');
  });

  it('未知のエラーは fallback を返す', () => {
    expect(getErrorMessage(null, '既定文言')).toBe('既定文言');
  });
});
