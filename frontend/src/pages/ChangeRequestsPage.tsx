import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listChangeRequests, type ListParams } from '../api/changeRequests';
import type { ChangeRequestSummary, PageMeta, Environment, ChangeRequestStatus, RiskLevel } from '../types/api';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../lib/errorMessages';
import ChangeRequestTable from '../components/ChangeRequestTable';
import Pager from '../components/Pager';

const ENVS: Environment[] = ['development', 'staging', 'production'];
const STATUSES: ChangeRequestStatus[] = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'RETURNED', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'ROLLED_BACK', 'CANCELLED'];
const RISKS: RiskLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

type Filters = { environment?: Environment; status?: ChangeRequestStatus; risk?: RiskLevel };

export default function ChangeRequestsPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<ChangeRequestSummary[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<Filters>({});
  const [page, setPage] = useState(0);
  const size = 20;

  useEffect(() => {
    let active = true;
    setLoading(true);
    const params: ListParams = { ...filters, page, size, sort: 'createdAt,desc' };
    listChangeRequests(params)
      .then((res) => { if (active) { setRows(res.content); setMeta(res.meta); setError(null); } })
      .catch((e) => { if (active) setError(getErrorMessage(e, '一覧の取得に失敗しました')); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [filters, page]);

  const onFilter = (patch: Filters) => { setPage(0); setFilters((f) => ({ ...f, ...patch })); };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">変更申請一覧</h1>
        {user?.role === 'REQUESTER' && (
          <Link to="/change-requests/new" className="rounded bg-blue-600 px-4 py-2 text-sm font-bold text-white hover:bg-blue-700">新規作成</Link>
        )}
      </div>
      <div className="mb-4 flex flex-wrap gap-3">
        <Select label="環境" value={filters.environment ?? ''} options={ENVS} onChange={(v) => onFilter({ environment: (v || undefined) as Environment })} />
        <Select label="状態" value={filters.status ?? ''} options={STATUSES} onChange={(v) => onFilter({ status: (v || undefined) as ChangeRequestStatus })} />
        <Select label="リスク" value={filters.risk ?? ''} options={RISKS} onChange={(v) => onFilter({ risk: (v || undefined) as RiskLevel })} />
      </div>
      {error && <div role="alert" className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      {loading ? (
        <div className="text-gray-500">読み込み中…</div>
      ) : (
        <ChangeRequestTable rows={rows} />
      )}
      {meta && <Pager meta={meta} page={page} onChange={setPage} />}
    </div>
  );
}

function Select({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (v: string) => void }) {
  return (
    <label className="text-sm">
      <span className="mr-1 text-gray-600">{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)} className="rounded border border-gray-300 px-2 py-1">
        <option value="">すべて</option>
        {options.map((o) => <option key={o} value={o}>{o}</option>)}
      </select>
    </label>
  );
}
