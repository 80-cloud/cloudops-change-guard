import { Component, type ReactNode } from 'react';

interface Props { children: ReactNode; }
interface State { hasError: boolean; }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="p-8 text-gray-600">
          <h1 className="text-lg font-bold">問題が発生しました</h1>
          <p className="mt-2 text-sm">ページを再読み込みしてください。</p>
        </div>
      );
    }
    return this.props.children;
  }
}
