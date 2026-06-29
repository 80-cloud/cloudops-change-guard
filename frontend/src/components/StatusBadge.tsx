import type { ChangeRequestStatus } from '../types/api';

const M: Record<ChangeRequestStatus, { label: string; cls: string }> = {
  DRAFT: { label: '下書き', cls: 'bg-gray-100 text-gray-700 border-gray-300' },
  SUBMITTED: { label: '申請済', cls: 'bg-blue-100 text-blue-800 border-blue-300' },
  UNDER_REVIEW: { label: '査閲中', cls: 'bg-indigo-100 text-indigo-800 border-indigo-300' },
  APPROVED: { label: '承認済', cls: 'bg-teal-100 text-teal-800 border-teal-300' },
  REJECTED: { label: '却下', cls: 'bg-rose-100 text-rose-800 border-rose-300' },
  RETURNED: { label: '差戻し', cls: 'bg-amber-100 text-amber-800 border-amber-300' },
  SCHEDULED: { label: '実施予定', cls: 'bg-sky-100 text-sky-800 border-sky-300' },
  IN_PROGRESS: { label: '実施中', cls: 'bg-violet-100 text-violet-800 border-violet-300' },
  COMPLETED: { label: '完了', cls: 'bg-green-100 text-green-800 border-green-300' },
  FAILED: { label: '失敗', cls: 'bg-red-100 text-red-800 border-red-400' },
  ROLLED_BACK: { label: '切戻し済', cls: 'bg-orange-100 text-orange-800 border-orange-300' },
  CANCELLED: { label: '取消', cls: 'bg-gray-100 text-gray-500 border-gray-300' },
};

export default function StatusBadge({ status }: { status: ChangeRequestStatus }) {
  const s = M[status];
  return <span className={`inline-flex items-center rounded border px-2 py-0.5 text-xs font-bold ${s.cls}`}>{s.label}</span>;
}
