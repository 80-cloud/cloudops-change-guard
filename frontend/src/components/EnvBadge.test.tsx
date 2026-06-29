import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import EnvBadge from './EnvBadge';

describe('EnvBadge', () => {
  it('development を「開発」と表示する', () => {
    render(<EnvBadge env="development" />);
    expect(screen.getByText('開発')).toBeInTheDocument();
  });

  it('production は「本番」を強調表示する（ring 付き）', () => {
    render(<EnvBadge env="production" />);
    expect(screen.getByText('本番').className).toContain('ring');
  });
});
