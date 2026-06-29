import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatusBadge from './StatusBadge';

describe('StatusBadge', () => {
  it('DRAFT は下書きを表示する', () => {
    render(<StatusBadge status="DRAFT" />);
    expect(screen.getByText('下書き')).toBeInTheDocument();
  });

  it('UNDER_REVIEW は査閲中を表示する', () => {
    render(<StatusBadge status="UNDER_REVIEW" />);
    expect(screen.getByText('査閲中')).toBeInTheDocument();
  });
});
