import type { RiskLevel } from '../types/api';

const STYLES: Record<RiskLevel, { label: string; icon: string; cls: string }> = {
  LOW: { label: '低', icon: '●', cls: 'bg-green-100 text-green-800 border-green-300' },
  MEDIUM: { label: '中', icon: '◆', cls: 'bg-yellow-100 text-yellow-800 border-yellow-300' },
  HIGH: { label: '高', icon: '▲', cls: 'bg-orange-100 text-orange-800 border-orange-300' },
  CRITICAL: { label: '重大', icon: '■', cls: 'bg-red-100 text-red-800 border-red-400' },
};

export default function RiskBadge({ level }: { level: RiskLevel | null }) {
  if (!level) {
    return <span className="inline-flex items-center gap-1 rounded border border-gray-300 bg-gray-100 px-2 py-0.5 text-xs text-gray-600">未判定</span>;
  }
  const s = STYLES[level];
  return (
    <span className={`inline-flex items-center gap-1 rounded border px-2 py-0.5 text-xs font-bold ${s.cls}`} title={`リスク: ${s.label}`}>
      <span aria-hidden="true">{s.icon}</span>
      <span>リスク{s.label}</span>
    </span>
  );
}
