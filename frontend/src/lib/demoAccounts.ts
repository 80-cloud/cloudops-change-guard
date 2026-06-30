// デモ用クイックログイン設定。VITE_DEMO_LOGINS=false のときだけ無効化（既定 on）。
// dev で frontend/.env が無いと import.meta.env.VITE_DEMO_LOGINS は undefined ＝ on のまま。
export const DEMO_LOGINS_ENABLED = import.meta.env.VITE_DEMO_LOGINS !== 'false';

// 資格は e2e の helpers.ts USERS（admin/req1/rev1/op1）および seed 既定値と一致させる（単一情報源）。
export const DEMO_ACCOUNTS = [
  { label: '管理者', username: 'admin', password: 'ChangeMe!2026' },
  { label: '申請者', username: 'req1', password: 'demo' },
  { label: '査閲者', username: 'rev1', password: 'demo' },
  { label: '実施者', username: 'op1', password: 'demo' },
] as const;
