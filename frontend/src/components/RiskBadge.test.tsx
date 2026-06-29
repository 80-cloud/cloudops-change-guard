import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import RiskBadge from './RiskBadge';

describe('RiskBadge', () => {
  it('null は未判定を表示する', () => {
    render(<RiskBadge level={null} />);
    expect(screen.getByText('未判定')).toBeInTheDocument();
  });

  it('CRITICAL はリスク重大を表示する', () => {
    render(<RiskBadge level="CRITICAL" />);
    expect(screen.getByText('リスク重大')).toBeInTheDocument();
  });
});
