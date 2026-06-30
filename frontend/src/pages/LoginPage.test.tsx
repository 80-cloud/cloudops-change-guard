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

describe('LoginPage クイックログイン', () => {
  beforeEach(() => {
    navigateMock.mockReset();
    loginMock.mockReset();
    loginMock.mockResolvedValue({});
  });

  it('4ロール分のクイックログインボタンを表示する', () => {
    render(<LoginPage />);
    for (const name of ['管理者で入る', '申請者で入る', '査閲者で入る', '実施者で入る']) {
      expect(screen.getByRole('button', { name })).toBeInTheDocument();
    }
  });

  it('「管理者で入る」で admin 資格の login を呼び / に遷移する', async () => {
    render(<LoginPage />);
    fireEvent.click(screen.getByRole('button', { name: '管理者で入る' }));
    expect(loginMock).toHaveBeenCalledWith('admin', 'ChangeMe!2026');
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/', { replace: true }));
  });
});
