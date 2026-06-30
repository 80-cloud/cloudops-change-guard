// デモ用クイックログイン設定。VITE_DEMO_LOGINS=false のときだけ無効化（既定 on）。
// dev で frontend/.env が無いと import.meta.env.VITE_DEMO_LOGINS は undefined ＝ on のまま。
export const DEMO_LOGINS_ENABLED = import.meta.env.VITE_DEMO_LOGINS !== 'false';

// デモは全機能が見える管理者に集約（他ロールは手入力フォームから入れる）。
// 資格は seed 既定値・e2e の USERS と一致させる。
export const DEMO_ACCOUNT = { username: 'admin', password: 'ChangeMe!2026' } as const;
