import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ChangeRequestsPage from './pages/ChangeRequestsPage';
import ChangeRequestNewPage from './pages/ChangeRequestNewPage';
import ChangeRequestDetailPage from './pages/ChangeRequestDetailPage';
import PendingApprovalPage from './pages/PendingApprovalPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/change-requests" element={<ChangeRequestsPage />} />
        <Route
          path="/change-requests/new"
          element={
            <ProtectedRoute roles={['REQUESTER']}>
              <ChangeRequestNewPage />
            </ProtectedRoute>
          }
        />
        <Route path="/change-requests/:id" element={<ChangeRequestDetailPage />} />
        <Route
          path="/pending-approval"
          element={
            <ProtectedRoute roles={['REVIEWER']}>
              <PendingApprovalPage />
            </ProtectedRoute>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
