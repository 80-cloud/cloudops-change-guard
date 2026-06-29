import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const linkCls = ({ isActive }: { isActive: boolean }) =>
  `rounded px-3 py-1.5 text-sm ${isActive ? 'bg-blue-600 text-white' : 'text-gray-700 hover:bg-gray-100'}`;

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const onLogout = async () => { await logout(); navigate('/login', { replace: true }); };

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-3">
          <span className="font-bold text-gray-800">CloudOps Change Guard</span>
          <nav className="flex gap-1">
            <NavLink to="/" end className={linkCls}>ダッシュボード</NavLink>
            <NavLink to="/change-requests" className={linkCls}>変更申請</NavLink>
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm">
          <span className="text-gray-600">{user?.displayName}（{user?.role}）</span>
          <button onClick={onLogout} className="rounded border border-gray-300 px-3 py-1 hover:bg-gray-100">ログアウト</button>
        </div>
      </div>
    </header>
  );
}
