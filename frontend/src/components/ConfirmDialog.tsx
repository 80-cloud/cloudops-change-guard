import type { ReactNode } from 'react';

export default function ConfirmDialog({
  title, message, confirmLabel = 'OK', danger, busy, error, onConfirm, onCancel, children,
}: {
  title: string;
  message?: string;
  confirmLabel?: string;
  danger?: boolean;
  busy?: boolean;
  error?: string | null;
  onConfirm: () => void;
  onCancel: () => void;
  children?: ReactNode;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-md rounded-lg bg-white p-5 shadow-xl">
        <h2 className="text-lg font-bold text-gray-800">{title}</h2>
        {message && <p className="mt-2 text-sm text-gray-600">{message}</p>}
        {children && <div className="mt-3">{children}</div>}
        {error && <div role="alert" className="mt-3 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
        <div className="mt-4 flex justify-end gap-2">
          <button type="button" disabled={busy} onClick={onCancel} className="rounded px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-40">キャンセル</button>
          <button type="button" disabled={busy} onClick={onConfirm} className={`rounded px-4 py-2 text-sm font-bold text-white disabled:opacity-40 ${danger ? 'bg-red-600 hover:bg-red-700' : 'bg-blue-600 hover:bg-blue-700'}`}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
