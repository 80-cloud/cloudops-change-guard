import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LoginPage from './LoginPage';

const { navigateMock, loginMock } = vi.hoisted(() => ({
  navigateMock: vi.fn(),
  loginMock: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ login: loginMock }),
}));

describe('LoginPage デモクイックログイン', () => {
  beforeEach(() => {
    navigateMock.mockReset();
    loginMock.mockReset();
    loginMock.mockResolvedValue({});
  });

  it('デモボタンを1つだけ表示する', () => {
    render(<LoginPage />);
    expect(screen.getByRole('button', { name: 'デモを試す（管理者）' })).toBeInTheDocument();
  });

  it('デモボタンで admin 資格の login を呼び / に遷移する', async () => {
    render(<LoginPage />);
    fireEvent.click(screen.getByRole('button', { name: 'デモを試す（管理者）' }));
    expect(loginMock).toHaveBeenCalledWith('admin', 'ChangeMe!2026');
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/', { replace: true }));
  });
});
