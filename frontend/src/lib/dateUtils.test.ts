import { describe, it, expect } from 'vitest';
import { formatDateTime } from './dateUtils';

describe('formatDateTime', () => {
  it('null は — を返す', () => { expect(formatDateTime(null)).toBe('—'); });
  it('不正な文字列は — を返す', () => { expect(formatDateTime('not-a-date')).toBe('—'); });
  it('ISO文字列を整形する', () => { expect(formatDateTime('2026-06-29T00:00:00Z')).toMatch(/2026/); });
});
