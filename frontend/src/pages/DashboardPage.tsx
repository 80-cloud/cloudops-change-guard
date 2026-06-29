import { useAuth } from '../context/AuthContext';
import RiskBadge from '../components/RiskBadge';
import EnvBadge from '../components/EnvBadge';

export default function DashboardPage() {
  const { user } = useAuth();
  return (
    <div>
      <h1 className="mb-2 text-2xl font-bold text-gray-800">ダッシュボード</h1>
      <p className="text-gray-500">ようこそ、{user?.displayName} さん（{user?.role}）。集計表示は今後の increment で実装します。</p>
      <div className="mt-4 flex flex-wrap gap-2">
        <EnvBadge env="production" /><EnvBadge env="staging" /><EnvBadge env="development" />
        <RiskBadge level="CRITICAL" /><RiskBadge level="HIGH" /><RiskBadge level="LOW" /><RiskBadge level={null} />
      </div>
    </div>
  );
}
