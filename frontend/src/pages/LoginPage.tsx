import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../lib/errorMessages';
import { DEMO_ACCOUNTS, DEMO_LOGINS_ENABLED } from '../lib/demoAccounts';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'ユーザー名またはパスワードが正しくありません'));
    } finally {
      setSubmitting(false);
    }
  };

  const onQuickLogin = async (username: string, password: string) => {
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'クイックログインに失敗しました'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <form onSubmit={onSubmit} className="w-full max-w-sm space-y-4 rounded-2xl bg-white p-8 shadow">
        <h1 className="text-xl font-bold text-gray-800">CloudOps Change Guard</h1>
        <p className="text-sm text-gray-500">ログインしてください</p>
        {error && <div role="alert" className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
        <label className="block text-sm">
          <span className="text-gray-600">ユーザー名</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm" />
        </label>
        <label className="block text-sm">
          <span className="text-gray-600">パスワード</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required
            className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm" />
        </label>
        <button type="submit" disabled={submitting}
          className="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-bold text-white transition hover:bg-blue-700 disabled:opacity-50">
          {submitting ? 'ログイン中…' : 'ログイン'}
        </button>
        {DEMO_LOGINS_ENABLED && (
          <div className="space-y-2 border-t border-gray-200 pt-4">
            <p className="text-xs text-gray-500">デモ用クイックログイン</p>
            <div className="grid grid-cols-2 gap-2">
              {DEMO_ACCOUNTS.map((acc) => (
                <button
                  key={acc.username}
                  type="button"
                  disabled={submitting}
                  onClick={() => onQuickLogin(acc.username, acc.password)}
                  className="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-50 disabled:opacity-50"
                >
                  {acc.label}で入る
                </button>
              ))}
            </div>
          </div>
        )}
      </form>
    </div>
  );
}
