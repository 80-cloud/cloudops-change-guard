import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getDashboardSummary } from '../api/dashboard';
import type { DashboardSummaryResponse, ChangeRequestStatus, ChangeRequestSummary } from '../types/api';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../lib/errorMessages';
import { formatDateTime } from '../lib/dateUtils';
import StatusBadge from '../components/StatusBadge';
import ChangeRequestTable from '../components/ChangeRequestTable';

const STATUS_ORDER: ChangeRequestStatus[] = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'RETURNED', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'ROLLED_BACK', 'CANCELLED'];

export default function DashboardPage() {
  const { user } = useAuth();
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getDashboardSummary()
      .then((res) => { if (active) { setSummary(res); setError(null); } })
      .catch((e) => { if (active) setError(getErrorMessage(e, 'ダッシュボードの取得に失敗しました')); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const shownStatuses = summary ? STATUS_ORDER.filter((s) => (summary.statusCounts[s] ?? 0) > 0) : [];

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-gray-800">ダッシュボード</h1>
      <p className="mb-4 text-sm text-gray-500">ようこそ、{user?.displayName} さん（{user?.role}）。</p>
      {error && <div role="alert" className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      {loading || !summary ? (
        <div className="text-gray-500">読み込み中…</div>
      ) : (
        <div className="space-y-6">
          <section>
            <h2 className="mb-2 text-lg font-bold text-gray-700">状態別の件数</h2>
            {shownStatuses.length === 0 ? (
              <div className="text-gray-500">対象の変更申請はありません。</div>
            ) : (
              <div className="flex flex-wrap gap-3">
                {shownStatuses.map((s) => (
                  <div key={s} className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2">
                    <StatusBadge status={s} />
                    <span className="text-lg font-bold text-gray-800">{summary.statusCounts[s] ?? 0}</span>
                  </div>
                ))}
              </div>
            )}
          </section>

          <DashSection title="高リスクの申請" rows={summary.highRisk} empty="高リスクの申請はありません。" />
          <DashSection title="承認待ち" rows={summary.pendingApproval} empty="承認待ちはありません。" />
          <DashSection title="実施予定" rows={summary.scheduled} empty="実施予定はありません。" />

          {summary.recentAudit.length > 0 && (
            <section>
              <h2 className="mb-2 text-lg font-bold text-gray-700">最近の操作履歴</h2>
              <ul className="divide-y divide-gray-100 rounded-lg border border-gray-200 bg-white text-sm">
                {summary.recentAudit.map((a) => (
                  <li key={a.id} className="flex items-center justify-between px-3 py-2">
                    <span className="text-gray-700">
                      <Link to={`/change-requests/${a.changeRequestId}`} className="text-blue-600 hover:underline">#{a.changeRequestId}</Link>
                      {' '}
                      {a.summary ?? a.actionType}（ユーザー #{a.actorId}）
                    </span>
                    <span className="text-gray-500">{formatDateTime(a.createdAt)}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </div>
      )}
    </div>
  );
}

function DashSection({ title, rows, empty }: { title: string; rows: ChangeRequestSummary[]; empty: string }) {
  return (
    <section>
      <h2 className="mb-2 text-lg font-bold text-gray-700">{title}（{rows.length}）</h2>
      <ChangeRequestTable rows={rows} emptyMessage={empty} />
    </section>
  );
}
