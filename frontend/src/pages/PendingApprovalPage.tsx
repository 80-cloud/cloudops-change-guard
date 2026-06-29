import { useEffect, useState } from 'react';
import { getPendingApproval } from '../api/changeRequests';
import type { ChangeRequestSummary, PageMeta } from '../types/api';
import { getErrorMessage } from '../lib/errorMessages';
import ChangeRequestTable from '../components/ChangeRequestTable';
import Pager from '../components/Pager';

export default function PendingApprovalPage() {
  const [rows, setRows] = useState<ChangeRequestSummary[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const size = 20;

  useEffect(() => {
    let active = true;
    setLoading(true);
    getPendingApproval({ page, size, sort: 'createdAt,desc' })
      .then((res) => { if (active) { setRows(res.content); setMeta(res.meta); setError(null); } })
      .catch((e) => { if (active) setError(getErrorMessage(e, '承認待ちの取得に失敗しました')); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page]);

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-800">承認待ち</h1>
      <p className="mb-4 text-sm text-gray-500">あなたがまだ判断していない、査閲中の変更申請です。</p>
      {error && <div role="alert" className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      {loading ? (
        <div className="text-gray-500">読み込み中…</div>
      ) : (
        <ChangeRequestTable rows={rows} emptyMessage="承認待ちはありません。" />
      )}
      {meta && <Pager meta={meta} page={page} onChange={setPage} />}
    </div>
  );
}
