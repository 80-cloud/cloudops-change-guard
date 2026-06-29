import { useEffect, useState } from 'react';
import { getPolicies } from '../api/policies';
import type { PolicyResponse } from '../types/api';
import { getErrorMessage } from '../lib/errorMessages';

const EFFECT_LABEL: Record<string, string> = {
  BLOCK: 'ブロック',
  REQUIRE_DUAL_APPROVAL: '2名承認',
  REQUIRE_ADDITIONAL_APPROVAL: '追加承認',
  REQUIRE_REASON: '理由必須',
  REQUIRE_MAINTENANCE_WINDOW: 'メンテ枠必須',
};
const EFFECT_CLS: Record<string, string> = {
  BLOCK: 'bg-red-100 text-red-800 border-red-400',
  REQUIRE_DUAL_APPROVAL: 'bg-orange-100 text-orange-800 border-orange-300',
  REQUIRE_ADDITIONAL_APPROVAL: 'bg-amber-100 text-amber-800 border-amber-300',
  REQUIRE_REASON: 'bg-sky-100 text-sky-800 border-sky-300',
  REQUIRE_MAINTENANCE_WINDOW: 'bg-violet-100 text-violet-800 border-violet-300',
};
const EFFECT_FALLBACK_CLS = 'bg-gray-100 text-gray-700 border-gray-300';
const SCOPE_LABEL: Record<string, string> = {
  ALL: '全環境',
  production: '本番',
  staging: '検証',
  development: '開発',
};

export default function PoliciesPage() {
  const [rows, setRows] = useState<PolicyResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getPolicies()
      .then((res) => { if (active) { setRows(res); setError(null); } })
      .catch((e) => { if (active) setError(getErrorMessage(e, 'ポリシーの取得に失敗しました')); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-gray-800">ポリシー一覧</h1>
      <p className="mb-4 text-sm text-gray-500">変更申請に適用される統制ルールです（閲覧のみ）。</p>
      {error && <div role="alert" className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      {loading ? (
        <div className="text-gray-500">読み込み中…</div>
      ) : rows.length === 0 ? (
        <div className="text-gray-500">ポリシーは登録されていません。</div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-600">
              <tr>
                <th className="px-3 py-2">コード</th>
                <th className="px-3 py-2">名前</th>
                <th className="px-3 py-2">対象環境</th>
                <th className="px-3 py-2">効果</th>
                <th className="px-3 py-2">状態</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.code} className={`border-t border-gray-100 ${p.enabled ? '' : 'opacity-50'}`}>
                  <td className="px-3 py-2 font-mono text-xs text-gray-700">{p.code}</td>
                  <td className="px-3 py-2 text-gray-800">{p.name}</td>
                  <td className="px-3 py-2 text-gray-600">{SCOPE_LABEL[p.environmentScope] ?? p.environmentScope}</td>
                  <td className="px-3 py-2">
                    <span className={`inline-flex items-center rounded border px-2 py-0.5 text-xs font-bold ${EFFECT_CLS[p.effect] ?? EFFECT_FALLBACK_CLS}`}>
                      {EFFECT_LABEL[p.effect] ?? p.effect}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-gray-600">{p.enabled ? '有効' : '無効'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
