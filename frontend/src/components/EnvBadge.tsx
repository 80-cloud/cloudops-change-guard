import type { Environment } from '../types/api';

const STYLES: Record<Environment, { label: string; cls: string }> = {
  development: { label: '開発', cls: 'bg-slate-100 text-slate-700 border-slate-300' },
  staging: { label: '検証', cls: 'bg-sky-100 text-sky-800 border-sky-300' },
  production: { label: '本番', cls: 'bg-red-50 text-red-800 border-red-400 ring-1 ring-red-400' },
};

export default function EnvBadge({ env }: { env: Environment }) {
  const s = STYLES[env];
  return <span className={`inline-flex items-center rounded border px-2 py-0.5 text-xs font-bold ${s.cls}`}>{s.label}</span>;
}
