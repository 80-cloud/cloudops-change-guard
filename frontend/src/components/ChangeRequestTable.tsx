import { Link } from 'react-router-dom';
import type { ChangeRequestSummary } from '../types/api';
import { formatDateTime } from '../lib/dateUtils';
import RiskBadge from './RiskBadge';
import EnvBadge from './EnvBadge';
import StatusBadge from './StatusBadge';

export default function ChangeRequestTable({
  rows,
  emptyMessage = '該当する変更申請はありません。',
}: {
  rows: ChangeRequestSummary[];
  emptyMessage?: string;
}) {
  if (rows.length === 0) return <div className="text-gray-500">{emptyMessage}</div>;
  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead className="bg-gray-50 text-gray-600">
          <tr>
            <th className="px-3 py-2">タイトル</th>
            <th className="px-3 py-2">環境</th>
            <th className="px-3 py-2">リスク</th>
            <th className="px-3 py-2">状態</th>
            <th className="px-3 py-2">実施予定</th>
            <th className="px-3 py-2">作成</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id} className={r.targetEnvironment === 'production' ? 'border-t border-red-200 bg-red-50' : 'border-t border-gray-100'}>
              <td className="px-3 py-2">
                {r.targetEnvironment === 'production' && <span className="mr-1 rounded bg-red-600 px-1.5 py-0.5 text-xs font-bold text-white">本番</span>}
                <Link to={`/change-requests/${r.id}`} className="text-blue-600 hover:underline">{r.title}</Link>
              </td>
              <td className="px-3 py-2"><EnvBadge env={r.targetEnvironment} /></td>
              <td className="px-3 py-2"><RiskBadge level={r.riskLevel} /></td>
              <td className="px-3 py-2"><StatusBadge status={r.status} /></td>
              <td className="px-3 py-2 text-gray-600">{formatDateTime(r.scheduledAt)}</td>
              <td className="px-3 py-2 text-gray-600">{formatDateTime(r.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
