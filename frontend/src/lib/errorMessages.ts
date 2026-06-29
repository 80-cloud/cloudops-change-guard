import { AxiosError } from 'axios';

const STATUS_MESSAGES: Record<number, string> = {
  400: 'リクエストの内容に問題があります。入力をご確認ください',
  401: 'ログインが必要です。もう一度ログインしてください',
  403: 'この操作を行う権限がありません',
  404: '見つかりませんでした',
  409: '状態が変化しています。最新の状態を読み込み直してやり直してください',
  422: '入力内容に誤りがあります。各項目をご確認ください',
  429: '短時間に何度もアクセスがありました。少し待ってからお試しください',
  500: 'サーバーで問題が発生しました。少し待ってからもう一度お試しください',
  502: 'サーバーに繋がりませんでした。少し待ってからお試しください',
  503: 'サーバーが混み合っています。少し待ってからお試しください',
};

const NETWORK_MESSAGE = 'サーバーに接続できません。バックエンドが起動しているか確認してください';
const FALLBACK_MESSAGE = 'うまく処理できませんでした。少し待ってからもう一度お試しください';

export function getErrorMessage(error: unknown, fallback: string = FALLBACK_MESSAGE): string {
  if (error instanceof AxiosError) {
    if (error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED') return NETWORK_MESSAGE;
    const data = error.response?.data as { error?: { message?: string }; message?: string } | undefined;
    const backendMessage = data?.error?.message ?? data?.message;
    if (typeof backendMessage === 'string' && backendMessage.trim()) return backendMessage.trim();
    const status = error.response?.status;
    if (status && STATUS_MESSAGES[status]) return STATUS_MESSAGES[status];
  }
  return fallback;
}

export { STATUS_MESSAGES, NETWORK_MESSAGE, FALLBACK_MESSAGE };
