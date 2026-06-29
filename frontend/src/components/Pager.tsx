import type { PageMeta } from '../types/api';

export default function Pager({
  meta,
  page,
  onChange,
}: {
  meta: PageMeta;
  page: number;
  onChange: (page: number) => void;
}) {
  if (meta.totalPages <= 1) return null;
  return (
    <div className="mt-4 flex items-center gap-3 text-sm">
      <button disabled={page <= 0} onClick={() => onChange(page - 1)} className="rounded border border-gray-300 px-3 py-1 disabled:opacity-40">前へ</button>
      <span className="text-gray-600">{meta.page + 1} / {meta.totalPages}（全{meta.totalElements}件）</span>
      <button disabled={page >= meta.totalPages - 1} onClick={() => onChange(page + 1)} className="rounded border border-gray-300 px-3 py-1 disabled:opacity-40">次へ</button>
    </div>
  );
}
