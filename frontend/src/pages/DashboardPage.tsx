import { useAuth } from '../context/AuthContext';
import RiskBadge from '../components/RiskBadge';
import EnvBadge from '../components/EnvBadge';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  return (
    <div className="mx-auto max-w-3xl p-8">
      <header className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">ダッシュボード</h1>
        <div className="flex items-center gap-3 text-sm">
          <span className="text-gray-600">{user?.displayName}（{user?.role}）</span>
          <button onClick={() => logout()} className="rounded border border-gray-300 px-3 py-1 hover:bg-gray-100">ログアウト</button>
        </div>
      </header>
      <p className="text-gray-500">Phase 5 Inc 0：足場と認証フローが動作しています。各画面は順次実装します。</p>
      <div className="mt-4 flex flex-wrap gap-2">
        <EnvBadge env="production" />
        <EnvBadge env="staging" />
        <EnvBadge env="development" />
        <RiskBadge level="CRITICAL" />
        <RiskBadge level="HIGH" />
        <RiskBadge level="LOW" />
        <RiskBadge level={null} />
      </div>
    </div>
  );
}
